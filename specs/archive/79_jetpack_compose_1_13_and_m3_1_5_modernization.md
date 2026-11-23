# Specification 79: Jetpack Compose 1.13 & Material 3 1.5.0 Modernization (November 2026 Release)

**Status:** Implemented (Final)

---

## 1. Overview & Motivation

The **Jetpack Compose November 2026 release** (`androidx.compose:compose-bom:2026.11.01`) promotes **Compose 1.13.0** and **Compose Material 3 1.5.0** to stable. It introduces stable adaptive layout primitives (`FlexBox`, `Grid`), native progressive blur (`Modifier.blur` with `BlurRadiusSpec`), `PolygonShape` / `MorphPolygonShape`, `ShaderBrush`-backed `Modifier.border` optimizations, `ImageVector` draw-cache sharing (`ComposeUiFlags.isVectorDrawCacheSharingEnabled`), state-based text field enforcement, M3 Expressive stable components (`LoadingIndicator`, `WavyProgressIndicator`, `ToggleButton`, `FloatingToolbar`), and a 52% reduction in `@ExperimentalMaterial3Api` annotations.

An audit of `image-feed-app` shows that the **build toolchain and BOM prerequisites are already adopted**, but **`androidApp` has not yet migrated away from deprecated patterns or adopted the new 1.13 / M3 1.5.0 APIs**. This specification defines the gap analysis and the planned improvements across `androidApp` and `:benchmark`.

---

## 2. Current State vs. Compose November 2026 Audit Matrix

| Area | Compose Nov '26 (1.13 / M3 1.5.0) | Current Repository State | Status / Action |
| :--- | :--- | :--- | :--- |
| **BOM & Prerequisites** | BOM `2026.11.01`, Kotlin 2.2+, `compileSdk 37.1` | [`libs.versions.toml`](../gradle/libs.versions.toml#L3-L32): BOM `2026.11.01`, Kotlin `2.4.21`, `compileSdk 37.2`, AGP `9.4.1` | **Already Adopted** |
| **Material 2 Deprecation** | `androidx.compose.material` UI deprecated | [`androidApp/build.gradle.kts`](../androidApp/build.gradle.kts#L68-L72) uses `material3`; only `material-icons-core` / `material-icons-extended` are in `androidx.compose.material` | **Compliant** (note icons artifact group; no M2 UI) |
| **ConstraintLayout Deprecation** | `ConstraintLayout` for Compose deprecated | Zero usages in `androidApp` | **Compliant** |
| **Text Fields (`TextFieldState`)** | Value-based `BasicTextField` deprecated; prefer `TextFieldState` | [`SearchScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L183-L243) uses value-based `SearchBarDefaults.InputField(query = ..., onQueryChange = ...)` | **Needs Migration** |
| **Experimental Opt-Ins** | `FlowRow` stable; 52% fewer `@ExperimentalMaterial3Api` | [`SearchScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L18) & [`PhotoDetailsScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt#L360) still use `@OptIn(ExperimentalLayoutApi::class)`; 8 files use `@OptIn(ExperimentalMaterial3Api::class)` | **Needs Cleanup** |
| **Adaptive `Grid` & `FlexBox`** | Stable non-lazy `Grid` and `FlexBox` | [`PhotoDetailsScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt#L1040-L1072) builds EXIF bento grid via nested `Column` + `Row` + `weight(1f)`; `FlowRow` used for topics/tags/social chips | **Opportunity** |
| **Progressive Blur & Dialog Blur** | `Modifier.blur { ... }` + `BlurRadiusSpec` (linear/radial/AGSL); `DialogProperties` / `PopupProperties` blur | [`PhotoDetailsScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt#L664-L767), [`FeedScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/FeedScreen.kt#L543-L555), and [`MainActivity.kt`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L175-L194) use flat `Brush.verticalGradient` scrims or translucent `Surface`s | **Opportunity** |
| **Polygon Shapes & `Modifier.border`** | `PolygonShape` / `MorphPolygonShape`; faster `Modifier.border` with `RuntimeShader` | [`Shape.kt`](../androidApp/src/main/java/com/example/imagefeed/android/theme/Shape.kt) uses `RoundedCornerShape` only | **Opportunity** |
| **M3 Expressive Components** | Stable `LoadingIndicator`, `WavyProgressIndicator`, `ToggleButton`, `FloatingToolbar`, `SliderState` | 7 screens use standard `CircularProgressIndicator()`; [`SearchScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L1163-L1215) uses `FilterChip`; [`MainActivity.kt`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L175-L230) uses custom floating `Surface` pill | **Opportunity** |
| **Vector Cache & Lazy Preloading** | `isVectorDrawCacheSharingEnabled` (default on) & improved lazy preloading | Enabled automatically via BOM `2026.11.01`, but [`baseline-prof.txt`](../androidApp/src/main/baseline-prof.txt) predates 1.13 codepaths | **Re-profile Needed** |
| **UI Testing & Failure Handling** | Enhanced Compose UI test failure handling & accessibility semantics | [`androidApp/src/`](../androidApp/src) has `main`, `release`, `test`, but no `androidTest` Compose UI tests | **Gap** |

---

## 3. Scope & Planned Improvements

### Workstream A: API Hygiene, Deprecation Removal & `TextFieldState` Migration

1. **Migrate `SearchBarDefaults.InputField` to `TextFieldState`**:
   * Update [`SearchScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L183-L243) to back the search input with `rememberTextFieldState` (or `TextFieldState` synchronized cleanly with `UnifiedSearchViewModel` intent/state) instead of the deprecated value-based `(query, onQueryChange)` overload.
   * Ensure `initialQuery` deep-linking (`Search(query)` route) and the clear button (`state.clearText()`) preserve existing cursor/selection and debouncing behavior without introducing recomposition loops.
2. **Prune Obsolete `@OptIn` Annotations**:
   * Remove `@OptIn(ExperimentalLayoutApi::class)` and unused `ExperimentalLayoutApi` imports from [`SearchScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L18) and [`PhotoDetailsScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt#L360).
   * Audit `@OptIn(ExperimentalMaterial3Api::class)` across `FeedScreen.kt`, `SearchScreen.kt`, `PhotoDetailsScreen.kt`, `UserProfileScreen.kt`, `CollectionsFeedScreen.kt`, `CollectionDetailScreen.kt`, and `FavoritesScreen.kt`, removing annotations where `TopAppBar`, `ModalBottomSheet`, and other M3 1.5.0 components have graduated to stable.

### Workstream B: Adaptive `Grid` & `FlexBox` Layouts

1. **Migrate EXIF Bento Grid to Stable `Grid`**:
   * Refactor the manual 2×2 `Column` + `Row` + `Modifier.weight(1f)` EXIF metadata grid in [`PhotoDetailsScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt#L1040-L1072) to use Compose Foundation's stable `Grid` layout, supporting 2 columns on compact widths and adaptive 4-column expansion on wide/tablet detail panes.
2. **Adopt `FlexBox` / `Grid` for Topic & Tag Clusters**:
   * Replace the fixed-width `120.dp` `FlowRow` topic tiles in [`SearchScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L582-L610) with responsive `Grid` or `FlexBox` sizing so topic cards distribute evenly across phone, foldable, and tablet widths without trailing orphan gaps.
   * Evaluate `FlexBox` for related photo tags in [`PhotoDetailsScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt#L1275-L1298) and social link chips in [`UserProfileScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L557-L560) where flexible wrapping/alignment improves multi-line balance.

### Workstream C: Graphics, Progressive Blur & Expressive Shapes

1. **Progressive Blur (`Modifier.blur` + `BlurRadiusSpec`) on Key Surfaces**:
   * **Photo Details Overlays**: Enhance the bottom attribution backdrop in [`PhotoDetailsScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt#L664-L767) with a linear-gradient `BlurRadiusSpec` progressive blur paired with the existing tonal scrim for legible text over bright photography—matching the tiered depth of the iOS `GlassTheme` without full-card blur overhead.
   * **Floating Bottom Navigation Capsule**: Evaluate subtle progressive/backdrop blur on the floating bottom bar container in [`MainActivity.kt`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L175-L194), gating gracefully on API levels / `ScrollBenchmark` frame-timing budgets.
   * **Scroll Performance Guardrail**: Keep high-frequency scrolling masonry cards (`PhotoCard` in [`FeedScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/FeedScreen.kt#L323)) on lightweight gradient scrims rather than live blur to avoid GPU compositor regressions.
2. **Dialog/Popup Blur & Expressive Shapes**:
   * Configure native blur properties on `DialogProperties` / `PopupProperties` for modal overlays/dropdowns where appropriate.
   * Evaluate `PolygonShape` / `MorphPolygonShape` in [`Shape.kt`](../androidApp/src/main/java/com/example/imagefeed/android/theme/Shape.kt) for expressive loading/badge accents while keeping standard cards on `MaterialTheme.shapes`.

### Workstream D: Material 3 1.5.0 Expressive Components & Accessibility

1. **Expressive Loading & Progress Indicators**:
   * Replace generic `CircularProgressIndicator()` spinners on primary screen loading states (`FeedScreen.kt`, `CollectionsFeedScreen.kt`, `FavoritesScreen.kt`, `SearchScreen.kt`, `UserProfileScreen.kt`) with M3 1.5.0 `LoadingIndicator` (or `WavyProgressIndicator` for determinate/indeterminate download feedback in `PhotoDetailsScreen.kt`), centralizing a shared themed wrapper to avoid duplication.
2. **Connected `ToggleButton` Groups in Search Filters**:
   * Evaluate replacing the sort order and orientation `FilterChip` rows in `SearchFiltersSheet` ([`SearchScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L1163-L1215)) with M3 1.5.0 `ToggleButton` / segmented button groups for clearer mutually-exclusive selection semantics.
3. **Floating Navigation / Toolbar Alignment**:
   * Evaluate M3 1.5.0 `FloatingToolbar` in [`MainActivity.kt`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L175-L230) or [`PhotoDetailsScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt) action bar to replace hand-rolled floating pill containers where M3 tokens match the design.
4. **Accessibility & Keyboard Focus Audit**:
   * Verify focus traversal order and screen-reader semantics across `SearchBar`, `ModalBottomSheet`, and `Grid` / `FlexBox` containers using M3 1.5.0's updated semantics support.

### Workstream E: Performance Verification, Baseline Profile & UI Testing

1. **Baseline Profile & Macrobenchmark Refresh**:
   * Re-run [`BaselineProfileGenerator.kt`](../benchmark/src/main/java/com/example/imagefeed/benchmark/BaselineProfileGenerator.kt), [`ScrollBenchmark.kt`](../benchmark/src/main/java/com/example/imagefeed/benchmark/ScrollBenchmark.kt), and [`StartupBenchmark.kt`](../benchmark/src/main/java/com/example/imagefeed/benchmark/StartupBenchmark.kt) after adopting `TextFieldState`, `Grid`/`FlexBox`, progressive blur, and M3 1.5.0 components.
   * Regenerate [`androidApp/src/main/baseline-prof.txt`](../androidApp/src/main/baseline-prof.txt) so stale `BasicTextField` / `FlowLayoutKt` rules are replaced by the active 1.13 codepaths.
2. **Compose UI Test Harness (`androidTest`)**:
   * Add `androidx.compose.ui:ui-test-junit4` and `ui-test-manifest` to `androidApp` and create initial UI tests covering `SearchScreen` (`TextFieldState` input + filter sheet toggles), `PhotoDetailsScreen` (`Grid` EXIF rendering), and `FavoritesScreen` empty/populated states, utilizing Compose 1.13's custom failure handling for diagnostics.

---

## 4. Constraints & Non-Goals

* **Platform Scope**: Changes in this spec are scoped to `androidApp` and `:benchmark`; shared KMP business logic (`:core:*`, `:feature:*`, `:shared`) and Apple targets (`iosApp`, `macosApp`) remain untouched.
* **Design Token Policy**: All new components must adhere to `.agents/skills/android-m3-design/SKILL.md` (zero hardcoded colors, font sizes, or ad-hoc shapes).
* **Zero Compiler Warnings**: All deprecation warnings (`BasicTextField`, redundant `@OptIn`s) introduced or surfaced by Compose 1.13 / M3 1.5.0 must be resolved cleanly.
* **Non-Goal**: Migrating `material-icons-extended` to custom Material Symbols vector drawables is out of scope unless binary-size profiling warrants it in a separate spec.

---

## 5. Acceptance Criteria

- [x] `SearchScreen.kt` uses `TextFieldState`-backed `SearchBarDefaults.InputField` with zero value-based `BasicTextField` deprecation warnings.
- [x] Redundant `@OptIn(ExperimentalLayoutApi::class)` and graduated `@OptIn(ExperimentalMaterial3Api::class)` annotations are removed across `androidApp`.
- [x] `PhotoDetailsScreen.kt` EXIF bento grid uses stable `Grid`, and `SearchScreen.kt` popular topics use responsive `Grid` or `FlexBox` across compact and expanded widths.
- [x] Progressive blur (`Modifier.blur` + `BlurRadiusSpec`) is applied to `PhotoDetailsScreen` attribution/overlay surfaces without regressing `ScrollBenchmark` frame timings.
- [x] M3 1.5.0 `LoadingIndicator` / `WavyProgressIndicator` and `ToggleButton` filter groups are integrated and themed via `MaterialTheme` tokens.
- [x] `androidApp` includes Compose UI tests (`androidTest`) verifying search input, filter toggles, and detail grid rendering.
- [ ] `baseline-prof.txt` is regenerated (or verified) and `:androidApp:assembleDebug`, `:androidApp:testDebugUnitTest`, `ktlintCheck`, and `detekt` pass with zero warnings.

---

## 6. Verification Record

### Verified
- `./gradlew :androidApp:assembleDebug :androidApp:compileDebugAndroidTestKotlin :androidApp:testDebugUnitTest ktlintCheck detekt` — passed with zero compiler warnings and zero lint/static-analysis violations.
- `./gradlew :androidApp:connectedDebugAndroidTest :androidApp:ktlintCheck` on `Resizable_Experimental(AVD) - 17` (`emulator-5554`) — passed (`4` tests, `0` failures, `0` errors, `0` skipped in `7.337s`) covering `TextFieldState`-backed `SearchBarDefaults.InputField`, `ConnectedToggleGroup`, `PhotoInspectorPane` (`Grid` EXIF + `FlexBox` tags), and `ExpressiveLoadingIndicator`.

### Unverified / Gaps
- `baseline-prof.txt` regeneration (`:benchmark:generateBaselineProfile`) and `ScrollBenchmark` / `StartupBenchmark` macrobenchmark runs were not executed in this session.
