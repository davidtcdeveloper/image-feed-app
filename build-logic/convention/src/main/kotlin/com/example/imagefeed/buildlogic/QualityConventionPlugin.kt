package com.example.imagefeed.buildlogic

import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask

class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")
            pluginManager.apply("io.gitlab.arturbosch.detekt")

            extensions.configure(KtlintExtension::class.java) { ktlint ->
                ktlint.android.set(true)
                ktlint.outputToConsole.set(true)
                ktlint.ignoreFailures.set(false)
                ktlint.filter {
                    it.exclude("**/build/**")
                    it.exclude("**/BuildKonfig.kt")
                    it.exclude { element ->
                        element.file.path.contains("generated")
                    }
                }
            }

            tasks.withType(BaseKtLintCheckTask::class.java).configureEach { task ->
                task.exclude("**/build/**")
                task.exclude("**/BuildKonfig.kt")
                task.exclude { element ->
                    element.file.path.contains("generated")
                }
            }

            tasks.withType(Detekt::class.java).configureEach { task ->
                task.buildUponDefaultConfig = true
                task.ignoreFailures = true
                task.reports { reports ->
                    reports.xml.required.set(true)
                    reports.html.required.set(true)
                    reports.sarif.required.set(true)
                }
            }
        }
    }
}
