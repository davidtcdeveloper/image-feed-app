import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val unsplashApiKey = localProperties.getProperty("unsplash.api.key") ?: ""

kotlin {
    jvmToolchain(17)
    android {
        namespace = "com.example.imagefeed.core.network"
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

tasks.named("generateBuildKonfig") {
    doLast {
        val generatedFile = file("build/generated/source/buildkonfig/commonMain/com/example/imagefeed/core/network/BuildKonfig.kt")
        if (generatedFile.exists()) {
            generatedFile.writeText(
                generatedFile.readText().replace(Regex("^  (?=public|internal)", RegexOption.MULTILINE), "    "),
            )
        }
    }
}
