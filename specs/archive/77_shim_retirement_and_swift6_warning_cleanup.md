# Specification 77: Legacy `:shared` Model Shim Retirement & Swift 6 Warning Cleanup

**Status:** Implemented (Final)

## 1. Overview & Motivation

Two deferred code hygiene items logged in [`wiki/issue-log.md`](../../wiki/issue-log.md) (`ISS-003` and `ISS-005`) prevent strict zero-dead-code and zero-warning compliance across Kotlin and Swift targets:

1. **Legacy Model `typealias` Shims in `:shared` (`ISS-003`)**:
   * `shared/src/commonMain/kotlin/com/example/imagefeed/model/PhotoStats.kt` and `shared/src/commonMain/kotlin/com/example/imagefeed/model/UserStats.kt` remained from modularization Specs 47–48 as backward-compatibility `typealias` shims pointing to `com.example.imagefeed.details.model.PhotoStats` and `com.example.imagefeed.profile.model.UserStats`.
   * [`UserProfileScreen.kt`](../../androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L123) was the sole remaining Kotlin caller still importing `com.example.imagefeed.model.UserStats`; `PhotoStats` had no remaining Kotlin usages.
2. **Pre-Existing Swift 6 Warnings (`ISS-005`)**:
   * [`KFImage.swift`](../../iosApp/iosApp/KFImage.swift#L192-L194) invoked `Self.downsample(data:maxPixelSize:)` inside `Task.detached`, triggering Swift 6 actor-isolation and `Placeholder` `#IsolatedConformances` warnings because `KFImage<Placeholder>` inherits `@MainActor` isolation via `SwiftUI.View`.
   * [`UserProfileView.swift`](../../iosApp/iosApp/UserProfileView.swift#L382-L388) constructed `Text("\(title), \(count) items")` inside `tabAccessibilityLabel(for:)` using `LocalizedStringKey` string interpolation (`appendInterpolation` deprecation warning), while [`Localizable.xcstrings`](../../iosApp/iosApp/Localizable.xcstrings) uses `"%@, %@ items"`.

---

## 2. Scope & Design

### A. Retire `:shared` Model `typealias` Shims (`ISS-003`)
1. Updated [`UserProfileScreen.kt`](../../androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L123) to import `com.example.imagefeed.profile.model.UserStats` directly.
2. Deleted `shared/src/commonMain/kotlin/com/example/imagefeed/model/PhotoStats.kt` and `shared/src/commonMain/kotlin/com/example/imagefeed/model/UserStats.kt`.
3. Verified Swift Objective-C framework interop remains intact for [`PhotoDetailsViewModel.swift`](../../iosApp/iosApp/PhotoDetailsViewModel.swift#L13), [`UserProfileView.swift`](../../iosApp/iosApp/UserProfileView.swift#L573), and [`UserProfileViewModel.swift`](../../iosApp/iosApp/UserProfileViewModel.swift#L11) (`:feature:details` and `:feature:profile` are exported in [`shared/build.gradle.kts`](../../shared/build.gradle.kts#L30-L35)).

### B. Eliminate Pre-Existing Swift 6 Warnings (`ISS-005`)
1. **`KFImage.swift`**: Extracted `downsample(data:maxPixelSize:)` out of the `@MainActor`-isolated generic `KFImage<Placeholder>` struct into a private `ImageDownsampler` enum (`static func downsample(data:maxPixelSize:)`) so `Task.detached(priority: .userInitiated)` runs off the main actor without referencing `Self` on a generic `View`.
2. **`UserProfileView.swift`**: Updated `tabTitle(for:)` to return localized `String` via `String(localized:)` and updated `tabAccessibilityLabel(for:)` to construct `Text("\(title), \(count.formatted()) items")`, matching [`Localizable.xcstrings`](../../iosApp/iosApp/Localizable.xcstrings#L18-L27) with zero `appendInterpolation` deprecation warnings.

---

## 3. Wiki Impact

* Updated [`wiki/issue-log.md`](../../wiki/issue-log.md) (`ISS-003` and `ISS-005`) to link to this archived specification; no architectural changes needed in [`wiki/architecture-and-modules.md`](../../wiki/architecture-and-modules.md).

---

## 4. Acceptance Criteria

- [x] `UserProfileScreen.kt` imports `com.example.imagefeed.profile.model.UserStats`; `shared/src/commonMain/kotlin/com/example/imagefeed/model/PhotoStats.kt` and `UserStats.kt` are deleted.
- [x] `KFImage.swift` builds cleanly under `iosApp` and `macosApp` with zero actor-isolation or `#IsolatedConformances` warnings.
- [x] `UserProfileView.swift` builds cleanly under `iosApp` and `macosApp` with zero `appendInterpolation` deprecation warnings while preserving localized accessibility labels in `Localizable.xcstrings`.
- [x] `./gradlew allTests ktlintCheck detekt :androidApp:assembleDebug` passes with zero errors or compiler warnings.
- [x] `xcodegen generate --spec iosApp/project.yml` and `xcodebuild` for both `iosApp` (iOS Simulator) and `macosApp` (`platform=macOS,arch=arm64`) pass with zero compiler warnings on modified files.
- [x] `swiftformat --lint .` and `swiftlint lint iosApp/iosApp` pass with zero formatting diffs and zero new SwiftLint violations.

---

## 5. Verification Record

* **Verified**:
  * `ANDROID_HOME=/Users/davidtiagoconceicao/Library/Android/sdk ./gradlew allTests ktlintCheck detekt :androidApp:assembleDebug`: `BUILD SUCCESSFUL in 26s` (`530 actionable tasks: 310 executed, 220 from cache`) with zero Kotlin compiler warnings.
  * `swiftformat --lint .`: `0/24 files require formatting, 98 files skipped`.
  * `swiftlint lint iosApp/iosApp`: `Done linting! Found 78 violations, 0 serious in 24 files` (identical pre-existing baseline, zero new violations in `KFImage.swift` or `UserProfileView.swift`).
  * `xcodegen generate --spec iosApp/project.yml`: Regenerated `iosApp/iosApp.xcodeproj`.
  * `ANDROID_HOME=/Users/davidtiagoconceicao/Library/Android/sdk xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`: `** BUILD SUCCEEDED **` with zero Swift compiler warnings.
  * `ANDROID_HOME=/Users/davidtiagoconceicao/Library/Android/sdk xcodebuild -project iosApp/iosApp.xcodeproj -scheme macosApp -destination 'platform=macOS,arch=arm64' build CODE_SIGNING_ALLOWED=NO`: `** BUILD SUCCEEDED **` with zero Swift compiler warnings.
* **Unverified / Gaps**:
  * Interactive UI / VoiceOver runtime inspection on a booted iOS Simulator, macOS window, or Android Emulator was not executed in this CLI session.
