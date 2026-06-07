# Specification 15: Gradle & KMP Modernization (AGP 9.0+)

This document outlines the final modernization of the project's build system to comply with AGP 9.2.1 and Kotlin Multiplatform 2.4.0, following the recommended "Built-in Kotlin" and "New DSL" architecture.

## Implementation Details

### 1. AGP Full Modernization Activation
*   **Action:** Removed legacy flags and enabled `android.newDsl=true` and `android.builtInKotlin=true` in `gradle.properties`.
*   **Reason:** This aligns the project with the future of Android development, enabling the more efficient, type-safe DSL and built-in Kotlin support within the Android Gradle Plugin.

### 2. Android App Migration
*   **Action:** Removed `id('org.jetbrains.kotlin.android')` from `androidApp/build.gradle.kts`.
*   **Reason:** With `android.builtInKotlin=true`, the `com.android.application` plugin handles Kotlin compilation natively, making the separate Kotlin plugin redundant and its usage deprecated.

### 3. Shared Library Migration (Android-KMP Plugin)
*   **Action:** Replaced `com.android.library` with the modern `com.android.kotlin.multiplatform.library` plugin.
*   **Action:** Migrated from `androidTarget()` to the `android { ... }` block inside the `kotlin { ... }` extension.
*   **Reason:** The new Android-KMP plugin is specifically designed for multiplatform libraries. It uses a streamlined single-variant architecture and integrates more cleanly with Kotlin Multiplatform's DSL.

### 5. Target Architecture Optimization (Apple Silicon)
*   **Action:** Removed Intel-specific targets (`iosX64()` and `macosX64()`).
*   **Reason:** These targets are for Intel-based Macs. Removing them resolves architecture mismatch warnings (like `iosX64Test` being disabled) when building on Apple Silicon. The project now focuses on `iosArm64`, `iosSimulatorArm64`, and `macosArm64`.

## Verification
*   `./gradlew help` executes successfully.
*   All previous deprecation warnings related to `org.jetbrains.kotlin.android` and legacy `Project.android` extensions have been resolved.
*   The project now uses the most modern, recommended Gradle architecture for KMP projects.
