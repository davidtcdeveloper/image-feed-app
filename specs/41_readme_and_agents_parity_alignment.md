# Specification: README Alignment & Parity with AGENTS.md Source of Truth

**Status:** Implemented

## 1. Overview & Problem Statement

[`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) serves as the non-negotiable single source of truth for repository invariants, architecture boundaries, dual-platform build policies, security/compliance rules, and verification standards across Android, iOS, macOS, and Kotlin Multiplatform shared code.

A comprehensive review comparing [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md) against [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) and its companion runbooks (primarily [`.agents/skills/verification-and-linting/SKILL.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/.agents/skills/verification-and-linting/SKILL.md)) identified several inconsistencies, omissions, and drifted references in [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md):

1. **iOS Build & Verification Commands**:
   * [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md#L253-L273) previously specified changing directory (`cd iosApp && xcodegen`) and running `./gradlew :shared:compileKotlinIosSimulatorArm64` as its command-line verification step.
   * [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md#L28-L31) and the verification runbook mandate root-level project generation and building the full iOS application target:
     ```bash
     xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
     ```
   * Compiling only `:shared:compileKotlinIosSimulatorArm64` verifies solely the Kotlin Multiplatform shared framework, leaving the Swift UI shell uncompiled and failing the repository's dual-platform verification invariant.

2. **Code Quality Toolchain Omissions**:
   * [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md#L286-L302) provides both linting and auto-formatting commands for Kotlin (`./gradlew ktlintCheck detekt` and `./gradlew ktlintFormat`), but under `# Swift linting & formatting` it previously listed only lint checks (`swiftlint lint iosApp/iosApp` and `swiftformat --lint .`), omitting the canonical `swiftformat .` auto-formatting command defined in [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) and [`.agents/skills/verification-and-linting/SKILL.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/.agents/skills/verification-and-linting/SKILL.md#L80-L84).

3. **Unsplash API Compliance & Retina CDN Scaling**:
   * [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md#L46) Core Invariant 4 explicitly requires multiplying layout points by display scale (`UIScreen.main.scale`) for CDN image requests to ensure sharp Retina rendering.
   * [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md#L305-L313) details hotlinking, `ixid` parameter retention, attribution links with UTM parameters, and download tracking, but previously omitted this mandatory Retina display scaling rule.

4. **Stale Living Code References in Navigation & Architecture**:
   * [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md#L128-L131) previously described Android Navigation 3 routes as `Screen.Feed`, `Screen.Collections`, `Screen.CollectionDetails`, `Screen.Search`, `Screen.PhotoDetails`, `Screen.UserProfile`. In the living codebase ([`Routes.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/navigation/Routes.kt#L13-L41)), the sealed hierarchy is named `AppRoute` (`AppRoute.Feed`, `AppRoute.Collections`, `AppRoute.CollectionDetails`, `AppRoute.Search`, `AppRoute.PhotoDetails`, `AppRoute.UserProfile`).
   * [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md#L132-L137) previously claimed SwiftUI navigation uses `FeedPathItem` and `CollectionPathItem` enums. In [`ContentView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L104-L126), both `FeedPathItem` and `CollectionPathItem` are `struct`s wrapping an inner `ItemType` enum, rather than enums themselves.
   * [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md#L162-L167) listed an incomplete subset of presenters (`FeedPresenter, UnifiedSearchPresenter, PhotoDetailsPresenter, CollectionDetailPresenter, UserProfilePresenter`), omitting [`CollectionsFeedPresenter`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/CollectionsFeedPresenter.kt) and [`RandomPhotoPresenter`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/RandomPhotoPresenter.kt).

5. **Platform Scope & Dual-Platform Verification Terminology**:
   * Clarify the scope of the **Dual-Platform Build Policy** described in [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) and [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md): while the application supports macOS via the shared Xcode project, pre-commit CI and automated verification focus on the dual mobile targets (`androidApp` and `iosApp`).

---

## 2. Core Invariants from `AGENTS.md` (Source of Truth)

All updates to [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md) must adhere strictly to the following principles defined in [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md):

1. **Dual-Platform Build Policy**:
   * Android: `./gradlew :androidApp:assembleDebug`
   * iOS: `xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`
   * Never substitute framework compilation (`:shared:compileKotlinIosSimulatorArm64`) for the actual application shell build when documenting build verification.

2. **Code Hygiene & Verification Toolchain**:
   * Kotlin format: `./gradlew ktlintFormat`
   * Kotlin check & static analysis: `./gradlew ktlintCheck detekt`
   * Swift format: `swiftformat .`
   * Swift format check: `swiftformat --lint .`
   * Swift static analysis: `swiftlint lint iosApp/iosApp`

3. **Security & Unsplash API Compliance**:
   * Preserve all existing compliance guidelines (hotlinking, `ixid`, attribution, download tracking).
   * Include the mandatory requirement to multiply layout points by display scale (`UIScreen.main.scale`) for CDN requests to guarantee sharp Retina rendering.

4. **Living Code & Anti-Staleness**:
   * Documentation must reflect active, living types and declarations: [`AppRoute`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/navigation/Routes.kt#L13), [`FeedPathItem`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L116), [`CollectionPathItem`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L104), and the full suite of presenters in [`shared/src/commonMain/kotlin/com/example/imagefeed/presentation/`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/).

---

## 3. Scope of Changes for `README.md`

### 3.1. How to Build and Run
* **iOS Application Build Instructions**:
  * Replace `cd iosApp && xcodegen` with the root command `xcodegen generate --spec iosApp/project.yml` for consistency with [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md).
  * In the command-line verification section, replace or augment `./gradlew :shared:compileKotlinIosSimulatorArm64` with the canonical `xcodebuild` invocation from [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md):
    ```bash
    # Generate Xcode project and verify iOS Simulator application build
    xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
    ```

### 3.2. Code Quality & Static Analysis
* Update the Swift section under `Code Quality & Static Analysis` to include both auto-formatting and linting checks:
  ```bash
  # Automatically format Swift code
  swiftformat .

  # Swift linting & formatting checks
  swiftformat --lint .
  swiftlint lint iosApp/iosApp
  ```

### 3.3. Unsplash API Compliance Guidelines
* Add the display density and Retina scaling requirement to the Unsplash API Compliance list:
  * **Retina CDN Optimization**: Multiply layout points by display scale (`UIScreen.main.scale` on iOS/macOS, display density on Android) when constructing `&w=` URL parameters to deliver sharp Retina imagery without requesting excessive raw file dimensions.

### 3.4. Navigation Architecture Details
* In Section 3 (*Screen Navigation Architecture Details*):
  * Update the Android sealed route list from `Screen.*` to `AppRoute`:
    `AppRoute.Feed`, `AppRoute.Collections`, `AppRoute.CollectionDetails(collectionId)`, `AppRoute.Search(query)`, `AppRoute.PhotoDetails(photoId)`, `AppRoute.UserProfile(username)`.
  * Update the iOS navigation description to specify the `FeedPathItem` and `CollectionPathItem` structs (clarifying that each wraps an `ItemType` enum rather than being top-level enums).

### 3.5. Technical Architecture Diagram
* In the ASCII architecture diagram under `Presenters`:
  * Include [`CollectionsFeedPresenter`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/CollectionsFeedPresenter.kt) and [`RandomPhotoPresenter`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/RandomPhotoPresenter.kt) alongside the existing presenter listing.

---

## 4. Implementation Steps

1. **Update [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md)**:
   * Apply all modifications detailed in Section 3, ensuring complete synchronization with [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) and living code symbols.
2. **Update Tracking in [`specs/steps.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/steps.md)**:
   * Register Step 43 documenting the documentation parity and alignment changes.
3. **Run Documentation & Static Checks**:
   * Verify all links and commands in [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md).
   * Confirm compliance with repository invariants.

---

## 5. Verification & Acceptance Criteria

- [x] [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md) command-line iOS build instructions match [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) (`xcodegen generate --spec iosApp/project.yml` and `xcodebuild ... CODE_SIGNING_ALLOWED=NO`).
- [x] [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md) Code Quality section includes `swiftformat .` for auto-formatting Swift code.
- [x] [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md) Unsplash Compliance section includes the Retina display scaling requirement (`UIScreen.main.scale`).
- [x] Android Navigation 3 route references in [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md) use `AppRoute.*` matching [`Routes.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/navigation/Routes.kt).
- [x] iOS navigation references in [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md) correctly identify `FeedPathItem` and `CollectionPathItem` as structs.
- [x] Architecture presenter diagram in [`README.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/README.md) lists all active presenters.
- [x] [`specs/steps.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/steps.md) indexes Step 43.
