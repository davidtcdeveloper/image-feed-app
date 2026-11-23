plugins {
    alias(libs.plugins.imagefeed.kmp.library)
}

kotlin {
    android {
        namespace = "com.example.imagefeed.core.mvi"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlin.coroutines.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
