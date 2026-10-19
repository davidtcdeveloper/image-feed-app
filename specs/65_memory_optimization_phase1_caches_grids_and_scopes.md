# Specification 65: Runtime Memory Optimization Phase 1 — Image Caches, Grid Virtualization & ViewModel Scope Leaks

**Status:** Approved

## 1. Overview & Problem Statement

Runtime memory profiling across Shared KMP, Android (`androidApp`), and iOS/macOS (`iosApp`) identified four immediate, high-impact sources of heap bloat, defeated viewport recycling, and leaked coroutine scopes:

1. **Broken Lazy Grid Virtualization & Runaway Pagination on Android Profile**:
   - In [`UserProfileContent`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L265-L330), the outer container is a `Column` with `Modifier.verticalScroll(scrollState)` ([L278-L283](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L278-L283)) wrapping a `Box` constrained to `.heightIn(min = 400.dp, max = 2000.dp)` ([L299-L304](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L299-L304)).
   - Inside that box, both [`PortfolioTabContent`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L680-L760) ([L716-L726](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L716-L726)) and [`CollectionsTabContent`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L763-L855) ([L815-L826](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L815-L826)) render a nested `LazyVerticalStaggeredGrid` with `.heightIn(max = 2000.dp)` and `userScrollEnabled = false`.
   - Because the inner grid is given a 2000dp layout constraint and cannot scroll independently, Compose composes and decodes all items that fit within 2000dp at once (defeating lazy viewport recycling). Worse, `listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index` in the pagination `LaunchedEffect` ([L685-L696](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L685-L696), [L774-L785](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L774-L785)) immediately sees the end of the initial page inside the 2000dp window and triggers runaway auto-pagination without user scrolling. (Supersedes the profiling note in [Spec 62](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/62_android_scroll_chrome_and_horizontal_row_hygiene.md#L20).)

2. **Uncached Full-Resolution Image Decoding & Eager Placeholders on iOS/macOS (`KFImage.swift`)**:
   - [`KFImage`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/KFImage.swift#L3-L70) wraps SwiftUI's basic `AsyncImage(url:)` ([L36](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/KFImage.swift#L36)), which relies only on `URLCache` without an application-managed, byte-cost-bounded decoded image cache (`NSCache`) or memory-warning eviction.
   - Full-resolution network payloads are decoded into full-sized bitmap buffers rather than downsampling via `ImageIO` (`CGImageSourceCreateThumbnailAtIndex` with `kCGImageSourceCreateThumbnailFromImageAlways` and `kCGImageSourceShouldCacheImmediately`).
   - `.placeholder` ([L14-L18](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/KFImage.swift#L14-L18)) eagerly evaluates `AnyView(content())` when constructing `KFImage` on every parent body evaluation, allocating type-erased view hierarchies even when the image is already cached or loaded.

3. **Android Coil Heap Allocation, Disk Cache Policy & Missing `onTrimMemory` Hook**:
   - [`ImageFeedApplication`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/ImageFeedApplication.kt#L20-L44) allocates 25% of the application heap to Coil's `MemoryCache` ([L29](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/ImageFeedApplication.kt#L29)), uses default `ARGB_8888` (4 bytes/pixel) for all opaque JPEG photos instead of allowing `RGB_565` (2 bytes/pixel) where alpha is unnecessary, and configures a 100 MB local `DiskCache` ([L32-L41](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/ImageFeedApplication.kt#L32-L41)), which conflicts with **Core Invariant 4** in [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) ("Hotlink exact Unsplash photo URLs; never cache image files locally").
   - `ImageFeedApplication` does not override `onTrimMemory(level: Int)` or `onLowMemory()`, leaving both Coil's `MemoryCache` and [`BlurHashDecoder.cache`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/util/BlurHashDecoder.kt#L28) untrimmed when the OS signals memory pressure.

4. **Eager iOS Flow Wrappers on Android & Uncleaned `RandomPhotoViewModel` Instances**:
   - [`MviViewModel`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/core/mvi/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/MviViewModel.kt#L23-L33) eagerly instantiates `val iosState: CommonFlow<S> = CommonFlow(state)` ([L28](file:///Users/davidtiagoconceicao/Developer/image-feed-app/core/mvi/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/MviViewModel.kt#L28)) and `val iosEffects: CommonFlow<E> = CommonFlow(effects)` ([L32](file:///Users/davidtiagoconceicao/Developer/image-feed-app/core/mvi/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/MviViewModel.kt#L32)) on every ViewModel creation, allocating wrapper objects on Android even though Android observes `state` and `effects` directly.
   - In [`FeedViewModel.fetchRandomPhotoId`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/FeedViewModel.swift#L66-L80), each call creates a fresh [`RandomPhotoViewModel`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/feature/feed/src/commonMain/kotlin/com/example/imagefeed/presentation/random/RandomPhotoViewModel.kt#L15-L85) via `MetroHelper.shared.getRandomPhotoViewModel()` and watches `iosState` with a local `var closeable: Closeable?`. Because `state` is a `StateFlow` whose initial emission has `isLoading = true, photo = null, error = null`, if `FeedViewModel` is deallocated, or if `fetchRandomPhotoId` is re-invoked before completion, or if the coroutine is cancelled, the local watcher and `randomVm`'s `viewModelScope` are never closed/cleared, and `ContentView.handleShake()` ([`ContentView.swift` L514-L522](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L514-L522)) leaves `isShaking = true` forever on error because `completion` is only called on `photo != null`.

---

## 2. Technical Design

### A. Single Virtualized `LazyVerticalStaggeredGrid` in [`UserProfileScreen.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt)
1. Remove the outer `Column(Modifier.fillMaxSize().verticalScroll(scrollState))` ([L278-L283](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L278-L283)) and the `.heightIn(min = 400.dp, max = 2000.dp)` wrapper `Box` ([L299-L304](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L299-L304)) in `UserProfileContent`.
2. Restructure `UserProfileContent` to use a single, full-screen `LazyVerticalStaggeredGrid(columns = if (state.activeTab == ProfileTab.COLLECTIONS) calculateCollectionGridColumns() else calculateGridColumns(), modifier = Modifier.fillMaxSize())` (matching the pattern in [`CollectionDetailScreen.kt` L209-L283](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/CollectionDetailScreen.kt#L209-L283)):
   - Render `ProfileHeaderSection(user = user)` inside `item(span = StaggeredGridItemSpan.FullLine, key = "profile_header", contentType = "header")`.
   - Render `ProfileTabSelector(...)` inside `item(span = StaggeredGridItemSpan.FullLine, key = "profile_tabs", contentType = "tabs")`.
   - Depending on `state.activeTab`:
     - **`ProfileTab.PORTFOLIO` / `ProfileTab.LIKES`**:
       - When `photos.isEmpty() && state.isLoadingContent`, emit 12 [`PhotoCardSkeleton`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/util/AnimationUtils.kt#L168-L210) items directly into the parent `LazyVerticalStaggeredGridScope` (`items(12, key = { "photo_skeleton_$it" }, contentType = { "photo_skeleton" })`). Do **not** wrap [`PhotoGridSkeleton()`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/util/AnimationUtils.kt#L213-L232) inside a `FullLine` item, as `PhotoGridSkeleton` is itself a `LazyVerticalStaggeredGrid` and nesting it inside an unconstrained grid item throws `IllegalStateException` at runtime.
       - When `photos.isEmpty() && !state.isLoadingContent`, emit the empty-state message inside `item(span = StaggeredGridItemSpan.FullLine, key = "empty_${state.activeTab}", contentType = "empty")`.
       - Otherwise, emit photo items directly into the parent `LazyVerticalStaggeredGridScope` via `itemsIndexed(photos, key = { _, photo -> "${state.activeTab}_${photo.id}" }, contentType = { _, _ -> "photo_card" })`.
     - **`ProfileTab.COLLECTIONS`**: when `collections.isEmpty() && state.isLoadingContent`, emit 3 `CollectionMosaicCardSkeleton()` items (`key = { "col_skeleton_$it" }`); when empty and not loading, emit a `FullLine` empty item; otherwise emit `CollectionRowLayout` items directly in the parent grid (`key = { _, col -> "col_${col.id}" }`, `contentType = { _, _ -> "collection_card" }`).
     - **`ProfileTab.INSIGHTS`**: emit `InsightsTabContent` inside `item(span = StaggeredGridItemSpan.FullLine, key = "insights_content", contentType = "insights")`.
   - Remove or dismantle the legacy `PortfolioTabContent` and `CollectionsTabContent` composables ([L680-L855](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L680-L855)) entirely to eliminate dead code and duplicate nested pagination effects per AGENTS.md Core Invariant 3.
3. Consolidate pagination prefetching into a single `LaunchedEffect(gridState, state.activeTab)` on the outer `gridState`, gated by `state.activeTab != ProfileTab.INSIGHTS`, `!currentLoading`, and `!currentHasReachedEnd`, accounting for the 2 header items in `totalItemsCount` (`totalItems > 2`).

### B. Downsampling, Cost-Bounded `NSCache` & Lazy Placeholders in [`KFImage.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/KFImage.swift)
1. **Lazy Placeholder Closure**:
   - Replace `private var placeholderView: AnyView?` ([L5, L14-L18](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/KFImage.swift#L5)) with a lazy closure `private var placeholderBuilder: (() -> AnyView)?` (or generic `KFImage<Placeholder: View>`) so the placeholder view is only instantiated when the image phase is `.empty` or `.failure` (cache miss).
2. **Shared Byte-Bounded In-Memory Image Cache (`ImageMemoryCache`)**:
   - Create a thread-safe singleton `ImageMemoryCache` backed by `NSCache<NSURL, PlatformImage>` (`UIImage` on iOS, `NSImage` on macOS) with:
     - `totalCostLimit = 50 * 1024 * 1024` (50 MB) and `countLimit = 150`.
     - Per-entry cost computed from decoded pixel dimensions: `cgImage.bytesPerRow * cgImage.height` (or `width * height * 4`).
   - On iOS (`#if os(iOS)`), subscribe to `UIApplication.didReceiveMemoryWarningNotification` via `NotificationCenter.default` to call `cache.removeAllObjects()`.
3. **`ImageIO` Thumbnail Downsampling Pipeline**:
   - Add an optional `targetWidthPx: Int?` (or `maxPixelSize: Int = 1080`) parameter to `KFImage` (automatically inferring `maxPixelSize` from URL query param `w=` when present, multiplied by display scale if needed, or defaulting to a safe ceiling).
   - Fetch image `Data` via `URLSession.shared.data(from: url)` (respecting cancellation when the SwiftUI `.task(id: url)` is cancelled on cell recycling) and decode off the main actor using `CGImageSourceCreateWithData` + `CGImageSourceCreateThumbnailAtIndex` with:
     - `kCGImageSourceCreateThumbnailFromImageAlways: true`
     - `kCGImageSourceShouldCacheImmediately: true`
     - `kCGImageSourceCreateThumbnailWithTransform: true`
     - `kCGImageSourceThumbnailMaxPixelSize: maxPixelSize`
   - Store the downsampled image in `ImageMemoryCache` with its byte cost and render synchronously on cache hits without flashing the placeholder.

### C. Coil 3 Memory Tuning, Disk Cache Removal & `onTrimMemory` in [`ImageFeedApplication.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/ImageFeedApplication.kt)
1. **Reduce Heap Cap & Enable `RGB_565`**:
   - In `ImageFeedApplication.newImageLoader` ([L23-L43](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/ImageFeedApplication.kt#L23-L43)), lower `MemoryCache.Builder().maxSizePercent(context, 0.25)` to `0.15` (15% of application heap).
   - Enable `allowRgb565(true)` (via `coil3.request.allowRgb565`) on the `ImageLoader.Builder` so opaque JPEGs without alpha channels decode at 2 bytes/pixel instead of 4 bytes/pixel (50% bitmap memory reduction).
2. **Remove Persistent DiskCache (Unsplash Hotlink Compliance)**:
   - Remove the `.diskCache { ... }` block ([L32-L41](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/ImageFeedApplication.kt#L32-L41)) and unused `coil3.disk.DiskCache` / `okio.Path.Companion.toPath` imports so image files are never cached on disk, strictly honoring **Core Invariant 4** in [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md). (Supersedes the `DiskCache` clause of [Spec 50](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/50_image_pipeline_and_blurhash_caching.md#L66-L70).)
3. **Implement `onTrimMemory` & `onLowMemory`**:
   - Override `onTrimMemory(level: Int)` and `onLowMemory()` in `ImageFeedApplication` (noting that `TRIM_MEMORY_UI_HIDDEN = 20` is numerically greater than `TRIM_MEMORY_RUNNING_LOW = 10` and `TRIM_MEMORY_RUNNING_CRITICAL = 15`):
     - When `level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND || level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL || level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW` (and in `onLowMemory()`), clear Coil's `SingletonImageLoader.get(this).memoryCache?.clear()` and call `BlurHashDecoder.clearCache()`.
     - When `level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN || level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE`, trim Coil's memory cache to 50% capacity (`memoryCache?.let { it.trimToSize(it.size / 2) }`) and call `BlurHashDecoder.clearCache()`.

### D. Lazy `CommonFlow` Allocation & Deterministic `RandomPhotoViewModel` Cleanup
1. **Lazy `iosState` and `iosEffects` in [`MviViewModel.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/core/mvi/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/MviViewModel.kt#L28-L32)**:
   - Change `val iosState: CommonFlow<S> = CommonFlow(state)` ([L28](file:///Users/davidtiagoconceicao/Developer/image-feed-app/core/mvi/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/MviViewModel.kt#L28)) and `val iosEffects: CommonFlow<E> = CommonFlow(effects)` ([L32](file:///Users/davidtiagoconceicao/Developer/image-feed-app/core/mvi/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/MviViewModel.kt#L32)) to `by lazy(LazyThreadSafetyMode.PUBLICATION) { CommonFlow(...) }` so Android never instantiates `CommonFlow` wrappers.
2. **Leak-Free `fetchRandomPhotoId` in [`FeedViewModel.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/FeedViewModel.swift#L66-L86) & [`ContentView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L514-L522)**:
   - Store active random-photo resources on `FeedViewModel` as instance properties (`private var randomPhotoCloseable: Closeable?` and `private var randomPhotoViewModel: shared.RandomPhotoViewModel?`).
   - At the start of `fetchRandomPhotoId(completion: @escaping (String?) -> Void)`, cancel and clear any prior in-flight `randomPhotoCloseable?.close()` and `randomPhotoViewModel?.clear()`.
   - When `state?.photo` or `state?.error` arrives (ignoring the initial loading emission where `state?.isLoading == true`), invoke `completion(photo?.id)`, close `randomPhotoCloseable`, clear `randomPhotoViewModel`, and nil out both references.
   - Also clean up `randomPhotoCloseable?.close()` and `randomPhotoViewModel?.clear()` inside `FeedViewModel.deinit` ([L82-L86](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/FeedViewModel.swift#L82-L86)).
   - Update [`ContentView.handleShake()`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L514-L522) to accept `photoId: String?` and always reset `isShaking = false` even on failure.

---

## 3. Verification & Testing Strategy

1. **Unit & Integration Tests**:
   - Run `./gradlew allTests` to verify `MviViewModel` lazy `iosState`/`iosEffects` initialization and `RandomPhotoViewModelTest` pass.
   - Verify `BlurHashDecoderTest` passes with `BlurHashDecoder.clearCache()` invoked on memory trim.
2. **Android Memory & Layout Profiling**:
   - Open `UserProfileScreen` for a user with 100+ photos: confirm in Layout Inspector that only viewport-visible `PhotoCard` items (~6–10 cards) are composed at once, and confirm in Network Inspector that page 2 is **not** requested until scrolling near the bottom of page 1.
   - Trigger `adb shell am send-trim-memory <pkg> RUNNING_LOW` and verify Coil's `MemoryCache` and `BlurHashDecoder` evict cached bitmaps.
3. **iOS Memory Profiling**:
   - Scroll through 100+ photos in `Feed` and `Search` under Xcode Instruments (Allocations / VM Tracker): verify decoded image memory stays bounded by `NSCache.totalCostLimit` (~50 MB) and drops immediately when simulating a memory warning in Simulator (`Debug -> Simulate Memory Warning`).
4. **Dual-Platform Build & Lint**:
   - Run `./gradlew :androidApp:assembleDebug :androidApp:lintDebug ktlintCheck detekt`.
   - Run `xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`.

---

## 4. Acceptance Criteria

- [ ] `UserProfileScreen.kt` uses a single full-screen `LazyVerticalStaggeredGrid` with `StaggeredGridItemSpan.FullLine` header and tab selector items; nested `.verticalScroll` and `.heightIn(max = 2000.dp)` are completely removed.
- [ ] Opening `UserProfileScreen` loads only page 1 initially and recycles off-screen photo/collection cards normally.
- [ ] `KFImage.swift` evaluates placeholder views lazily only on cache misses, downsamples images off the main thread via `CGImageSourceCreateThumbnailAtIndex`, caches decoded images in a byte-cost-bounded `NSCache`, and evicts on `UIApplication.didReceiveMemoryWarningNotification`.
- [ ] `ImageFeedApplication.kt` sets Coil `MemoryCache` `maxSizePercent` to `0.15`, enables `allowRgb565(true)`, removes the 100 MB local `DiskCache`, and implements `onTrimMemory` / `onLowMemory` to trim/clear Coil's memory cache and `BlurHashDecoder.clearCache()`.
- [ ] `MviViewModel.iosState` and `MviViewModel.iosEffects` are initialized `by lazy`.
- [ ] `FeedViewModel.swift` stores and deterministically cleans up `randomPhotoCloseable` and `randomPhotoViewModel` on completion, error, re-invocation, and `deinit`.
- [ ] Zero compiler warnings, zero dead code, and both Android and iOS builds + unit tests pass.
