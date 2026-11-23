package com.example.imagefeed.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            pluginManager.apply("imagefeed.quality")

            val sdk = androidSdkConfig()

            extensions.configure(KotlinMultiplatformExtension::class.java) { kmp ->
                kmp.jvmToolchain(17)

                (kmp as ExtensionAware).extensions.configure(
                    "android",
                    Action<KotlinMultiplatformAndroidLibraryTarget> { android ->
                        android.compileSdk {
                            configureFromCatalog(sdk)
                        }
                        android.minSdk = sdk.minSdk
                        android.withHostTest {}
                    },
                )

                kmp.iosArm64()
                kmp.iosSimulatorArm64()
                kmp.macosArm64()
            }
        }
    }
}
