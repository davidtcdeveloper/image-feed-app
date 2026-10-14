# Specification 50: Android Image Pipeline & BlurHash Decoding Cache

**Status:** Approved

## 1. Overview & Problem Statement

Profiling of the Android feed (`FeedScreen.kt` and `CollectionsFeedScreen.kt`) identified noticeable scroll jank and frame drops during fling gestures. A deep technical evaluation revealed two interconnected bottlenecks within the image asset acquisition and decoding pipeline:

1. **Uncached, Main-Thread BlurHash Decoding (Issue 1)**:
   - BlurHash placeholder decoding is currently executed on-demand per cell inside [`FeedScreen.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/FeedScreen.kt#L313-L315), [`CollectionsFeedScreen.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/CollectionsFeedScreen.kt#L258-L262), [`CollectionDetailScreen.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/CollectionDetailScreen.kt#L305-L307), and [`PhotoDetailsScreen.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt#L281-L283) using `produceState`.
   - By default, `produceState` launches on `Dispatchers.Main.immediate`. The trigonometric decoding loop in [`BlurHashDecoder.decode`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/util/BlurHashDecoder.kt#L15-L69) ($O(xComp \times yComp \times width \times height)$ cosine calculations) executes directly on the Android UI thread during scroll frames.
   - Crucially, [`BlurHashDecoder.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/util/BlurHashDecoder.kt) lacks an in-memory cache (`LruCache<String, Bitmap>`). As lazy list items are recycled out of and back into view, the exact same BlurHash strings are re-decoded repeatedly, allocating new `Bitmap` instances every time and causing heavy Garbage Collection (GC) pauses during flings.

2. **Unconfigured Coil ImageLoader & Missing Cache Infrastructure (Issue 3)**:
   - [`ImageFeedApplication.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/ImageFeedApplication.kt) currently contains an empty `onCreate()` implementation without configuring a Coil `ImageLoader` singleton.
   - Without an explicit `MemoryCache` and `DiskCache` configuration on Coil 3 (`io.coil-kt.coil3:coil-compose:3.6.3`), image requests rely on default limits without heap percentage sizing or persistent disk storage.
   - During fast flings, previously decoded bitmaps are quickly evicted from memory, forcing repeated network fetches or re-decoding from uncached HTTP streams, compounding UI thread delays and network churn.

By grouping these two image loading and decoding pipeline concerns, this specification establishes an optimized, thread-safe media pipeline that offloads computation from the UI thread, introduces multi-level in-memory and disk caching, and enforces Unsplash API compliance.

---

## 2. Technical Architecture & Component Design

```mermaid
graph TD
    subgraph UI Composables
        Card[PhotoCard / CollectionMosaicCard]
        Painter[rememberAsyncImagePainter]
    end

    subgraph BlurHash Pipeline
        CacheCheck{In-Memory LRU Cache Hit?}
        SyncBitmap[Immediate Synchronous Bitmap Return]
        DefaultDispatcher[Dispatchers.Default Computation]
        BlurDecoder[BlurHashDecoder.decode]
        CacheStore[Store in BlurHashCache LruCache]
    end

    subgraph Coil 3 ImageLoader Pipeline
        App[ImageFeedApplication : SingletonImageLoader.Factory]
        CoilMem[Coil MemoryCache (25% Application Heap)]
        CoilDisk[Coil DiskCache (100MB cacheDir)]
        OkHttpEngine[Ktor / OkHttp Network Engine]
    end

    Card -->|Placeholder Request| CacheCheck
    CacheCheck -->|Yes| SyncBitmap
    CacheCheck -->|No| DefaultDispatcher
    DefaultDispatcher --> BlurDecoder
    BlurDecoder --> CacheStore
    CacheStore --> SyncBitmap
    SyncBitmap --> Painter

    Card -->|Image URL Request| Painter
    Painter --> CoilMem
    CoilMem -->|Cache Miss| CoilDisk
    CoilDisk -->|Cache Miss| OkHttpEngine
```

### A. Coil 3 ImageLoader Singleton Factory ([`ImageFeedApplication.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/ImageFeedApplication.kt))
- Implement `coil3.SingletonImageLoader.Factory` on `ImageFeedApplication`.
- **Memory Cache Configuration**:
  - Sized dynamically to **25% of available application heap** using `MemoryCache.Builder().maxSizePercent(context, 0.25).strongReferencesEnabled(true).build()`.
  - Enables strong reference caching to prevent premature bitmap recycling during rapid scrolling within the same session.
- **Disk Cache Configuration**:
  - Located in `context.cacheDir.resolve("image_cache").absolutePath.toPath()` using Okio Path API (provided transitively via Coil 3).
  - Sized to **100 MB** (`100L * 1024 * 1024`).
  - Strict compliance with **Core Invariant 4 (Security & Unsplash API Compliance)**: Transient client HTTP caching in `cacheDir` (managed by Android OS LRU eviction) preserving the hotlinked URL with `ixid` parameter intact, rather than persisting image files to permanent storage or custom databases.
- **Crossfade & Transition Defaults**:
  - Configure default crossfade transition (200ms) at the `ImageLoader` level, ensuring smooth bitmap swap without composable flickering.

### B. BlurHash Asynchronous Offloading & In-Memory LRU Cache ([`BlurHashDecoder.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/util/BlurHashDecoder.kt))
- **LRU Bitmap Cache**:
  - Introduce an internal `LruCache<String, Bitmap>` sized for 200–300 small BlurHash bitmaps (~500 KB to 1 MB max memory footprint, as a $32 \times 20$ ARGB_8888 bitmap requires only $\approx 2.5\text{ KB}$).
  - Composite cache key: `"${blurHash}_${width}x${height}_${punch}"` to avoid collision across different aspect ratios or punch levels.
- **Immediate Synchronous Cache Inspection (`getCached`)**:
  - Expose a synchronous lookup function: `fun getCached(blurHash: String?, width: Int, height: Int, punch: Float = 1.0f): Bitmap?`.
  - Allows composables to set the initial state synchronously if the bitmap was already decoded, eliminating the 1-frame blank placeholder flicker.
- **Boundary Clamping & Dimension Validation**:
  - Guard `BlurHashDecoder.decode` against invalid non-positive dimensions: `if (width <= 0 || height <= 0) return null`.
  - In callers calculating dynamic height (e.g. `(32 / aspectRatio).toInt()`), clamp using `.coerceAtLeast(1)` to guard against panoramic/ultrawide aspect ratios that could result in zero or negative values.
- **Asynchronous Background Decoding (`decodeAsync`)**:
  - Expose a suspending function: `suspend fun decode(blurHash: String?, width: Int, height: Int, punch: Float = 1.0f): Bitmap? = withContext(Dispatchers.Default) { ... }`.
  - Shifts all trigonometric Math calculations ($cos, pow$) and `Bitmap.createBitmap` allocation from the Main thread to `Dispatchers.Default`.
  - Automatically stores decoded results in the LRU cache prior to returning.
- **Composable Lifecycle Integration**:
  - Provide a standardized, reusable Compose helper:
    [`rememberBlurHashPlaceholder(blurHash: String?, width: Int, height: Int): Bitmap?`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/util/BlurHashDecoder.kt)
    that initializes with `getCached()` and only launches a coroutine via `produceState` on cache misses.

---

## 3. Implementation Plan & Source Modifications

### Step 1: Configure Coil 3 ImageLoader in [`ImageFeedApplication.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/ImageFeedApplication.kt)
- Update `ImageFeedApplication` to implement `SingletonImageLoader.Factory`.
- Provide `newImageLoader(context: PlatformContext): ImageLoader` with:
  - `MemoryCache.Builder().maxSizePercent(context, 0.25).build()`
  - `DiskCache.Builder().directory(context.cacheDir.resolve("image_cache").absolutePath.toPath()).maxSizeBytes(100L * 1024 * 1024).build()`
  - `.crossfade(true)`
  - Respect HTTP cache headers and hotlink URLs.

### Step 2: Implement LRU Cache & Background Offload in [`BlurHashDecoder.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/util/BlurHashDecoder.kt)
- Add thread-safe `LruCache<String, Bitmap>` instance.
- Add `getCached(key)` helper.
- Add boundary dimension validation (`width <= 0 || height <= 0`).
- Update `decode(...)` to check cache first, execute decoding, and insert into cache.
- Add suspending `decodeAsync(...)` dispatched on `Dispatchers.Default`.
- Add composable helper function `rememberBlurHashPlaceholder(...)` encapsulating `produceState` with initial cached value.

### Step 3: Migrate Calling Screens to Use Optimized BlurHash Helper & Clamped Dimensions
- Update [`FeedScreen.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/FeedScreen.kt#L313-L315) in `PhotoCard` with clamped height `(32 / aspectRatio).toInt().coerceAtLeast(1)`.
- Update [`CollectionsFeedScreen.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/CollectionsFeedScreen.kt#L258-L262) in `CollectionMosaicCard`.
- Update [`CollectionDetailScreen.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/CollectionDetailScreen.kt#L305-L307) in `CollectionDetailScreen`.
- Update [`PhotoDetailsScreen.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt#L281-L283) in `PhotoDetailsScreen` with clamped height.

---

## 4. Verification & Testing Strategy

1. **Unit Testing**:
   - Author `BlurHashDecoderTest.kt` verifying:
     - Synchronous cache hits on repeated calls with identical keys.
     - Key isolation across differing aspect ratios and resolutions.
     - Safe handling of null, empty, or corrupted BlurHash strings.
     - Boundary dimension handling (e.g. `width = 0`, `height = 0`, or negative dimensions safely returning `null` without throwing `IllegalArgumentException`).
     - LRU eviction behavior under simulated cache pressure.
2. **Performance & Profiling**:
   - Inspect Android Studio Profiler (CPU & Memory):
     - Verify zero `cos()` or BlurHash computation traces on the Main thread.
     - Observe steady memory footprint with zero rapid GC spikes during sustained flings.
3. **Build & Lint Verification**:
   - Run `./gradlew :androidApp:assembleDebug`.
   - Run `./gradlew ktlintCheck detekt`.

---

## 5. Acceptance Criteria

- [ ] `ImageFeedApplication` implements `SingletonImageLoader.Factory` configuring Coil 3 with a 25% heap `MemoryCache` and 100MB `DiskCache`.
- [ ] `BlurHashDecoder` contains an in-memory `LruCache` preventing repeated decoding of previously decoded placeholders.
- [ ] All BlurHash decoding computations execute strictly on background threads (`Dispatchers.Default`) with zero UI thread overhead.
- [ ] Dimension boundaries are strictly validated and clamped to prevent zero/negative size crashes on extreme aspect ratios.
- [ ] Screens (`FeedScreen`, `CollectionsFeedScreen`, `CollectionDetailScreen`, `PhotoDetailsScreen`) read cached placeholders synchronously during initial composition.
- [ ] Complies fully with Unsplash API hotlink and attribution requirements.
- [ ] Dual-platform compilation passes without errors or warnings.
