# Business Rules, Domain Contracts & API Compliance

This page documents both product-domain behaviors and non-business technical/operational invariants enforced across `:core:*`, `:feature:*`, `androidApp`, and `iosApp` (`iosApp` + `macosApp`).

---

## 1. Unsplash API Compliance & Security

* **Credential Injection (Zero Secrets in VCS)**: API keys (`unsplash.api.key`, `google.maps.api.key`) live exclusively in git-ignored `local.properties`. Shared KMP code reads `BuildKonfig.UNSPLASH_API_KEY` injected into Ktor's `Authorization: Client-ID` header in [`HttpClientFactory.kt`](../core/network/src/commonMain/kotlin/com/example/imagefeed/core/network/HttpClientFactory.kt).
* **Direct Hotlinking (No Persistent Image Disk Cache)**: Photos must always be loaded from direct Unsplash URLs without caching image bytes on disk or intermediate servers. Android Coil explicitly omits a disk cache in [`ImageFeedApplication.kt`](../androidApp/src/main/java/com/example/imagefeed/android/ImageFeedApplication.kt), and Apple [`KFImage.swift`](../iosApp/iosApp/KFImage.swift) caches decoded thumbnails strictly in an ephemeral in-memory `NSCache` (`ImageMemoryCache`).
* **`ixid` Parameter Preservation**: Query transformations on Unsplash URLs must preserve tracking parameters such as `ixid`. [`PhotoUrls.getOptimalUrl`](../core/model/src/commonMain/kotlin/com/example/imagefeed/model/Photo.kt#L38-L65) strips only `w=`, `q=`, and `auto=` before appending resized parameters, leaving `ixid` and other tokens intact.
* **Prominent Photographer Attribution & UTM Referrals**: Every photo card, collection card, detail header, and profile link displays photographer attribution and links out with referral query parameters (`?utm_source=ImageFeedApp&utm_medium=referral`).
* **Download Tracking Trigger**: Saving or downloading a photo triggers [`PhotoDownloadTracker.trackDownload`](../core/network/src/commonMain/kotlin/com/example/imagefeed/core/network/PhotoDownloadTracker.kt) against `photo.links.downloadLocation`. Never call this endpoint automatically on feed display.

---

## 2. Dynamic CDN Image Sizing & Retina Scaling

* **Point-to-Pixel Scaling Formula**: Platform UI shells multiply layout container width in points/dp by display scale factor (`UIScreen.main.scale` on iOS, `NSScreen.backingScaleFactor` on macOS, display density on Android) before requesting images. See [`AdaptiveLayoutHelper.swift`](../iosApp/iosApp/AdaptiveLayoutHelper.swift#L41-L51).
* **Clamped Optimal URL Parameters**: [`PhotoUrls.getOptimalUrl(targetWidthPx, quality)`](../core/model/src/commonMain/kotlin/com/example/imagefeed/model/Photo.kt#L38-L65) clamps `targetWidthPx` to `[64, 2400]` and `quality` to `[10, 100]` (default `80`), producing `&w=<px>&q=<quality>&auto=format`.
* **Thumbnail & Backdrop Caps**: Small preview thumbnails (collection 3-photo mosaics, search result rows) must request container-sized URLs rather than `urls.regular`, and blurred detail backdrops downsample to `maxPixelSize: 160` (`PhotoDetailsView.swift`).

---

## 3. Feed, Topic Curation & Pagination Rules

* **Editorial vs. Topic Feeds**: [`FeedState.selectedTopicSlug`](../feature/feed/src/commonMain/kotlin/com/example/imagefeed/presentation/feed/FeedState.kt) defaults to `"editorial"`. Selecting `"editorial"` queries the general photos endpoint (`perPage = 15`); any other topic slug queries topic photos and cancels in-flight page jobs to prevent stale cross-topic updates ([`FeedViewModel.kt`](../feature/feed/src/commonMain/kotlin/com/example/imagefeed/presentation/feed/FeedViewModel.kt)).
* **Cross-Page ID Deduplication**: Because live Unsplash feeds shift between page requests, incoming pages are deduplicated against already-loaded items via [`filterDistinctAgainst`](../core/network/src/commonMain/kotlin/com/example/imagefeed/core/network/CollectionsExt.kt). Pagination sets `hasReachedEnd = true` if fewer than `perPage` items return or if a non-empty page yields zero distinct new items.
* **Prefetch & Concurrency Guards**: Feed/search pagination guards against concurrent requests (`isLoading || isRefreshing || hasReachedEnd`) and triggers prefetching ahead of the list bottom (6-item lookahead threshold verified in [`PaginationFlowTest.kt`](../androidApp/src/test/java/com/example/imagefeed/android/util/PaginationFlowTest.kt)).
* **Random Photo Discovery (Shake / Shortcut)**: Device shake (Android/iOS) or `⌘S` / toolbar action (macOS) fetches a single random photo via [`RandomPhotoViewModel.kt`](../feature/feed/src/commonMain/kotlin/com/example/imagefeed/presentation/random/RandomPhotoViewModel.kt) and routes to Photo Details.

---

## 4. Unified Search & Filtering Rules

* **Single 300ms Coroutine Debounce**: [`UnifiedSearchViewModel.kt`](../feature/search/src/commonMain/kotlin/com/example/imagefeed/presentation/search/UnifiedSearchViewModel.kt) debounces query updates by `300.milliseconds` inside `viewModelScope` and cancels pending jobs on clear or submit (`SubmitSearch` executes immediately without waiting for debounce). Platform UI bindings must not apply a second debounce layer.
* **Tri-Facet Segmentation**: Search partitions into `PHOTOS`, `COLLECTIONS`, and `USERS` tabs. Selecting a color filter automatically switches to `SearchTab.PHOTOS`.
* **Inactive Tab Memory Capping**: Switching away from a search tab trims cached inactive lists to at most 20 items and resets page counters to `1` (`UnifiedSearchViewModel.setTab`) to bound retained heap memory.
* **Clickable Tags**: Tapping a tag on Photo Details navigates directly to the Search screen pre-populated with that tag query.

---

## 5. Local Favorites Persistence Rules

* **Local-Only Scope**: Bookmarks/favorites persist locally on-device with no cross-device sync or Unsplash OAuth user-likes integration.
* **Metadata-Only Hotlinking Storage**: [`FavoritePhotoEntity`](../core/database/src/commonMain/kotlin/com/example/imagefeed/core/database/FavoritePhotoEntity.kt) stores serialized `Photo` JSON (`favorite_photos`) and `createdAt` timestamps without caching image binaries on disk, preserving Unsplash CDN hotlinking (`ixid` intact) and photographer attribution.
* **Ordering & Offline Fallback**: Saved items sort newest-first (`ORDER BY createdAt DESC`), re-favoriting updates `createdAt` to the front, and [`PhotoDetailsViewModel`](../feature/details/src/commonMain/kotlin/com/example/imagefeed/presentation/details/PhotoDetailsViewModel.kt) seeds photo metadata from [`FavoritesRepository.getFavoriteById`](../core/database/src/commonMain/kotlin/com/example/imagefeed/core/database/FavoritesRepository.kt) so bookmarked photos remain viewable offline.

---

## 6. Error Mapping, Localization & RTL Invariants

* **Typed Domain Errors**: Repositories and ViewModels never construct raw user-facing error strings. Exceptions map via [`Throwable.toAppError()`](../core/network/src/commonMain/kotlin/com/example/imagefeed/core/network/AppErrorMapper.kt) to sealed [`AppError`](../core/model/src/commonMain/kotlin/com/example/imagefeed/model/AppError.kt) (`Network`, `Timeout`, `NotFound`, `Server(code)`, `Unknown`).
* **Platform Localization Tables**: All visible UI text is localized via Android `strings.xml` / `plurals.xml` (`AppErrorLocalization.kt`) and Apple [`Localizable.xcstrings`](../iosApp/iosApp/Localizable.xcstrings) ([`AppErrorLocalization.swift`](../iosApp/iosApp/AppErrorLocalization.swift)). Only internal API topic slugs, logging, and icon identifiers remain unlocalized.
* **Native Formatters & RTL Safety**: Compact metric formatting and dates must use platform locale-aware formatters (`CompactDecimalFormat` / `DateTimeFormatter` on Android; Swift `.formatted(.number.notation(.compactName))` / `date.formatted` on Apple). Navigation icons and spacing must mirror cleanly in RTL (`Icons.AutoMirrored.Filled.ArrowBack`, `chevron.backward`, `Start`/`End` margins).
