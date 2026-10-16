package com.example.imagefeed.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Iso
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.imagefeed.android.adaptive.DevicePosture
import com.example.imagefeed.android.adaptive.LocalAdaptiveLayoutInfo
import com.example.imagefeed.android.theme.PhotoSwatch
import com.example.imagefeed.android.theme.deriveTonalSwatches
import com.example.imagefeed.android.theme.generatePhotoColorScheme
import com.example.imagefeed.android.util.BLURHASH_PLACEHOLDER_MAX_HEIGHT
import com.example.imagefeed.android.util.BLURHASH_PLACEHOLDER_MIN_HEIGHT
import com.example.imagefeed.android.util.BLURHASH_PLACEHOLDER_WIDTH
import com.example.imagefeed.android.util.rememberBlurHashPlaceholder
import com.example.imagefeed.details.model.PhotoStats
import com.example.imagefeed.di.MetroHelper
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.User
import com.example.imagefeed.model.ProfileImage
import com.example.imagefeed.model.UserLinks
import com.example.imagefeed.model.PhotoUrls
import com.example.imagefeed.model.PhotoLinks
import com.example.imagefeed.model.Exif
import com.example.imagefeed.model.Location
import com.example.imagefeed.model.Position
import com.example.imagefeed.model.Tag
import com.example.imagefeed.details.model.StatMetric
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.CompositionLocalProvider
import com.example.imagefeed.android.adaptive.AdaptiveLayoutInfo
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.ui.unit.DpSize
import com.example.imagefeed.presentation.details.PhotoDetailsEffect
import com.example.imagefeed.presentation.details.PhotoDetailsIntent
import com.example.imagefeed.presentation.details.PhotoDetailsState
import com.example.imagefeed.presentation.details.PhotoDetailsViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.util.*

private const val DUAL_PANE_CANVAS_WEIGHT = 0.58f
private const val DUAL_PANE_INSPECTOR_WEIGHT = 0.42f
private const val MIN_ZOOM_SCALE = 1f
private const val MAX_ZOOM_SCALE = 4f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
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

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PhotoDetailsEffect.DismissScreen -> onBack()
                is PhotoDetailsEffect.ShowErrorSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
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
        onBack = onBack,
        onUserClick = onUserClick,
        onTagClick = onTagClick,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoDetailsScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    state: PhotoDetailsState,
    onRetry: () -> Unit,
    onTrackDownload: (Photo) -> Unit,
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
                            text = "Details",
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
                                contentDescription = "Back",
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
                                    contentDescription = "Open in Web",
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
                        CircularProgressIndicator()
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
                            text = "Failed to load photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text("Retry")
                        }
                    }
                } else {
                    state.photo?.let { photo ->
                        PhotoDetailsContent(
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            photo = photo,
                            stats = state.stats,
                            onTrackDownload = { onTrackDownload(photo) },
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoDetailsContent(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    photo: Photo,
    stats: PhotoStats?,
    onTrackDownload: () -> Unit,
    onUserClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onShowSnackbar: (String) -> Unit,
) {
    val context = LocalPlatformContext.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val adaptiveLayoutInfo = LocalAdaptiveLayoutInfo.current
    val posture = adaptiveLayoutInfo.posture

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
        val aspectRatio = photo.width.toFloat() / photo.height.toFloat()
        val placeholderHeight =
            (
                if (aspectRatio > 0f && aspectRatio.isFinite()) {
                    (BLURHASH_PLACEHOLDER_WIDTH / aspectRatio).toInt()
                } else {
                    BLURHASH_PLACEHOLDER_WIDTH
                }
            ).coerceIn(BLURHASH_PLACEHOLDER_MIN_HEIGHT, BLURHASH_PLACEHOLDER_MAX_HEIGHT)

        val placeholderBitmap =
            rememberBlurHashPlaceholder(
                blurHash = photo.blurHash,
                width = BLURHASH_PLACEHOLDER_WIDTH,
                height = placeholderHeight,
            )

        val painter =
            rememberAsyncImagePainter(
                model =
                    ImageRequest
                        .Builder(context)
                        .data(photo.urls.raw + "&w=" + imageWidthPx + "&q=85&auto=format")
                        .crossfade(true)
                        .build(),
                placeholder = placeholderBitmap?.let { BitmapPainter(it.asImageBitmap()) },
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
                        onTrackDownload = onTrackDownload,
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
                        onTrackDownload = onTrackDownload,
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
                    onTrackDownload = onTrackDownload,
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoCanvasPane(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    photo: Photo,
    painter: Painter,
    aspectRatio: Float,
    isDualPane: Boolean,
    onUserClick: (String) -> Unit,
) {
    val context = LocalPlatformContext.current

    if (isDualPane) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transformState =
            rememberTransformableState { _, zoomChange, panChange, _ ->
                scale = (scale * zoomChange).coerceIn(MIN_ZOOM_SCALE, MAX_ZOOM_SCALE)
                if (scale > MIN_ZOOM_SCALE) {
                    offset += panChange
                } else {
                    offset = Offset.Zero
                }
            }

        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .clipToBounds(),
            contentAlignment = Alignment.Center,
        ) {
            with(sharedTransitionScope) {
                Image(
                    painter = painter,
                    contentDescription = photo.altDescription ?: photo.description,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .transformable(state = transformState)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y,
                            ).sharedElement(
                                sharedContentState = rememberSharedContentState(key = "photo_img_${photo.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                            ),
                    contentScale = ContentScale.Fit,
                )
            }

            // Floating photographer attribution badge at bottom
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            ),
                        ).padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { onUserClick(photo.user.username) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    val avatarPainter =
                        rememberAsyncImagePainter(
                            model =
                                ImageRequest
                                    .Builder(context)
                                    .data(photo.user.profileImage.medium)
                                    .crossfade(true)
                                    .build(),
                        )

                    Image(
                        painter = avatarPainter,
                        contentDescription = "Photographer avatar",
                        modifier =
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = photo.user.name,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (photo.user.username.isNotEmpty()) {
                            Text(
                                text = "@${photo.user.username}",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Profile",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    } else {
        // Standard compact container
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio.coerceIn(0.6f, 1.8f)),
        ) {
            with(sharedTransitionScope) {
                Image(
                    painter = painter,
                    contentDescription = photo.altDescription ?: photo.description,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = "photo_img_${photo.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                            ),
                    contentScale = ContentScale.Crop,
                )
            }

            // Photographer Attribution on bottom-overlay
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            ),
                        ).padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onUserClick(photo.user.username)
                            },
                ) {
                    val avatarPainter =
                        rememberAsyncImagePainter(
                            model =
                                ImageRequest
                                    .Builder(context)
                                    .data(photo.user.profileImage.medium)
                                    .crossfade(true)
                                    .build(),
                        )

                    Image(
                        painter = avatarPainter,
                        contentDescription = "Photographer avatar",
                        modifier =
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = photo.user.name,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        if (photo.user.username.isNotEmpty()) {
                            Text(
                                text = "@${photo.user.username}",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Profile",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PhotoInspectorPane(
    modifier: Modifier = Modifier,
    photo: Photo,
    stats: PhotoStats?,
    showPhotographerHeader: Boolean = false,
    onUserClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onShowSnackbar: (String) -> Unit,
) {
    val context = LocalPlatformContext.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier =
            modifier
                .padding(16.dp),
    ) {
        // Optional photographer header in dual-pane sidebar
        if (showPhotographerHeader) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.medium,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onUserClick(photo.user.username) },
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val avatarPainter =
                        rememberAsyncImagePainter(
                            model =
                                ImageRequest
                                    .Builder(context)
                                    .data(photo.user.profileImage.medium)
                                    .crossfade(true)
                                    .build(),
                        )

                    Image(
                        painter = avatarPainter,
                        contentDescription = "Photographer avatar",
                        modifier =
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = photo.user.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (photo.user.username.isNotEmpty()) {
                            Text(
                                text = "@${photo.user.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Photo title / description if exists
        val caption = photo.description ?: photo.altDescription
        if (!caption.isNullOrEmpty()) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        // Quick Stats panel
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatItem(
                modifier = Modifier.weight(1f),
                label = "Views",
                value = stats?.views?.total?.let { formatStatValue(it) } ?: "--",
                icon = Icons.Default.Visibility,
            )
            StatItem(
                modifier = Modifier.weight(1f),
                label = "Downloads",
                value = stats?.downloads?.total?.let { formatStatValue(it) } ?: "--",
                icon = Icons.Default.FileDownload,
            )
            StatItem(
                modifier = Modifier.weight(1f),
                label = "Likes",
                value = stats?.likes?.total?.let { formatStatValue(it) } ?: "--",
                icon = Icons.Default.Favorite,
            )
        }

        // Custom Analytics / Line Chart using Canvas
        if (stats != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "HISTORICAL VIEWS (LAST 30 DAYS)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))

            stats.views.historical?.values?.let { historicalList ->
                if (historicalList.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            StatisticsLineChart(
                                data = historicalList.map { it.value.toFloat() },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(160.dp),
                            )
                        }
                    }
                }
            }
        }

        // EXIF Camera Information (2x2 Bento Grid)
        photo.exif?.let { exif ->
            val hasExif =
                !exif.make.isNullOrBlank() ||
                    !exif.model.isNullOrBlank() ||
                    !exif.exposureTime.isNullOrBlank() ||
                    !exif.aperture.isNullOrBlank() ||
                    !exif.focalLength.isNullOrBlank() ||
                    exif.iso != null
            if (hasExif) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "CAMERA & LENS SPECS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                val make = exif.make?.trim()?.takeIf { it.isNotEmpty() }
                val model = exif.model?.trim()?.takeIf { it.isNotEmpty() }
                val cameraTitle =
                    when {
                        make != null && model != null && model.startsWith(make, ignoreCase = true) -> model
                        make != null && model != null -> "$make $model"
                        model != null -> model
                        make != null -> make
                        else -> ""
                    }
                if (cameraTitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = cameraTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                val apertureVal =
                    exif.aperture?.trim()?.takeIf { it.isNotEmpty() }?.let {
                        if (it.startsWith("f/")) it else "f/$it"
                    } ?: "--"
                val shutterVal =
                    exif.exposureTime?.trim()?.takeIf { it.isNotEmpty() }?.let {
                        if (it.endsWith("s")) it else "${it}s"
                    } ?: "--"
                val focalVal =
                    exif.focalLength?.trim()?.takeIf { it.isNotEmpty() }?.let {
                        if (it.endsWith("mm")) it else "${it}mm"
                    } ?: "--"
                val isoVal = exif.iso?.toString() ?: "--"

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ExifBentoCell(
                            icon = Icons.Default.Camera,
                            label = "Aperture",
                            value = apertureVal,
                            modifier = Modifier.weight(1f),
                        )
                        ExifBentoCell(
                            icon = Icons.Default.Timer,
                            label = "Shutter",
                            value = shutterVal,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ExifBentoCell(
                            icon = Icons.Default.Lens,
                            label = "Lens",
                            value = focalVal,
                            modifier = Modifier.weight(1f),
                        )
                        ExifBentoCell(
                            icon = Icons.Default.Iso,
                            label = "ISO",
                            value = isoVal,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Color Palette Swatches Row
        val swatches = remember(photo.color) { deriveTonalSwatches(photo.color) }
        if (swatches.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "COLOR PALETTE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                swatches.forEach { swatch ->
                    SwatchPill(
                        swatch = swatch,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Hex Color", swatch.hex))
                            onShowSnackbar("Copied ${swatch.hex} to clipboard")
                        },
                    )
                }
            }
        }

        // Location card with map launcher
        photo.location?.let { location ->
            val lat = location.position?.latitude
            val lon = location.position?.longitude
            val isZero = lat == 0.0 && lon == 0.0
            if (!isZero && (!location.name.isNullOrEmpty() || (lat != null && lon != null))) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "LOCATION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = MaterialTheme.shapes.medium,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                val query = location.name ?: ""
                                val gmmIntentUri =
                                    if (lat != null && lon != null) {
                                        Uri.parse("geo:$lat,$lon?q=${Uri.encode(query)}")
                                    } else {
                                        Uri.parse("geo:0,0?q=${Uri.encode(query)}")
                                    }
                                try {
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(mapIntent)
                                    } else {
                                        val webMapUrl = "https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"
                                        val webMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webMapUrl))
                                        context.startActivity(webMapIntent)
                                    }
                                } catch (_: Exception) {
                                    // Ignore if map cannot be opened
                                }
                            },
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location Pin",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = location.name ?: "Unknown Coordinates",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                val latStr =
                                    location.position
                                        ?.latitude
                                        ?.toString()
                                        ?.take(7)
                                val lonStr =
                                    location.position
                                        ?.longitude
                                        ?.toString()
                                        ?.take(7)
                                Text(
                                    text =
                                        if (latStr != null && lonStr != null) {
                                            "Lat: $latStr, Lon: $lonStr"
                                        } else {
                                            "Open in Google Maps"
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Map Launcher",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        if (lat != null && lon != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val positionLatLng = remember(lat, lon) { LatLng(lat, lon) }
                            val cameraPositionState =
                                rememberCameraPositionState {
                                    position = CameraPosition.fromLatLngZoom(positionLatLng, 12f)
                                }
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(MaterialTheme.shapes.small),
                            ) {
                                GoogleMap(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraPositionState = cameraPositionState,
                                    properties =
                                        MapProperties(
                                            mapStyleOptions =
                                                MapStyleOptions.loadRawResourceStyle(
                                                    context,
                                                    R.raw.map_style_dark,
                                                ),
                                        ),
                                    uiSettings =
                                        MapUiSettings(
                                            zoomControlsEnabled = false,
                                            myLocationButtonEnabled = false,
                                            mapToolbarEnabled = false,
                                            scrollGesturesEnabled = false,
                                            zoomGesturesEnabled = false,
                                            tiltGesturesEnabled = false,
                                            rotationGesturesEnabled = false,
                                        ),
                                ) {
                                    Marker(
                                        state = remember(positionLatLng) { MarkerState(position = positionLatLng) },
                                        title = location.name ?: "Captured Location",
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tags / Categories Flow Layout
        photo.tags?.let { tags ->
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "RELATED TAGS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        SuggestionChip(
                            onClick = { onTagClick(tag.title) },
                            label = {
                                Text(
                                    text = tag.title.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            shape = MaterialTheme.shapes.small,
                            colors =
                                SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            border = null,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(84.dp))
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineSmall,
    labelStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.labelMedium,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = valueStyle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = labelStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun ExifBentoCell(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun SwatchPill(
    swatch: PhotoSwatch,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(swatch.color)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = swatch.hex,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun FloatingActionToolbar(
    photo: Photo,
    onTrackDownload: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val haptic = LocalHapticFeedback.current
    var isBookmarked by rememberSaveable(photo.id) { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Split download pill (Full / RAW)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Full Resolution Download
                    Row(
                        modifier =
                            Modifier
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTrackDownload()
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(photo.urls.full))
                                        context.startActivity(browserIntent)
                                    } catch (_: Exception) {
                                        onShowSnackbar("Could not open download link")
                                    }
                                }.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Full Resolution",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Full",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Divider
                    Box(
                        modifier =
                            Modifier
                                .height(20.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)),
                    )

                    // RAW Download
                    Row(
                        modifier =
                            Modifier
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTrackDownload()
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(photo.urls.raw))
                                        context.startActivity(browserIntent)
                                    } catch (_: Exception) {
                                        onShowSnackbar("Could not open download link")
                                    }
                                }.padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "RAW",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Share Button
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    try {
                        val sendIntent =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, photo.description ?: "Unsplash Photo")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "${photo.links.html}?utm_source=ImageFeedApp&utm_medium=referral",
                                )
                            }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Photo")
                        context.startActivity(shareIntent)
                    } catch (_: Exception) {
                        onShowSnackbar("Unable to share photo")
                    }
                },
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)),
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Local Bookmark Button
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isBookmarked = !isBookmarked
                    onShowSnackbar(if (isBookmarked) "Saved to bookmarks" else "Removed from bookmarks")
                },
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)),
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Remove Bookmark" else "Bookmark",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
fun StatisticsLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val maxVal = data.maxOrNull() ?: 1f
    val minVal = data.minOrNull() ?: 0f

    Column(modifier = modifier) {
        // Upper reference bound: Max value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Peak: ${formatStatValue(maxVal.toInt())}",
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "30-Day Trend",
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
                val diff = if (maxVal == minVal) 1f else maxVal - minVal
                val width = size.width
                val height = size.height

                val topY = height * 0.08f
                val bottomY = height * 0.92f

                // Reference bounds dashed guidelines
                drawLine(
                    color = outlineVariant.copy(alpha = 0.25f),
                    start = Offset(0f, topY),
                    end = Offset(width, topY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
                )
                drawLine(
                    color = outlineVariant.copy(alpha = 0.25f),
                    start = Offset(0f, bottomY),
                    end = Offset(width, bottomY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
                )

                if (data.size == 1) {
                    val y = height / 2f
                    drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = Offset(width / 2f, y))
                    return@Canvas
                }

                val stepX = width / (data.size - 1).coerceAtLeast(1)
                val points =
                    data.mapIndexed { idx, value ->
                        val x = idx * stepX
                        val normalized = if (maxVal == minVal) 0.5f else (value - minVal) / diff
                        val y = topY + (1f - normalized) * (bottomY - topY)
                        Offset(x, y)
                    }

                // 1. Draw smooth gradient filling area under cubic Bezier curve
                val fillPath =
                    Path().apply {
                        moveTo(points[0].x, height)
                        lineTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val midX = (prev.x + curr.x) / 2f
                            cubicTo(
                                x1 = midX,
                                y1 = prev.y,
                                x2 = midX,
                                y2 = curr.y,
                                x3 = curr.x,
                                y3 = curr.y,
                            )
                        }
                        lineTo(points.last().x, height)
                        close()
                    }

                drawPath(
                    path = fillPath,
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    primaryColor.copy(alpha = 0.35f),
                                    tertiaryColor.copy(alpha = 0.05f),
                                    Color.Transparent,
                                ),
                            startY = 0f,
                            endY = height,
                        ),
                )

                // 2. Draw cubic Bezier curve with horizontal gradient stroke
                val strokePath =
                    Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val midX = (prev.x + curr.x) / 2f
                            cubicTo(
                                x1 = midX,
                                y1 = prev.y,
                                x2 = midX,
                                y2 = curr.y,
                                x3 = curr.x,
                                y3 = curr.y,
                            )
                        }
                    }

                drawPath(
                    path = strokePath,
                    brush =
                        Brush.horizontalGradient(
                            colors = listOf(primaryColor, tertiaryColor),
                            startX = 0f,
                            endX = width,
                        ),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )

                // 3. Draw key dot markers
                points.forEachIndexed { idx, point ->
                    if (idx == 0 || idx == points.size - 1 || idx % (data.size / 4).coerceAtLeast(1) == 0) {
                        drawCircle(
                            color = primaryColor,
                            radius = 4.dp.toPx(),
                            center = point,
                        )
                        drawCircle(
                            color = surfaceColor,
                            radius = 2.dp.toPx(),
                            center = point,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        // Lower reference bound: Min value and latest reading
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Min: ${formatStatValue(minVal.toInt())}",
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            data.lastOrNull()?.let { latestVal ->
                Text(
                    text = "Latest: ${formatStatValue(latestVal.toInt())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

fun formatStatValue(value: Int): String =
    when {
        value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format(Locale.US, "%.1fK", value / 1_000.0)
        else -> value.toString()
    }

@Preview(showBackground = true, name = "Photo Details - Compact")
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun PhotoDetailsScreenPreview() {
    val samplePhoto =
        Photo(
            id = "photo_123",
            createdAt = "2026-01-01T00:00:00Z",
            width = 1920,
            height = 1080,
            color = "#000000",
            blurHash = "LEHV6nWB2yk8pyo0adR*.7kCMdnj",
            description = "A beautiful scenery",
            altDescription = "Scenic view",
            urls =
                PhotoUrls(
                    raw = "https://example.com/raw.jpg",
                    full = "https://example.com/full.jpg",
                    regular = "https://example.com/regular.jpg",
                    small = "https://example.com/small.jpg",
                    thumb = "https://example.com/thumb.jpg",
                ),
            user =
                User(
                    id = "user_1",
                    username = "johndoe",
                    name = "John Doe",
                    profileImage =
                        ProfileImage(
                            small = "https://example.com/s.jpg",
                            medium = "https://example.com/m.jpg",
                            large = "https://example.com/l.jpg",
                        ),
                    links =
                        UserLinks(
                            self = "https://api.unsplash.com/users/johndoe",
                            html = "https://unsplash.com/@johndoe",
                            photos = "https://api.unsplash.com/users/johndoe/photos",
                        ),
                ),
            links =
                PhotoLinks(
                    self = "https://api.unsplash.com/photos/photo_123",
                    html = "https://unsplash.com/photos/photo_123",
                    download = "https://unsplash.com/photos/photo_123/download",
                    downloadLocation = "https://api.unsplash.com/photos/photo_123/download?ixid=123",
                ),
            exif =
                Exif(
                    make = "Canon",
                    model = "EOS R5",
                    exposureTime = "1/200",
                    aperture = "2.8",
                    focalLength = "50.0",
                    iso = 100,
                ),
            location =
                Location(
                    name = "Lisbon, Portugal",
                    city = "Lisbon",
                    country = "Portugal",
                    position = Position(latitude = 38.7223, longitude = -9.1393),
                ),
            tags = listOf(Tag(title = "nature")),
        )

    val sampleStats = 
        PhotoStats(
            id = "photo_123",
            downloads = StatMetric(total = 120),
            views = StatMetric(total = 5000),
            likes = StatMetric(total = 350)
        )

    val sampleState = PhotoDetailsState(
        photo = samplePhoto,
        stats = sampleStats,
        isLoading = false,
        error = null
    )

    CompositionLocalProvider(
        LocalAdaptiveLayoutInfo provides AdaptiveLayoutInfo(
            windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp)),
            posture = DevicePosture.Normal,
            screenWidthDp = 400,
            screenHeightDp = 800
        )
    ) {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                PhotoDetailsScreen(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility,
                    state = sampleState,
                    onRetry = {},
                    onTrackDownload = {},
                    onBack = {},
                    onUserClick = {},
                    onTagClick = {}
                )
            }
        }
    }
}
