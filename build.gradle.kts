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

    // Build and analysis tools
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

subprojects {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    pluginManager.apply("io.gitlab.arturbosch.detekt")

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension>("ktlint") {
        android.set(true)
        outputToConsole.set(true)
        ignoreFailures.set(false)
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>(io.gitlab.arturbosch.detekt.Detekt::class.java).configureEach {
        buildUponDefaultConfig = true
        ignoreFailures = true
        reports.xml.required.set(true)
        reports.html.required.set(true)
        reports.sarif.required.set(true)
    }
}
