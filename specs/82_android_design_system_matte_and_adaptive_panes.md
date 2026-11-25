# Specification 82: Android Design System Foundation, Matte Tokens, Screen Decomposition & Pane-Aware Layout

**Status:** Accepted

* **Wave:** Wave 1 — Disjoint Foundations
* **Depends On:** None
* **Parallelizable With:** [Spec 81](81_kmp_color_engine_and_navigation_seed_cache.md), [Spec 83](83_apple_design_system_matte_and_light_mode_hygiene.md)
* **Research Mapping:** [Cross-Platform Material Craft Evaluation](researches/material_design_cross_platform_ui_craft_evaluation.md) — `ARCH-04` (foundation), `ARCH-05`, `ARCH-06`, Craft `2.1`, Craft `2.2` (Android), Craft `2.7` (Android Light Mode tokens)
* **Exclusive File Ownership:** `androidApp/src/main/java/com/example/imagefeed/android/designsystem/**`, `components/**`, `details/**`, `search/**`, `profile/**`, [`AdaptiveUtils.kt`](../androidApp/src/main/java/com/example/imagefeed/android/AdaptiveUtils.kt), `theme/PhotoThemePalette.kt`, and structural extractions in [`FeedScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/FeedScreen.kt), [`CollectionsFeedScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/CollectionsFeedScreen.kt), [`CollectionDetailScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/CollectionDetailScreen.kt), [`FavoritesScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/FavoritesScreen.kt), [`PhotoDetailsScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt), [`SearchScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt), [`UserProfileScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt), [`NavigationSections.kt`](../androidApp/src/main/java/com/example/imagefeed/android/navigation/NavigationSections.kt)

---

## 1. Overview & Codebase Baseline

Building on Compose BOM `2026.11.01` ([Spec 79](archive/79_jetpack_compose_1_13_and_m3_1_5_modernization.md)) and the ephemeral visual review baseline ([Spec 80](archive/80_cross_platform_ui_visual_review.md)), `androidApp` still carries structural and token debt that prevents parallel Wave-2 craft work:

1. **Monolithic Feature Files (`ARCH-05`)**: [`PhotoDetailsScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt) (`1,906` lines), [`SearchScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt) (`1,565` lines), and [`UserProfileScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt) (`1,215` lines) bundle layout, cards, charts, and sheets in single files, while `SharedTransitionScope` and `AnimatedVisibilityScope` are threaded manually through every composable signature.
2. **Pane-Width Blindness (`ARCH-05`)**: [`AdaptiveUtils.kt`](../androidApp/src/main/java/com/example/imagefeed/android/AdaptiveUtils.kt#L88-L135) computes grid columns and Retina CDN `itemWidthPx` from full-window `LocalAdaptiveLayoutInfo.current.screenWidthDp`, ignoring narrower list/detail panes under Navigation 3's `ListDetailSceneStrategy`.
3. **Matte Scrim & Token Hygiene (Craft `2.2` & `2.7`)**: [`PhotoCard`](../androidApp/src/main/java/com/example/imagefeed/android/FeedScreen.kt#L315-L456) overlays a full-width `0.75f` black gradient scrim across the bottom edge of every photograph; [`TopicCapsuleCard`](../androidApp/src/main/java/com/example/imagefeed/android/FeedScreen.kt#L507-L569) and Popular Topics ([`SearchScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L640-L661)) hardcode `Color.White.copy(alpha = 0.15f)` borders and dark slate hexes (`0xFF1E293B` / `0xFF0F172A`).
4. **Dead / Inconsistent Profile Navigation (`ARCH-06`)**: [`SearchScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L739-L741) passes `onUserClick = {}` to `PhotoCard`, while [`CollectionsFeedScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/CollectionsFeedScreen.kt#L513-L518) and [`CollectionDetailScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/CollectionDetailScreen.kt#L272-L276) launch external browser intents instead of navigating to `AppRoute.UserProfile`.

---

## 2. Scope & Technical Workstreams

### Workstream A (`ARCH-04`): Internal `designsystem/` Foundation
1. Create `androidApp/src/main/java/com/example/imagefeed/android/designsystem/`:
   * `SurfaceTokens.kt` (`MatteSurface`, semantic container/outline helpers adhering to [`.agents/skills/android-m3-design/SKILL.md`](../.agents/skills/android-m3-design/SKILL.md)).
   * `SatinHeaderScaffold.kt` + `Modifier.satinProgressiveBlur` implementing top-edge progressive `Modifier.blur` (`BlurRadiusSpec.verticalGradient`) strictly gated on `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU` (`API 33+`) and disabled when reduced motion / `LocalInspectionMode.current` is active, degrading cleanly on API 29–32 to a smooth multi-stop vertical `MaterialTheme.colorScheme.surface` alpha scrim.
   * `SharedTransitionLocals.kt` providing `LocalAppSharedTransition` (`SharedTransitionScope` + `AnimatedVisibilityScope`) and a centralized `SatinOcclusionController` state holder.
   * Clean placeholder/scaffold modules `FloatNavigationChrome.kt` and `ExpressiveShapes.kt` so Wave-2 Specs 84, 85, and 86 operate on disjoint files with zero merge conflicts.

### Workstream B (`ARCH-05`): `LocalPaneWidthDp` & Monolith Decomposition
1. Introduce `LocalPaneWidthDp` (measured at Navigation 3 pane boundaries with quantized Dp updates) and update [`AdaptiveUtils.kt`](../androidApp/src/main/java/com/example/imagefeed/android/AdaptiveUtils.kt#L88-L135) so `calculateGridColumns`, `calculateGridColumnCount`, `calculateCollectionGridColumns`, `calculateUserGridColumns`, `calculatePhotoItemWidthPx`, and `calculateCollectionMosaicCardWidthPx` read `LocalPaneWidthDp.current` (falling back to `LocalAdaptiveLayoutInfo.current.screenWidthDp` when unprovided). Add unit tests in `AdaptiveUtilsTest`.
2. Decompose monoliths into cohesive subpackages while keeping all public `*Screen` composable entry signatures intact:
   * `components/PhotoCard.kt`, `components/CollectionMosaicCard.kt`
   * `details/` (`PhotoCanvasPane.kt`, `PhotoInspectorPane.kt`, `PhotoActionToolbar.kt`, `StatisticsChart.kt`)
   * `search/` (`SearchHeader.kt`, `SearchDiscoveryView.kt`, `SearchFiltersSheet.kt`, `SearchResultCards.kt`)
   * `profile/` (`UserProfileHeader.kt`, `UserInsightsView.kt`, `UserProfileTabs.kt`)
   * `theme/PhotoThemePalette.kt` (extracting `PhotoSwatch`, `generatePhotoColorScheme`, `deriveTonalSwatches` out of [`Theme.kt`](../androidApp/src/main/java/com/example/imagefeed/android/theme/Theme.kt#L18-L225) so Spec 84 and Spec 86 do not touch the same theme file).

### Workstream C (Craft `2.2` & `2.7`): Matte Attribution Capsule & Light-Mode Token Hygiene
1. In `components/PhotoCard.kt`, replace the full-width `0.75f` black bottom gradient scrim with a compact Matte corner attribution capsule (high-contrast tonal scrim pill, zero live backdrop blur) that preserves clean photographic edges and accessible photographer tap targets.
2. Replace `Color.White.copy(alpha = 0.15f)` hairline borders and hardcoded `0xFF1E293B` / `0xFF0F172A` fallback gradients in `TopicCapsuleCard` ([`FeedScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/FeedScreen.kt#L507-L569)) and Popular Topics (`SearchDiscoveryView.kt`, extracted from [`SearchScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L640-L661)) with semantic `MaterialTheme.colorScheme` Matte tokens (`outlineVariant`, `secondaryContainer`, `surfaceContainerHigh`, `onSecondaryContainer`).

### Workstream D (`ARCH-06`): In-App `AppRoute.UserProfile` Routing Parity
1. Replace the no-op `onUserClick = {}` in `SearchScreen` `PhotosResultGrid` ([`SearchScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L739-L741)), curator clicks in [`CollectionsFeedScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/CollectionsFeedScreen.kt#L513-L518), and photographer clicks in [`CollectionDetailScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/CollectionDetailScreen.kt#L272-L276) with `onUserClick(User)` callbacks wired through [`NavigationSections.kt`](../androidApp/src/main/java/com/example/imagefeed/android/navigation/NavigationSections.kt) to `navigator.navigate(AppRoute.UserProfile(user.username))`.
2. Preserve explicit UTM referral browser intents on "Open in Web" actions inside detail and profile screens per [Business Rules](../wiki/business-rules.md#L12).

---

## 3. Constraints, Non-Goals, Wiki & Skill Impact

* **Zero Hardcoded Tokens**: Follow [`.agents/skills/android-m3-design/SKILL.md`](../.agents/skills/android-m3-design/SKILL.md); zero `Color.White` / `Color(0xFF...)` literals outside domain photo swatches.
* **Preserve `MainActivity.kt` Ownership**: Do not modify `MainActivity.kt` in this spec (`Spec 84` owns it); keep existing `SharedTransitionScope` / `AnimatedVisibilityScope` parameters on `*Screen` composables working (defaulting or bridged cleanly to `LocalAppSharedTransition`) until `Spec 85` finishes rollout.
* **Progressive Visual Review Readiness**: Add stateless `*ScreenContent` wrappers and `@Preview` functions (Compact Light, Compact Dark, Expanded `900×700`) only for screens directly modified in this spec per [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md).
* **Wiki Impact**:
  * [`wiki/design-systems-and-navigation.md`](../wiki/design-systems-and-navigation.md) — Document `designsystem/` package, `LocalPaneWidthDp`, `PhotoCard` Matte corner capsule, and consistent `AppRoute.UserProfile` routing.
  * [`wiki/performance-and-memory.md`](../wiki/performance-and-memory.md) — Document pane-width Retina CDN image sizing and zero-blur Matte card invariant.
* **Skill & Guideline Impact**:
  * [`.agents/skills/android-m3-design/SKILL.md`](../.agents/skills/android-m3-design/SKILL.md) — Document the `designsystem/` package (`Matte -> Satin -> Float` surface hierarchy, `MatteSurface`, API 33+-gated `Modifier.satinProgressiveBlur` with API 29–32 scrim fallback, `LocalAppSharedTransition`), `LocalPaneWidthDp` pane-width adaptive calculation in §4, and Matte corner attribution capsules replacing full-width photo scrims.
  * [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md) — Append newly preview-ready Android screens (`FeedScreen`, `CollectionsFeedScreen`, `SearchScreen`) under §4 `Currently Snapshot-Ready Screens`.

---

## 4. Verification & UI Visual Review Plan

* **Build, Unit Tests & Linters** (per [`.agents/skills/verification-and-linting/SKILL.md`](../.agents/skills/verification-and-linting/SKILL.md)):
  ```bash
  ./gradlew :androidApp:assembleDebug :androidApp:testDebugUnitTest :androidApp:compileDebugAndroidTestKotlin ktlintCheck detekt
  ```
* **Compose UI Tests** (verify existing tests pass on `PhotoDetailsScreen`, `SearchScreen`, `ExpressiveComponents`):
  ```bash
  ./gradlew :androidApp:connectedDebugAndroidTest
  ```
* **Mandatory Before / After UI Visual Review** (per [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md)):
  * Capture Before & After Android Compose Preview Sidecar renders (`Compact Light`, `Compact Dark`, `Expanded 900×700`) into `<Artifact Directory Path>/ui-review/spec-82/{before,after}/android/` for `FeedScreen`, `CollectionsFeedScreen`, `SearchScreen`, and `PhotoDetailsScreen`.
  * Inspect representative PNGs with `read_file` and present a Before/After visual comparison artifact for user approval prior to marking `Implemented (Final)`.

---

## 5. Acceptance Criteria

- [ ] `designsystem/` provides `SurfaceTokens.kt` (`MatteSurface`), `SatinHeaderScaffold.kt` + API 33+-gated `Modifier.satinProgressiveBlur` (with API 29–32 scrim fallback), `SharedTransitionLocals.kt`, and clean `FloatNavigationChrome.kt` / `ExpressiveShapes.kt` scaffolds.
- [ ] `LocalPaneWidthDp` feeds `AdaptiveUtils` so column counts and Retina CDN widths adapt accurately inside `ListDetailSceneStrategy` panes, verified by unit tests.
- [ ] `PhotoDetailsScreen`, `SearchScreen`, `UserProfileScreen`, shared `PhotoCard` / `CollectionMosaicCard`, and `PhotoThemePalette` are decomposed into clean subpackages with preserved screen behavior.
- [ ] `PhotoCard` uses a compact Matte corner attribution capsule, and `TopicCapsuleCard` / Popular Topics use semantic `MaterialTheme.colorScheme` tokens in Light and Dark themes.
- [ ] All photographer and curator taps across `SearchScreen`, `CollectionsFeedScreen`, and `CollectionDetailScreen` navigate in-app to `AppRoute.UserProfile(username)`.
- [ ] Before/After Compose Preview Sidecar visual review is approved, and `:androidApp:assembleDebug`, `:androidApp:testDebugUnitTest`, `:androidApp:connectedDebugAndroidTest`, `ktlintCheck`, and `detekt` pass with zero warnings.
- [ ] Affected [`wiki/`](../wiki/README.md) pages, [`.agents/skills/android-m3-design/SKILL.md`](../.agents/skills/android-m3-design/SKILL.md), and [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md) are updated.

---

## 6. Verification Record

### Verified
*(Pending implementation)*

### Unverified / Gaps
*(Pending implementation)*
