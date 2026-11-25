package com.example.imagefeed.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.imagefeed.android.components.PhotoCard
import com.example.imagefeed.android.designsystem.SatinHeaderScaffold
import com.example.imagefeed.android.navigation.AppRoute
import com.example.imagefeed.android.preview.PreviewScaffold
import com.example.imagefeed.android.preview.UiPreviewFixtures
import com.example.imagefeed.android.theme.ExpressiveLoadingIndicator
import com.example.imagefeed.android.theme.ExpressiveShapeTokens
import com.example.imagefeed.android.util.localizedMessage
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.User
import com.example.imagefeed.presentation.favorites.FavoritesEffect
import com.example.imagefeed.presentation.favorites.FavoritesIntent
import com.example.imagefeed.presentation.favorites.FavoritesState
import com.example.imagefeed.presentation.favorites.FavoritesViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    reselectEvents: SharedFlow<NavKey>? = null,
    onPhotoClick: (Photo) -> Unit,
    onUserClick: (User) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = FavoritesState())
    val listState = rememberLazyStaggeredGridState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FavoritesEffect.ShowFavoriteRemovedSnackbar -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.details_bookmark_removed))
                    }
                }
                is FavoritesEffect.ShowErrorSnackbar -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    launch {
                        snackbarHostState.showSnackbar(effect.error.localizedMessage(context))
                    }
                }
            }
        }
    }

    LaunchedEffect(reselectEvents) {
        reselectEvents?.collect { key ->
            if (key == AppRoute.Favorites && listState.layoutInfo.totalItemsCount > 0) {
                listState.animateScrollToItem(0)
            }
        }
    }

    FavoritesScreenContent(
        state = state,
        listState = listState,
        snackbarHostState = snackbarHostState,
        onRemoveFavorite = { viewModel.dispatch(FavoritesIntent.RemoveFavorite(it)) },
        onPhotoClick = onPhotoClick,
        onUserClick = onUserClick,
    )
}

@Composable
fun FavoritesScreenContent(
    state: FavoritesState,
    onRemoveFavorite: (String) -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onUserClick: (User) -> Unit,
    listState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    SatinHeaderScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = calculateBottomContentPadding(8.dp)),
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.favorites_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding()),
        ) {
            if (state.isLoading) {
                ExpressiveLoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.photos.isEmpty()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(top = paddingValues.calculateTopPadding())
                            .padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(88.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = ExpressiveShapeTokens.SoftScallopShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.favorites_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.favorites_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                val itemWidthPx = calculatePhotoItemWidthPx()
                var selectedPhotoId by rememberSaveable { mutableStateOf<String?>(null) }

                LazyVerticalStaggeredGrid(
                    columns = calculateGridColumns(),
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = 8.dp,
                            top = paddingValues.calculateTopPadding() + 8.dp,
                            end = 8.dp,
                            bottom = calculateBottomContentPadding(8.dp),
                        ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                ) {
                    itemsIndexed(
                        state.photos,
                        key = { _, photo -> photo.id },
                        contentType = { _, _ -> "photo_card" },
                    ) { index, photo ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            PhotoCard(
                                index = index,
                                photo = photo,
                                itemWidthPx = itemWidthPx,
                                isTransitionSource = photo.id == selectedPhotoId,
                                onClick = {
                                    selectedPhotoId = photo.id
                                    onPhotoClick(photo)
                                },
                                onUserClick = {
                                    selectedPhotoId = photo.id
                                    onUserClick(photo.user)
                                },
                            )
                            IconButton(
                                onClick = {
                                    onRemoveFavorite(photo.id)
                                },
                                modifier =
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f)),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = stringResource(R.string.details_bookmark_remove),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "Favorites - Populated Light")
@Composable
fun FavoritesScreenPopulatedPreview() {
    PreviewScaffold(widthDp = 412, heightDp = 892, darkTheme = false) { _, _ ->
        FavoritesScreenContent(
            state = UiPreviewFixtures.favoritesState,
            onRemoveFavorite = {},
            onPhotoClick = {},
            onUserClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "Favorites - Empty Light")
@Composable
fun FavoritesScreenEmptyPreview() {
    PreviewScaffold(widthDp = 412, heightDp = 892, darkTheme = false) { _, _ ->
        FavoritesScreenContent(
            state = FavoritesState(isLoading = false),
            onRemoveFavorite = {},
            onPhotoClick = {},
            onUserClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "Favorites - Empty Dark")
@Composable
fun FavoritesScreenEmptyDarkPreview() {
    PreviewScaffold(widthDp = 412, heightDp = 892, darkTheme = true) { _, _ ->
        FavoritesScreenContent(
            state = FavoritesState(isLoading = false),
            onRemoveFavorite = {},
            onPhotoClick = {},
            onUserClick = {},
        )
    }
}
