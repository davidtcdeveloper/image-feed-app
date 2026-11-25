---
name: ui-visual-review
description: >-
  Capture ephemeral Before and After UI snapshots across Android (Compose Preview Sidecar),
  iOS (Fast View Hosting or Window-Server XCUITest / RenderPreview), and macOS, and present
  visual comparisons in a session artifact before completing any UI specification.
---

# Cross-Platform UI Visual Review Runbook

## Purpose & Triggers

Use this skill whenever implementing or reviewing a specification or change that touches UI layout, styling, themes, typography, navigation chrome, or adaptive screens in `androidApp/` or `iosApp/iosApp/`.

---

## 1. Ephemeral Storage Contract

* **Zero Git-Committed PNGs**: Never save or commit snapshot PNGs inside the repository.
* **Session Artifact Folder**: Store all captures inside the active Antigravity conversation's artifact directory:
  * Before edits: `<Artifact Directory Path>/ui-review/<spec-id>/before/<platform>/`
  * After edits: `<Artifact Directory Path>/ui-review/<spec-id>/after/<platform>/`
* Both `before/` and `after/` run on the exact same machine and OS version within minutes, eliminating environmental font/compositor drift without repository bloat.
* All offline previews and snapshot suites must remain Unsplash-compliant using color/BlurHash placeholders and static fixture models ([`UiPreviewFixtures.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/preview/UiPreviewFixtures.kt), [`UIPreviewFixtures.swift`](../../../iosApp/iosApp/UIPreviewFixtures.swift))—never fetch or store real Unsplash images.

---

## 2. Platform Capture Tools & When to Use Each

### A. Android — Antigravity Compose Preview Sidecar
Use the **Android Compose Preview** sidecar (`compose-preview` skill) targeting `gradle://.?:androidApp@debug`.

* **Precondition**: Ensure `local.properties` sets `sdk.dir` in git worktrees, and verify `~/.android/cli/sidecars/compose-preview.json` responds via `curl -s <URL>/api/status`.
* **Sidecar Outdated Recovery**: If the sidecar log shows the outdated-CLI page, run `android update` (unsandboxed), then `POST http://127.0.0.1:<PORT>/__launcher/retry` so the launcher starts `android sidecar run compose-preview`.
* **Render Command**: Send `POST <URL>/api/session` with header `X-Sidecar-Conversation: <conversation-id>`, `"project_dir"`, `"reference": "gradle://.?:androidApp@debug"`, `"fqns"`, `"output_dir"`, and `"wait": true`. Never invoke `android preview render` directly.
* **Screen Previews**: Target stateless `@Preview` functions (Compact Light, Compact Dark, Expanded `900x700`) defined alongside ready screens in `androidApp/src/main/java/com/example/imagefeed/android/*Screen.kt` (wrapped in `PreviewScaffold` from [`UiPreviewFixtures.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/preview/UiPreviewFixtures.kt)).

### B. Apple (`iosApp` & `macosApp`) — Two-Tier Capture Matrix

Always run `xcodegen generate --spec iosApp/project.yml` after any `project.yml` or source-list change. Pass `SNAPSHOT_OUTPUT_DIR` (`TEST_RUNNER_SNAPSHOT_OUTPUT_DIR`, `SIMCTL_CHILD_SNAPSHOT_OUTPUT_DIR`, `SNAPSHOT_OUTPUT_DIR`) so tests write PNGs straight to the artifact directory.

| Mode | Mechanism | Speed (Post-Build) | When Recommended |
| :--- | :--- | :--- | :--- |
| **Mode A: Fast View Hosting *(Default)*** | `iosAppTests` / `macosAppTests` ([`UIVisualSnapshotTests.swift`](../../../iosApp/iosAppTests/UIVisualSnapshotTests.swift)) via `UIHostingController` / `NSHostingView` | ~2–5s | **~85% of UI specs:** Typography, spacing, grid columns, cards/badges, color tokens, Light/Dark mode, Dynamic Type, and empty/error states. |
| **Mode B: Window-Server Capture** | `iosAppUITests` ([`UIWindowServerSnapshotUITests.swift`](../../../iosApp/iosAppUITests/UIWindowServerSnapshotUITests.swift)) + `Xcode MCP RenderPreview` | ~15–25s | **~15% of UI specs:** `.ultraThinMaterial` backdrop blurs & vibrancy ([`SurfaceTokens.swift`](../../../iosApp/iosApp/DesignSystem/SurfaceTokens.swift)), floating safe-area bars ([`FloatingTabCapsule.swift`](../../../iosApp/iosApp/FloatingTabCapsule.swift)), detented bottom sheets ([`PhotoInspectorView.swift`](../../../iosApp/iosApp/PhotoInspectorView.swift)), and macOS `NavigationSplitView` chrome ([`MacOSContentView.swift`](../../../iosApp/iosApp/MacOSContentView.swift)). |

* **Mode A Commands (unsandboxed)**:
  ```bash
  xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 18 Pro' -only-testing:iosAppTests/UIVisualSnapshotTests CODE_SIGNING_ALLOWED=NO
  xcodebuild test -project iosApp/iosApp.xcodeproj -scheme macosApp -destination 'platform=macOS,arch=arm64' -only-testing:macosAppTests/UIVisualSnapshotTests CODE_SIGNING_ALLOWED=NO
  ```
* **Mode B Command (iOS Simulator only; avoid macOS CLI `XCUITest` to prevent blocking Accessibility TCC prompts)**:
  ```bash
  xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 18 Pro' -only-testing:iosAppUITests/UIWindowServerSnapshotUITests CODE_SIGNING_ALLOWED=NO
  ```
* **macOS Window-Server Alternative**: Use `Xcode MCP RenderPreview` against `#Preview` blocks in `iosApp/iosApp/*.swift` when inspecting macOS live material blurs.

---

## 3. Mandatory Before / After Visual Review Gate

1. **Capture Before**: Before editing UI files, capture the affected screens into `<Artifact Directory Path>/ui-review/<spec-id>/before/`.
2. **Implement & Capture After**: Complete UI changes, pass builds/linters per [`verification-and-linting`](../verification-and-linting/SKILL.md), and capture the same screens into `.../after/`.
3. **Inspect Pixels**: Open representative `before` and `after` PNGs with `read_file` to verify non-blank rendering, contrast, and layout alignment.
4. **Present Visual Diff Artifact & Pause**: Create a session review artifact in `<Artifact Directory Path>` presenting Before vs. After images in Markdown comparison tables and ````carousel` slide blocks. Ask the user to review and approve the visual diff **before** marking any UI spec `Implemented (Final)` or archiving it.

---

## 4. Progressive Screen Readiness & Troubleshooting

* **Scope Rule**: Only make a screen preview/snapshot-ready when the active spec modifies that screen—never run codebase-wide refactoring sweeps upfront.
* **Ordering Rule**: Land behavior-preserving readiness fixes *before* capturing the baseline (`readiness fix → verify & capture before → apply UI change → capture after`).
* **Placement & Safety**: Prefer `#if DEBUG` or inspection-mode guards; preserve all public screen signatures and avoid silent production no-ops.
* **Post-Refactor Smoke Check**: After any screen readiness refactor, verify navigation callbacks, toolbar actions, and interactive controls on that screen remain wired identically to production across both compact and desktop shells.

| Symptom During Capture | Progressive Fix Pattern |
| :--- | :--- |
| Screen requires a live ViewModel | Extract a stateless `*ScreenContent` composable keeping the public `*Screen` signature unchanged (Android), or add a `#if DEBUG` ViewModel preview initializer and optional `initialViewModel = nil` parameter (Apple; see [`PhotoDetailsViewModel.swift`](../../../iosApp/iosApp/PhotoDetailsViewModel.swift)). |
| Blank or half-faded cards on Frame 0 | Bypass entrance animations when `LocalInspectionMode.current` ([`AnimationUtils.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/util/AnimationUtils.kt)) or `isUISnapshotMode \|\| accessibilityReduceMotion` ([`AnimationPrimitives.swift`](../../../iosApp/iosApp/AnimationPrimitives.swift)) is active. |
| Crash or stall in map/platform widgets | Render a themed placeholder without hardcoded UI strings when `LocalInspectionMode.current` is true (see [`PhotoDetailsScreen.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt)), or omit coordinates/network triggers in offline fixtures. |
| Missing remote images | Rely on color/BlurHash placeholders by default; add offline fixture image seeding only when image content itself is under visual review. |
| Non-deterministic visual diffs | Isolate clock, network, or random inputs behind fixture parameters or `isUISnapshotMode` / `LocalInspectionMode`. |

* **Currently Snapshot-Ready Screens** *(update as screens become ready; log unsuccessful or deferred readiness gaps in [`wiki/issue-log.md`](../../../wiki/issue-log.md))*:
  * [`PhotoDetailsScreen.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt) & [`PhotoDetailsView.swift`](../../../iosApp/iosApp/PhotoDetailsView.swift) (pilot baseline).
  * [`FeedScreen.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/FeedScreen.kt) (`FeedScreenContent`, Compact Light/Dark + Expanded `900×700`).
  * [`CollectionsFeedScreen.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/CollectionsFeedScreen.kt) (`CollectionsFeedScreenContent`, Compact Light/Dark + Expanded `900×700`).
  * [`CollectionDetailScreen.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/CollectionDetailScreen.kt) (`CollectionDetailScreenContent`, Compact Light/Dark + Expanded `900×700`).
  * [`FavoritesScreen.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/FavoritesScreen.kt) (`FavoritesScreenContent`, Populated Light + Empty Light/Dark).
  * [`SearchScreen.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt) (`SearchScreenContent`, Discovery Light/Dark/Expanded `900×700` + Search Results Compact Light).
  * [`UserProfileScreen.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt) (`UserProfileScreenContent`, Compact Light/Dark + Expanded `900×700`).
  * [`FloatNavigationChrome.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/designsystem/FloatNavigationChrome.kt) (`CompactFloatBottomBarLightPreview`, `CompactFloatBottomBarDarkPreview`, `ExpandedNavigationRailPreview`), [`ExpressiveShapes.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/designsystem/ExpressiveShapes.kt) (`ApertureRandomizeButtonRestPreview`, `ApertureRandomizeButtonLoadingPreview`), and [`PhotoDetailPlaceholder.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailPlaceholder.kt) (`PhotoDetailPlaceholderLightPreview`, `PhotoDetailPlaceholderDarkPreview`).
  * [`PhotosFeedView.swift`](../../../iosApp/iosApp/PhotosFeedView.swift) (`PhotosFeedTabView`, iOS Compact Light/Dark + macOS `1280×800` Light/Dark).
  * [`CollectionsFeedView.swift`](../../../iosApp/iosApp/CollectionsFeedView.swift) (`CollectionsFeedView`, iOS Compact/Regular Light + macOS `1280×800` Light).
  * [`SearchView.swift`](../../../iosApp/iosApp/SearchView.swift) (`SearchView` results & `SearchFiltersSheetView`, iOS Compact Light + macOS `1280×800` Light).
  * [`UserProfileView.swift`](../../../iosApp/iosApp/UserProfileView.swift) (`UserProfileView` Insights, iOS Compact Light/Dark + macOS `1280×800` Light/Dark).
