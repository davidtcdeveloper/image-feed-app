pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "image-feed-app"
include(":core:model")
include(":core:mvi")
include(":core:network")
include(":feature:details")
include(":feature:feed")
include(":feature:search")
include(":feature:collections")
include(":feature:profile")
include(":shared")
include(":androidApp")

