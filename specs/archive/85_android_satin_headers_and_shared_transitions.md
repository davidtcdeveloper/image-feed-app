# Specification 85: Android Feathering Satin Headers, Occlusion Masking & Avatar Shared Transitions

**Status:** Implemented (Final)

* **Wave:** Wave 2 — Parallel Feature & Craft Execution
* **Depends On:** [Spec 81](81_kmp_color_engine_and_navigation_seed_cache.md), [Spec 82](82_android_design_system_matte_and_adaptive_panes.md)
* **Parallelizable With:** [Spec 84](84_android_float_navigation_and_random_mvi.md), [Spec 86](86_android_immersive_photo_details_and_zoom.md), [Spec 87](87_apple_float_navigation_shell_and_macos_search.md), [Spec 88](88_apple_satin_headers_zoom_transitions_and_unified_routes.md), [Spec 89](89_apple_photo_details_inspector_parity_and_glass_morphing.md)
* **Research Mapping:** [Cross-Platform Material Craft Evaluation](researches/material_design_cross_platform_ui_craft_evaluation.md) — `ARCH-01` (Android seeding), `ARCH-04` (Satin & occlusion rollout), Craft `2.3` (Android), Craft `2.4` (Android), Craft `2.6` (Android avatar transitions & grid micro-morphs)
* **Exclusive File Ownership:** [`FeedScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/FeedScreen.kt), [`CollectionsFeedScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/CollectionsFeedScreen.kt), [`CollectionDetailScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/CollectionDetailScreen.kt), [`FavoritesScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/FavoritesScreen.kt), `androidApp/src/main/java/com/example/imagefeed/android/search/**`, `androidApp/src/main/java/com/example/imagefeed/android/profile/**`, `androidApp/src/main/java/com/example/imagefeed/android/components/**`, `androidApp/src/main/java/com/example/imagefeed/android/designsystem/SatinHeaderScaffold.kt`, `androidApp/src/main/java/com/example/imagefeed/android/designsystem/SharedTransitionLocals.kt`, [`NavigationSections.kt`](../androidApp/src/main/java/com/example/imagefeed/android/navigation/NavigationSections.kt)

---

## 1. Overview & Codebase Baseline

With `designsystem/` primitives and decomposed screen packages established in [Spec 82](82_android_design_system_matte_and_adaptive_panes.md) and `NavigationPreviewCache` available from [Spec 81](81_kmp_color_engine_and_navigation_seed_cache.md), Android's grid and collection screens still exhibit three visual seams identified in [the UI craft evaluation](researches/material_design_cross_platform_ui_craft_evaluation.md):

1. **Rigid Top-Bar Clipping (Craft `2.3`)**: [`FeedScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/FeedScreen.kt#L168-L246), [`CollectionsFeedScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/CollectionsFeedScreen.kt#L142-L186), [`FavoritesScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/FavoritesScreen.kt#L105-L137), [`SearchScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt), and [`UserProfileScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt) apply `Modifier.padding(paddingValues)` to outer wrappers, hard-clipping masonry grids beneath opaque top bars, while [`CollectionDetailScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/CollectionDetailScreen.kt#L180-L194) snaps between `Color.Transparent` and `0.95f` surface opacity.
2. **Satin Occlusion Pop & Cover Backdrop Snap (Craft `2.4`)**: When popping back from `PhotoDetailsScreen`, any grid card tucked behind the top header animates inside the shared-element overlay and snaps into header blur upon completion. Additionally, [`CollectionDetailScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/CollectionDetailScreen.kt#L346-L350) chains `.sharedBounds(...).blur(20.dp)` statically, causing an abrupt blur pop.
3. **Missing Avatar Hero Continuity & Micro-Morphs (Craft `2.6`)**: Photographer avatars cut sharply into `UserProfileScreen`, `FeedScreen` still shows `Icons.Default.Lightbulb` for randomize, `FavoritesScreen` empty state uses an unframed icon, and `SearchColorDial` snaps selection without morphing.

---

## 2. Scope & Technical Workstreams

### Workstream A (Craft `2.3`): Edge-to-Edge Feathering Satin Headers
1. Complete `SatinHeaderScaffold.kt` and adopt it across `FeedScreen`, `CollectionsFeedScreen`, `CollectionDetailScreen`, `FavoritesScreen`, `search/`, and `profile/`:
   * Pass measured header height into `LazyVerticalStaggeredGrid(contentPadding = ...)` rather than clipping outer containers so photography scrolls edge-to-edge behind transparent top chrome.
   * Drive `Modifier.satinProgressiveBlur` (`BlurRadiusSpec.verticalGradient` on API 33+, smooth multi-stop surface scrim on API 29–32) via draw-phase snapshot lambdas reading scroll offset and header height — zero recompositions during flings (honoring Spec 62).
   * Connect `TopAppBarDefaults.enterAlwaysScrollBehavior()` plus quick-return collapse on compact `FeedScreen` topic strip and `SearchScreen` color dial/tabs, and offset `PullToRefreshBox` indicator top padding below the Satin header.
   * Replace `CollectionDetailScreen`'s discrete `showCollapsedTitle` color jump with continuous scroll-linked Satin feathering.

### Workstream B (Craft `2.4` & `ARCH-01`): Satin Occlusion Masking, `CollectionDetail` Fix & Frame-0 Seeding
1. Provide `LocalAppSharedTransition` (`SharedTransitionLocals.kt`) across [`NavigationSections.kt`](../androidApp/src/main/java/com/example/imagefeed/android/navigation/NavigationSections.kt) and all 5 photo grids (`Feed`, `Favorites`, `Search`, `CollectionDetail`, `UserProfile`), retiring manual `SharedTransitionScope` / `AnimatedVisibilityScope` parameter plumbing from internal card hierarchies.
2. Wire `SatinOcclusionController` to track `isTransitionSource` bounds against the Satin header's bottom edge: when `sharedTransitionScope.isTransitionActive` and a returning photo intersects the header region (including interactive Predictive Back scrubs), hold Satin blur at `0.dp` (scrim-only) and feather blur smoothly back over `450–650ms` after transition settlement.
3. Animate `CollectionDetailHeader` backdrop blur ([`CollectionDetailScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/CollectionDetailScreen.kt#L346-L350)) smoothly across the `col_cover_*` shared transition instead of chaining static `.blur(20.dp)`.
4. Seed `NavigationPreviewCache` (`Spec 81`) in [`NavigationSections.kt`](../androidApp/src/main/java/com/example/imagefeed/android/navigation/NavigationSections.kt) right before navigating to `AppRoute.PhotoDetails(photo.id)` so Frame 0 renders immediately across all 5 grids.

### Workstream C (Craft `2.6`): Photographer Avatar Shared Transitions & Expressive Micro-Morphs
1. Add `Modifier.sharedElement` (`key = "avatar_${user.username}"`, gated by `isTransitionSource`) from `components/PhotoCard.kt`, `CollectionDetailScreen`, and `search/UserRowCard` into the `88.dp` profile header avatar in `profile/UserProfileScreen.kt`.
2. Replace `Icons.Default.Lightbulb` in [`FeedScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/FeedScreen.kt#L183) with an 8-sided camera-aperture `PolygonShape` randomize button (self-contained or shared via `ExpressiveShapes.kt` scaffolded in Spec 82, zero cross-spec edits to `MainActivity.kt`).
3. Add a soft-scallop `PolygonShape` container behind the empty-state icon in [`FavoritesScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/FavoritesScreen.kt#L149-L155), and apply `CircleShape` ↔ rounded-squircle morphing to the active swatch in `search/SearchColorDial`.

---

## 3. Constraints, Non-Goals, Wiki & Skill Impact

* **Disjoint Ownership**: Do not touch `MainActivity.kt` (`Spec 84`), `details/**` (`Spec 86`), or `ExpressiveShapes.kt` implementation owned by `Spec 84` (consume only the `Spec 82` contract or define screen-local aperture/scallop shapes cleanly if independent).
* **120Hz GPU Budget (`ADR-07`)**: Never apply `Modifier.blur` inside `PhotoCard` or `CollectionMosaicCard` grid items; progressive blur runs strictly once at the top-level scrollable edge container on API 33+.
* **Wiki Impact**:
  * [`wiki/design-systems-and-navigation.md`](../wiki/design-systems-and-navigation.md) — Document edge-to-edge Satin headers, `SatinOcclusionController` (450–650ms feather), avatar shared elements, and grid micro-morphs.
  * [`wiki/performance-and-memory.md`](../wiki/performance-and-memory.md) — Document draw-phase Satin scroll lambda and API 33+ blur gating.
* **Skill & Guideline Impact**:
  * [`.agents/skills/android-m3-design/SKILL.md`](../.agents/skills/android-m3-design/SKILL.md) — Update §2 (`App Bars`) and §3.5 (`Shared Element Transitions`) to mandate `SatinHeaderScaffold` edge-to-edge `contentPadding` + draw-phase `satinProgressiveBlur`, `LocalAppSharedTransition` (retiring manual scope parameter threading), `SatinOcclusionController` (`450–650ms` blur feather on occluded Predictive Back pops), `NavigationPreviewCache` seeding, and `avatar_${username}` shared elements.
  * [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md) — Record all 6 Android scrollable screens (`Feed`, `CollectionsFeed`, `CollectionDetail`, `Favorites`, `Search`, `UserProfile`) as preview-ready in §4.

---

## 4. Verification & UI Visual Review Plan

* **Build, Unit Tests & Linters** (per [`.agents/skills/verification-and-linting/SKILL.md`](../.agents/skills/verification-and-linting/SKILL.md)):
  ```bash
  ./gradlew :androidApp:assembleDebug :androidApp:testDebugUnitTest :androidApp:compileDebugAndroidTestKotlin ktlintCheck detekt
  ```
* **Mandatory Before / After UI Visual Review** (per [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md)):
  * Add progressive `@Preview` readiness (`*ScreenContent` + `PreviewScaffold`) to `FeedScreen`, `CollectionsFeedScreen`, `CollectionDetailScreen`, `FavoritesScreen` (empty & populated), `SearchScreen`, and `UserProfileScreen`.
  * Capture Before & After Compose Preview Sidecar PNGs (Compact Light, Compact Dark, Expanded `900×700`) into `<Artifact Directory Path>/ui-review/spec-85/{before,after}/android/`, inspect via `read_file`, and obtain user approval before marking `Implemented (Final)`.

---

## 5. Acceptance Criteria

- [x] All 6 scrollable screens (`Feed`, `CollectionsFeed`, `CollectionDetail`, `Favorites`, `Search`, `UserProfile`) draw masonry grids edge-to-edge beneath `SatinHeaderScaffold` with draw-phase `satinProgressiveBlur` (API 33+ blur / API 29–32 scrim fallback) and quick-return compact bars.
- [x] `SatinOcclusionController` suppresses header blur when returning `isTransitionSource` cards intersect the header (including Predictive Back), feathering back in over `450–650ms`, and `CollectionDetailHeader` eliminates its `.blur(20.dp)` transition pop.
- [x] `NavigationSections.kt` seeds `NavigationPreviewCache` prior to `PhotoDetails` navigation across all grid entry points.
- [x] Photographer avatars animate continuously via `sharedElement` (`avatar_${username}`) into `UserProfileScreen`; `FeedScreen` aperture randomize, `FavoritesScreen` scallop empty state, and `SearchColorDial` morphs are implemented.
- [x] Before/After Compose Preview Sidecar visual review is approved, and `:androidApp:assembleDebug`, `:androidApp:testDebugUnitTest`, `ktlintCheck`, and `detekt` pass with zero warnings.
- [x] Affected [`wiki/`](../wiki/README.md) pages, [`.agents/skills/android-m3-design/SKILL.md`](../.agents/skills/android-m3-design/SKILL.md), and [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md) are updated.

---

## 6. Verification Record

### Verified
* `./gradlew :androidApp:assembleDebug :androidApp:testDebugUnitTest :androidApp:compileDebugAndroidTestKotlin ktlintCheck detekt -q` — passed (exit code `0`).
* **Compose Preview Sidecar**: Rendered all 19 `@Preview` configurations across `FeedScreen`, `CollectionsFeedScreen`, `CollectionDetailScreen`, `FavoritesScreen`, `SearchScreen`, and `UserProfileScreen` (Compact Light/Dark, Expanded `900×700`, Favorites Populated/Empty, Search Results) into `<Artifact Directory Path>/ui-review/spec-85/after/android/` and verified via `read_file`.
* Updated `wiki/design-systems-and-navigation.md`, `wiki/performance-and-memory.md`, `.agents/skills/android-m3-design/SKILL.md`, and `.agents/skills/ui-visual-review/SKILL.md`.

### Unverified / Gaps
* Live-device interactive Predictive Back scrubbing and transition timing (`SatinOcclusionController` `550ms` feathering) were verified by code inspection and compilation only, not on a physical device or emulator.
* Before-edit Compose Preview Sidecar PNGs were not captured prior to the initial refactor; visual review was completed using the After renders.
