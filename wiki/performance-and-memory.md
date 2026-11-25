# Performance, Memory & Image Pipeline Engineering

This document summarizes the active memory, image-decoding, scrolling, and release optimization architecture established across Specs 50–70.

---

## 1. Image Pipelines & BlurHash Caching

### Android (Coil 3 & `BlurHashDecoder`)
* **Coil Memory-Only Configuration**: [`ImageFeedApplication.kt`](../androidApp/src/main/java/com/example/imagefeed/android/ImageFeedApplication.kt) configures `MemoryCache` with `maxSizePercent(context, 0.15)`, `strongReferencesEnabled(true)`, `allowRgb565(true)`, `crossfade(200)`, and **zero persistent disk cache** to enforce Unsplash hotlinking rules and cap RAM usage.
* **System Memory Trim**: `onTrimMemory` and `onLowMemory` trim or clear Coil's memory cache and invoke `BlurHashDecoder.clearCache()`.
* **BlurHash Caching & Quantization**: [`BlurHashDecoder.kt`](../androidApp/src/main/java/com/example/imagefeed/android/util/BlurHashDecoder.kt) maintains a byte-cost-bounded `LruCache` (`DEFAULT_CACHE_MAX_BYTES = 1 MB`), deduplicates concurrent decodes via `ConcurrentHashMap<String, CompletableDeferred<Bitmap?>>`, quantizes placeholder heights into 4px buckets (`quantizeHeight`), and returns remembered `BitmapPainter` instances (`rememberBlurHashPlaceholder`).

### Apple (`KFImage` & `ImageMemoryCache`)
* **Generic Lazy Placeholder**: [`KFImage.swift`](../iosApp/iosApp/KFImage.swift) uses generic `KFImage<Placeholder: View>` (avoiding `AnyView` type erasure) and evaluates placeholder closures lazily only on cache miss or failure.
* **Bounded `NSCache` & `ImageIO` Downsampling**: `ImageMemoryCache` enforces `totalCostLimit = 50 MB` and `countLimit = 150`, computes exact bitmap byte costs, evicts on `UIApplication.didReceiveMemoryWarningNotification` (iOS), and downsamples off the main thread via `CGImageSourceCreateThumbnailAtIndex` with `kCGImageSourceShouldCacheImmediately`.

---

## 2. Network, Storage & DI Hygiene

* **Zero Pretty-Printing / Body Buffering**: [`HttpClientFactory.kt`](../core/network/src/commonMain/kotlin/com/example/imagefeed/core/network/HttpClientFactory.kt) shares a single `unsplashJson` (`prettyPrint = false`) and sets Ktor `LogLevel.NONE`.
* **Off-Main-Thread Local Storage Serialization**: [`RoomFavoritesRepository.kt`](../core/database/src/commonMain/kotlin/com/example/imagefeed/core/database/RoomFavoritesRepository.kt) moves `Photo` JSON encoding/decoding off the main thread onto `Dispatchers.Default` (`flowOn` / `withContext`) and deduplicates `observeIsFavorite` emissions via `.distinctUntilChanged()`.
* **Scoped Repository Singletons**: Feature repositories, `FavoritesRepository`, and core `HttpClient` / `PhotoDownloadTracker` are `@SingleIn(AppScope::class)` in [`Metro.kt`](../shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt) to avoid duplicate client/repository allocations across screen pushes.
* **Frame-0 Ephemeral Preview Seeding**: [`NavigationPreviewCache.kt`](../core/model/src/commonMain/kotlin/com/example/imagefeed/model/cache/NavigationPreviewCache.kt) (`@SingleIn(AppScope::class)`, `maxEntries = 16`) caches bounded in-memory `Photo`, `PhotoCollection`, and `User` metadata DTOs (zero image binaries or disk files per ADR-02) so [`PhotoDetailsViewModel.kt`](../feature/details/src/commonMain/kotlin/com/example/imagefeed/presentation/details/PhotoDetailsViewModel.kt) seeds `PhotoDetailsState.photo` synchronously on Frame 0 before falling back to Room.
* **Concurrent Photo Details & Stats Loading**: [`PhotoDetailsViewModel.kt`](../feature/details/src/commonMain/kotlin/com/example/imagefeed/presentation/details/PhotoDetailsViewModel.kt) fetches `getPhotoDetails(photoId)` and `getPhotoStats(photoId)` concurrently via `coroutineScope { async ... }`, emitting `photo` immediately upon details completion without blocking behind `stats`.

---

## 3. Compose & SwiftUI Hot-Path Optimizations

### Android Compose
* **Zero-Blur Matte Card Invariant & Draw-Phase Satin Scroll**: [`PhotoCard.kt`](../androidApp/src/main/java/com/example/imagefeed/android/components/PhotoCard.kt), [`CollectionMosaicCard.kt`](../androidApp/src/main/java/com/example/imagefeed/android/components/CollectionMosaicCard.kt), and [`MatteSurface`](../androidApp/src/main/java/com/example/imagefeed/android/designsystem/SurfaceTokens.kt) use opaque/tonal Material 3 surfaces with zero live backdrop blur passes during masonry grid flings; progressive blur is restricted to top-level `SatinHeaderScaffold` containers on API 33+ (with API 29–32 scrim fallback) driven by draw-phase / layout-phase snapshot lambdas so scroll flings and quick-return header collapses incur zero recompositions.
* **Pane-Width Retina CDN Sizing**: [`AdaptiveUtils.kt`](../androidApp/src/main/java/com/example/imagefeed/android/AdaptiveUtils.kt) resolves `LocalPaneWidthDp` (measured and quantized at Navigation 3 pane boundaries via `ProvidePaneWidth`) so `calculatePhotoItemWidthPx` and `calculateCollectionMosaicCardWidthPx` request appropriately sized Unsplash CDN images inside narrow `ListDetailSceneStrategy` panes rather than over-fetching full-window widths.
* **Single-Transition Shimmer**: [`AnimationUtils.kt`](../androidApp/src/main/java/com/example/imagefeed/android/util/AnimationUtils.kt) shares a single `InfiniteTransition` and caches `ShaderBrush` draws via `drawWithCache`.
* **Hoisted Card Requests**: Feed and detail screens hoist aspect ratio calculations, BlurHash painters, and Coil `ImageRequest` objects outside scroll frames.
* **Full-Screen Virtualized Grids**: `UserProfileScreen` uses a single `LazyVerticalStaggeredGrid` with `StaggeredGridItemSpan.FullLine` header/tab spans rather than nested scrollable containers.
* **Static Maps & Cached Charts**: `PhotoDetailsScreen` runs Google Maps in Lite Mode (`GoogleMapOptions().liteMode(true)`) and remembers `MapStyleOptions` and chart `Path`/paint objects.

### Apple SwiftUI (`iosApp` / `macosApp`)
* **Zero-Blur Matte Content Cards**: [`CollectionMosaicCard`](../iosApp/iosApp/CollectionsFeedView.swift), [`UserCardView`](../iosApp/iosApp/SearchView.swift), and [`InsightsView`](../iosApp/iosApp/UserProfileView.swift) timeline chart cards use `.matteSurface()` (`SurfaceTokens.swift`) instead of nested `.ultraThinMaterial` live blurs over opaque canvases.
* **Container-Width Retina CDN Sizing**: [`AdaptiveLayoutHelper.swift`](../iosApp/iosApp/AdaptiveLayoutHelper.swift) measures quantized pane width via `.measureContainerWidth($containerWidth)` so [`PhotoGridCard`](../iosApp/iosApp/DesignSystem/PhotoGridCard.swift) and collection cards compute `2–5` photo columns (`1–3` collection columns) and request accurately scaled Unsplash CDN widths inside iPad Split View and resizable macOS windows.
* **$O(1)$ Column Partitioning & Memoization**: [`AdaptiveLayoutHelper.swift`](../iosApp/iosApp/AdaptiveLayoutHelper.swift) provides `IndexedItem<T>` and single-pass `partitionIntoColumns`. ViewModels memoize column buckets and invalidate only on backing array mutations, eliminating $O(N^2)$ `firstIndex(where:)` calls.
* **Isolated Scroll State**: [`ContentView.swift`](../iosApp/iosApp/ContentView.swift) extracts `CapsuleScrollCoordinator` so scroll-driven minimization invalidates only [`FloatingTabCapsule.swift`](../iosApp/iosApp/FloatingTabCapsule.swift) without recomposing feed masonry grids. Scroll geometry updates use quantized `.onScrollGeometryChange` on iOS 18+ / macOS 15+ (with iOS 17 `PreferenceKey` fallback).
* **Single-Pass GPU Effects**: [`AnimationPrimitives.swift`](../iosApp/iosApp/AnimationPrimitives.swift) uses single-pass `.blendMode(.sourceAtop)` shimmer (respecting `accessibilityReduceMotion`), [`SurfaceTokens.swift`](../iosApp/iosApp/DesignSystem/SurfaceTokens.swift) applies `GlassStyle.lightweight` on masonry attribution pills, and [`PhotoInspectorView.swift`](../iosApp/iosApp/PhotoInspectorView.swift) renders cached static `MKMapSnapshotter` maps instead of live `Map` instances.
* **Lazy ViewModel Initialization**: Feed, Collections, Detail, Search, Profile, and Collection Detail views defer KMP ViewModel allocation out of `View.init` using lazy `@State` creation inside `.task`.

---

## 4. Android Release, Startup & Apple Linking Tooling

* **R8 Shrinking & Obfuscation**: [`androidApp/build.gradle.kts`](../androidApp/build.gradle.kts) enables `isMinifyEnabled = true` on release and benchmark builds, paired with Room KMP / `RoomDatabaseConstructor` keep rules in [`proguard-rules.pro`](../androidApp/proguard-rules.pro).
* **Apple Dead-Code Stripping**: [`iosApp/project.yml`](../iosApp/project.yml) enables `DEAD_CODE_STRIPPING: YES` on `macosApp` so unreachable `sqlite-framework` cinterop stubs from `:core:database` strip cleanly during Debug linking.
* **Macrobenchmark & Baseline Profiles**: The [`:benchmark`](../benchmark/build.gradle.kts) module generates [`baseline-prof.txt`](../androidApp/src/release/generated/baselineProfiles/baseline-prof.txt) and [`startup-prof.txt`](../androidApp/src/release/generated/baselineProfiles/startup-prof.txt) wired via `androidx.profileinstaller` to pre-compile hot Compose startup and scroll paths.
