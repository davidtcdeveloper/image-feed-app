plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

kotlin {
    jvmToolchain(17)
    android {
        namespace = "com.example.imagefeed"
        compileSdk = 37
        minSdk = 29
        withHostTest {}
    }

    // Register iOS targets directly
    iosArm64()
    iosSimulatorArm64()

    // Register macOS targets directly
    macosArm64()

    // Configure frameworks for iOS targets
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "shared"
            isStatic = true
            export(project(":core:model"))
            export(project(":core:mvi"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(project(":core:mvi"))
            api(project(":core:network"))

            // Lifecycle ViewModel (KMP)
            implementation(libs.androidx.lifecycle.viewmodel)

            // Coroutines & Concurrency
            implementation(libs.kotlin.coroutines.core)

            // Networking (Ktor) - using bundle
            implementation(libs.bundles.ktor.common)

            // Serialization
            implementation(libs.kotlin.serialization.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

metro {
    enabled = true
    debug = false
    // desugaredProviderSeverity = "WARN"
}
