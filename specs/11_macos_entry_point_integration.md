# Step 11: macOS Entry Point Integration & iOS UI Reuse

This specification captures the macOS desktop entry-point work that reuses the existing iOS SwiftUI code path and native KMP shared logic.

## Scope
1. Configure Multiplatform targets in Gradle:
   * Update `shared/build.gradle.kts` to register native targets such as `macosX64()` and `macosArm64()`.
   * Introduce an intermediate `appleMain` source set that both `iosMain` and `macosMain` depend on, moving Darwin-specific code (Ktor engine, serializers) into the shared Apple layer.
2. Define the macOS target in XcodeGen:
   * Update `iosApp/project.yml` to define a `macosApp` target with `type: application`, `platform: macOS`, and `deploymentTarget: "14.0"`.
   * Point the macOS source path at the existing `iosApp` folder so the same SwiftUI code is compiled for desktop.
   * Verify the SPM setup includes Kingfisher for the macOS target.
3. Refactor UIKit/iOS dependencies:
   * Replace hard-coded `UIScreen.main.bounds` image sizing with responsive `GeometryReader` or container-aware frame logic.
   * Replace iOS-only rounded-corner helpers with native SwiftUI equivalents such as `UnevenRoundedRectangle`.
   * Wrap touch-only features such as `UIImpactFeedbackGenerator` and shake overlays with `#if os(iOS)` / `#if os(macOS)` conditions.
4. Implement desktop navigation and container shells:
   * Add a macOS root shell using `NavigationSplitView` to switch between Photos and Collections.
   * Reuse the same `PhotosFeedTabView` and `CollectionsFeedTabView` views inside the master-detail layout.
5. Enable desktop shuffle triggers:
   * Add `.keyboardShortcut("s", modifiers: .command)` and a toolbar shuffle button as the macOS alternative to physical shake gestures.
6. Verify compilation and lifecycle integration:
   * Run `xcodegen` and build the `macosApp` target.
   * Confirm KMP framework linking, Koin startup, image loading, and adaptive grid rendering work on macOS.

## Outcome
This spec documents the desktop integration work that should be tracked separately from the shared photo-detail fix, while keeping the implementation notes aligned with the existing `steps.md` checklist.
