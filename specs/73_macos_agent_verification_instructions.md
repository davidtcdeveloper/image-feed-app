# Specification 73: macOS Agent Verification Instructions & Build Hygiene

**Status:** Proposed

## 1. Overview & Problem Statement

The app ships on Android, iOS, and macOS, but agent instructions only enforce Android + iOS verification. As a result, agents routinely skip the `macosApp` build and macOS-only regressions surface late (see Specs [13](13_macos_back_button_duplication_fix.md), [52](52_macos_title_and_ios_sheet_navigation_fixes.md), [71](71_macos_horizontal_category_scroll_fix.md)).

Verified facts (October 2026):

* `macosApp` is a **second target in [`iosApp/project.yml`](../iosApp/project.yml)** compiling the **same `iosApp/iosApp` Swift sources** as `iosApp`. Every Swift edit therefore also compiles for macOS.
* The top-level `macosApp/` directory is untracked, stale Gradle build output (`build/classes`, `build/kotlin`, …) from a removed module. It is not part of `settings.gradle.kts` and misleads agents into thinking macOS code lives there.
* Kotlin targets register `macosArm64()` across `:shared`, `:core:*`, and `:feature:*`. Gradle exposes `:shared:compileKotlinMacosArm64`, `:shared:linkDebugFrameworkMacosArm64`, and `:shared:macosArm64Test` (already aggregated by `:shared:allTests`).
* The macOS build succeeds with:
  `xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme macosApp -destination 'platform=macOS,arch=arm64' build CODE_SIGNING_ALLOWED=NO`
* The build emits one warning, violating the Zero Compiler Warning Policy:
  *"Run script build phase 'Compile Kotlin Shared Framework' will be run during every build because it does not specify any outputs."*

---

## 2. Gap Analysis

| # | Location | Gap |
|---|---|---|
| G1 | [`AGENTS.md`](../AGENTS.md) invariant 2 | "Dual-Platform Build Policy" requires only Android + iOS. |
| G2 | [`README.md`](../README.md) (Engineering Policies) | States automated verification focuses on the "dual mobile targets", implicitly excusing macOS. |
| G3 | [verification-and-linting](../.agents/skills/verification-and-linting/SKILL.md) | No macOS `xcodebuild` / `compileKotlinMacosArm64` command; pre-commit checklist and warning policy list iOS only; XcodeGen section omits that `project.yml` defines both Apple targets; no Xcode MCP guidance to switch scheme to `macosApp`. |
| G4 | `AGENTS.md` invariant 4 | Retina rule cites only `UIScreen.main.scale` (unavailable on macOS; code uses `NSScreen.main?.backingScaleFactor`). |
| G5 | `AGENTS.md` / skills | No statement that `iosApp/iosApp` sources are shared by the `macosApp` target; stale `macosApp/` folder suggests otherwise. |
| G6 | [ios-glass-design](../.agents/skills/ios-glass-design/SKILL.md) | No checklist of iOS-only APIs that require `#if os(iOS)` guards. |
| G7 | [kmp-testing](../.agents/skills/kmp-testing/SKILL.md) | Does not mention `:shared:macosArm64Test` as a fast host-native Kotlin/Native test target. |
| G8 | `iosApp/project.yml` | Kotlin pre-build script phases lack `basedOnDependencyAnalysis: false`, producing a build warning. |

---

## 3. Decisions

* **Separate builds**: iOS and macOS are verified with separate `xcodebuild` invocations. No combined wrapper script.
* **Conditional macOS requirement**: The macOS build is required only for **Apple-specific changes**, defined as any change touching:
  * `iosApp/**` (Swift sources, `Localizable.xcstrings`, asset catalogs, `project.yml`, `project.pbxproj`);
  * Kotlin Apple source sets (`appleMain`, `iosMain`, `macosMain`) in any module;
  * Kotlin/Native target or framework configuration (`macosArm64()`, `binaries.framework`, `export(...)`) in Gradle build scripts.
  Shared `commonMain`-only changes remain covered by Android + iOS builds, since both Apple targets consume the identical exported framework API and Swift sources.
* **Fix the build-script warning** in this change.
* **Delete the stale `macosApp/` directory** without adding a new `.gitignore` entry. The obsolete `/macosApp/.build` line in `.gitignore` is removed under the Zero Dead Code Policy.

---

## 4. Proposed Changes

### 4.1 `AGENTS.md` (no net line growth; currently 72 lines, target < 75)
* Invariant 1: one clause stating `macosApp` is a target in `iosApp/project.yml` sharing the `iosApp/iosApp` sources.
* Invariant 2: rename to **"Platform Build Policy"**. Always verify Android + iOS for shared/platform changes, and **additionally macOS for Apple-specific changes** (per §3). Reference the verification skill for exact commands instead of inlining the long `xcodebuild` lines.
* Invariant 4: Retina rule reads "display scale (`UIScreen.main.scale` on iOS, `NSScreen.backingScaleFactor` on macOS)".

### 4.2 [verification-and-linting](../.agents/skills/verification-and-linting/SKILL.md)
* §2C: add `./gradlew :shared:compileKotlinMacosArm64`.
* New §2E **macOS Application Build** with the verified command from §1.
* §4 Policy: replace "Dual-Platform Build Policy" with the conditional rule from §3, including the Apple-specific change triggers.
* §4 Zero Compiler Warning Policy and Pre-Commit Checklist: include the macOS build when the change is Apple-specific.
* §4 Apple Tooling & XcodeGen: note `project.yml` defines both `iosApp` and `macosApp`; when using Xcode MCP tools, switch scheme to `macosApp` (`XcodeSwitchScheme`) and rebuild.
* Update the skill frontmatter description to mention macOS.

### 4.3 [ios-glass-design](../.agents/skills/ios-glass-design/SKILL.md)
* Add a declarative **"macOS Compile-Safety Checklist"** (bullet list, no code blocks) of iOS-only APIs requiring `#if os(iOS)` guards: UIKit types (`UIScreen`, `UIImage`, `UIApplication`), `UIImpactFeedbackGenerator`, `presentationDetents` / `presentationBackgroundInteraction`, `navigationBarTitleDisplayMode`, `.topBarLeading` / `.topBarTrailing` toolbar placements, `textInputAutocapitalization`, `statusBarHidden`, and size-class queries.
* Reference [`AdaptiveLayoutHelper.swift`](../iosApp/iosApp/AdaptiveLayoutHelper.swift) as the living example of guarded platform branching.

### 4.4 [kmp-testing](../.agents/skills/kmp-testing/SKILL.md)
* One line noting `./gradlew :shared:macosArm64Test` runs Kotlin/Native tests natively on the host (included in `:shared:allTests`).

### 4.5 `README.md`
* Align the Engineering Policies bullet with the conditional macOS policy.

### 4.6 `iosApp/project.yml` & `.gitignore`
* Add `basedOnDependencyAnalysis: false` to the "Compile Kotlin Shared Framework" `preBuildScripts` entry of **both** `iosApp` and `macosApp` targets; regenerate `project.pbxproj` via XcodeGen.
* Remove the obsolete `/macosApp/.build` entry from `.gitignore`.

### 4.7 Workspace Cleanup
* Delete the untracked top-level `macosApp/` directory.

---

## 5. Acceptance Criteria

- [ ] `AGENTS.md` documents the conditional macOS build requirement, the shared `iosApp/iosApp` source layout, and the macOS display-scale API, and stays under 75 lines.
- [ ] Verification skill contains the macOS Kotlin compile and `xcodebuild -scheme macosApp` commands, Apple-specific trigger list, updated checklist, and Xcode MCP scheme guidance.
- [ ] ios-glass-design skill contains the macOS Compile-Safety Checklist without verbatim code blocks.
- [ ] kmp-testing skill mentions `:shared:macosArm64Test`.
- [ ] README Engineering Policies no longer excludes macOS from verification.
- [ ] `iosApp/project.yml` script phases set `basedOnDependencyAnalysis: false`; `project.pbxproj` regenerated and in sync.
- [ ] `macosApp/` directory deleted; stale `.gitignore` entry removed; no new ignore entry added.
- [ ] `./gradlew :androidApp:assembleDebug` succeeds.
- [ ] iOS `xcodebuild` (scheme `iosApp`) succeeds with zero warnings.
- [ ] macOS `xcodebuild` (scheme `macosApp`) succeeds with zero warnings.
- [ ] `specs/steps.md` Step 73 marked `(Completed)` and this spec set to `**Status:** Implemented`.
