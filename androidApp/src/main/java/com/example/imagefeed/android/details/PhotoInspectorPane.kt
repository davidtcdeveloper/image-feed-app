package com.example.imagefeed.android.details

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Iso
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.imagefeed.android.R
import com.example.imagefeed.android.adaptive.LocalAdaptiveLayoutInfo
import com.example.imagefeed.android.designsystem.LocalAppSharedTransition
import com.example.imagefeed.android.theme.PhotoSwatch
import com.example.imagefeed.android.theme.deriveTonalSwatches
import com.example.imagefeed.details.model.PhotoStats
import com.example.imagefeed.model.Photo
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoInspectorPane(
    modifier: Modifier = Modifier,
    photo: Photo,
    stats: PhotoStats?,
    showPhotographerHeader: Boolean = false,
    enableAvatarSharedElement: Boolean = true,
    onUserClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onShowSnackbar: (String) -> Unit,
) {
    val context = LocalPlatformContext.current
    val haptic = LocalHapticFeedback.current
    val sharedTransition = LocalAppSharedTransition.current

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
                    val baseAvatarModifier = Modifier.size(44.dp)
                    val avatarModifier =
                        if (enableAvatarSharedElement && photo.user.username.isNotBlank() && sharedTransition != null) {
                            with(sharedTransition.sharedTransitionScope) {
                                baseAvatarModifier.sharedElement(
                                    sharedContentState =
                                        rememberSharedContentState(
                                            key = "avatar_${photo.user.username}",
                                        ),
                                    animatedVisibilityScope = sharedTransition.animatedVisibilityScope,
                                )
                            }
                        } else {
                            baseAvatarModifier
                        }

                    Image(
                        painter = avatarPainter,
                        contentDescription = stringResource(R.string.content_desc_photographer_avatar),
                        contentScale = ContentScale.Crop,
                        modifier =
                            avatarModifier
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
                        contentDescription = stringResource(R.string.search_user_card_view_profile_accessibility),
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
                label = stringResource(R.string.details_stat_views),
                value = stats?.views?.total?.let { formatStatValue(it) } ?: "--",
                icon = Icons.Default.Visibility,
            )
            StatItem(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.details_stat_downloads),
                value = stats?.downloads?.total?.let { formatStatValue(it) } ?: "--",
                icon = Icons.Default.FileDownload,
            )
            StatItem(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.details_stat_likes),
                value = stats?.likes?.total?.let { formatStatValue(it) } ?: "--",
                icon = Icons.Default.Favorite,
            )
        }

        // Custom Analytics / Line Chart using Canvas
        if (stats != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.details_views_trend_header),
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
                    text = stringResource(R.string.details_exif_header),
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

                val isCompact = LocalAdaptiveLayoutInfo.current.isCompactWidth
                Grid(
                    modifier = Modifier.fillMaxWidth(),
                    config = {
                        repeat(if (isCompact) 2 else 4) { column(1.fr) }
                        gap(8.dp)
                    },
                ) {
                    ExifBentoCell(
                        icon = Icons.Default.Camera,
                        label = stringResource(R.string.details_exif_aperture),
                        value = apertureVal,
                    )
                    ExifBentoCell(
                        icon = Icons.Default.Timer,
                        label = stringResource(R.string.details_exif_exposure),
                        value = shutterVal,
                    )
                    ExifBentoCell(
                        icon = Icons.Default.Lens,
                        label = stringResource(R.string.details_exif_focal_length),
                        value = focalVal,
                    )
                    ExifBentoCell(
                        icon = Icons.Default.Iso,
                        label = stringResource(R.string.details_exif_iso),
                        value = isoVal,
                    )
                }
            }
        }

        // Color Palette Swatches Row
        val swatches = remember(photo.color) { deriveTonalSwatches(photo.color) }
        if (swatches.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.details_color_palette_header),
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
                            clipboard?.setPrimaryClip(
                                ClipData.newPlainText(context.getString(R.string.details_clipboard_hex_label), swatch.hex),
                            )
                            onShowSnackbar(context.getString(R.string.details_copied_to_clipboard, swatch.hex))
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
                    text = stringResource(R.string.details_location_header),
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
                                contentDescription = stringResource(R.string.details_location_pin_accessibility),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = location.name ?: stringResource(R.string.details_location_unknown),
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
                                            stringResource(R.string.details_location_lat_lon, latStr, lonStr)
                                        } else {
                                            stringResource(R.string.details_location_open_maps)
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(R.string.content_desc_map_launcher),
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
                            val mapStyleOptions =
                                remember(context) {
                                    MapStyleOptions.loadRawResourceStyle(
                                        context,
                                        R.raw.map_style_dark,
                                    )
                                }
                            val mapProperties = remember(mapStyleOptions) { MapProperties(mapStyleOptions = mapStyleOptions) }
                            val mapUiSettings =
                                remember {
                                    MapUiSettings(
                                        zoomControlsEnabled = false,
                                        myLocationButtonEnabled = false,
                                        mapToolbarEnabled = false,
                                        scrollGesturesEnabled = false,
                                        zoomGesturesEnabled = false,
                                        tiltGesturesEnabled = false,
                                        rotationGesturesEnabled = false,
                                    )
                                }
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(MaterialTheme.shapes.small),
                            ) {
                                if (LocalInspectionMode.current) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp),
                                        )
                                    }
                                } else {
                                    GoogleMap(
                                        modifier = Modifier.fillMaxSize(),
                                        cameraPositionState = cameraPositionState,
                                        googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
                                        properties = mapProperties,
                                        uiSettings = mapUiSettings,
                                    ) {
                                        val markerTitle = location.name ?: stringResource(R.string.details_location_captured)
                                        Marker(
                                            state = remember(positionLatLng) { MarkerState(position = positionLatLng) },
                                            title = markerTitle,
                                        )
                                    }
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
                    text = stringResource(R.string.details_related_tags_header),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlexBox(
                    modifier = Modifier.fillMaxWidth(),
                    config = {
                        wrap(FlexWrap.Wrap)
                        gap(8.dp)
                    },
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
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = MaterialTheme.typography.headlineSmall,
    labelStyle: TextStyle = MaterialTheme.typography.labelMedium,
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
    icon: ImageVector,
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
