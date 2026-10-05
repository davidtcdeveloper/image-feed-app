---
name: verification-and-linting
description: >-
  Run verification commands, compile checks, static analysis, and code formatters
  across Kotlin (shared/androidApp) and Swift (iosApp/macosApp). Use when diagnosing
  build errors, verifying changes, checking warnings, or formatting code prior to commit.
---

# Verification, Tooling & Static Analysis Runbook

## Purpose & Triggers

This skill defines the canonical verification, build diagnostics, code formatting, and static analysis procedures for this Kotlin Multiplatform repository.
Use this skill when:
- Verifying code changes across shared Kotlin, Android Compose, or Apple SwiftUI targets.
- Diagnosing build errors or compiler warnings.
- Running unit, integration, or package-level tests.
- Formatting code or executing linting and static analysis prior to committing changes.
- Updating dependencies, Gradle version catalogs, or Apple project configurations.

---

## 1. Sandbox Execution Policy & Host Environment Permissions

Build, test, and code-analysis commands interact directly with external system SDKs:
- Java Development Kit (JDK) and Android SDK / command-line tools.
- User-level Gradle cache located at `~/.gradle`.
- Apple Xcode developer tools and simulator runtimes (`xcrun`, `xcodebuild`, `simctl`).

### Policy
In sandboxed agent environments, these commands require host execution permissions (e.g., `BypassSandbox: true` or elevated execution mode). Standard file reading, file editing, and basic git operations run in standard workspace mode.

---

## 2. Core Verification Commands

Execute the following commands to verify builds and tests across target platforms:

### A. Shared Integration & Unit Tests
Run the shared test suite in `shared/src/commonTest`:
```bash
./gradlew :shared:allTests
```

### B. Android Debug Application Build
Verify compilation and packaging for the Android target:
```bash
./gradlew :androidApp:assembleDebug
```

### C. Kotlin Native Apple Framework Compilation
Verify Kotlin/Native compilation for Apple Silicon iOS Simulator:
```bash
./gradlew :shared:compileKotlinIosSimulatorArm64
```

### D. iOS Simulator Application Build
Always regenerate the Xcode project from XcodeGen specifications before running xcodebuild:
```bash
xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
```

---

## 3. Formatting & Static Analysis Toolchain

Run the project linters and formatters to maintain a zero-violation baseline. Never commit unformatted code or bypass static analysis errors.

### A. Kotlin (shared & androidApp)
- **Auto-format Kotlin code:**
  ```bash
  ./gradlew ktlintFormat
  ```
- **Run Kotlin lint check & detekt static analysis:**
  ```bash
  ./gradlew ktlintCheck detekt
  ```

### B. Swift (iosApp & macosApp)
- **Auto-format Swift code:**
  ```bash
  swiftformat .
  ```
- **Verify SwiftFormat without modifying files:**
  ```bash
  swiftformat --lint .
  ```
- **Run SwiftLint static analysis:**
  ```bash
  swiftlint lint iosApp/iosApp
  ```

---

## 4. Verification Policies & Hygiene Rules

### Dual-Platform Build Policy
Whenever changes affect `shared/` or `iosApp/`, verify **BOTH** the Android build (`:androidApp:assembleDebug`) and the iOS build via `xcodebuild` (iOS Simulator). Never assume a shared change works in Swift simply because the Kotlin compiler passed.

### Zero Compiler Warning Policy
Actively monitor and resolve compiler warnings. After every significant change, execute the relevant compiler verification commands (`./gradlew :shared:compileKotlinIosSimulatorArm64`, `./gradlew :androidApp:compileDebugKotlin`, or `xcodebuild`). Fix all deprecations and warnings immediately to keep the build log clean.

### Zero Dead Code Policy
Do not introduce speculative, unused helper properties, functions, or parameters. When replacing a parameter, service, or pattern during refactoring, eliminate the obsolete code, variables, and imports across all call sites in the same change set.

### Pre-Commit Review & QA Checklist
Before staging and committing changes:
1. **Dual Build Verification**: Run `./gradlew :androidApp:assembleDebug` and `xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO` with zero compiler warnings.
2. **Static Analysis & Linters**: Execute `./gradlew ktlintCheck detekt` and `swiftformat --lint .` / `swiftlint lint iosApp/iosApp`.
3. **Spec Checklist**: Confirm all acceptance criteria checkboxes in the active `specs/<id>.md` are completed (`- [x]`).
4. **Artifact Cleanup**: Remove all temporary review reports, logs, and teammate scratch directories from `.agents/`.
5. **Scoped Staging**: Stage only the files targeted by the user's prompt (e.g., `git add <files>`), preserving the required commit message template.

### Cross-Language Reference Auditing
Before removing or refactoring any declaration in `shared/commonMain`, search both Kotlin files (`androidApp/`, `shared/`) and Swift files (`iosApp/`). Keep in mind Swift interop name transformations (e.g., Kotlin `description` maps to Swift `description_`, and callback signatures can vary). Code that is invoked exclusively from Swift and has no Kotlin call sites must be annotated with:
```kotlin
@Suppress("unused") // Invoked on Swift code
```

### Apple Tooling & XcodeGen Synchronization
When modifying Apple project settings, target sources, or dependencies:
1. Update `iosApp/project.yml`.
2. Maintain synchronization between `iosApp/project.yml` and `iosApp/iosApp.xcodeproj/project.pbxproj` (especially pre-build script overrides like `OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED`).
3. Always run `xcodegen generate --spec iosApp/project.yml` prior to invoking `xcodebuild` or Xcode MCP tools (`BuildProject`, `RunProject`, `RunAllTests`).

### Dependency & Version Catalog Rules
- Prefer `gradle/libs.versions.toml` for all dependency declarations; avoid ad-hoc versions in module build scripts.
- Treat dependency upgrades as coordinated changes: update the version catalog, module scripts, and related specification notes together.
- Preserve the existing `BuildKonfig` / `local.properties` pattern for API keys. Never commit secrets to version control.
