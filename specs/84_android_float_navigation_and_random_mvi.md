# Specification 84: Android Float Glass Navigation, Expressive Motion & Random Photo MVI

**Status:** Accepted

* **Wave:** Wave 2 — Parallel Feature & Craft Execution
* **Depends On:** [Spec 81](81_kmp_color_engine_and_navigation_seed_cache.md), [Spec 82](82_android_design_system_matte_and_adaptive_panes.md)
* **Parallelizable With:** [Spec 85](85_android_satin_headers_and_shared_transitions.md), [Spec 86](86_android_immersive_photo_details_and_zoom.md), [Spec 87](87_apple_float_navigation_shell_and_macos_search.md), [Spec 88](88_apple_satin_headers_zoom_transitions_and_unified_routes.md), [Spec 89](89_apple_photo_details_inspector_parity_and_glass_morphing.md)
* **Research Mapping:** [Cross-Platform Material Craft Evaluation](researches/material_design_cross_platform_ui_craft_evaluation.md) — `ARCH-03` (Android completion), `ARCH-04` (Float chrome), Craft `2.5` (Android nav), Craft `2.6` (`PolygonShape` / `MorphPolygonShape`), Craft `2.7` (bottom bar Light Mode fix)
* **Exclusive File Ownership:** [`MainActivity.kt`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt), `androidApp/src/main/java/com/example/imagefeed/android/designsystem/FloatNavigationChrome.kt`, `androidApp/src/main/java/com/example/imagefeed/android/designsystem/ExpressiveShapes.kt`, [`Theme.kt`](../androidApp/src/main/java/com/example/imagefeed/android/theme/Theme.kt), [`Shape.kt`](../androidApp/src/main/java/com/example/imagefeed/android/theme/Shape.kt), [`PhotoDetailPlaceholder.kt`](../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailPlaceholder.kt), [`Metro.kt`](../shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt) (removing `getFeedRepository()`)

---

## 1. Overview & Codebase Baseline

[`MainActivity.kt`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt) currently violates three core architectural and Material 3 Expressive standards identified in [the UI craft evaluation](researches/material_design_cross_platform_ui_craft_evaluation.md):

1. **Hand-Rolled Faux-Glass Nav & Tween Transitions (Craft `2.5` & `2.7`)**: Lines [`175–238`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L175-L238) render a custom `CircleShape` bottom pill with hardcoded `Color.White.copy(alpha = 0.20f)` borders; lines [`124–160`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L124-L160) render a bare static `NavigationRail`; and lines [`311–357`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L311-L357) drive `NavDisplay` transitions with `FastOutSlowInEasing` tweens instead of Material 3 Expressive `MotionScheme` springs.
2. **Direct Repository Leak (`ARCH-03`)**: [`MainActivity.kt`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L87) injects `FeedRepository` directly via [`MetroHelper.getFeedRepository()`](../shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt#L100-L102) and launches imperative `lifecycleScope.launch` + `Toast.makeText` inside [`handleShake()`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L380-L400), bypassing [`RandomPhotoViewModel.kt`](../feature/feed/src/commonMain/kotlin/com/example/imagefeed/presentation/random/RandomPhotoViewModel.kt).
3. **Static Empty-State & Randomize Chrome (Craft `2.6`)**: [`PhotoDetailPlaceholder.kt`](../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailPlaceholder.kt#L38-L43) and the rail randomize trigger use plain icons without Compose 1.13 `PolygonShape` / `MorphPolygonShape` craft.

---

## 2. Scope & Technical Workstreams

### Workstream A (Craft `2.5`): M3 Expressive Float Navigation & `MotionScheme` Springs
1. Implement `FloatNavigationChrome.kt` (scaffolded in Spec 82) and adopt it in [`MainActivity.kt`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L123-L241):
   * Compact width (`< 600dp`): Replace the hand-rolled `CircleShape` bottom pill and `Color.White` border with Material 3 Expressive `ShortNavigationBar` / floating tonal navigation chrome grounded in `MaterialTheme.colorScheme.surfaceContainerHigh` + `outlineVariant` and tonal elevation (full Light/Dark parity).
   * Medium/Expanded widths (`>= 600dp`): Upgrade `NavigationRail` to M3 Expressive `WideNavigationRail` with collapsible/expressive item styling and safe-drawing insets.
2. Upgrade `ImageFeedTheme` in [`Theme.kt`](../androidApp/src/main/java/com/example/imagefeed/android/theme/Theme.kt#L228-L247) to `MaterialExpressiveTheme` (using Compose Material 3 `1.5.0` from BOM `2026.11.01`) with expressive spatial and effects `MotionScheme` springs.
3. Replace `tween(durationMillis = 350, easing = FastOutSlowInEasing)` in `NavDisplay` `transitionSpec`, `popTransitionSpec`, and `predictivePopTransitionSpec` ([`MainActivity.kt`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L311-L357)) with `MaterialTheme.motionScheme` physics springs while preserving directional `NavigationEvent.EDGE_LEFT` predictive-back behavior.

### Workstream B (`ARCH-03` Completion): `RandomPhotoViewModel` Integration & Escape-Hatch Removal
1. Replace `MetroHelper.getFeedRepository()`, `isFetchingRandom`, `lifecycleScope.launch`, and `Toast.makeText` in [`MainActivity.kt`](../androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L87-L93) with `RandomPhotoViewModel` instantiated via `MetroHelper.getRandomPhotoViewModel(autoFetchOnInit = false)` (`Spec 81`) scoped cleanly via `viewModel`.
2. Dispatch `RandomPhotoIntent.FetchRandomPhoto()` on accelerometer shake and rail/feed randomize triggers; collect `RandomPhotoEffect.PhotoFetched` to seed `NavigationPreviewCache` (`Spec 81`) before calling `navigator.navigate(AppRoute.PhotoDetails(photoId))`; and map `RandomPhotoEffect.FetchFailed` via [`AppErrorLocalization.kt`](../androidApp/src/main/java/com/example/imagefeed/android/util/AppErrorLocalization.kt) into an M3 `SnackbarHost`.
3. Delete `MetroHelper.getFeedRepository()` from [`Metro.kt`](../shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt#L100-L102) and verify no Kotlin or Swift call sites remain.

### Workstream C (Craft `2.6`): `PolygonShape` / `MorphPolygonShape` Primitives
1. Implement Compose 1.13+ `PolygonShape` and `MorphPolygonShape` helpers in `ExpressiveShapes.kt` (and semantic shape helpers in [`Shape.kt`](../androidApp/src/main/java/com/example/imagefeed/android/theme/Shape.kt)) strictly for controls, badges, and empty-state backdrops (never clipping photographs into polygons).
2. Add an 8-sided camera-aperture `PolygonShape` randomize container (`ApertureRandomizeButton`) that morphs/rotates smoothly while `RandomPhotoViewModel` `state.isLoading` is `true`, wiring it into the `WideNavigationRail` header in `MainActivity.kt` and exporting it cleanly for `FeedScreen` (`Spec 85`).
3. Apply a warm organic soft-scallop `PolygonShape` container backdrop behind the icon in [`PhotoDetailPlaceholder.kt`](../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailPlaceholder.kt#L38-L43).

---

## 3. Constraints, Non-Goals, Wiki & Skill Impact

* **File Ownership Boundary**: Do not edit feature screens (`FeedScreen`, `SearchScreen`, `UserProfileScreen`, `PhotoDetailsScreen`, etc.), which are owned concurrently by Specs 85 and 86.
* **Strict MVI Boundary**: `androidApp` must contain zero direct repository imports or calls after removing `MetroHelper.getFeedRepository()`.
* **Accessibility**: Respect `ANIMATOR_DURATION_SCALE` / reduced motion on `ApertureRandomizeButton` rotation and spring specs.
* **Wiki Impact**:
  * [`wiki/design-systems-and-navigation.md`](../wiki/design-systems-and-navigation.md) — Document `MaterialExpressiveTheme`, `ShortNavigationBar`, `WideNavigationRail`, spring `NavDisplay` specs, and `PolygonShape` primitives.
  * [`wiki/architecture-and-modules.md`](../wiki/architecture-and-modules.md) — Document removal of `MetroHelper.getFeedRepository()` and completion of Shared MVI boundary for random photo discovery.
* **Skill & Guideline Impact**:
  * [`.agents/skills/android-m3-design/SKILL.md`](../.agents/skills/android-m3-design/SKILL.md) — Update §1.C (`Shape Scale`) with the `PolygonShape` / `MorphPolygonShape` policy (permitted strictly for micro-controls, badges, and empty-state containers in `ExpressiveShapes.kt`; never clip photographs), and update §2 & §4 to specify `MaterialExpressiveTheme` + `MotionScheme` physics springs, `ShortNavigationBar` (`< 600dp`), and `WideNavigationRail` (`>= 600dp`).
  * [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md) — Record `FloatNavigationChrome` and `PhotoDetailPlaceholder` preview coverage in §4.

---

## 4. Verification & UI Visual Review Plan

* **Build, Unit Tests & Linters** (per [`.agents/skills/verification-and-linting/SKILL.md`](../.agents/skills/verification-and-linting/SKILL.md); touching `Metro.kt` in `:shared` requires iOS build verification too):
  ```bash
  ./gradlew allTests :androidApp:assembleDebug :androidApp:testDebugUnitTest ktlintCheck detekt
  xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
  ```
* **Mandatory Before / After UI Visual Review** (per [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md)):
  * Add stateless `@Preview` functions for `FloatNavigationChrome` (Compact Light, Compact Dark, Expanded `WideNavigationRail`) and `PhotoDetailPlaceholder` (Light/Dark), capturing Before/After PNGs via the Android Compose Preview Sidecar into `<Artifact Directory Path>/ui-review/spec-84/{before,after}/android/`.
  * Inspect rendered PNGs with `read_file` and obtain user approval on the comparison artifact before marking `Implemented (Final)`.

---

## 5. Acceptance Criteria

- [ ] `FloatNavigationChrome.kt` replaces the hand-rolled `CircleShape` bottom pill (`Color.White` border) with M3 Expressive `ShortNavigationBar` / tonal chrome and upgrades the tablet rail to `WideNavigationRail`.
- [ ] `ImageFeedTheme` uses `MaterialExpressiveTheme` and `NavDisplay` uses spring `MotionScheme` specs while preserving directional predictive back.
- [ ] `MainActivity` routes shake and randomize triggers through `RandomPhotoViewModel` (`autoFetchOnInit = false`) with `NavigationPreviewCache` seeding and M3 `SnackbarHost` error localization; `MetroHelper.getFeedRepository()` is deleted.
- [ ] `ExpressiveShapes.kt` provides `PolygonShape` / `MorphPolygonShape` powering `ApertureRandomizeButton` and `PhotoDetailPlaceholder`'s soft-scallop backdrop.
- [ ] Before/After Compose Preview Sidecar visual review is approved, and `./gradlew allTests :androidApp:assembleDebug ktlintCheck detekt` + `iosApp` `xcodebuild` pass with zero compiler warnings.
- [ ] Affected [`wiki/`](../wiki/README.md) pages, [`.agents/skills/android-m3-design/SKILL.md`](../.agents/skills/android-m3-design/SKILL.md), and [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md) are updated.

---

## 6. Verification Record

### Verified
*(Pending implementation)*

### Unverified / Gaps
*(Pending implementation)*
