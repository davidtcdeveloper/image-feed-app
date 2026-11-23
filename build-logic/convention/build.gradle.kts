import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
}

group = "com.example.imagefeed.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

ktlint {
    outputToConsole.set(true)
    ignoreFailures.set(false)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.metro.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("quality") {
            id = "imagefeed.quality"
            implementationClass = "com.example.imagefeed.buildlogic.QualityConventionPlugin"
        }
        register("kmpLibrary") {
            id = "imagefeed.kmp.library"
            implementationClass = "com.example.imagefeed.buildlogic.KmpLibraryConventionPlugin"
        }
        register("metro") {
            id = "imagefeed.metro"
            implementationClass = "com.example.imagefeed.buildlogic.MetroConventionPlugin"
        }
        register("androidApplication") {
            id = "imagefeed.android.application"
            implementationClass = "com.example.imagefeed.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidTest") {
            id = "imagefeed.android.test"
            implementationClass = "com.example.imagefeed.buildlogic.AndroidTestConventionPlugin"
        }
    }
}
