package com.example.imagefeed.android.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
sealed interface AppRoute :
    NavKey,
    JavaSerializable {
    @Serializable
    data object Feed : AppRoute

    @Serializable
    data object Collections : AppRoute

    @Serializable
    data class CollectionDetails(
        val collectionId: String,
    ) : AppRoute

    @Serializable
    data class Search(
        val query: String = "",
    ) : AppRoute

    @Serializable
    data class PhotoDetails(
        val photoId: String,
    ) : AppRoute

    @Serializable
    data class UserProfile(
        val username: String,
    ) : AppRoute
}

data class TopLevelDestination(
    val route: AppRoute,
    val icon: ImageVector,
    val label: String,
)

val TOP_LEVEL_DESTINATIONS =
    listOf(
        TopLevelDestination(AppRoute.Feed, Icons.Default.Home, "Photos"),
        TopLevelDestination(AppRoute.Collections, Icons.AutoMirrored.Filled.List, "Collections"),
        TopLevelDestination(AppRoute.Search(), Icons.Default.Search, "Search"),
    )
