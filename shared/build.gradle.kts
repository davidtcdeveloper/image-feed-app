import java.util.Properties
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("com.codingfeline.buildkonfig")
    kotlin("plugin.serialization")
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                
                // Networking (Ktor)
                implementation("io.ktor:ktor-client-core:2.3.12")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
                implementation("io.ktor:ktor-client-logging:2.3.12")
                
                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
                
                // Dependency Injection
                implementation("io.insert-koin:koin-core:3.5.6")
            }
        }
        
        val androidMain by getting {
            dependencies {
                // Ktor Android Engine
                implementation("io.ktor:ktor-client-okhttp:2.3.12")
            }
        }
        
        // Define iosMain manually
        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                // Ktor iOS/Darwin Engine
                implementation("io.ktor:ktor-client-darwin:2.3.12")
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
    compileSdk = 34
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
