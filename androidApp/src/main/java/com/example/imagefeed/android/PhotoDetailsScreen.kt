package com.example.imagefeed.android

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
import com.example.imagefeed.android.util.BlurHashDecoder
import com.example.imagefeed.di.MetroHelper
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoStats
import com.example.imagefeed.presentation.PhotoDetailsState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale

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
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
) {
    val context = LocalPlatformContext.current
    val presenter = remember(photoId) { MetroHelper.getPhotoDetailsPresenter(photoId) }
    DisposableEffect(presenter) {
        onDispose { presenter.clear() }
    }
    val state by presenter.state.collectAsStateWithLifecycle(initialValue = PhotoDetailsState())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "DETAILS",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    state.photo?.let { photo ->
                        IconButton(onClick = {
                            val webUrl = "${photo.links.html}?utm_source=ImageFeedApp&utm_medium=referral"
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                            context.startActivity(browserIntent)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Open in Web",
                                tint = Color.White,
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0F0F11),
                    ),
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFF0F0F11)),
        ) {
            if (state.isLoading && state.photo == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
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
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.error ?: "",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { presenter.loadDetails() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
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
                        onTrackDownload = { presenter.trackDownload() },
                        onUserClick = onUserClick,
                        onTagClick = onTagClick,
                    )
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
) {
    val context = LocalPlatformContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val adaptiveLayoutInfo = LocalAdaptiveLayoutInfo.current
    val isDualPane = adaptiveLayoutInfo.useDualPaneDetails
    val posture = adaptiveLayoutInfo.posture

    val screenWidthDp = configuration.screenWidthDp
    val canvasWidthDp =
        if (isDualPane && posture !is DevicePosture.TableTop) {
            (screenWidthDp * DUAL_PANE_CANVAS_WEIGHT).toInt().dp
        } else {
            screenWidthDp.dp
        }
    val imageWidthPx = with(density) { canvasWidthDp.roundToPx() }
    val aspectRatio = photo.width.toFloat() / photo.height.toFloat()

    val placeholderBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, photo.blurHash) {
        value = BlurHashDecoder.decode(photo.blurHash, 32, (32 / aspectRatio).toInt())
    }

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
                            .background(Color.Black),
                )
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0F0F11)),
            ) {
                PhotoInspectorPane(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                    photo = photo,
                    stats = stats,
                    showPhotographerHeader = false,
                    onTrackDownload = onTrackDownload,
                    onUserClick = onUserClick,
                    onTagClick = onTagClick,
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
                            .background(Color.Black),
                )
            }
            Box(
                modifier =
                    Modifier
                        .weight(DUAL_PANE_INSPECTOR_WEIGHT)
                        .fillMaxHeight()
                        .background(Color(0xFF0F0F11)),
            ) {
                PhotoInspectorPane(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                    photo = photo,
                    stats = stats,
                    showPhotographerHeader = true,
                    onTrackDownload = onTrackDownload,
                    onUserClick = onUserClick,
                    onTagClick = onTagClick,
                )
            }
        }
    } else {
        // Compact Phones (Single-column vertical scroll)
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
                onTrackDownload = onTrackDownload,
                onUserClick = onUserClick,
                onTagClick = onTagClick,
            )
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
                    .background(Color(0xFF070709))
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
                            .clip(RoundedCornerShape(24.dp))
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (photo.user.username.isNotEmpty()) {
                            Text(
                                text = "@${photo.user.username}",
                                color = Color.LightGray.copy(alpha = 0.8f),
                                fontSize = 11.sp,
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
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (photo.user.username.isNotEmpty()) {
                            Text(
                                text = "@${photo.user.username}",
                                color = Color.LightGray.copy(alpha = 0.8f),
                                fontSize = 12.sp,
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
    onTrackDownload: () -> Unit,
    onUserClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
) {
    val context = LocalPlatformContext.current

    Column(
        modifier =
            modifier
                .padding(16.dp),
    ) {
        // Optional photographer header in dual-pane sidebar
        if (showPhotographerHeader) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                shape = RoundedCornerShape(12.dp),
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
                                .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = photo.user.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (photo.user.username.isNotEmpty()) {
                            Text(
                                text = "@${photo.user.username}",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Profile",
                        tint = Color.LightGray,
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
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        // Quick Stats panel
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatItem(
                label = "Views",
                value = stats?.views?.total?.let { formatStatValue(it) } ?: "--",
                icon = Icons.Default.Info,
            )
            StatItem(
                label = "Downloads",
                value = stats?.downloads?.total?.let { formatStatValue(it) } ?: "--",
                icon = Icons.Default.PlayArrow,
            )
            StatItem(
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
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))

            stats.views.historical?.values?.let { historicalList ->
                if (historicalList.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            StatisticsLineChart(
                                data = historicalList.map { it.value.toFloat() },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                            )
                        }
                    }
                }
            }
        }

        // Download Trigger Action Card
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                onTrackDownload()
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(photo.urls.full))
                context.startActivity(browserIntent)
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        ) {
            Icon(imageVector = Icons.Default.Done, contentDescription = "Download")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Download High Resolution Image", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        // EXIF Camera Information
        photo.exif?.let { exif ->
            if (exif.make != null || exif.model != null || exif.exposureTime != null || exif.aperture != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "CAMERA & LENS SPECS",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ExifRow(label = "Camera", value = "${exif.make ?: ""} ${exif.model ?: ""}".trim())
                        ExifRow(label = "Aperture", value = exif.aperture?.let { "f/$it" })
                        ExifRow(label = "Exposure Time", value = exif.exposureTime?.let { "${it}s" })
                        ExifRow(label = "Focal Length", value = exif.focalLength?.let { "${it}mm" })
                        ExifRow(label = "ISO Speed", value = exif.iso?.toString())
                    }
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
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                    shape = RoundedCornerShape(12.dp),
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
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    val webMapUrl = "https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"
                                    val webMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webMapUrl))
                                    context.startActivity(webMapIntent)
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
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = location.name ?: "Unknown Coordinates",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
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
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Map Launcher",
                                tint = Color.LightGray,
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
                                        .clip(RoundedCornerShape(8.dp)),
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
                                        state = MarkerState(position = positionLatLng),
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
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        Box(
                            modifier =
                                Modifier
                                    .background(Color(0xFF22222A), RoundedCornerShape(16.dp))
                                    .clickable { onTagClick(tag.title) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = tag.title.uppercase(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        shape = RoundedCornerShape(12.dp),
        modifier =
            Modifier
                .width(105.dp),
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
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ExifRow(
    label: String,
    value: String?,
) {
    if (!value.isNullOrEmpty()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, color = Color.Gray, fontSize = 13.sp)
            Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun StatisticsLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.clipToBounds()) {
        if (data.isEmpty()) return@Canvas

        val maxVal = data.maxOrNull() ?: 1f
        val minVal = data.minOrNull() ?: 0f
        val diff = if (maxVal == minVal) 1f else maxVal - minVal

        val width = size.width
        val height = size.height

        val stepX = width / (data.size - 1).coerceAtLeast(1)

        val points =
            data.mapIndexed { idx, value ->
                val x = idx * stepX
                val y = height - ((value - minVal) / diff) * height * 0.85f - (height * 0.05f)
                Offset(x, y)
            }

        // 1. Draw smooth gradient filling area under curve
        val fillPath =
            Path().apply {
                moveTo(0f, height)
                points.forEach { point ->
                    lineTo(point.x, point.y)
                }
                lineTo(width, height)
                close()
            }
        drawPath(
            path = fillPath,
            brush =
                Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                    startY = 0f,
                    endY = height,
                ),
        )

        // 2. Draw line path
        val strokePath =
            Path().apply {
                points.forEachIndexed { idx, point ->
                    if (idx == 0) {
                        moveTo(point.x, point.y)
                    } else {
                        lineTo(point.x, point.y)
                    }
                }
            }
        drawPath(
            path = strokePath,
            color = Color.White,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )

        // 3. Draw key dot markers
        points.forEachIndexed { idx, point ->
            if (idx == 0 || idx == points.size - 1 || idx % (data.size / 4).coerceAtLeast(1) == 0) {
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = point,
                )
                drawCircle(
                    color = Color(0xFF1E1E24),
                    radius = 2.dp.toPx(),
                    center = point,
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
