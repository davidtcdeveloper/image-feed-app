package com.example.imagefeed.android.details

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import com.example.imagefeed.android.R
import com.example.imagefeed.model.Photo

@Composable
fun FloatingActionToolbar(
    photo: Photo,
    isFavorite: Boolean,
    onTrackDownload: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val haptic = LocalHapticFeedback.current

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
                                        onShowSnackbar(context.getString(R.string.details_download_error))
                                    }
                                }.padding(horizontal = 12.dp, vertical = 9.dp),
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
                                        onShowSnackbar(context.getString(R.string.details_download_error))
                                    }
                                }.padding(horizontal = 10.dp, vertical = 9.dp),
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
                        val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.details_share_title))
                        context.startActivity(shareIntent)
                    } catch (_: Exception) {
                        onShowSnackbar(context.getString(R.string.details_share_error))
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
                    contentDescription = stringResource(R.string.content_desc_share),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Local Bookmark Button
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleFavorite()
                },
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)),
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription =
                        if (isFavorite) {
                            stringResource(R.string.details_bookmark_remove)
                        } else {
                            stringResource(R.string.details_bookmark_add)
                        },
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
