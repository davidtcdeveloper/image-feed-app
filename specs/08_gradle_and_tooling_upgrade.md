# Implementation Step 8: Gradle and Tooling Upgrade Plan (Gradle 9.5.1, Kotlin 2.4.0, AGP 9.1.1)

This specification outlines the evaluation, requirements, and step-by-step execution plan to upgrade the project's build system—specifically Gradle, Kotlin, and the Android Gradle Plugin (AGP)—to their absolute latest stable versions.

---

## 1. Current State vs. Target State

Below is the comparison of the current tooling versions in the codebase against the latest available stable versions as of June 2026:

| Tool / Plugin | Current Version | Target Version (Latest Stable) | Role / Impact |
| :--- | :--- | :--- | :--- |
| **Gradle** | `9.5.1` | `9.5.1` (or next stable `9.6.x`) | Central build runner. Ensure compatibility with JVM 17/21 and Configuration Cache. |
| **Kotlin** | `2.3.20` | `2.4.0` | Multiplatform language version, compiler performance, and native iOS linking optimization. |
| **Android Gradle Plugin** | `8.8.0` | `9.1.1` | Android compilation, packaging, and asset generation. |
| **Java JDK / Toolchain**| `17` | `17` or `21` | Required execution environment for Gradle 9.x and AGP 9.x. |

---

## 2. Key Impacts & Technical Considerations

Before performing the upgrade, several critical architectural updates and potential deprecations must be evaluated:

### A. JDK 17/21 Baseline Requirement
- **Impact:** AGP 9.1.1 and Gradle 9.5+ mandate **JDK 17** at a minimum for running the builds, with **JDK 21** highly recommended.
- **Action:** Ensure the developer environment (Android Studio / terminal) has `JAVA_HOME` pointed to JDK 17 or JDK 21.

### B. Configuration Cache & Isolated Projects
- **Impact:** Gradle 9.x enforces strict compliance with the **Configuration Cache**. Features like dynamic project evaluation during task execution are restricted.
- **Action:** Ensure build scripts do not reference `Project` instances directly during task execution. Plugins must be configuration-cache compliant.

### C. Kotlin 2.4.0 Multiplatform & Swift Interop
- **Impact:** Kotlin 2.4.0 changes how native framework binaries are exported, improving Swift export safety and performance.
- **Action:** Ensure our SwiftUI view models still conform cleanly. Test the linked framework using standard Xcode builds.

### D. AGP 9.1.1 Plugin Integration Changes
- **Impact:** In AGP 9.x, Gradle integrates Kotlin by default, reducing the need for verbose legacy plugin application code.
- **Action:** Use version catalog plugins correctly as configured in Phase 7.

---

## 3. Detailed Upgrade Plan

### Step 1: Update the Gradle Wrapper
Upgrade the Gradle distribution used by the wrapper:
```bash
./gradlew wrapper --gradle-version 9.5.1 --distribution-type bin
```
This updates `gradle/wrapper/gradle-wrapper.properties` with:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.5.1-bin.zip
```

### Step 2: Update Version Catalog Coordinates
In `gradle/libs.versions.toml`, update the version variables inside `[versions]` block:
```toml
[versions]
# Kotlin & Plugins
kotlin = "2.4.0"
agp = "9.1.1"
buildkonfig = "0.15.2"  # Ensure compatibility with Kotlin 2.4.0
```

### Step 3: Align JVM Toolchains and Options
In `shared/build.gradle.kts` and `androidApp/build.gradle.kts`, ensure that the Kotlin DSL is fully compliant with Gradle 9.x/Kotlin 2.4.0 standards.

1. **In `shared/build.gradle.kts`:**
   ```kotlin
   kotlin {
       androidTarget {
           compilerOptions {
               jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
           }
       }
   }
   ```

2. **In `androidApp/build.gradle.kts`:**
   ```kotlin
   android {
       compileSdk = 35  # Recommended for AGP 9.1.x
       defaultConfig {
           targetSdk = 35
       }
       compileOptions {
           sourceCompatibility = JavaVersion.VERSION_17
           targetCompatibility = JavaVersion.VERSION_17
       }
       kotlin {
           jvmToolchain(17)
       }
   }
   ```

### Step 4: Verification and Validation Steps
Once the plans are approved, the upgrade will be verified using:
1. **Clean Workspace:**
   ```bash
   ./gradlew clean
   ```
2. **Verify Configuration Cache Compatibility:**
   ```bash
   ./gradlew assembleDebug --configuration-cache
   ```
3. **Verify Multiplatform Targets:**
   ```bash
   ./gradlew compileKotlinIosX64 compileKotlinIosArm64 compileKotlinIosSimulatorArm64
   ```
4. **Xcode Verification:**
   ```bash
   xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -arch arm64 build
   ```
