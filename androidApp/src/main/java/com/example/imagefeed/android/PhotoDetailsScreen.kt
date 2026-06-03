package com.example.imagefeed.android

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.imagefeed.android.util.BlurHashDecoder
import com.example.imagefeed.di.KoinHelper
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoStats
import com.example.imagefeed.presentation.PhotoDetailsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailsScreen(
    photoId: String,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit
) {
    val context = LocalContext.current
    val presenter = remember(photoId) { KoinHelper.getPhotoDetailsPresenter(photoId) }
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
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    state.photo?.let { photo ->
                        IconButton(onClick = {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("${photo.links.html}?utm_source=ImageFeedApp&utm_medium=referral"))
                            context.startActivity(browserIntent)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Open in Web",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0F0F11)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF0F0F11))
        ) {
            if (state.isLoading && state.photo == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (state.error != null && state.photo == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Failed to load photo", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = state.error ?: "", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { presenter.loadDetails() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("Retry")
                    }
                }
            } else {
                state.photo?.let { photo ->
                    PhotoDetailsContent(
                        photo = photo,
                        stats = state.stats,
                        onTrackDownload = { presenter.trackDownload() },
                        onUserClick = onUserClick
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PhotoDetailsContent(
    photo: Photo,
    stats: PhotoStats?,
    onTrackDownload: () -> Unit,
    onUserClick: (String) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    val screenWidthDp = configuration.screenWidthDp
    val imageWidthPx = with(density) { screenWidthDp.dp.roundToPx() }
    val aspectRatio = photo.width.toFloat() / photo.height.toFloat()

    val placeholderBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, photo.blurHash) {
        value = BlurHashDecoder.decode(photo.blurHash, 32, (32 / aspectRatio).toInt())
    }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(photo.urls.raw + "&w=" + imageWidthPx + "&q=85&auto=format")
            .crossfade(true)
            .build(),
        placeholder = placeholderBitmap?.let { BitmapPainter(it.asImageBitmap()) }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // High quality main photo container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio.coerceIn(0.6f, 1.8f))
        ) {
            Image(
                painter = painter,
                contentDescription = photo.altDescription ?: photo.description,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Photographer Attribution on bottom-overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onUserClick(photo.user.username)
                        }
                ) {
                    val avatarPainter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(photo.user.profileImage.medium)
                            .crossfade(true)
                            .build()
                    )

                    Image(
                        painter = avatarPainter,
                        contentDescription = "Photographer avatar",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = photo.user.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!photo.user.username.isNullOrEmpty()) {
                            Text(
                                text = "@${photo.user.username}",
                                color = Color.LightGray.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // External Unsplash profile link indicator
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "View Profile",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Photo title / description if exists
            val caption = photo.description ?: photo.altDescription
            if (!caption.isNullOrEmpty()) {
                Text(
                    text = caption,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Quick Stats panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "Views", value = stats?.views?.total?.let { formatStatValue(it) } ?: "--", icon = Icons.Default.Info)
                StatItem(label = "Downloads", value = stats?.downloads?.total?.let { formatStatValue(it) } ?: "--", icon = Icons.Default.PlayArrow)
                StatItem(label = "Likes", value = stats?.likes?.total?.let { formatStatValue(it) } ?: "--", icon = Icons.Default.Favorite)
            }

            // Custom Analytics / Line Chart using Canvas
            if (stats != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "HISTORICAL VIEWS (LAST 30 DAYS)",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                stats.views.historical?.values?.let { historicalList ->
                    if (historicalList.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                StatisticsLineChart(
                                    data = historicalList.map { it.value.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
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
                    // Prompt native download or open raw link
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(photo.urls.full))
                    context.startActivity(browserIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
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
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
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
                if (!location.name.isNullOrEmpty() || (location.position?.latitude != null && location.position?.longitude != null)) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "LOCATION INFORMATION",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val lat = location.position?.latitude
                                val lon = location.position?.longitude
                                val query = location.name ?: ""
                                val gmmIntentUri = if (lat != null && lon != null) {
                                    Uri.parse("geo:$lat,$lon?q=${Uri.encode(query)}")
                                } else {
                                    Uri.parse("geo:0,0?q=${Uri.encode(query)}")
                                }
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    // Fallback to web map
                                    val webMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"))
                                    context.startActivity(webMapIntent)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location Pin",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = location.name ?: "Unknown Coordinates",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (location.position?.latitude != null) {
                                        "Lat: ${location.position?.latitude?.toString()?.take(7)}, Lon: ${location.position?.longitude?.toString()?.take(7)}"
                                    } else {
                                        "Open in Google Maps"
                                    },
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Map Launcher",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
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
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF22222A), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tag.title.uppercase(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(105.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ExifRow(label: String, value: String?) {
    if (!value.isNullOrEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.Gray, fontSize = 13.sp)
            Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun StatisticsLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.clipToBounds()) {
        if (data.isEmpty()) return@Canvas

        val maxVal = data.maxOrNull() ?: 1f
        val minVal = data.minOrNull() ?: 0f
        val diff = if (maxVal == minVal) 1f else maxVal - minVal

        val width = size.width
        val height = size.height

        val stepX = width / (data.size - 1).coerceAtLeast(1)

        val points = data.mapIndexed { idx, value ->
            val x = idx * stepX
            val y = height - ((value - minVal) / diff) * height * 0.85f - (height * 0.05f)
            Offset(x, y)
        }

        // 1. Draw smooth gradient filling area under curve
        val fillPath = Path().apply {
            moveTo(0f, height)
            points.forEach { point ->
                lineTo(point.x, point.y)
            }
            lineTo(width, height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // 2. Draw line path
        val strokePath = Path().apply {
            points.forEachIndexed { idx, point ->
                if (idx == 0) moveTo(point.x, point.y)
                else lineTo(point.x, point.y)
            }
        }
        drawPath(
            path = strokePath,
            color = Color.White,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // 3. Draw key dot markers
        points.forEachIndexed { idx, point ->
            if (idx == 0 || idx == points.size - 1 || idx % (data.size / 4).coerceAtLeast(1) == 0) {
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = Color(0xFF1E1E24),
                    radius = 2.dp.toPx(),
                    center = point
                )
            }
        }
    }
}

fun formatStatValue(value: Int): String {
    return when {
        value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
        else -> value.toString()
    }
}
