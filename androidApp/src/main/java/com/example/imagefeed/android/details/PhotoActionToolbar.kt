package com.example.imagefeed.android.details

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import coil3.compose.LocalPlatformContext
import com.example.imagefeed.android.R
import com.example.imagefeed.android.designsystem.MorphPolygonShape
import com.example.imagefeed.model.Photo

const val PHOTO_TOOLBAR_TAG = "photo_floating_toolbar"
const val PHOTO_TOOLBAR_BOOKMARK_TAG = "photo_toolbar_bookmark"

private const val BOOKMARK_UNSELECTED_CONTAINER_ALPHA = 0.12f
private const val SPLIT_DIVIDER_ALPHA = 0.30f
private const val BOOKMARK_CIRCLE_VERTICES = 8
private const val BOOKMARK_SQUIRCLE_CORNER_RADIUS = 0.30f

internal val BookmarkMorph =
    Morph(
        start = RoundedPolygon.circle(numVertices = BOOKMARK_CIRCLE_VERTICES).normalized(),
        end =
            RoundedPolygon
                .rectangle(
                    width = 1f,
                    height = 1f,
                    rounding = CornerRounding(radius = BOOKMARK_SQUIRCLE_CORNER_RADIUS),
                ).normalized(),
    )

/**
 * Material 3 1.5.0 Expressive [HorizontalFloatingToolbar] for photo actions (Full/RAW tracked
 * download split pill, Share intent, and [MorphPolygonShape]-animated local Bookmark toggle).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingActionToolbar(
    photo: Photo,
    isFavorite: Boolean,
    onTrackDownload: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
) {
    val context = LocalPlatformContext.current
    val haptic = LocalHapticFeedback.current

    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier.testTag(PHOTO_TOOLBAR_TAG),
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        scrollBehavior = scrollBehavior,
    ) {
        SplitDownloadAction(
            photo = photo,
            onTrackDownload = onTrackDownload,
            onShowSnackbar = onShowSnackbar,
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Share Button
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                try {
                    val sendIntent =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                photo.description ?: context.getString(R.string.details_share_subject_fallback),
                            )
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "${photo.links.html}?utm_source=ImageFeedApp&utm_medium=referral",
                            )
                        }
                    val shareTitle = context.getString(R.string.details_share_title)
                    val shareIntent = Intent.createChooser(sendIntent, shareTitle)
                    context.startActivity(shareIntent)
                } catch (_: Exception) {
                    onShowSnackbar(context.getString(R.string.details_share_error))
                }
            },
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = stringResource(R.string.content_desc_share),
                tint = LocalContentColor.current,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Local Bookmark Toggle with CircleShape <-> Upright Rounded-Squircle MorphPolygonShape
        BookmarkMorphToggle(
            isFavorite = isFavorite,
            onToggle = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleFavorite()
            },
        )
    }
}

@Composable
private fun SplitDownloadAction(
    photo: Photo,
    onTrackDownload: () -> Unit,
    onShowSnackbar: (String) -> Unit,
) {
    val context = LocalPlatformContext.current
    val haptic = LocalHapticFeedback.current

    Surface(
        // Provide MinimumInteractiveLeftAlignmentLine at x = 0 so HorizontalFloatingToolbar's
        // MinimumInteractiveBalancedPaddingNode balances against the start edge instead of
        // inflating toolbar height from the trailing IconButton's horizontal offset.
        modifier = Modifier.minimumInteractiveComponentSize(),
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
                                onShowSnackbar(context.getString(R.string.details_download_error))
                            }
                        }.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = stringResource(R.string.details_action_download_accessibility),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.details_download_full),
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
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = SPLIT_DIVIDER_ALPHA)),
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
                                onShowSnackbar(context.getString(R.string.details_download_error))
                            }
                        }.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.details_download_raw),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BookmarkMorphToggle(
    isFavorite: Boolean,
    onToggle: () -> Unit,
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val isReducedMotion = remember(context, isInspection) { isInspection || isReducedMotionActive(context) }

    val morphProgress by animateFloatAsState(
        targetValue = if (isFavorite) 1f else 0f,
        animationSpec = if (isReducedMotion) snap() else MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "bookmark_morph_progress",
    )
    val containerColor by animateColorAsState(
        targetValue =
            if (isFavorite) {
                MaterialTheme.colorScheme.primary
            } else {
                LocalContentColor.current.copy(alpha = BOOKMARK_UNSELECTED_CONTAINER_ALPHA)
            },
        animationSpec = if (isReducedMotion) snap() else MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "bookmark_container_color",
    )
    val iconTint by animateColorAsState(
        targetValue =
            if (isFavorite) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                LocalContentColor.current
            },
        animationSpec = if (isReducedMotion) snap() else MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "bookmark_icon_tint",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val accessibilityLabel =
        stringResource(
            if (isFavorite) R.string.details_bookmark_remove else R.string.details_bookmark_add,
        )

    Box(
        modifier =
            Modifier
                .testTag(PHOTO_TOOLBAR_BOOKMARK_TAG)
                .minimumInteractiveComponentSize()
                .semantics { contentDescription = accessibilityLabel }
                .toggleable(
                    value = isFavorite,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Switch,
                    onValueChange = { onToggle() },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        shape = MorphPolygonShape(morph = BookmarkMorph, percentage = morphProgress)
                        clip = true
                    }.background(containerColor)
                    .indication(
                        interactionSource = interactionSource,
                        indication = ripple(),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
