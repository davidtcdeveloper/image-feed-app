package com.example.imagefeed.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("imagefeed.quality")

            val sdk = androidSdkConfig()

            extensions.configure(ApplicationExtension::class.java) { android ->
                android.compileSdk {
                    configureFromCatalog(sdk)
                }
                android.defaultConfig {
                    minSdk = sdk.minSdk
                    targetSdk = sdk.targetSdk
                }
                android.compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }

            extensions.configure(KotlinAndroidProjectExtension::class.java) { kotlin ->
                kotlin.jvmToolchain(17)
            }
        }
    }
}
