package com.example.imagefeed.buildlogic

import com.android.build.api.dsl.TestExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.test")
            pluginManager.apply("imagefeed.quality")

            val sdk = androidSdkConfig()

            extensions.configure(TestExtension::class.java) { android ->
                android.compileSdk {
                    configureFromCatalog(sdk)
                }
                android.defaultConfig {
                    minSdk = sdk.minSdk
                    targetSdk = sdk.targetSdk
                }
            }

            extensions.configure(KotlinAndroidProjectExtension::class.java) { kotlin ->
                kotlin.jvmToolchain(17)
            }
        }
    }
}
