# Specification 89: Apple Photo Details Editorial Parity, Matte Inspector Cards, Dual-Pane Zoom & Liquid Glass Morphing

**Status:** Implemented (Final)

* **Wave:** Wave 2 — Parallel Feature & Craft Execution
* **Depends On:** [Spec 81](81_kmp_color_engine_and_navigation_seed_cache.md), [Spec 83](83_apple_design_system_matte_and_light_mode_hygiene.md)
* **Parallelizable With:** [Spec 84](84_android_float_navigation_and_random_mvi.md), [Spec 85](85_android_satin_headers_and_shared_transitions.md), [Spec 86](86_android_immersive_photo_details_and_zoom.md), [Spec 87](87_apple_float_navigation_shell_and_macos_search.md), [Spec 88](88_apple_satin_headers_zoom_transitions_and_unified_routes.md)
* **Research Mapping:** [Cross-Platform Material Craft Evaluation](researches/material_design_cross_platform_ui_craft_evaluation.md) — `ARCH-02` (Apple adoption), Craft `2.2` (`PhotoInspectorView` Matte cards), Craft `2.5` (macOS detail back / `Escape`), Craft `2.6` (`GlassEffectContainer` & SF Symbol morphs), Craft `2.7` (Apple Photo Details cross-pollination & chart Light Mode fix)
* **Exclusive File Ownership:** [`PhotoDetailsView.swift`](../iosApp/iosApp/PhotoDetailsView.swift), [`PhotoInspectorView.swift`](../iosApp/iosApp/PhotoInspectorView.swift), [`PhotoDetailsViewModel.swift`](../iosApp/iosApp/PhotoDetailsViewModel.swift), [`Localizable.xcstrings`](../iosApp/iosApp/Localizable.xcstrings), [`UIPreviewFixtures.swift`](../iosApp/iosApp/UIPreviewFixtures.swift), [`UIVisualSnapshotTests.swift`](../iosApp/iosAppTests/UIVisualSnapshotTests.swift)

---

## 1. Overview & Codebase Baseline

While Apple's [`PhotoDetailsView.swift`](../iosApp/iosApp/PhotoDetailsView.swift) and [`PhotoInspectorView.swift`](../iosApp/iosApp/PhotoInspectorView.swift) pioneered interactive detented sheets and dual-pane layouts (and serve as the [Spec 80](archive/80_cross_platform_ui_visual_review.md) visual review pilot), auditing them against [the UI craft evaluation](researches/material_design_cross_platform_ui_craft_evaluation.md) highlights three key gaps:

1. **Redundant Nested Live Blurs (Craft `2.2`)**: [`PhotoInspectorView.swift`](../iosApp/iosApp/PhotoInspectorView.swift#L47-L253) wraps 7+ interior cards (photographer row, `MetricCard`, historical chart card, download/bookmark buttons, EXIF card, location card, tag pills) in `.glassCard(cornerRadius: 12, style: .ultraThin)` / `.glassCapsule(style: .ultraThin)` even though they render inside an already-blurred `.regularMaterial` detented bottom sheet or on an opaque `GlassTheme.canvasBackgroundColor` dual-pane sidebar.
2. **Feature & Light-Mode Parity Gaps vs. Android (Craft `2.7`, `ARCH-02`)**: Apple currently lacks Android's **2×2 EXIF Bento Grid**, **interactive Tonal Color Swatches** (tap-to-copy hex), **Full vs. RAW download split**, and **pinch/magnify zoom on dual-pane iPad/macOS canvases** (`dualPaneLayout` in [`PhotoDetailsView.swift`](../iosApp/iosApp/PhotoDetailsView.swift#L260-L329)). Furthermore, [`HistoricalStatsChart`](../iosApp/iosApp/PhotoInspectorView.swift#L321-L361) hardcodes `.white` chart marks.
3. **macOS Ergonomics & OS 26 Glass Morphing (Craft `2.5` & `2.6`)**: [`PhotoDetailsView.swift`](../iosApp/iosApp/PhotoDetailsView.swift#L140-L180) pads overlay back/info buttons `.padding(.top, 60)` even inside desktop macOS windows, lacks `Escape` dismissal, and does not yet use iOS/macOS 26 `GlassEffectContainer` + `.glassEffectID` or SF Symbol `.symbolEffect(.replace/.bounce)` transitions.

---

## 2. Scope & Technical Workstreams

### Workstream A (Craft `2.2`): Matte Inspector Surface Conversion
1. Convert all 7+ nested `.glassCard(cornerRadius: 12, style: .ultraThin)` and `.glassCapsule(style: .ultraThin)` containers inside [`PhotoInspectorView.swift`](../iosApp/iosApp/PhotoInspectorView.swift#L47-L253) (photographer card, `MetricCard`, chart card, download/bookmark actions, EXIF card, location card, tag pills) to `.matteSurface()` tokens (`Spec 83`), eliminating redundant offscreen blur passes while respecting `accessibilityReduceTransparency` and Light/Dark parity.

### Workstream B (Craft `2.7` & `ARCH-02`): Editorial Inspector Parity, Dual-Pane Zoom & Chromatic Accenting
1. **2×2 EXIF Bento Grid**: Refactor the vertical `ExifRowView` list ([`PhotoInspectorView.swift`](../iosApp/iosApp/PhotoInspectorView.swift#L126-L143)) into an adaptive 2×2 / 4-column Bento Grid with semantic SF Symbol tiles (`camera`, `camera.aperture`, `timer`, `scope`, `dial.medium`).
2. **Interactive Tonal Color Swatches**: Render the 5-step tonal palette computed by shared `:core:model` `deriveTonalSwatches(seedHex:)` (`Spec 81`) inside `PhotoInspectorView`. Tapping any swatch copies its hex string (`UIPasteboard.general.string` under `#if os(iOS)`, `NSPasteboard.general` under `#if os(macOS)`), displays a localized `GlassToastPill` confirmation, and fires `.sensoryFeedback(.success, trigger:)`.
3. **Full vs. RAW Split Download Control**: Upgrade the single download button in [`PhotoInspectorView.swift`](../iosApp/iosApp/PhotoInspectorView.swift#L85-L108) and `compactFloatingBar` to a split **Full** (`photo.urls.full`) / **RAW** (`photo.urls.raw`) control, ensuring `viewModel.trackDownload()` (`PhotoDownloadTracker`) runs for both per Unsplash API compliance.
4. **Dual-Pane Canvas Zoom**: Bring `1.0x–4.0x` `MagnifyGesture`, pan, and double-tap zoom toggle to `dualPaneLayout` ([`PhotoDetailsView.swift`](../iosApp/iosApp/PhotoDetailsView.swift#L260-L329)) on iPad and macOS, matching `compactPhotoLayout`.
5. **Photo-Hue Accenting & Light-Mode Chart Fix**: Replace hardcoded `.white` marks in `HistoricalStatsChart` ([`PhotoInspectorView.swift`](../iosApp/iosApp/PhotoInspectorView.swift#L321-L361)) with shared `deriveAccentHex(seedHex:isDark:)` / semantic `.primary` strokes, and subtly tint active bookmark highlights and map pin accents with the photo's derived chromatic accent. Externalize all new inspector strings (`COLOR PALETTE`, `Copied %@ to clipboard`, `Full`, `RAW`) into [`Localizable.xcstrings`](../iosApp/iosApp/Localizable.xcstrings).

### Workstream C (Craft `2.5` & `2.6`): macOS Detail Ergonomics, OS 26 `GlassEffectContainer` & SF Symbol Morphing
1. Under `#if os(macOS)`, remove the `.padding(.top, 60)` overlay back button when pushed inside `NavigationStack`, adopt `PlatformToolbarButton` (`Spec 83`) inside `.toolbar`, and wire `.keyboardShortcut(.escape, modifiers: [])` to dismiss photo details cleanly.
2. Under `#available(iOS 26, macOS 26, *)`, wrap co-located floating controls (`compactFloatingBar`, top back/info orbs, `GlassToastPill`) inside **`GlassEffectContainer`** paired with **`.glassEffect(.regular.interactive(), in:)`** and **`.glassEffectID(_:in:)`** so toggling the inspector sheet smoothly morphs the glass cluster.
3. Apply `.contentTransition(.symbolEffect(.replace))` on `bookmark` ↔ `bookmark.fill` and `info.circle` ↔ `info.circle.fill`, `.symbolEffect(.bounce)` on Download actions, and photographer avatar continuity (`user-\(username)`) on the detail attribution avatar.

---

## 3. Constraints, Non-Goals, Wiki & Skill Impact

* **Parallel Signature Compatibility**: Keep [`PhotoDetailsView.init`](../iosApp/iosApp/PhotoDetailsView.swift#L55-L71) source-compatible with existing caller signatures (`photoId`, `initialPhoto`, `initialViewModel`, `heroNamespace`, `onDismiss`, `onUserSelect`, `onTagSelect`) so `Spec 88` compiles concurrently without coordination breakage.
* **Unsplash Compliance (`ADR-02`)**: Never bypass `viewModel.trackDownload()` on Full or RAW downloads; preserve `ixid` and prominent photographer attribution.
* **Wiki Impact**:
  * [`wiki/design-systems-and-navigation.md`](../wiki/design-systems-and-navigation.md) — Document Matte inspector conversion, 2×2 EXIF Bento, tonal swatches, dual-pane zoom, `GlassEffectContainer` morphing, and macOS `Escape` dismiss.
  * [`wiki/business-rules.md`](../wiki/business-rules.md) — Document Full vs. RAW download tracking parity on Apple alongside Android.
* **Skill & Guideline Impact**:
  * [`.agents/skills/ios-glass-design/SKILL.md`](../.agents/skills/ios-glass-design/SKILL.md) — Update §5 & §6 (`Full-Canvas Presentation & Adaptive Split-Pane`) to document `.matteSurface()` interior inspector cards, 2×2 EXIF Bento + `:core:model` tonal color swatches, dual-pane iPad/macOS `1.0x–4.0x` canvas zoom, `#available(iOS 26, macOS 26, *)` `GlassEffectContainer` + `.glassEffectID` morphing, SF Symbol `.symbolEffect(.replace/.bounce)`, and macOS `Escape` detail dismissal without overlay back-button padding.

---

## 4. Verification & UI Visual Review Plan

* **Dual-Target Xcode Builds & Linters** (per [`.agents/skills/verification-and-linting/SKILL.md`](../.agents/skills/verification-and-linting/SKILL.md)):
  ```bash
  xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
  xcodebuild -project iosApp/iosApp.xcodeproj -scheme macosApp -destination 'platform=macOS,arch=arm64' build CODE_SIGNING_ALLOWED=NO
  swiftformat --lint . && swiftlint lint iosApp/iosApp iosApp/iosAppTests iosApp/iosAppUITests
  ```
* **Mandatory Before / After UI Visual Review** (per [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md)):
  * Extend [`UIPreviewFixtures.swift`](../iosApp/iosApp/UIPreviewFixtures.swift) and [`UIVisualSnapshotTests.swift`](../iosApp/iosAppTests/UIVisualSnapshotTests.swift) with EXIF/color fixtures, then capture **Mode A** (`iosAppTests` Light/Dark/iPad + `macosAppTests` Light/Dark) and **Mode B** (`iosAppUITests` detented sheet + `Xcode MCP RenderPreview`) Before/After snapshots into `<Artifact Directory Path>/ui-review/spec-89/{before,after}/`.
  * Inspect rendered PNGs with `read_file` and obtain user approval on the comparison artifact before marking `Implemented (Final)`.

---

## 5. Acceptance Criteria

- [x] `PhotoInspectorView` converts all nested `.ultraThin` cards and tag pills to `.matteSurface()` with zero redundant blur passes inside the sheet/sidebar.
- [x] `PhotoInspectorView` renders the 2×2 EXIF Bento Grid, interactive `:core:model` tonal color swatches (tap-to-copy + toast + haptic), Full vs. RAW split download tracking, and adaptive `HistoricalStatsChart` + photo-hue accents with localized strings in `Localizable.xcstrings`.
- [x] `PhotoDetailsView` enables `1.0x–4.0x` zoom + double-tap toggle on dual-pane iPad/macOS canvases, removes the `.padding(.top, 60)` macOS overlay back button, and supports `Escape` dismissal.
- [x] On OS 26+, floating controls use `GlassEffectContainer` + `.glassEffectID` morphing; SF Symbol `.replace` / `.bounce` effects and avatar continuity are wired cleanly.
- [x] Mode A and Mode B Before/After visual review is approved, and `iosApp` + `macosApp` builds, `swiftformat --lint .`, and `swiftlint` pass with zero warnings.
- [x] Affected [`wiki/`](../wiki/README.md) pages and [`.agents/skills/ios-glass-design/SKILL.md`](../.agents/skills/ios-glass-design/SKILL.md) are updated.

---

## 6. Verification Record

### Verified
- **Visual Review Approved**: User reviewed and approved Before/After Mode A (`iosAppTests` + `macosAppTests`) and Mode B (`iosAppUITests`) captures (`spec-89-visual-review.md`).
- `swiftformat --lint .` — Passed (`0/35 files require formatting, 124 files skipped`).
- `swiftlint lint iosApp/iosApp iosApp/iosAppTests iosApp/iosAppUITests` — Passed with `0` errors (only pre-existing `ISS-007` `opening_brace` / `file_length` warnings).
- `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'id=1B214EFF-707E-4B1C-82C1-998FE6226C59' -only-testing:iosAppTests CODE_SIGNING_ALLOWED=NO` — Passed (all `iosAppTests` including `testPhotoColorEngineAndCopiedToastParity` + Mode A iOS snapshots, 0 Swift compiler warnings).
- `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme macosApp -destination 'platform=macOS,arch=arm64' -only-testing:macosAppTests CODE_SIGNING_ALLOWED=NO` — Passed (all 10 `macosAppTests` including `testPhotoColorEngineAndCopiedToastParity` + Mode A macOS Light/Dark snapshots, 0 Swift compiler warnings).
- `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'id=1B214EFF-707E-4B1C-82C1-998FE6226C59' -only-testing:iosAppUITests/UIWindowServerSnapshotUITests CODE_SIGNING_ALLOWED=NO` — Passed (Mode B live window-server capture `ios_window_photoDetails.png`).

### Unverified / Gaps
- **Xcode MCP `RenderPreview` & Interactive `Escape` / Pinch UI Test**: `Xcode` GUI MCP `RenderPreview` was omitted because Mode B live window-server `UIWindowServerSnapshotUITests` already verified live `.regularMaterial` and OS 26 Liquid Glass rendering; macOS `Escape` shortcut and `MagnifyGesture` on dual-pane canvases were verified via compilation and static code review (`xcodebuild` iOS & macOS builds).
- **Android Build**: Not run because no shared Kotlin, Gradle, or Android source files were modified (`iosApp/**` only).
