plugins {
    alias(libs.plugins.imagefeed.kmp.library)
    alias(libs.plugins.imagefeed.metro)
}

kotlin {
    android {
        namespace = "com.example.imagefeed.feature.favorites"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(project(":core:mvi"))
            implementation(project(":core:network"))
            implementation(project(":core:database"))
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlin.coroutines.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
