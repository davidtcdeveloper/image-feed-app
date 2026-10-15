plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

kotlin {
    jvmToolchain(17)
    android {
        namespace = "com.example.imagefeed.feature.search"
        compileSdk = 37
        minSdk = 29
        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()
    macosArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(project(":core:mvi"))
            implementation(project(":core:network"))
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlin.coroutines.core)
            implementation(libs.bundles.ktor.common)
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
}
