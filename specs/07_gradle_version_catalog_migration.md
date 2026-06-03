# Implementation Step 7: Gradle Version Catalog Migration (`libs.versions.toml`)

This specification outlines the architecture and execution plan to migrate the current hardcoded dependency configurations across all Gradle build scripts into a unified, centralized **Gradle Version Catalog** (`libs.versions.toml`). This aligns with modern Android and Kotlin Multiplatform (KMP) development best practices.

---

## 1. Why Migrate to Gradle Version Catalog?

- **Single Source of Truth:** All versions and dependency coordinates reside in a single, git-tracked file (`gradle/libs.versions.toml`).
- **Type-Safe Accessors:** Gradle automatically generates type-safe builders (e.g., `libs.ktor.client.core`) for all declared libraries and plugins, preventing syntax typos and runtime version mismatches.
- **Unified Updates:** Upgrading a library across the `shared` module, `androidApp`, or custom modules requires changing a single line in the TOML file.
- **Dependency Grouping (Bundles):** Related dependencies (like Jetpack Compose libraries or Ktor components) can be bundled together and imported in a single statement.

---

## 2. Defining the Catalog: `gradle/libs.versions.toml`

A new directory `gradle/` must be created at the repository root, containing a `libs.versions.toml` file structured into four key blocks: `[versions]`, `[libraries]`, `[bundles]`, and `[plugins]`.

### Target `gradle/libs.versions.toml` File Structure:

```toml
[versions]
# Kotlin & Plugins
kotlin = "2.3.20"
agp = "8.8.0"          # Android Gradle Plugin
buildkonfig = "0.15.2"

# Core KMP Libraries
coroutines = "1.8.1"
serialization = "1.7.1"
koin = "3.5.6"

# Networking & Logging (Ktor)
ktor = "2.3.12"

# Android/Compose Libraries
androidx-activity = "1.9.0"
androidx-lifecycle = "2.8.2"
androidx-navigation = "2.7.7"
androidx-compose-bom = "2024.06.00"
coil = "2.7.0"
junit = "4.13.2"

[libraries]
# Coroutines & Serialization
kotlin-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlin-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }

# Ktor Network Client
ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { group = "io.ktor", name = "ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { group = "io.ktor", name = "ktor-client-darwin", version.ref = "ktor" }
ktor-client-logging = { group = "io.ktor", name = "ktor-client-logging", version.ref = "ktor" }
ktor-client-contentNegotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }

# Koin Dependency Injection
koin-core = { group = "io.insert-koin", name = "koin-core", version.ref = "koin" }
koin-android-compose = { group = "io.insert-koin", name = "koin-androidx-compose", version.ref = "koin" }

# Jetpack Compose BOM & Core Views
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "androidx-compose-bom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }

# Android Lifecycle, Activity, Navigation & Image Loading
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "androidx-activity" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "androidx-lifecycle" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "androidx-navigation" }
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

# Testing
junit = { group = "junit", name = "junit", version.ref = "junit" }

[bundles]
# Grouping related dependencies for concise import blocks
ktor-common = [
    "ktor-client-core",
    "ktor-client-contentNegotiation",
    "ktor-serialization-json",
    "ktor-client-logging"
]
compose-ui = [
    "androidx-compose-ui",
    "androidx-compose-ui-graphics",
    "androidx-compose-ui-tooling-preview",
    "androidx-compose-material3"
]

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
buildkonfig = { id = "com.codingfeline.buildkonfig", version.ref = "buildkonfig" }
```

---

## 3. Step-by-Step Refactoring Plan

### Step 1: Update Root Build Configuration (`build.gradle.kts`)
Simplify plugin applications to use version catalog definitions:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.buildkonfig) apply false
}
```

---

### Step 2: Refactor Shared Module Build Configuration (`shared/build.gradle.kts`)
Replace hardcoded strings inside source sets block with catalog accessors:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.kotlin.serialization)
}

// ... targets configuration ...

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Centralized Coroutines & Serialization
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.serialization.json)
                
                // Bundled Ktor Networking core, negotiations, serializer, and logging
                implementation(libs.bundles.ktor.common)
                
                // Dependency Injection
                implementation(libs.koin.core)
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        
        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        // ... ios native sub-targets ...
    }
}
```

---

### Step 3: Refactor Android Application Build Configuration (`androidApp/build.gradle.kts`)
Clean up dependencies mapping by utilising the unified Compose Bundle and library catalogs:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ... android config ...

dependencies {
    implementation(project(":shared"))
    
    // Jetpack Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    
    // Bundled Core UI Elements (UI, graphics, tooling-preview, material3)
    implementation(libs.bundles.compose.ui)
    
    // Core Compose components
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime-compose)
    
    // Coil (Image Loading) & Koin DI
    implementation(libs.coil-compose)
    implementation(libs.koin.android.compose)
    
    // Tooling/Test
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
```

---

## 4. Verification Checklist

- [ ] **Catalog Creation:** Create `gradle` directory and populate `libs.versions.toml`.
- [ ] **Root Build Sync:** Apply alias loaders in `/build.gradle.kts` and trigger Gradle sync.
- [ ] **Shared Module Migration:** Replace coordinates in `shared/build.gradle.kts` and verify multiplatform targets resolve cleanly.
- [ ] **Android App Migration:** Replace coordinates in `androidApp/build.gradle.kts` and verify type-safety binding.
- [ ] **Compilation Validation:** Run `./gradlew assembleDebug` to guarantee 100% compilation success without deprecated properties or broken links.
