# Specification 86: Android Immersive Photo Details, Compact Pinch-to-Zoom & M3 Floating Action Toolbar

**Status:** Accepted

* **Wave:** Wave 2 — Parallel Feature & Craft Execution
* **Depends On:** [Spec 81](81_kmp_color_engine_and_navigation_seed_cache.md), [Spec 82](82_android_design_system_matte_and_adaptive_panes.md)
* **Parallelizable With:** [Spec 84](84_android_float_navigation_and_random_mvi.md), [Spec 85](85_android_satin_headers_and_shared_transitions.md), [Spec 87](87_apple_float_navigation_shell_and_macos_search.md), [Spec 88](88_apple_satin_headers_zoom_transitions_and_unified_routes.md), [Spec 89](89_apple_photo_details_inspector_parity_and_glass_morphing.md)
* **Research Mapping:** [Cross-Platform Material Craft Evaluation](researches/material_design_cross_platform_ui_craft_evaluation.md) — `ARCH-02` (Android adoption), Craft `2.5` (`HorizontalFloatingToolbar`), Craft `2.6` (bookmark morph & avatar transition), Craft `2.7` (Android Photo Details immersion)
* **Exclusive File Ownership:** `androidApp/src/main/java/com/example/imagefeed/android/details/**`, `androidApp/src/main/java/com/example/imagefeed/android/theme/PhotoThemePalette.kt`, [`UiPreviewFixtures.kt`](../androidApp/src/main/java/com/example/imagefeed/android/preview/UiPreviewFixtures.kt), `androidApp/src/androidTest/java/com/example/imagefeed/android/*`, [`baseline-prof.txt`](../androidApp/src/main/baseline-prof.txt)

---

## 1. Overview & Codebase Baseline

Comparing Android's decomposed `details/` subpackage ([Spec 82](82_android_design_system_matte_and_adaptive_panes.md), originating from [`PhotoDetailsScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt)) against Apple and [the UI craft evaluation](researches/material_design_cross_platform_ui_craft_evaluation.md) reveals four specific gaps:

1. **Missing Compact-Phone Pinch-to-Zoom (Craft `2.7`)**: [`PhotoCanvasPane`](../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt#L612-L733) gates `transformable` zoom inside `if (isDualPane)` — compact Android phones cannot pinch-zoom or double-tap-zoom photographs at all.
2. **Unguarded Progressive Blur & Opaque Chrome (Craft `2.3` & `2.7`)**: `Modifier.attributionBottomProgressiveBlur` ([`PhotoDetailsScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt#L827-L841), added in [Spec 79](archive/79_jetpack_compose_1_13_and_m3_1_5_modernization.md)) applies Compose 1.13 `BlurRadiusSpec` without checking `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU`, reduced motion, or `LocalInspectionMode`, while the screen uses an opaque top bar instead of floating edge-to-edge Satin chrome over an ambient `photo.color` canvas.
3. **Hand-Rolled Floating Toolbar & Duplicated HSL Math (`ARCH-02`, Craft `2.5` & `2.6`)**: [`FloatingActionToolbar`](../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt#L1457-L1614) hand-rolls a `CircleShape` pill instead of M3 1.5.0 `HorizontalFloatingToolbar`; [`Theme.kt`](../androidApp/src/main/java/com/example/imagefeed/android/theme/Theme.kt#L43-L225) (extracted to `PhotoThemePalette.kt`) duplicates HSL math now centralized in `:core:model` (`Spec 81`); and bookmark/avatar continuity lack expressive polish.
4. **Pending Baseline Profile Refresh**: [Spec 79](archive/79_jetpack_compose_1_13_and_m3_1_5_modernization.md#L110-L111) noted that `baseline-prof.txt` was not regenerated after Compose 1.13 adoption.

---

## 2. Scope & Technical Workstreams

### Workstream A (Craft `2.7` & `ARCH-02`): Compact Pinch-to-Zoom, Satin Overlay Bar & Shared Color Engine
1. Enable `1.0x–4.0x` pinch-to-zoom (`transformable` + pan bounds) and double-tap zoom toggle inside `details/PhotoCanvasPane.kt` on **compact phones** as well as dual-pane tablets/foldables, ensuring vertical scroll yields cleanly to parent scroll when `scale <= 1f` and disabling `attributionBottomProgressiveBlur` while zoomed (`scale > 1f`).
2. Gate `attributionBottomProgressiveBlur` strictly on `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU` (`API 33+`), normal motion scale, and `!LocalInspectionMode.current` (falling back gracefully to the tonal scrim on API 29–32).
3. Float a transparent **Satin** `CenterAlignedTopAppBar` edge-to-edge over the top of `PhotoCanvasPane`, backed by a subtle ambient tint derived from `photo.color` / `generatePhotoColorScheme`.
4. Refactor `theme/PhotoThemePalette.kt` so `deriveTonalSwatches` delegates directly to `:core:model`'s `PhotoColorEngine` (`Spec 81`), deleting duplicated `rgbToHsl` / `hslToColor` / `colorToHex` / `parseHexArgb` implementations.

### Workstream B (Craft `2.5` & `2.6`): M3 Expressive `HorizontalFloatingToolbar`, Bookmark Morph & Avatar Continuity
1. Replace the hand-rolled `FloatingActionToolbar` in `details/PhotoActionToolbar.kt` with Material 3 1.5.0 **`HorizontalFloatingToolbar`** configured with `FloatingToolbarDefaults.exitAlwaysScrollBehavior` and `vibrantFloatingToolbarColors()`, preserving the Full vs. RAW split download action (`PhotoDownloadTracker`), Share intent, and Bookmark action.
2. Apply `CircleShape` ↔ rounded-squircle `MorphPolygonShape` animation to the Bookmark toggle state inside the floating toolbar.
3. Attach `Modifier.sharedElement` (`key = "avatar_${photo.user.username}"`) via `LocalAppSharedTransition` (`Spec 82`) to the photographer avatar inside `PhotoCanvasPane` and `PhotoInspectorPane` for seamless continuity into `UserProfileScreen`.

### Workstream C (Verification & Baseline Profile Hygiene): `androidTest` & `baseline-prof.txt`
1. Extend Compose UI `androidTest` suites in `androidApp/src/androidTest/` to cover compact canvas double-tap/zoom state reset, `HorizontalFloatingToolbar` Full/RAW & Bookmark semantics, and `:core:model`-backed `SwatchPill` rendering.
2. Refresh or verify [`baseline-prof.txt`](../androidApp/src/main/baseline-prof.txt) against Compose 1.13 / M3 Expressive details codepaths (or document emulator/benchmark availability cleanly in `## 6. Verification Record`).

---

## 3. Constraints, Non-Goals, Wiki & Skill Impact

* **Disjoint File Ownership**: Do not edit `MainActivity.kt`, `Theme.kt` (`Spec 84`), or grid screens (`Spec 85`). Preserve public `PhotoDetailsScreen` signature compatibility with [`NavigationSections.kt`](../androidApp/src/main/java/com/example/imagefeed/android/navigation/NavigationSections.kt#L193-L210).
* **Unsplash Compliance (`ADR-02`)**: Both Full and RAW download actions must call `onTrackDownload` (`PhotoDownloadTracker.trackDownload`) and keep attribution legible on `PhotoCanvasPane`.
* **Inspection Mode Safety**: Keep `GoogleMap` `LocalInspectionMode.current` placeholder and update [`UiPreviewFixtures.kt`](../androidApp/src/main/java/com/example/imagefeed/android/preview/UiPreviewFixtures.kt) previews for `HorizontalFloatingToolbar` and `PhotoCanvasPane`.
* **Wiki Impact**:
  * [`wiki/design-systems-and-navigation.md`](../wiki/design-systems-and-navigation.md) — Document compact zoom, Satin overlay top bar, `HorizontalFloatingToolbar`, and bookmark morph on Android Photo Details.
  * [`wiki/performance-and-memory.md`](../wiki/performance-and-memory.md) — Document API 33+ blur gating on `attributionBottomProgressiveBlur` and refreshed `baseline-prof.txt`.
* **Skill & Guideline Impact**:
  * [`.agents/skills/android-m3-design/SKILL.md`](../.agents/skills/android-m3-design/SKILL.md) — Update §2 & §4 (`Dual-Pane & Full-Canvas Detail Views`) to document `HorizontalFloatingToolbar` (`exitAlwaysScrollBehavior` + `vibrantFloatingToolbarColors()`), compact + dual-pane `1.0x–4.0x` pinch/double-tap zoom with `1.0x` parent-scroll handoff, edge-to-edge Satin detail top bar, and `API 33+` gating on progressive blur modifiers.

---

## 4. Verification & UI Visual Review Plan

* **Build, Unit Tests & Linters** (per [`.agents/skills/verification-and-linting/SKILL.md`](../.agents/skills/verification-and-linting/SKILL.md)):
  ```bash
  ./gradlew :androidApp:assembleDebug :androidApp:testDebugUnitTest :androidApp:compileDebugAndroidTestKotlin ktlintCheck detekt
  ./gradlew :androidApp:connectedDebugAndroidTest
  ```
* **Mandatory Before / After UI Visual Review** (per [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md)):
  * Render `PhotoDetailsScreenPreview` (Compact Light), `PhotoDetailsDarkPreview` (Compact Dark), and `PhotoDetailsExpandedPreview` (`900×700`) via the Android Compose Preview Sidecar into `<Artifact Directory Path>/ui-review/spec-86/{before,after}/android/`.
  * Inspect rendered PNGs with `read_file` and obtain user approval on the visual comparison artifact before marking `Implemented (Final)`.

---

## 5. Acceptance Criteria

- [ ] Compact phones support `1.0x–4.0x` pinch-to-zoom and double-tap zoom in `PhotoCanvasPane` with clean parent-scroll handoff at `1.0x`.
- [ ] `attributionBottomProgressiveBlur` is strictly gated on `API 33+` / normal motion / `!LocalInspectionMode`, and `PhotoDetailsScreen` floats an edge-to-edge Satin `CenterAlignedTopAppBar` over an ambient `photo.color` canvas.
- [ ] `PhotoThemePalette.kt` delegates tonal swatch generation to `:core:model` `PhotoColorEngine` with zero duplicated HSL math.
- [ ] `PhotoActionToolbar` adopts M3 1.5.0 `HorizontalFloatingToolbar` (`exitAlwaysScrollBehavior`, `vibrantFloatingToolbarColors()`), Bookmark `MorphPolygonShape` feedback, and photographer avatar `sharedElement` continuity.
- [ ] Before/After Compose Preview Sidecar visual review is approved, and `:androidApp:assembleDebug`, `:androidApp:testDebugUnitTest`, `:androidApp:connectedDebugAndroidTest`, `ktlintCheck`, and `detekt` pass with zero warnings.
- [ ] Affected [`wiki/`](../wiki/README.md) pages and [`.agents/skills/android-m3-design/SKILL.md`](../.agents/skills/android-m3-design/SKILL.md) are updated.

---

## 6. Verification Record

### Verified
*(Pending implementation)*

### Unverified / Gaps
*(Pending implementation)*
