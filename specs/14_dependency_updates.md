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

---

## July 2026 Dependency Assessment & Update Plan

An audit of the codebase's versions catalog (`gradle/libs.versions.toml`) and project settings was performed on July 18, 2026, comparing the current setup to the latest available stable releases.

### 1. Dependency Analysis Table

Below is the comparison of currently declared versions against target versions for July 2026:

| Dependency Library / Plugin | Current Version | Target Version | Update Status | Key Benefits / Impact |
| :--- | :--- | :--- | :--- | :--- |
| **Gradle Wrapper** | `9.5.1` | `9.6.1` | Outdated | Better performance, configuration cache optimizations, and build lifecycle bug fixes. |
| **Kotlin** | `2.4.0` | `2.4.10` | Outdated | Compiler performance enhancements, K2 compiler fixes, and improved platform linking stability. |
| **Android Gradle Plugin (AGP)** | `9.2.1` | `9.3.0` | Outdated | Full compatibility with Gradle 9.5+, minor configuration improvements. |
| **BuildKonfig** | `0.21.2` | `0.22.0` | Outdated | Minor multiplatform configuration fixes. |
| **ktlint (Gradle Plugin)** | `13.0.0` | `14.2.0` | Outdated | Enhanced speed, better support for Isolated Projects, and newer formatting rules. |
| **detekt (Gradle Plugin)** | `1.23.7` | `1.23.8` | Outdated | Stable bug fixes in the 1.x branch. (2.x is currently in alpha and has coordinate changes). |
| **Kotlinx Coroutines** | `1.11.0` | `1.11.0` | Up to Date | No action required. |
| **Kotlinx Serialization** | `1.11.0` | `1.11.0` | Up to Date | No action required. |
| **Metro DI** | `1.2.1` | `1.3.0` | Outdated | Faster compile-time dependency graph generation and FIR analyzer updates. |
| **Ktor Client** | `3.5.0` | `3.5.1` | Outdated | Engine optimizations, minor logging and negotiation tweaks. |
| **AndroidX Activity** | `1.13.0` | `1.13.0` | Up to Date | No action required. |
| **AndroidX Lifecycle** | `2.10.0` | `2.10.0` | Up to Date | No action required. |
| **AndroidX Navigation** | `2.9.8` | `2.9.8` | Up to Date | No action required. |
| **AndroidX Navigation 3** | `1.2.0-alpha04` | `1.2.0-alpha06` | Outdated | Alpha stability updates for modern state-driven Compose navigation. |
| **AndroidX Lifecycle Navigation 3** | `2.11.0-rc01` | `2.11.0` | Outdated | Transition from release candidate to official stable release. |
| **AndroidX Compose BOM** | `2026.05.01` | `2026.06.01` | Outdated | Updates individual Compose packages to latest bug-fix revisions. |
| **Coil** | `3.5.0` | `3.5.0` | Up to Date | No action required. |
| **play-services-maps** | `19.2.0` | `20.0.0` | Outdated | Major version update for Android Play Services Maps SDK. |
| **maps-compose** | `8.3.0` | `8.3.1` | Outdated | Stability fixes for Composable Maps elements. |

### 2. Integration & Migration Considerations

#### A. Gradle 9.6.1 & AGP 9.3.0 Compatibility
*   Gradle 9.6.x requires JDK 17+ or JDK 21+ for compilation. The project is already configured with `jvmToolchain(17)`, which is fully supported.
*   Check for deprecations inside build files regarding tasks or syntax when transitioning to AGP 9.3.0.

#### B. AndroidX Navigation 3 & Lifecycle Navigation 3
*   Upgrading `navigation3` to `1.2.0-alpha06` and `lifecycle-viewmodel-navigation3` to `2.11.0` (stable) is a safe step. It aligns the experimental Navigation 3 modules with stable Lifecycle lifecycle-aware architecture components.

#### C. play-services-maps 20.0.0 & maps-compose 8.3.1
*   Upgrading the Google Maps components provides a major SDK update.
*   Be sure to check if any deprecated renderer options require manifest additions such as `<uses-library android:name="org.apache.http.legacy" android:required="false" />` on target platforms.

### 3. Step-by-Step Execution Plan

We recommend performing the updates in distinct, sequential phases:

#### Phase A: Build System & Linter Tooling Upgrades
1.  **Gradle Wrapper Update:**
    Update `gradle/wrapper/gradle-wrapper.properties` to `https\://services.gradle.org/distributions/gradle-9.6.1-bin.zip`.
2.  **Kotlin, AGP, and Compiler Tooling Updates:**
    In `gradle/libs.versions.toml`, update:
    *   `kotlin = "2.4.10"`
    *   `agp = "9.3.0"`
    *   `buildkonfig = "0.22.0"`
3.  **Linter Upgrades:**
    In `gradle/libs.versions.toml`, update:
    *   `ktlint = "14.2.0"`
    *   `detekt = "1.23.8"`
4.  **Verification:**
    Verify compilation and linter configurations by running `./gradlew ktlintCheck detekt`. Fix any new rules or warning changes immediately.

#### Phase B: Core Frameworks & DI Update
1.  **Ktor & Metro DI Updates:**
    In `gradle/libs.versions.toml`, update:
    *   `ktor = "3.5.1"`
    *   `metro = "1.2.1"` -> `metro = "1.3.0"`
2.  **Verification:**
    Run `./gradlew :shared:allTests` to ensure shared networking, serialization, and DI components compile and pass tests successfully.

#### Phase C: UI, AndroidX & Google Maps Upgrades
1.  **AndroidX BOM & Navigation Updates:**
    In `gradle/libs.versions.toml`, update:
    *   `androidx-compose-bom = "2026.06.01"`
    *   `androidx-navigation3 = "1.2.0-alpha06"`
    *   `androidx-lifecycle-navigation3 = "2.11.0"`
2.  **Maps SDK Updates:**
    In `gradle/libs.versions.toml`, update:
    *   `play-services-maps = "20.0.0"`
    *   `maps-compose = "8.3.1"`
3.  **Verification:**
    Build the full Android debug target using `./gradlew :androidApp:assembleDebug` and execute a manual validation of image loading, maps, navigation transitions, and animation rendering.

#### Phase D: Apple Platform Verification
1.  Verify shared Apple target compilation by running `./gradlew :shared:compileKotlinIosSimulatorArm64`.
2.  Regenerate the Xcode project (`cd iosApp && xcodegen`) and compile target simulators in Xcode to verify SPM-free SwiftUI building remains green.

