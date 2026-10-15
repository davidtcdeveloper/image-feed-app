plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
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
            export(project(":feature:details"))
            export(project(":feature:feed"))
            export(project(":feature:search"))
            export(project(":feature:collections"))
            export(project(":feature:profile"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(project(":core:mvi"))
            implementation(project(":core:network"))
            api(project(":feature:details"))
            api(project(":feature:feed"))
            api(project(":feature:search"))
            api(project(":feature:collections"))
            api(project(":feature:profile"))

            // Lifecycle ViewModel (KMP)
            implementation(libs.androidx.lifecycle.viewmodel)

            // Coroutines & Concurrency
            implementation(libs.kotlin.coroutines.core)
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
