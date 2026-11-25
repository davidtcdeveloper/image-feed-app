package com.example.imagefeed.android

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.imagefeed.android.adaptive.DevicePosture
import com.example.imagefeed.android.adaptive.LocalAdaptiveLayoutInfo
import com.example.imagefeed.android.details.FloatingActionToolbar
import com.example.imagefeed.android.details.PhotoCanvasPane
import com.example.imagefeed.android.details.PhotoInspectorPane
import com.example.imagefeed.android.preview.PreviewScaffold
import com.example.imagefeed.android.preview.UiPreviewFixtures
import com.example.imagefeed.android.theme.ExpressiveLoadingIndicator
import com.example.imagefeed.android.theme.generatePhotoColorScheme
import com.example.imagefeed.android.util.BLURHASH_PLACEHOLDER_WIDTH
import com.example.imagefeed.android.util.BlurHashDecoder
import com.example.imagefeed.android.util.localizedMessage
import com.example.imagefeed.android.util.rememberBlurHashPlaceholder
import com.example.imagefeed.details.model.PhotoStats
import com.example.imagefeed.di.MetroHelper
import com.example.imagefeed.model.Photo
import com.example.imagefeed.presentation.details.PhotoDetailsEffect
import com.example.imagefeed.presentation.details.PhotoDetailsIntent
import com.example.imagefeed.presentation.details.PhotoDetailsState
import com.example.imagefeed.presentation.details.PhotoDetailsViewModel
import kotlinx.coroutines.launch

private const val DUAL_PANE_CANVAS_WEIGHT = 0.58f
private const val DUAL_PANE_INSPECTOR_WEIGHT = 0.42f

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoDetailsScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    photoId: String,
    viewModel: PhotoDetailsViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(key = "photo_$photoId") {
            MetroHelper.getPhotoDetailsViewModel(photoId)
        },
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = PhotoDetailsState())
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalPlatformContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PhotoDetailsEffect.DismissScreen -> onBack()
                is PhotoDetailsEffect.ShowFavoriteToggledSnackbar -> {
                    val messageRes =
                        if (effect.isFavorite) {
                            R.string.details_bookmark_saved
                        } else {
                            R.string.details_bookmark_removed
                        }
                    snackbarHostState.currentSnackbarData?.dismiss()
                    launch {
                        snackbarHostState.showSnackbar(context.getString(messageRes))
                    }
                }
                is PhotoDetailsEffect.ShowErrorSnackbar -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    launch {
                        snackbarHostState.showSnackbar(effect.error.localizedMessage(context))
                    }
                }
            }
        }
    }

    PhotoDetailsScreen(
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        state = state,
        onRetry = { viewModel.dispatch(PhotoDetailsIntent.Retry) },
        onTrackDownload = { photo -> viewModel.dispatch(PhotoDetailsIntent.TrackDownload(photo)) },
        onToggleFavorite = { viewModel.dispatch(PhotoDetailsIntent.ToggleFavorite()) },
        onBack = onBack,
        onUserClick = onUserClick,
        onTagClick = onTagClick,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoDetailsScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    state: PhotoDetailsState,
    onRetry: () -> Unit,
    onTrackDownload: (Photo) -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val context = LocalPlatformContext.current
    val isDark = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()

    val dynamicScheme =
        remember(state.photo?.color, isDark) {
            generatePhotoColorScheme(state.photo?.color, isDark)
        }

    MaterialTheme(colorScheme = dynamicScheme) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.details_title),
                            style =
                                MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_desc_back),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    actions = {
                        state.photo?.let { photo ->
                            IconButton(onClick = {
                                try {
                                    val webUrl = "${photo.links.html}?utm_source=ImageFeedApp&utm_medium=referral"
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                                    context.startActivity(browserIntent)
                                } catch (_: Exception) {
                                    // Ignore if browser is not available
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = stringResource(R.string.details_action_open_web),
                                )
                            }
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                )
            },
        ) { paddingValues ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.surface),
            ) {
                if (state.isLoading && state.photo == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ExpressiveLoadingIndicator()
                    }
                } else if (state.error != null && state.photo == null) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.details_error_photo),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.error?.localizedMessage() ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                } else {
                    state.photo?.let { photo ->
                        PhotoDetailsContent(
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            photo = photo,
                            stats = state.stats,
                            isFavorite = state.isFavorite,
                            onTrackDownload = { onTrackDownload(photo) },
                            onToggleFavorite = onToggleFavorite,
                            onUserClick = onUserClick,
                            onTagClick = onTagClick,
                            onShowSnackbar = { message ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(message)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoDetailsContent(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    photo: Photo,
    stats: PhotoStats?,
    isFavorite: Boolean,
    onTrackDownload: () -> Unit,
    onToggleFavorite: () -> Unit,
    onUserClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onShowSnackbar: (String) -> Unit,
) {
    val context = LocalPlatformContext.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val adaptiveLayoutInfo = LocalAdaptiveLayoutInfo.current
    val posture = adaptiveLayoutInfo.posture

    val aspectRatio =
        if (photo.height > 0) {
            photo.width.toFloat() / photo.height.toFloat()
        } else {
            1f
        }
    val placeholderHeight = BlurHashDecoder.quantizeHeight(BLURHASH_PLACEHOLDER_WIDTH, aspectRatio)

    val placeholderPainter =
        rememberBlurHashPlaceholder(
            blurHash = photo.blurHash,
            width = BLURHASH_PLACEHOLDER_WIDTH,
            height = placeholderHeight,
        )

    // BoxWithConstraints is retained at the root of details content to preserve accurate pane-width
    // awareness on tablets, foldables, and adaptive list-detail scenes (where detail pane width < window width).
    // LocalAdaptiveLayoutInfo.screenWidthDp cannot substitute it because it reflects the full activity window.
    // Hoisting invariant calculations (aspectRatio, BlurHash placeholder) and caching the ImageRequest eliminates
    // recomposition allocation churn.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableWidthDp = maxWidth
        val isDualPane = adaptiveLayoutInfo.useDualPaneDetails && availableWidthDp >= 600.dp
        val canvasWidthDp =
            if (isDualPane && posture !is DevicePosture.TableTop) {
                (availableWidthDp.value * DUAL_PANE_CANVAS_WEIGHT).toInt().dp
            } else {
                availableWidthDp
            }
        val imageWidthPx = with(density) { canvasWidthDp.roundToPx() }

        val imageUrl =
            remember(photo.id, photo.urls.raw, imageWidthPx) {
                photo.urls.getOptimalUrl(imageWidthPx, 85)
            }

        val imageRequest =
            remember(context, imageUrl) {
                ImageRequest
                    .Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build()
            }

        val painter =
            rememberAsyncImagePainter(
                model = imageRequest,
                placeholder = placeholderPainter,
            )

        if (posture is DevicePosture.TableTop) {
            val hingeHeightDp =
                if (posture.isSeparating) {
                    with(density) { posture.hingeBounds.height().toDp() }
                } else {
                    0.dp
                }
            // Foldable TableTop Posture (Horizontal split: Top photo viewer, Bottom controls deck)
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                ) {
                    PhotoCanvasPane(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        photo = photo,
                        painter = painter,
                        aspectRatio = aspectRatio,
                        isDualPane = true,
                        onUserClick = onUserClick,
                    )
                }
                if (hingeHeightDp > 0.dp) {
                    Spacer(
                        modifier =
                            Modifier
                                .height(hingeHeightDp)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface),
                ) {
                    PhotoInspectorPane(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                        photo = photo,
                        stats = stats,
                        showPhotographerHeader = false,
                        onUserClick = onUserClick,
                        onTagClick = onTagClick,
                        onShowSnackbar = onShowSnackbar,
                    )
                    FloatingActionToolbar(
                        photo = photo,
                        isFavorite = isFavorite,
                        onTrackDownload = onTrackDownload,
                        onToggleFavorite = onToggleFavorite,
                        onShowSnackbar = onShowSnackbar,
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                    )
                }
            }
        } else if (isDualPane) {
            val hingeWidthDp =
                if (posture is DevicePosture.Book && posture.isSeparating) {
                    with(density) { posture.hingeBounds.width().toDp() }
                } else {
                    0.dp
                }
            // Tablets / Landscape / Foldable Book Posture (Vertical split: Left photo canvas, Right inspector)
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier =
                        Modifier
                            .weight(DUAL_PANE_CANVAS_WEIGHT)
                            .fillMaxHeight(),
                ) {
                    PhotoCanvasPane(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        photo = photo,
                        painter = painter,
                        aspectRatio = aspectRatio,
                        isDualPane = true,
                        onUserClick = onUserClick,
                    )
                }
                if (hingeWidthDp > 0.dp) {
                    Spacer(
                        modifier =
                            Modifier
                                .width(hingeWidthDp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .weight(DUAL_PANE_INSPECTOR_WEIGHT)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface),
                ) {
                    PhotoInspectorPane(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                        photo = photo,
                        stats = stats,
                        showPhotographerHeader = true,
                        onUserClick = onUserClick,
                        onTagClick = onTagClick,
                        onShowSnackbar = onShowSnackbar,
                    )
                    FloatingActionToolbar(
                        photo = photo,
                        isFavorite = isFavorite,
                        onTrackDownload = onTrackDownload,
                        onToggleFavorite = onToggleFavorite,
                        onShowSnackbar = onShowSnackbar,
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                    )
                }
            }
        } else {
            // Compact Phones (Single-column vertical scroll with anchored floating action bar)
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                ) {
                    PhotoCanvasPane(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        photo = photo,
                        painter = painter,
                        aspectRatio = aspectRatio,
                        isDualPane = false,
                        onUserClick = onUserClick,
                    )
                    PhotoInspectorPane(
                        modifier = Modifier.fillMaxWidth(),
                        photo = photo,
                        stats = stats,
                        showPhotographerHeader = false,
                        onUserClick = onUserClick,
                        onTagClick = onTagClick,
                        onShowSnackbar = onShowSnackbar,
                    )
                }
                FloatingActionToolbar(
                    photo = photo,
                    isFavorite = isFavorite,
                    onTrackDownload = onTrackDownload,
                    onToggleFavorite = onToggleFavorite,
                    onShowSnackbar = onShowSnackbar,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "Photo Details - Compact")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoDetailsScreenPreview() {
    PreviewScaffold(widthDp = 412, heightDp = 892, darkTheme = false) { sharedScope, visibilityScope ->
        PhotoDetailsScreen(
            sharedTransitionScope = sharedScope,
            animatedVisibilityScope = visibilityScope,
            state = UiPreviewFixtures.photoDetailsState,
            onRetry = {},
            onTrackDownload = {},
            onToggleFavorite = {},
            onBack = {},
            onUserClick = {},
            onTagClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "Photo Details - Dark")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoDetailsDarkPreview() {
    PreviewScaffold(widthDp = 412, heightDp = 892, darkTheme = true) { sharedScope, visibilityScope ->
        PhotoDetailsScreen(
            sharedTransitionScope = sharedScope,
            animatedVisibilityScope = visibilityScope,
            state = UiPreviewFixtures.photoDetailsState,
            onRetry = {},
            onTrackDownload = {},
            onToggleFavorite = {},
            onBack = {},
            onUserClick = {},
            onTagClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 700, name = "Photo Details - Expanded")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoDetailsExpandedPreview() {
    PreviewScaffold(widthDp = 900, heightDp = 700, darkTheme = false) { sharedScope, visibilityScope ->
        PhotoDetailsScreen(
            sharedTransitionScope = sharedScope,
            animatedVisibilityScope = visibilityScope,
            state = UiPreviewFixtures.photoDetailsState,
            onRetry = {},
            onTrackDownload = {},
            onToggleFavorite = {},
            onBack = {},
            onUserClick = {},
            onTagClick = {},
        )
    }
}
