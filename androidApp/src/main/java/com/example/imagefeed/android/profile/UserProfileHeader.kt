package com.example.imagefeed.android.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexJustifyContent
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import com.example.imagefeed.android.R
import com.example.imagefeed.android.designsystem.LocalAppSharedTransition
import com.example.imagefeed.model.User

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileHeaderSection(user: User) {
    val context = LocalPlatformContext.current
    val sharedTransition = LocalAppSharedTransition.current

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val baseAvatarModifier = Modifier.size(88.dp)
        val avatarModifier =
            if (sharedTransition != null) {
                with(sharedTransition.sharedTransitionScope) {
                    baseAvatarModifier.sharedElement(
                        sharedContentState = rememberSharedContentState(key = "avatar_${user.username}"),
                        animatedVisibilityScope = sharedTransition.animatedVisibilityScope,
                    )
                }
            } else {
                baseAvatarModifier
            }
        Image(
            painter = rememberAsyncImagePainter(model = user.profileImage.large),
            contentDescription = user.name,
            modifier =
                avatarModifier
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = user.name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
        )

        Text(
            text = "@${user.username}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        if (!user.location.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = stringResource(R.string.content_desc_location),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = user.location ?: "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (!user.bio.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = user.bio ?: "",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // Social Media Assist Chips
        val instagramHandle =
            user.social
                ?.instagramUsername
                ?.trim()
                ?.removePrefix("@")
                ?.ifBlank { null }
        val twitterHandle =
            user.social
                ?.twitterUsername
                ?.trim()
                ?.removePrefix("@")
                ?.ifBlank { null }
        val portfolioUrl =
            user.social
                ?.portfolioUrl
                ?.trim()
                ?.ifBlank { null }
                ?: user.portfolioUrl?.trim()?.ifBlank { null }

        val hasInstagram = instagramHandle != null
        val hasTwitter = twitterHandle != null
        val hasPortfolio = portfolioUrl != null

        if (hasInstagram || hasTwitter || hasPortfolio) {
            Spacer(modifier = Modifier.height(16.dp))
            FlexBox(
                modifier = Modifier.fillMaxWidth(),
                config = {
                    wrap(FlexWrap.Wrap)
                    justifyContent(FlexJustifyContent.Center)
                    gap(8.dp)
                },
            ) {
                if (instagramHandle != null) {
                    AssistChip(
                        onClick = {
                            val igUrl = "instagram://user?username=$instagramHandle"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(igUrl))
                            intent.setPackage("com.instagram.android")
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    // Fallback to web browser
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/$instagramHandle")),
                                    )
                                } catch (_: Exception) {
                                    // Ignore if no handler available
                                }
                            }
                        },
                        label = { Text("@$instagramHandle") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = stringResource(R.string.profile_social_instagram_accessibility),
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                    )
                }
                if (twitterHandle != null) {
                    AssistChip(
                        onClick = {
                            val twUrl = "twitter://user?screen_name=$twitterHandle"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(twUrl))
                            intent.setPackage("com.twitter.android")
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    // Fallback to web browser
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/$twitterHandle")),
                                    )
                                } catch (_: Exception) {
                                    // Ignore if no handler available
                                }
                            }
                        },
                        label = { Text("@$twitterHandle") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AlternateEmail,
                                contentDescription = stringResource(R.string.profile_social_twitter_accessibility),
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                    )
                }
                if (portfolioUrl != null) {
                    val formattedUrl =
                        when {
                            portfolioUrl.startsWith(
                                "http://",
                                ignoreCase = true,
                            ) ||
                                portfolioUrl.startsWith("https://", ignoreCase = true) -> portfolioUrl
                            else -> "https://$portfolioUrl"
                        }
                    AssistChip(
                        onClick = {
                            try {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
                                context.startActivity(webIntent)
                            } catch (_: Exception) {
                                // Ignore if no handler available
                            }
                        },
                        label = { Text(stringResource(R.string.profile_social_website_label)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = stringResource(R.string.profile_social_website_accessibility),
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                    )
                }
            }
        }
    }
}
