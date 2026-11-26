package com.example.imagefeed.android

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.imagefeed.android.adaptive.DevicePosture
import com.example.imagefeed.android.adaptive.LocalAdaptiveLayoutInfo
import com.example.imagefeed.android.designsystem.SatinHeaderScaffold
import com.example.imagefeed.android.details.FloatingActionToolbar
import com.example.imagefeed.android.details.PhotoCanvasPane
import com.example.imagefeed.android.details.PhotoInspectorPane
import com.example.imagefeed.android.preview.PreviewScaffold
import com.example.imagefeed.android.preview.UiPreviewFixtures
import com.example.imagefeed.android.theme.ExpressiveLoadingIndicator
import com.example.imagefeed.android.theme.derivePhotoAmbientTint
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
private const val AMBIENT_CANVAS_TINT_ALPHA = 0.55f
private const val AMBIENT_CANVAS_FADE_STOP = 0.45f
private const val DARK_SURFACE_LUMINANCE_THRESHOLD = 0.5f
private const val DETAIL_IMAGE_QUALITY = 85
private val FLOATING_TOOLBAR_SNACKBAR_CLEARANCE = 76.dp

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
        onUserClick = { username ->
            state.photo?.user?.takeIf { it.username == username }?.let {
                MetroHelper.seedUserPreview(it)
            }
            onUserClick(username)
        },
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
    val isDark = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE_THRESHOLD
    val coroutineScope = rememberCoroutineScope()

    val dynamicScheme =
        remember(state.photo?.color, isDark) {
            generatePhotoColorScheme(state.photo?.color, isDark)
        }
    val ambientTint =
        remember(state.photo?.color, isDark) {
            derivePhotoAmbientTint(state.photo?.color, isDark)
        }

    MaterialTheme(colorScheme = dynamicScheme) {
        val surfaceColor = MaterialTheme.colorScheme.surface
        val ambientBackdropBrush =
            remember(ambientTint, surfaceColor) {
                if (ambientTint != null) {
                    Brush.verticalGradient(
                        0f to ambientTint.copy(alpha = AMBIENT_CANVAS_TINT_ALPHA),
                        AMBIENT_CANVAS_FADE_STOP to surfaceColor,
                        1f to surfaceColor,
                    )
                } else {
                    Brush.verticalGradient(colors = listOf(surfaceColor, surfaceColor))
                }
            }

        SatinHeaderScaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier =
                        if (state.photo != null) {
                            Modifier.padding(bottom = FLOATING_TOOLBAR_SNACKBAR_CLEARANCE)
                        } else {
                            Modifier
                        },
                )
            },
            containerColor = surfaceColor,
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
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                )
            },
        ) { paddingValues ->
            val topHeaderPadding = paddingValues.calculateTopPadding()
            val bottomInsetPadding = paddingValues.calculateBottomPadding()

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(ambientBackdropBrush),
            ) {
                if (state.isLoading && state.photo == null) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(top = topHeaderPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        ExpressiveLoadingIndicator()
                    }
                } else if (state.error != null && state.photo == null) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(top = topHeaderPadding)
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
                            topHeaderPadding = topHeaderPadding,
                            bottomInsetPadding = bottomInsetPadding,
                            ambientTint = ambientTint,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
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
    topHeaderPadding: Dp = 0.dp,
    bottomInsetPadding: Dp = 0.dp,
    ambientTint: Color? = null,
) {
    val context = LocalPlatformContext.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val adaptiveLayoutInfo = LocalAdaptiveLayoutInfo.current
    val posture = adaptiveLayoutInfo.posture
    val toolbarScrollBehavior =
        FloatingToolbarDefaults.exitAlwaysScrollBehavior(
            exitDirection = FloatingToolbarExitDirection.Bottom,
        )
    val toolbarExitModifier =
        Modifier
            .offset(y = -(bottomInsetPadding + FloatingToolbarDefaults.ScreenOffset))
            .graphicsLayer {
                val limit = toolbarScrollBehavior.state.offsetLimit
                val collapseFraction =
                    if (limit < 0f) {
                        (toolbarScrollBehavior.state.offset / limit).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                alpha = 1f - collapseFraction
            }

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
                photo.urls.getOptimalUrl(imageWidthPx, DETAIL_IMAGE_QUALITY)
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
                        ambientTint = ambientTint,
                        enableAvatarSharedElement = true,
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
                            .nestedScroll(toolbarScrollBehavior)
                            .background(MaterialTheme.colorScheme.surface),
                ) {
                    PhotoInspectorPane(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(bottom = bottomInsetPadding),
                        photo = photo,
                        stats = stats,
                        showPhotographerHeader = false,
                        enableAvatarSharedElement = false,
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
                        scrollBehavior = toolbarScrollBehavior,
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .then(toolbarExitModifier),
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
                        ambientTint = ambientTint,
                        enableAvatarSharedElement = false,
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
                            .nestedScroll(toolbarScrollBehavior)
                            .background(MaterialTheme.colorScheme.surface),
                ) {
                    PhotoInspectorPane(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(top = topHeaderPadding, bottom = bottomInsetPadding),
                        photo = photo,
                        stats = stats,
                        showPhotographerHeader = true,
                        enableAvatarSharedElement = true,
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
                        scrollBehavior = toolbarScrollBehavior,
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .then(toolbarExitModifier),
                    )
                }
            }
        } else {
            // Compact Phones (Edge-to-edge canvas beneath floating Satin top bar + exitAlways toolbar)
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .nestedScroll(toolbarScrollBehavior),
            ) {
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
                        ambientTint = ambientTint,
                        enableAvatarSharedElement = true,
                        onUserClick = onUserClick,
                    )
                    PhotoInspectorPane(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = bottomInsetPadding),
                        photo = photo,
                        stats = stats,
                        showPhotographerHeader = false,
                        enableAvatarSharedElement = false,
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
                    scrollBehavior = toolbarScrollBehavior,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .then(toolbarExitModifier),
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

@Preview(
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Photo Details - Dark",
)
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
