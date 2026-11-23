plugins {
    alias(libs.plugins.imagefeed.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.example.imagefeed.core.model"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.serialization.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
