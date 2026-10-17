# Specification 63: Android Photo Details Layout & Chart Rendering Optimization

**Status:** Approved

## 1. Overview & Problem Statement

Covers the photo details screen layout scope and the two Canvas charts (`graphics-performance.md`, `layouts.md`, `state-and-stability.md`).

### Verified findings

#### A. `BoxWithConstraints` in `PhotoDetailsContent`
[`PhotoDetailsScreen.kt` L360](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt#L360) wraps the entire details content (TableTop, dual-pane, single-pane branches) in `BoxWithConstraints`, i.e. a `SubcomposeLayout`. It uses `maxWidth` for (1) `isDualPane` (`useDualPaneDetails && width >= 600.dp`) and (2) the requested image width.

**The audit's suggestion (replace with `LocalAdaptiveLayoutInfo`) is NOT safe as-is.** [`AdaptiveLayoutInfo`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/adaptive/AdaptiveLayoutInfo.kt) is computed from the **activity window** (`calculateWindowSizeClass(activity)`, `LocalConfiguration.screenWidthDp`). Details is also rendered as `ListDetailSceneStrategy.detailPane()` ([`navigation/NavigationSections.kt` L96, L160](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/navigation/NavigationSections.kt#L96)), where the pane is narrower than the window; `maxWidth` returns the real pane width, `screenWidthDp` does not. Replacing it would pick the wrong layout/image width on tablets and foldables.

Real cost is modest (one subcomposition at the root, not per list item). Additionally, inside the scope the `ImageRequest`, URL string, and `rememberAsyncImagePainter` are rebuilt on every recomposition of the scope (L370-L396), which is the part worth fixing.

#### B. `StatisticsLineChart` (details, L1559-L1748)
- Draw lambda allocates per draw: `data.mapIndexed` point list, two `Path`s with cubic segments, two gradient `Brush`es (+ `listOf`), and two `PathEffect.dashPathEffect(floatArrayOf(...))`.
- `maxOrNull()`/`minOrNull()` and derived `diff` recomputed on every recomposition / draw (min/max at L1571-L1572 in composition, `diff` inside the draw lambda).
- Correction to the audit: this chart has **no `dragX`** (it is static); drag handling exists only in the profile chart. Draw only reruns on size/data/theme changes, so impact is **low**.
- `formatStatValue` ([L1750](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt#L1750)) creates a `CompactDecimalFormat` on every call; called 3 times per chart recomposition and also from the profile chart.

#### C. `InteractiveTimelineChart` ([`UserProfileScreen.kt` L1055-L1252](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L1055))
- `dragX` (`mutableStateOf<Float?>`) is read **in composition** at L1080-L1083 (overlay header) so every drag event recomposes the whole chart `Column`, and again in the draw lambda (correct location).
- Draw lambda rebuilds points list, fill `Path`, stroke `Path`, and gradient `Brush` per draw, i.e. **every drag event** (dragX read in draw invalidates it) -> allocation churn during interaction (this is the high-impact part).
- `maxVal/minVal/diff` recomputed each recomposition without `remember(data)`.
- **Correctness bug**: header index uses a hard-coded `300.dp` step width (L1082) while the canvas uses its real `size.width` (L1179); on any width other than 300dp the header date/value does not match the highlighted point.
- **Dead animation**: `animateFloatAsState(targetValue = 1f)` (L1072) starts at its target, so `animProgress` is always 1 and the 800ms "draw" animation never plays (no-op state read).
- Haptics: `Vibrator` service lookup on every `onDragStart` (minor); consider `LocalHapticFeedback`.
- `pointerInput(Unit)` closure captures `context`; acceptable.

## 2. Technical Design

1. **Details layout scope**: keep pane-width awareness. Options (pick after measuring with Layout Inspector/Perfetto): (a) keep `BoxWithConstraints` but narrow its scope to the minimum subtree that needs `maxWidth`, hoisting everything that does not depend on it; (b) measure pane width via a lightweight `onSizeChanged`/layout-based holder (note one-frame lag risk) . Do **not** substitute `LocalAdaptiveLayoutInfo.screenWidthDp`. If measurement shows no impact, document as won't-change.
2. **Remember request**: `remember(photo.id, imageWidthPx)` for the details `ImageRequest` and URL; preserve `ixid`, `w`, `q=85`, `auto=format`.
3. **Charts - geometry in draw cache**: use `drawWithCache` (or `Modifier.drawBehind` + remembered objects) so points, paths, brushes, and `PathEffect` are built once per (size, data, colours) and reused; hoist the constant dash `PathEffect` as a top-level val. `remember(data)` for min/max/diff.
4. **Profile chart interaction**: read `dragX` only in draw for the canvas; for the header, expose a `derivedStateOf` hover index so recomposition happens only when the index changes, not per pixel. Compute index from actual canvas width (measured via `onSizeChanged` into state read lazily, or computed in the pointer handler) to remove the `300.dp` assumption.
5. **Animation**: either animate from 0 to 1 properly (e.g. `Animatable` driven by `LaunchedEffect(data)`, read in draw) or delete `animProgress` entirely, whichever matches intended design; document the decision. Per zero-dead-code policy, do not leave the no-op.
6. **Number format**: cache `CompactDecimalFormat` per locale (`remember(locale)` in composables, or a small locale-keyed holder) keeping API 29 compatibility (ICU, see Spec 59 invariant 4).

## 3. Verification

- Layout Inspector: during chart drag, only the overlay text recomposes at index changes; chart Canvas does not recompose.
- Allocation profiler (Android Studio) during 5s drag: no `Path`/`Brush`/`Offset` list churn per event.
- Manual: details screen on phone portrait, tablet landscape list-detail (pane narrower than window), foldable Book/TableTop postures if an emulator is available; image width and dual-pane decision unchanged vs before.
- Profile chart: tapping/dragging at the far right on a non-300dp-wide chart highlights matching date/value.
- `./gradlew :androidApp:assembleDebug`, lint, `:androidApp:testDebugUnitTest`.

## 4. Acceptance Criteria

- [ ] Documented decision on `BoxWithConstraints` (kept/narrowed/replaced) with measurement; `LocalAdaptiveLayoutInfo` not used as a pane-width substitute.
- [ ] Details `ImageRequest`/URL remembered; `ixid` preserved.
- [ ] Both charts build geometry/brushes/path effects once per size+data (not per draw or per drag event).
- [ ] `dragX` not read in composition for the whole chart; header uses a derived index.
- [ ] Hover index matches the highlighted point at any canvas width (no hard-coded 300dp).
- [ ] No-op `animProgress` fixed or removed.
- [ ] `formatStatValue` no longer allocates a formatter per call.
- [ ] Zero compiler warnings, no dead code; Android build passes.

> [!NOTE]
> Line numbers in this spec are approximate (working tree had uncommitted edits at authoring time); locate code by symbol name.
