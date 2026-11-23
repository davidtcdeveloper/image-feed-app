plugins {
    alias(libs.plugins.imagefeed.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.imagefeed.metro)
}

kotlin {
    android {
        namespace = "com.example.imagefeed.feature.collections"
    }

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
