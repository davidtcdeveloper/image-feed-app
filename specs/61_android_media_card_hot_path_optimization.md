# Specification 61: Android Media Card Hot-Path Optimization (PhotoCard, CollectionMosaicCard, Grid Helpers)

**Status:** Implemented

## 1. Overview & Problem Statement

Lazy grid items are the hottest composition path in the app. The audit (`lazy-layouts.md`, `state-and-stability.md`, `animation.md`) found per-item work that is repeated on every recomposition and on every item entering composition. [Spec 51](51_lazy_grid_motion_and_scroll_optimization.md) already gated `staggeredEntrance` (index < 6, `rememberSaveable`) and moved `bounceClick` to a lambda `graphicsLayer`; the items below are what remains.

### Verified findings

[`PhotoCard`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/FeedScreen.kt#L296-L420):
1. `calculatePhotoItemWidthPx()` ([`AdaptiveUtils.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/AdaptiveUtils.kt#L89-L95)) is called **per item**; it reads `LocalConfiguration`, `LocalDensity`, and `calculateGridColumnCount()` (a second `LocalConfiguration` read) for every card, although the value is identical for the whole grid.
2. `ImageRequest.Builder(...).build()` and the URL string concatenation (`raw + "&w=" + px + "&q=80&auto=format"`) run on every recomposition of the card; the `Brush.verticalGradient` + `listOf(...)` for the attribution scrim is allocated per recomposition; a second `ImageRequest` is built for the avatar.
3. `Modifier.sharedElement(rememberSharedContentState(key = "photo_img_<id>"))` is applied on **every** lazy item, registering a shared-content key per item whether or not a transition involving it is possible.
4. `staggeredEntrance` is a `@Composable` modifier factory (state + `LaunchedEffect` live in the caller's composition). It already early-returns for `index >= 6` and after first entry, so cost is bounded; converting to a `Modifier.Node` is **low priority / optional**.

Same patterns elsewhere (verified, beyond the original audit):
- `PhotoCard` is reused by [FeedScreen L263](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/FeedScreen.kt#L263), [SearchScreen L669](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L669), [UserProfileScreen L733](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L733), [CollectionDetailScreen L255](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/CollectionDetailScreen.kt#L255): all inherit the fix automatically.
- [`CollectionMosaicCard`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/CollectionsFeedScreen.kt#L256-L430) reads `LocalConfiguration`/`LocalDensity` per item, builds **three** `ImageRequest`s + interpolated URL strings per recomposition, and applies `sharedBounds` per item. `RelatedCollectionCard` in `CollectionDetailScreen.kt` (L475+) and Search user/collection grids (`SearchScreen.kt` L732, L882) should be checked for the same.
- `calculateGridColumns`, `calculateGridColumnCount`, `calculateCollectionGridColumns`, `calculateUserGridColumns` in `AdaptiveUtils.kt` read `LocalConfiguration` and allocate a `StaggeredGridCells` each recomposition. Cheap, but redundant given `LocalAdaptiveLayoutInfo` already exposes `screenWidthDp` (`adaptive/AdaptiveLayoutInfo.kt`).
- Note (accuracy, not perf): item width is `screenWidthDp / columns` (window-based), ignoring content padding/spacing and, in a `ListDetailSceneStrategy` list pane, the real pane width, so requests may be wider than needed. Changing the URL width changes Coil cache keys; treat as out-of-scope follow-up unless trivial.

## 2. Technical Design

1. **Hoist grid-invariant values**: compute `itemWidthPx` once at each grid call site (or inside a single remembered helper keyed on screen width/density/columns) and pass it into the card as a parameter. Drop the per-item `LocalConfiguration`/`LocalDensity` reads. Update the four `PhotoCard` call sites and `CollectionMosaicCard` accordingly (parameter-based, no new global state).
2. **Remember request & URL**: `remember(photo.id, itemWidthPx)` for the image URL and `ImageRequest`; same for avatar request keyed on the profile image URL, and for each `CollectionMosaicCard` request. Keep the Unsplash invariants from AGENTS.md: hotlink exact URLs, **preserve `ixid`** (it is part of `urls.raw`), keep `w`/`q`/`auto` params unchanged, never cache files locally. (Note: [Spec 66](66_memory_optimization_phase2_maps_thumbnails_and_blurhash.md) standardizes this URL construction into `photo.urls.getOptimalUrl(itemWidthPx)` in `:core:model`; implementers may consume `getOptimalUrl` directly if Spec 66 is landed or coordinated).
3. **Scrim**: replace per-recomposition `Brush.verticalGradient` with a remembered/top-level brush (colours are constants) or `drawWithCache`/`drawBehind`; avoid `listOf` allocation per frame.
4. **Gate shared elements**: apply `sharedElement`/`sharedBounds` only when the item can participate in a transition (e.g. gated by hoisted selected-id state or running transition state). Must keep back-navigation return transition working seamlessly when the source item has been scrolled off-screen or recycled. Implementer must validate on device and preserve Feed -> Details -> Back, Search -> Details, Profile -> Details, Collections -> Detail transitions.
5. **Grid helpers**: derive column logic from `LocalAdaptiveLayoutInfo.screenWidthDp` and wrap results in `remember(screenWidth)` (or make them pure functions of an `Int`, keeping `@Composable` wrappers thin). Keep `computeBottomContentPadding` pattern (pure function + thin composable) as the model.
6. **`staggeredEntrance` -> `Modifier.Node`**: **Explicitly deferred / Won't-do**. `staggeredEntrance` already gates execution by early-returning `this` for `index >= 6` and after first entry via `rememberSaveable { mutableStateOf(false) }`. Steady-state scrolling and recycling past index 5 introduce zero animations, coroutines, or allocations; composition-phase overhead is negligible after items 1–5.

## 3. Verification

- Layout Inspector recomposition counts on Feed fast fling: `PhotoCard` skipped when only scroll offset changes; no per-item `LocalConfiguration` invalidation.
- Rotate / split-screen resize: column count and request width update correctly (config-change path still works after hoisting).
- Manual check of all shared-element transitions listed above, forward and back, including after scrolling the source item off-screen.
- Compose compiler reports (stability): `PhotoCard` and `CollectionMosaicCard` skippable.
- `./gradlew :androidApp:assembleDebug`, lint, existing `:androidApp:testDebugUnitTest`; follow `kmp-testing` skill if any shared code changes (none expected).

## 4. Acceptance Criteria

- [x] `calculatePhotoItemWidthPx` (or replacement) is evaluated once per grid, not per item; no `LocalConfiguration`/`LocalDensity` reads inside item composables.
- [x] `ImageRequest` and URL strings in `PhotoCard` (main + avatar) and `CollectionMosaicCard` (3 requests) are remembered with correct keys; `ixid` and URL params unchanged.
- [x] Attribution scrim brush not allocated per recomposition.
- [x] Shared element / bounds modifiers gated; all four forward/back transitions verified manually.
- [x] Grid column helpers memoized or pure; behaviour on phone/tablet/foldable widths unchanged.
- [x] Decision on `staggeredEntrance` as `Modifier.Node` recorded (implemented or explicitly deferred) in this spec.
- [x] Zero compiler warnings, no dead code (unused imports/params removed).
- [x] Android build passes; iOS build verified if `shared/` touched (not expected).

> [!NOTE]
> Line numbers in this spec are approximate (working tree had uncommitted edits at authoring time); locate code by symbol name.
