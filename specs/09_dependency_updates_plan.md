# Implementation Step 9: Ecosystem Dependency Upgrade Plan

This specification outlines the evaluation, compatibility considerations, and the execution steps to upgrade all remaining ecosystem libraries in the application to their latest stable releases in 2026.

---

## 1. Dependency Analysis & Target Versions

Below is a detailed list of all ecosystem dependencies declared in `gradle/libs.versions.toml`, their current versions, and their corresponding target stable versions as of June 2026:

| Dependency Library | Current Version | Target Version (June 2026) | Coordinate Change Required? | Key Benefits / Impact |
| :--- | :--- | :--- | :--- | :--- |
| **Kotlinx Coroutines** | `1.8.1` | `1.11.0` | No | Performance gains, improved multi-threading support in KMP, and native Apple target optimizations. |
| **Kotlinx Serialization**| `1.7.1` | `1.11.0` | No | Compatibility with Kotlin 2.4.0, smaller JSON parsing footprint. |
| **Koin (Core & Compose)** | `3.5.6` | `4.2.1` | Yes (Android Compose coordinate) | Performance enhancements, closer integration with modern Compose compiler, and leaner runtime. |
| **Ktor Client** | `2.3.12` | `3.5.0` | Yes (Various Client Modules) | Major 3.x release, streamlined logging, HTTP content negotiation optimizations, smaller package size. |
| **Compose BOM** | `2024.06.00` | `2026.05.00` | No | Upgrades standard Compose UI packages (Material 3, graphics, layout) to latest stable versions. |
| **Navigation Compose** | `2.7.7` | `2.9.0` | No | Type-safe Compose navigation features, smoother backstack transitions. |
| **Coil Image Loader** | `2.7.0` | `3.4.0` | Yes (Coil 3 coordinate migration) | Transition from Coil 2 to Coil 3 (KMP-ready). Coordinate changes from `io.coil-kt:coil-compose` to `io.coil-kt.coil3:coil-compose`. |

---

## 2. Potential Breaking Changes & API Migrations

Upgrading across major versions (e.g., Ktor 2.x to 3.x, Coil 2.x to 3.x, and Koin 3.x to 4.x) introduces several API updates that will require source-level changes:

### A. Ktor Client 3.x Migration
- **Coordinate adjustments:** Content negotiation and serialization packages in Ktor 3.x might have minor coordinate packaging changes.
- **Client Configuration:** Custom client setup, timeout extensions, and logging configuration features are optimized but backwards-compatible in most scenarios.

### B. Coil 3.x KMP Integration
- **Coordinate migration:** Coil 3 relocates libraries under the `io.coil-kt.coil3` package group.
- **API change:** In Coil 3, configuring the custom cache and image loaders inside `androidApp` or the shared main code shifts to modern builder properties.

### C. Koin 4.x Upgrades
- **API updates:** Koin 4.x shifts package hierarchies slightly, especially around Compose integration.
- **Action:** Ensure standard `koin-androidx-compose` references are aligned.

---

## 3. Recommended Step-by-Step Execution Plan

To ensure minimum disruption, the upgrade should be conducted in isolated phases:

### Phase A: Core KMP Upgrades (Coroutines, Serialization, Koin)
1. Update `coroutines` to `1.11.0` and `serialization` to `1.11.0` in `gradle/libs.versions.toml`.
2. Update `koin` to `4.2.1`.
3. Verify compilation on shared and Android targets using `./gradlew compileKotlin`.

### Phase B: Ktor 3.x Network Client Migration
1. Update `ktor` version to `3.5.0` in `gradle/libs.versions.toml`.
2. Review and refactor any network client initialization code in `UnsplashApiClient.kt` if deprecated 2.x classes are flagged.
3. Validate compilation.

### Phase C: Jetpack Compose & UI Components Upgrade
1. Update `androidx-compose-bom` to `2026.05.00` and `androidx-navigation` to `2.9.0`.
2. Update `coil` version to `3.4.0` and update library group coordinates to `io.coil-kt.coil3` in `gradle/libs.versions.toml`.
3. Recompile the Android app using `./gradlew assembleDebug` and correct any Coil 3.x compiler warnings in `androidApp`.

### Phase D: iOS Verification & Linking
1. Re-run standard framework packaging and compilation.
2. Build `iosApp` targeting local simulator.
