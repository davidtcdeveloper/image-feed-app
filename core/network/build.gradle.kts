import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
import com.example.imagefeed.buildlogic.readLocalProperty

plugins {
    alias(libs.plugins.imagefeed.kmp.library)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.imagefeed.metro)
}

val unsplashApiKey = readLocalProperty("unsplash.api.key")

kotlin {
    android {
        namespace = "com.example.imagefeed.core.network"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(libs.ktor.client.core)
            implementation(libs.bundles.ktor.common)
            implementation(libs.kotlin.coroutines.core)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        appleMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

buildkonfig {
    packageName = "com.example.imagefeed.core.network"
    defaultConfigs {
        buildConfigField(Type.STRING, "UNSPLASH_API_KEY", unsplashApiKey)
    }
}
