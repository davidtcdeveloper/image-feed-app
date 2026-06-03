import java.util.Properties
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
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
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    
    // Register iOS targets directly
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    // Configure frameworks for iOS targets
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Coroutines & Concurrency
                implementation(libs.kotlin.coroutines.core)
                
                // Networking (Ktor) - using bundle
                implementation(libs.bundles.ktor.common)
                
                // Serialization
                implementation(libs.kotlin.serialization.json)
                
                // Dependency Injection
                implementation(libs.koin.core)
            }
        }
        
        val androidMain by getting {
            dependencies {
                // Ktor Android Engine
                implementation(libs.ktor.client.okhttp)
            }
        }
        
        // Define iosMain manually
        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                // Ktor iOS/Darwin Engine
                implementation(libs.ktor.client.darwin)
            }
        }
        
        // Connect native target source sets to iosMain
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
    }
}

android {
    namespace = "com.example.imagefeed"
    compileSdk = 37
    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

buildkonfig {
    packageName = "com.example.imagefeed"
    defaultConfigs {
        buildConfigField(Type.STRING, "UNSPLASH_API_KEY", unsplashApiKey)
    }
}
