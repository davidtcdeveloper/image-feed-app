import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.kotlin.serialization)
}

// Read Unsplash API Access Key from local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val unsplashApiKey = localProperties.getProperty("unsplash.api.key") ?: ""

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
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Coroutines & Concurrency
            implementation(libs.kotlin.coroutines.core)

            // Networking (Ktor) - using bundle
            implementation(libs.bundles.ktor.common)

            // Serialization
            implementation(libs.kotlin.serialization.json)

            // Dependency Injection
            implementation(libs.koin.core)
        }

        androidMain.dependencies {
            // Ktor Android Engine
            implementation(libs.ktor.client.okhttp)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        appleMain.dependencies {
            // Ktor iOS/macOS/Darwin Engine
            implementation(libs.ktor.client.darwin)
        }
    }
}

buildkonfig {
    packageName = "com.example.imagefeed"
    defaultConfigs {
        buildConfigField(Type.STRING, "UNSPLASH_API_KEY", unsplashApiKey)
    }
}

tasks.named("generateBuildKonfig") {
    doLast {
        val generatedFile = file("build/generated/source/buildkonfig/commonMain/com/example/imagefeed/BuildKonfig.kt")
        if (generatedFile.exists()) {
            generatedFile.writeText(
                generatedFile.readText().replace(Regex("^  (?=public|internal)", RegexOption.MULTILINE), "    "),
            )
        }
    }
}
