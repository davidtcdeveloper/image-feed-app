plugins {
    // Kotlin plugins
    id("org.jetbrains.kotlin.multiplatform") version "2.3.20" apply false
    id("org.jetbrains.kotlin.android") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false

    // Android plugins
    id("com.android.application") version "8.8.0" apply false
    id("com.android.library") version "8.8.0" apply false

    // BuildKonfig
    id("com.codingfeline.buildkonfig") version "0.15.2" apply false
}
