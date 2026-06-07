# Build and Dependency Rules

## Gradle and Tooling

- Prefer the existing `gradle/libs.versions.toml` version catalog instead of adding ad-hoc versions in build scripts.
- Keep Android, shared, and Apple build logic consistent when updating dependencies or plugin versions.
- When changing build configuration, preserve the current BuildKonfig/local.properties pattern for the Unsplash API key.

## Dependency Modernization

- Treat dependency upgrades as a coordinated change: update the version catalog, module scripts, and related spec notes together.
- Be careful with Ktor, Koin, Coil, and Apple-target build changes because they affect both shared and UI layers.
- Verify that changes still compile across the relevant targets before calling the work complete.

## Apple Build Notes

- If iOS/macOS changes are involved, keep the existing XcodeGen and Apple target setup in mind.
- Do not introduce duplicate platform-only logic when the shared module can absorb the behavior.
