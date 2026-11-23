package com.example.imagefeed.buildlogic

import com.android.build.api.dsl.CompileSdkSpec
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import java.io.StringReader
import java.util.Properties

internal data class AndroidSdkConfig(
    val compileSdk: Int,
    val compileSdkMinor: Int?,
    val minSdk: Int,
    val targetSdk: Int,
)

internal fun Project.androidSdkConfig(): AndroidSdkConfig {
    val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

    fun versionInt(alias: String): Int =
        libs
            .findVersion(alias)
            .get()
            .requiredVersion
            .toInt()

    return AndroidSdkConfig(
        compileSdk = versionInt("android-compileSdk"),
        compileSdkMinor = versionInt("android-compileSdkMinor").takeIf { it > 0 },
        minSdk = versionInt("android-minSdk"),
        targetSdk = versionInt("android-targetSdk"),
    )
}

internal fun CompileSdkSpec.configureFromCatalog(sdk: AndroidSdkConfig) {
    version =
        release(sdk.compileSdk) {
            minorApiLevel = sdk.compileSdkMinor
        }
}

fun Project.readLocalProperty(key: String): String {
    val content =
        providers
            .fileContents(isolated.rootProject.projectDirectory.file("local.properties"))
            .asText
            .orNull
            ?: return ""
    val properties = Properties()
    properties.load(StringReader(content))
    return properties.getProperty(key, "")
}
