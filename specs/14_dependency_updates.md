# Specification 14: Dependency Updates Plan

This document outlines the plan for updating the project's dependencies to their latest stable versions to ensure compatibility with Kotlin 2.4.0 and AGP 9.2.1, and to leverage the latest features and security fixes.

## Proposed Version Changes

| Library | Current Version | Target Version | Notes |
| :--- | :--- | :--- | :--- |
| **BuildKonfig** | 0.15.2 | 0.21.2 | Improved KMP support. |
| **Kotlinx Coroutines** | 1.10.1 | 1.11.0 | Stability and performance. |
| **Kotlinx Serialization** | 1.8.0 | 1.11.0 | Bug fixes and performance. |
| **Koin** | 3.5.6 | 4.2.1 | Major update, KMP improvements. |
| **Ktor** | 2.3.12 | 3.5.0 | Major update (Ktor 3), better performance and engine support. |
| **AndroidX Activity** | 1.9.0 | 1.13.0 | Modern Android features. |
| **AndroidX Lifecycle** | 2.8.2 | 2.10.0 | Better Compose integration. |
| **AndroidX Navigation** | 2.7.7 | 2.9.8 | Bug fixes and stability. |
| **AndroidX Compose BOM** | 2024.06.00 | 2026.05.01 | Latest Compose features and bug fixes. |
| **Coil** | 2.7.0 | 3.4.0 | Migration to Coil 3 (KMP support). |
| **Kingfisher (iOS/macOS)** | (Previous) | None (Removed) | Removed to use native SwiftUI `AsyncImage` (zero-dependency and SPM-free approach). |

## Migration Steps

### 1. Update Gradle Version Catalog
Update `gradle/libs.versions.toml` with the new versions. For Coil 3, the artifact coordinate changes from `io.coil-kt:coil-compose` to `io.coil-kt.coil3:coil-compose`.

### 2. Ktor 3 Migration
Ktor 3 introduces some breaking changes in package naming and configuration.
*   Update imports from `io.ktor.client.features.*` to `io.ktor.client.plugins.*` (if not already done).
*   Review `ContentNegotiation` and `Logging` plugin configurations.

### 3. Coil 3 Migration (KMP)
Coil 3 is a multiplatform library.
*   Update artifact to `io.coil-kt.coil3:coil-compose`.
*   Coil 3 might require changes in how `ImageLoader` is initialized in KMP.

### 4. Koin 4 Migration
*   Review DI module definitions for any breaking changes in DSL.
*   Update `koin-androidx-compose` to the new KMP-compatible version if applicable.

### 5. iOS/macOS SPM Update & Kingfisher Removal
*   Reverted the custom `KFImage` wrapper's implementation to use SwiftUI's native `AsyncImage`.
*   Removed the Kingfisher package dependency declaration from `iosApp/project.yml`.
*   Regenerated the Xcode project using `xcodegen` to produce a completely SPM-dependency-free codebase. This avoids any `safe.bareRepository` git/fetch errors in Xcode and simplifies compilation on local and CI setups.

## Verification Plan
1.  Run `./gradlew clean build` to verify Android and Shared module compilation.
2.  Execute Unit Tests in `shared` module.
3.  Deploy `androidApp` and verify image loading and navigation.
4.  Open `iosApp` in Xcode, update packages, and verify compilation.
5.  Deploy `iosApp` and `macosApp` to verify functionality.
