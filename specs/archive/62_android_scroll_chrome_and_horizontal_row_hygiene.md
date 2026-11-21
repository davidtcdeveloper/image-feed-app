# Specification 62: Android Scroll-Driven Chrome & Horizontal Row Hygiene

**Status:** Implemented

## 1. Overview & Problem Statement

Two small, independent hygiene issues around scrolling chrome and horizontal lists that can be fixed together (`state-and-stability.md`, `lazy-layouts.md`).

### Verified findings

1. **Composition-phase scroll read** in [`FeedScreen.kt` L183-L194](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/FeedScreen.kt#L183-L194): the topic strip `Box` passes `Modifier.background(if (scrollBehavior.state.contentOffset < 0f) ... else ...)`, evaluated inside the `topBar` slot. `contentOffset` changes on every scroll pixel, so the `topBar` slot scope (TopAppBar + topic `LazyRow`) is invalidated every frame of scrolling although only a boolean threshold matters. It also switches colour abruptly while the `TopAppBar` above animates its own `scrolledContainerColor`, causing a visible mismatch. `contentOffset` is only read in `FeedScreen.kt` (verified by grep).
2. **LazyRow audit** (all `LazyRow` usages in `androidApp`):
   - `FeedScreen.kt` L195 topics row: **already keyed** (`topic_editorial`, `it.id`); no `contentType`. Two item kinds both render `TopicCapsuleCard`, so `contentType` gain is marginal.
   - `SearchScreen.kt` L1165 colour filter row: `items(colors)` **has no key**; `colors` is defined at L1011 (verify it is `remember`ed). 11 fixed items inside a filter sheet.
   - `SearchScreen.kt` L1355 colour picker row: **already keyed** (`key = { it }`) - the original audit claim of "missing key" is not accurate here; each item has three `animate*AsState`, acceptable for 11 items.
   - `UserProfileScreen.kt` L561 `ProfileTabSelector`: **already keyed** (`it.name`); 4 items; uses `LazyListState.animateScrollToItem` for the active tab.
   - `CollectionDetailScreen.kt` L460 related collections: keyed (`it.id`) (not in the original audit).
   - Conclusion: only the Search L1165 row is truly unkeyed. `contentType` is not meaningful for homogeneous short rows.
3. **Plain Row candidates**: Search L1165 (11 constant items) could be a `Row` with `horizontalScroll`. `ProfileTabSelector` (4 items) relies on `LazyListState` scrolling to the active tab; converting needs a `ScrollState` + position math, so **keep LazyRow** unless the gain is demonstrated. Other rows hold network-driven lists and must stay lazy.
4. **Profile nested grids (addressed by [Spec 65](65_memory_optimization_phase1_caches_grids_and_scopes.md))**: [`UserProfileScreen.kt` L716-L726 and L815-L825](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L716) place `LazyVerticalStaggeredGrid` with `userScrollEnabled = false` and `heightIn(max = 2000.dp)` inside a `verticalScroll` parent (L282). This limits virtualization to a 2000dp window and makes pagination depend on the inner grid's visible items instead of the outer scroll position. This structural fix (migrating `UserProfileContent` to a single outer `LazyVerticalStaggeredGrid` with full-line header/tabs) has been formalized and scheduled as part of [Spec 65 (Phase 1 Memory Optimization)](65_memory_optimization_phase1_caches_grids_and_scopes.md).

## 2. Technical Design

1. **Topic strip background**: derive a boolean with `derivedStateOf` over `scrollBehavior.state.contentOffset` and consume it only where it can change colour without recomposing the slot (e.g. `animateColorAsState` driven value read in a draw lambda via `drawBehind`, or a colour from `TopAppBarDefaults`-consistent token). Preferred: match the TopAppBar's animated `scrolledContainerColor` behaviour so both surfaces transition together. Remove the direct composition read.
2. **Search colour row**: add a stable key (colour value/name) and `remember` its source list; evaluate converting to `Row`+`horizontalScroll` (acceptable either way; document choice).
3. **No-op items** (explicitly documented, no code): other keyed rows left as-is; `contentType` not added to homogeneous rows; `ProfileTabSelector` stays `LazyRow`.
4. **Profile nested grid virtualization**: deferred to [Spec 65](65_memory_optimization_phase1_caches_grids_and_scopes.md) to keep this spec strictly scoped to scroll chrome and horizontal row hygiene.

## 3. Verification

- Layout Inspector recomposition counts for `FeedScreen` topBar slot while scrolling the feed (before: increments per frame; after: only on threshold crossing or none).
- Visual check: topic strip colour matches the app bar through scroll start/stop, light and dark.
- Search filter sheet: selecting colours still works and state survives recomposition/reordering.
- `./gradlew :androidApp:assembleDebug`, lint.

## 4. Acceptance Criteria

- [x] No `scrollBehavior.state.contentOffset` read in composition in `FeedScreen.kt`.
- [x] Topic strip colour transitions consistently with the top app bar.
- [x] Search `LazyRow` at L1165 keyed (or replaced by a `Row`), source list remembered.
- [x] Spec records that other LazyRows were verified already keyed and intentionally left unchanged.
- [x] Profile nested-grid virtualization verified as tracked and handled by Spec 65.
- [x] Zero compiler warnings, no dead code; Android build passes.

> [!NOTE]
> Line numbers in this spec are approximate (working tree had uncommitted edits at authoring time); locate code by symbol name.
