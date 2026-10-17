package com.example.imagefeed.android.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.example.imagefeed.android.R
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
    @StringRes val labelRes: Int,
)

val TOP_LEVEL_DESTINATIONS =
    listOf(
        TopLevelDestination(AppRoute.Feed, Icons.Default.Home, R.string.nav_tab_photos),
        TopLevelDestination(AppRoute.Collections, Icons.AutoMirrored.Filled.List, R.string.nav_tab_collections),
        TopLevelDestination(AppRoute.Search(), Icons.Default.Search, R.string.nav_tab_search),
    )
