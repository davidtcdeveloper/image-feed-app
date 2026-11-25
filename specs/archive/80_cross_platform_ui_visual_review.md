# Specification 80: Cross-Platform Ephemeral UI Visual Review & Snapshot Validation

**Status:** Implemented (Final)

---

## 1. Overview & Motivation

To review UI updates safely across **Android (`androidApp`)**, **iOS (`iosApp`)**, and **macOS (`macosApp`)** before completing or merging any specification, this project adopts an **ephemeral Before/After visual capture and review workflow** with **zero PNG files committed to Git**.

Rather than refactoring every screen upfront, the codebase establishes a **minimal cross-platform infrastructure baseline** validated end-to-end on a single pilot screen (**Photo Details**), paired with a **progressive screen readiness workflow** in [`.agents/skills/ui-visual-review/SKILL.md`](../../.agents/skills/ui-visual-review/SKILL.md) so additional screens are made preview/snapshot-ready on demand only when future UI specs touch them.

---

## 2. Architecture & Tooling Matrix

| Platform | Capture Mechanism | Speed (Post-Build) | When Recommended |
| :--- | :--- | :--- | :--- |
| **Android (`androidApp`)** | **Antigravity Compose Preview Sidecar** (`compose-preview` HTTP session) | ~5–15s | All Compose UI, adaptive `WindowSizeClass` (`Compact` / `Expanded`), Light/Dark theme, and layout/component diffs. |
| **Apple Mode A (`iosApp` / `macosApp`)** | **Fast View Hosting (`XCTest`)** (`UIHostingController` / `NSHostingView` via `iosAppTests` & `macosAppTests`) | ~1–4s | **Default (~85% of UI specs):** Typography, spacing, grid columns, cards/badges, color tokens, Light/Dark mode, and empty/error states. |
| **Apple Mode B (`iosApp` / `macosApp`)** | **Window-Server Capture** (`iosAppUITests` `XCUITest` on iOS Simulator + `Xcode MCP RenderPreview` on `iosApp` / `macosApp`) | ~15–30s | **Glass & Chrome specs (~15% of UI specs):** `.ultraThinMaterial` backdrop blurs & vibrancy (`GlassTheme`), safe-area floating bars, detented bottom sheets, and macOS split-view chrome. |

---

## 3. Scope & Workstreams (Minimal Baseline + Photo Details Pilot)

### Workstream A: Android Inspection-Mode Baseline & Pilot Screen
1. Configure git-ignored `local.properties` (`sdk.dir`) in worktrees as required by Android tooling and the Compose Preview Sidecar.
2. Bypass `Modifier.staggeredEntrance` (`AnimationUtils.kt`) when `LocalInspectionMode.current` is true so entrance-animated cards render at full opacity on Frame 0.
3. Add `UiPreviewFixtures.kt` (`PreviewScaffold` + static pilot `PhotoDetailsState` fixtures).
4. Guard `GoogleMap` inside `PhotoDetailsScreen.kt` with a clean themed placeholder box (zero hardcoded UI strings) under `LocalInspectionMode.current`, alongside Compact Light, Compact Dark, and Expanded `900×700` `@Preview` functions.

### Workstream B: Apple Dual-Mode Harness Baseline & Pilot Screen
1. Add `EnvironmentValues.isUISnapshotMode` in `AnimationPrimitives.swift` so `StaggeredRevealModifier` renders immediately on Frame 0 during previews and snapshot tests.
2. Add `#if DEBUG` preview initializer to `PhotoDetailsViewModel.swift` and optional `initialViewModel: PhotoDetailsViewModel? = nil` + `#Preview("Photo Details")` to `PhotoDetailsView.swift`.
3. Add `#if DEBUG` `UIPreviewFixtures.swift` (scoped to `.photoDetails`, relying on `KFImage` hex-color placeholders and omitting map coordinates in offline fixtures) plus `--ui-fixture <screen>` launch handling in `iOSApp.swift`.
4. Configure `iosAppTests`, `macosAppTests`, and `iosAppUITests` in `iosApp/project.yml` (preserving `DEAD_CODE_STRIPPING: YES` on `macosApp` for `sqlite-framework` cinterop compatibility while keeping `UIPreviewFixtures` live via `iOSApp`) with `UIVisualSnapshotTests.swift` (Mode A) and `UIWindowServerSnapshotUITests.swift` (Mode B).

### Workstream C: Progressive Readiness Skill, Wiki & Visual Review Gate
1. Author `.agents/skills/ui-visual-review/SKILL.md` documenting ephemeral storage (`<Artifact Directory Path>/ui-review/`), Android/Mode A/Mode B selection guidance, the mandatory Before/After Visual Review Gate, and **Section 4: Progressive Screen Readiness & Troubleshooting** (scope rule, ordering rule, symptom→fix mapping, safety/signature rules, post-refactor smoke checks, and snapshot-ready screen tracking).
2. Index the skill in `AGENTS.md`, document the Visual Review Gate in `specs/README.md`, and update `wiki/README.md`, `wiki/design-systems-and-navigation.md`, and `wiki/adr-index.md` (`ADR-12`).

### Workstream D: Live Capture Validation & Full Verification
1. Render pilot `PhotoDetailsScreenPreview`, `PhotoDetailsDarkPreview`, and `PhotoDetailsExpandedPreview` via the Android Compose Preview Sidecar.
2. Execute live Mode A (`iosAppTests`, `macosAppTests`) and Mode B (`iosAppUITests`) captures outputting ephemeral PNGs into `<Artifact Directory Path>/ui-review/validation/`.
3. Inspect captured PNGs and compile a validation review artifact demonstrating cross-platform and carousel presentation.
4. Pass all Gradle tests, Android/iOS/macOS builds, `ktlintCheck`, `detekt`, `swiftformat --lint .`, and `swiftlint` with zero PNGs tracked in Git.

---

## 4. Constraints & Non-Goals

* **Zero Git-Committed PNGs**: All captured screenshots remain strictly ephemeral inside `<Artifact Directory Path>/ui-review/`.
* **Minimal Baseline Only**: Non-pilot screens (`Feed`, `Collections`, `CollectionDetail`, `Favorites`, `Search`, `UserProfile`) remain unmodified until touched by future UI specs.
* **Unsplash Compliance**: Never fetch or cache real Unsplash photos for tests; rely on color/BlurHash placeholders and static offline fixtures.
* **No macOS CLI `XCUITest` TCC Prompts**: Mode B `XCUITest` runs on iOS Simulator (`iosAppUITests`); macOS live chrome verification uses `macosAppTests` (`NSWindow` + `NSHostingView`) and Xcode `RenderPreview`.
* **Wiki Impact**: `wiki/README.md`, `wiki/design-systems-and-navigation.md`, `wiki/adr-index.md`.

---

## 5. Acceptance Criteria

- [x] `local.properties` configures `sdk.dir` without committing secrets, and `staggeredEntrance` + `GoogleMap` inspection guards render cleanly in Android previews.
- [x] `UiPreviewFixtures.kt` (`PreviewScaffold`) and pilot `@Preview` functions exist for `PhotoDetailsScreen.kt` while non-pilot Android screens remain untouched.
- [x] Apple `UIPreviewFixtures.swift`, `isUISnapshotMode`, `#if DEBUG --ui-fixture` routing, and pilot `PhotoDetailsView` / `PhotoDetailsViewModel` preview support build cleanly on both `iosApp` and `macosApp` with non-pilot Apple screens untouched.
- [x] `iosAppTests`, `macosAppTests` (Mode A), and `iosAppUITests` (Mode B) are configured in `iosApp/project.yml` and pass cleanly on the `PhotoDetails` pilot.
- [x] `.agents/skills/ui-visual-review/SKILL.md` (including Progressive Screen Readiness & Troubleshooting) is authored and indexed in `AGENTS.md`, `specs/README.md`, and `wiki/`.
- [x] Live capture validation executes successfully across Android Compose Preview Sidecar, iOS Mode A, macOS Mode A, and iOS Mode B, with all rendered PNGs inspected and presented in a session artifact.
- [x] `./gradlew allTests :androidApp:assembleDebug :androidApp:compileDebugAndroidTestKotlin ktlintCheck detekt`, `./gradlew -p build-logic :convention:ktlintCheck`, `iosApp` + `macosApp` `xcodebuild`, `swiftformat --lint .`, and `swiftlint` pass with zero serious violations and zero PNGs in `git status`.

---

## 6. Verification Record

### Verified
- **Build, Unit Tests & Linters**: `xcodegen generate --spec iosApp/project.yml`, `./gradlew allTests :androidApp:assembleDebug :androidApp:compileDebugAndroidTestKotlin ktlintCheck detekt`, `./gradlew -p build-logic :convention:ktlintCheck`, `xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO`, `xcodebuild build -project iosApp/iosApp.xcodeproj -scheme macosApp -destination 'platform=macOS,arch=arm64' CODE_SIGNING_ALLOWED=NO`, `swiftformat --lint .`, and `swiftlint lint iosApp/iosApp iosApp/iosAppTests iosApp/iosAppUITests` all exited `0` (`0` serious SwiftLint violations, `0` PNG files in `git status --short`).
- **Android Compose Preview Sidecar (`POST http://127.0.0.1:61740/api/session`)**: Job `xgjjjfy1` completed (`status: completed`, `error: null`), rendering `PhotoDetailsScreenPreview`, `PhotoDetailsDarkPreview`, and `PhotoDetailsExpandedPreview` into the ephemeral artifact folder.
- **Apple Mode A (`iosAppTests`)**: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 18 Pro' -only-testing:iosAppTests/UIVisualSnapshotTests CODE_SIGNING_ALLOWED=NO` — passed (`1` test, `0` failures in `2.737s`), producing 3 crisp iOS PNGs (`393×852` Light/Dark and `1024×768` Dark).
- **Apple Mode A (`macosAppTests`)**: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme macosApp -destination 'platform=macOS,arch=arm64' -only-testing:macosAppTests/UIVisualSnapshotTests CODE_SIGNING_ALLOWED=NO` — passed (`1` test, `0` failures in `1.219s`), producing 2 `1280×800` Light/Dark macOS PNGs.
- **Apple Mode B (`iosAppUITests`)**: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 18 Pro' -only-testing:iosAppUITests/UIWindowServerSnapshotUITests CODE_SIGNING_ALLOWED=NO` — passed on retry (`1` test, `0` failures in `26.956s`; first invocation hit a runner startup stall `-[XCTWaiter(StallHandling) handleStalledWait:]`), capturing `ios_window_photoDetails.png`.

### Unverified / Gaps
- Manual interactive app launch and UI navigation smoke testing were not executed; verification relies on automated builds, unit/snapshot tests, and sidecar renders.
- `Xcode MCP RenderPreview` was not invoked directly during validation (covered conceptually in the skill matrix for on-demand macOS material blur inspection).
