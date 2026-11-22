import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.TestExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    // Kotlin plugins
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // Android plugins
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false

    // Build and analysis tools
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidx.room) apply false
}

val androidCompileSdk = libs.versions.android.compileSdk.get().toInt()
val androidCompileSdkMinor = libs.versions.android.compileSdkMinor.get().toInt().takeIf { it > 0 }
val androidMinSdk = libs.versions.android.minSdk.get().toInt()
val androidTargetSdk = libs.versions.android.targetSdk.get().toInt()

subprojects {
    pluginManager.withPlugin("com.android.application") {
        extensions.configure<ApplicationExtension> {
            compileSdk {
                version = release(androidCompileSdk) {
                    minorApiLevel = androidCompileSdkMinor
                }
            }
            defaultConfig {
                minSdk = androidMinSdk
                targetSdk = androidTargetSdk
            }
        }
    }

    pluginManager.withPlugin("com.android.test") {
        extensions.configure<TestExtension> {
            compileSdk {
                version = release(androidCompileSdk) {
                    minorApiLevel = androidCompileSdkMinor
                }
            }
            defaultConfig {
                minSdk = androidMinSdk
                targetSdk = androidTargetSdk
            }
        }
    }

    pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            extensions.configure<KotlinMultiplatformExtension> {
                extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                    compileSdk {
                        version = release(androidCompileSdk) {
                            minorApiLevel = androidCompileSdkMinor
                        }
                    }
                    minSdk = androidMinSdk
                }
            }
        }
    }

    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    pluginManager.apply("io.gitlab.arturbosch.detekt")

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension>("ktlint") {
        android.set(true)
        outputToConsole.set(true)
        ignoreFailures.set(false)
        filter {
            exclude("**/build/**")
            exclude("**/BuildKonfig.kt")
            exclude { element ->
                element.file.path.contains("generated")
            }
        }
    }

    tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask>().configureEach {
        exclude("**/build/**")
        exclude("**/BuildKonfig.kt")
        exclude { element ->
            element.file.path.contains("generated")
        }
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>(io.gitlab.arturbosch.detekt.Detekt::class.java).configureEach {
        buildUponDefaultConfig = true
        ignoreFailures = true
        reports.xml.required.set(true)
        reports.html.required.set(true)
        reports.sarif.required.set(true)
    }
}
