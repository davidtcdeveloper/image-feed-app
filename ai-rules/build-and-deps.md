# Build and Dependency Rules

## Gradle and Tooling

- Prefer the existing `gradle/libs.versions.toml` version catalog instead of adding ad-hoc versions in build scripts.
- Keep Android, shared, and Apple build logic consistent when updating dependencies or plugin versions.
- When changing build configuration, preserve the current BuildKonfig/local.properties pattern for the Unsplash API key.
- Build, test, and code-analysis commands (`./gradlew`, `xcodebuild`, `xcodegen`, `swiftlint`, `swiftformat`) require host execution permissions (`BypassSandbox: true` in agent environments) because they interact with external system SDKs (JDK, Android SDK, Gradle cache in `~/.gradle`, and Xcode toolchain).

## Dependency Modernization

- Treat dependency upgrades as a coordinated change: update the version catalog, module scripts, and related spec notes together.
- Be careful with Ktor, Metro (DI), Coil, and Apple-target build changes because they affect both shared and UI layers.
- Verify that changes still compile across the relevant targets before calling the work complete.

## Apple Build Notes

- If iOS/macOS changes are involved, keep the existing XcodeGen and Apple target setup in mind.
- Do not introduce duplicate platform-only logic when the shared module can absorb the behavior.
- Before invoking Xcode CLI tools (`xcodebuild`) or lazy Xcode MCP tools (`BuildProject`, `RunProject`, `RunAllTests`, etc.), always regenerate the Xcode project using `xcodegen generate --spec iosApp/project.yml` whenever project files, sources, or configurations are modified.
- Always verify iOS builds after shared changes with:
  `xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`
- When editing Xcode project settings or build scripts, maintain synchronization between `iosApp/project.yml` and `iosApp/iosApp.xcodeproj/project.pbxproj` (especially pre-build script overrides like `OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED`).
- When auditing shared declarations for dead code, always inspect `iosApp/` for Swift consumers before deleting. Code that must be invoked from Swift only and has no calls from Kotlin should be annotated with `@Suppress("unused") // Invoked on Swift code`.

## Code Formatting and Linting

- For Swift: run `swiftformat .` to auto-format sources and `swiftformat --lint .` to verify. Run `swiftlint lint iosApp/iosApp` for Swift style rules.
- For Kotlin: run `./gradlew ktlintFormat` to auto-format sources and `./gradlew ktlintCheck detekt` to verify.
