package com.example.imagefeed.buildlogic

import dev.zacsweers.metro.gradle.MetroPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class MetroConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("dev.zacsweers.metro")

            extensions.configure(MetroPluginExtension::class.java) { metro ->
                metro.enabled.set(true)
                metro.debug.set(false)
            }
        }
    }
}
