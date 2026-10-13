@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.example.imagefeed.android.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.example.imagefeed.android.CollectionDetailScreen
import com.example.imagefeed.android.CollectionsFeedScreen
import com.example.imagefeed.android.FeedScreen
import com.example.imagefeed.android.PhotoDetailPlaceholder
import com.example.imagefeed.android.PhotoDetailsScreen
import com.example.imagefeed.android.SearchScreen
import com.example.imagefeed.android.UserProfileScreen
import com.example.imagefeed.di.MetroHelper

@Composable
fun <T> dropUnlessResumedWithArg(block: (T) -> Unit): (T) -> Unit {
    val lifecycleOwner = LocalLifecycleOwner.current
    return { value ->
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            block(value)
        }
    }
}

fun EntryProviderScope<NavKey>.feedSection(
    sharedTransitionScope: SharedTransitionScope,
    navigator: Navigator,
    onRandomClick: () -> Unit,
) {
    entry<AppRoute.Feed>(
        metadata =
            ListDetailSceneStrategy.listPane(
                detailPlaceholder = { PhotoDetailPlaceholder() },
            ),
    ) {
        FeedScreen(
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            viewModel =
                viewModel(key = "feed") {
                    MetroHelper.getFeedViewModel()
                },
            reselectEvents = navigator.reselectEvents,
            onPhotoClick =
                dropUnlessResumedWithArg { photo ->
                    navigator.navigate(AppRoute.PhotoDetails(photo.id))
                },
            onUserClick =
                dropUnlessResumedWithArg { user ->
                    navigator.navigate(AppRoute.UserProfile(user.username))
                },
            onSearchClick =
                dropUnlessResumed {
                    navigator.navigate(AppRoute.Search())
                },
            onRandomClick = onRandomClick,
        )
    }
}

fun EntryProviderScope<NavKey>.collectionsSection(
    sharedTransitionScope: SharedTransitionScope,
    navigator: Navigator,
) {
    entry<AppRoute.Collections>(
        metadata =
            ListDetailSceneStrategy.listPane(
                detailPlaceholder = { PhotoDetailPlaceholder() },
            ),
    ) {
        CollectionsFeedScreen(
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            viewModel =
                viewModel(key = "collections") {
                    MetroHelper.getCollectionsFeedViewModel()
                },
            reselectEvents = navigator.reselectEvents,
            onCollectionClick =
                dropUnlessResumedWithArg { collection ->
                    navigator.navigate(AppRoute.CollectionDetails(collection.id))
                },
            onSearchClick =
                dropUnlessResumed {
                    navigator.navigate(AppRoute.Search())
                },
        )
    }
    entry<AppRoute.CollectionDetails>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) { key ->
        CollectionDetailScreen(
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            collectionId = key.collectionId,
            viewModel =
                viewModel(key = "collection_${key.collectionId}") {
                    MetroHelper.getCollectionDetailViewModel(key.collectionId)
                },
            onBack = dropUnlessResumed { navigator.goBack() },
            onPhotoClick =
                dropUnlessResumedWithArg { photo ->
                    navigator.navigate(AppRoute.PhotoDetails(photo.id))
                },
            onCollectionClick =
                dropUnlessResumedWithArg { id ->
                    navigator.navigate(AppRoute.CollectionDetails(id))
                },
        )
    }
}

fun EntryProviderScope<NavKey>.searchSection(
    sharedTransitionScope: SharedTransitionScope,
    navigator: Navigator,
) {
    entry<AppRoute.Search>(
        metadata =
            ListDetailSceneStrategy.listPane(
                detailPlaceholder = { PhotoDetailPlaceholder() },
            ),
    ) { key ->
        SearchScreen(
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            initialQuery = key.query,
            viewModel =
                viewModel(key = "search_${key.query}") {
                    MetroHelper.getUnifiedSearchViewModel()
                },
            reselectEvents = navigator.reselectEvents,
            onBack = dropUnlessResumed { navigator.goBack() },
            onPhotoClick =
                dropUnlessResumedWithArg { photo ->
                    navigator.navigate(AppRoute.PhotoDetails(photo.id))
                },
            onUserClick =
                dropUnlessResumedWithArg { user ->
                    navigator.navigate(AppRoute.UserProfile(user.username))
                },
            onCollectionClick =
                dropUnlessResumedWithArg { collection ->
                    navigator.navigate(AppRoute.CollectionDetails(collection.id))
                },
        )
    }
}

fun EntryProviderScope<NavKey>.photoDetailsSection(
    sharedTransitionScope: SharedTransitionScope,
    navigator: Navigator,
) {
    entry<AppRoute.PhotoDetails>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) { key ->
        PhotoDetailsScreen(
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            photoId = key.photoId,
            viewModel =
                viewModel(key = "photo_${key.photoId}") {
                    MetroHelper.getPhotoDetailsViewModel(key.photoId)
                },
            onBack = dropUnlessResumed { navigator.goBack() },
            onUserClick =
                dropUnlessResumedWithArg { username ->
                    navigator.navigate(AppRoute.UserProfile(username))
                },
            onTagClick =
                dropUnlessResumedWithArg { tag ->
                    navigator.navigate(AppRoute.Search(query = tag))
                },
        )
    }
}

fun EntryProviderScope<NavKey>.userProfileSection(
    sharedTransitionScope: SharedTransitionScope,
    navigator: Navigator,
) {
    entry<AppRoute.UserProfile>(
        metadata = ListDetailSceneStrategy.extraPane(),
    ) { key ->
        UserProfileScreen(
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            username = key.username,
            viewModel =
                viewModel(key = "user_${key.username}") {
                    MetroHelper.getUserProfileViewModel(key.username)
                },
            onBack = dropUnlessResumed { navigator.goBack() },
            onPhotoClick =
                dropUnlessResumedWithArg { photo ->
                    navigator.navigate(AppRoute.PhotoDetails(photo.id))
                },
            onCollectionClick =
                dropUnlessResumedWithArg { collection ->
                    navigator.navigate(AppRoute.CollectionDetails(collection.id))
                },
        )
    }
}
