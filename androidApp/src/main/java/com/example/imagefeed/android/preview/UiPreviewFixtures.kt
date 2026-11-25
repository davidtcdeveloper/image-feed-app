package com.example.imagefeed.android.preview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.imagefeed.android.adaptive.AdaptiveLayoutInfo
import com.example.imagefeed.android.adaptive.DevicePosture
import com.example.imagefeed.android.adaptive.LocalAdaptiveLayoutInfo
import com.example.imagefeed.android.theme.ImageFeedTheme
import com.example.imagefeed.details.model.PhotoStats
import com.example.imagefeed.model.Exif
import com.example.imagefeed.model.HistoricalStats
import com.example.imagefeed.model.Location
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoLinks
import com.example.imagefeed.model.PhotoUrls
import com.example.imagefeed.model.Position
import com.example.imagefeed.model.ProfileImage
import com.example.imagefeed.model.Social
import com.example.imagefeed.model.StatMetric
import com.example.imagefeed.model.StatsValue
import com.example.imagefeed.model.Tag
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserLinks
import com.example.imagefeed.presentation.details.PhotoDetailsState

@Suppress("MagicNumber")
object UiPreviewFixtures {
    private val sampleUrls =
        PhotoUrls(
            raw = "https://fixture.imagefeed.local/photo_1",
            full = "https://fixture.imagefeed.local/photo_1",
            regular = "https://fixture.imagefeed.local/photo_1",
            small = "https://fixture.imagefeed.local/photo_1",
            thumb = "https://fixture.imagefeed.local/photo_1",
        )

    val sampleUser1 =
        User(
            id = "user_1",
            username = "clara_mendes",
            name = "Clara Mendes",
            portfolioUrl = "https://unsplash.com/@clara_mendes",
            profileImage =
                ProfileImage(
                    small = "https://fixture.imagefeed.local/avatar_1",
                    medium = "https://fixture.imagefeed.local/avatar_1",
                    large = "https://fixture.imagefeed.local/avatar_1",
                ),
            links =
                UserLinks(
                    self = "https://api.unsplash.com/users/clara_mendes",
                    html = "https://unsplash.com/@clara_mendes",
                    photos = "https://api.unsplash.com/users/clara_mendes/photos",
                ),
            bio = "Architectural & alpine landscape photographer documenting Nordic light.",
            location = "Lisbon, Portugal",
            totalLikes = 4820,
            totalPhotos = 312,
            totalCollections = 18,
            social =
                Social(
                    instagramUsername = "claramendes",
                    twitterUsername = "claramendes",
                    portfolioUrl = "https://claramendes.example.com",
                ),
        )

    val samplePhoto =
        Photo(
            id = "photo_1",
            createdAt = "2026-10-12T08:30:00Z",
            width = 3000,
            height = 4000,
            color = "#1F4E78",
            blurHash = "L6PZfSi_.AyE_3t7t7R**0o#DgR4",
            description = "Morning fog drifting across granite alpine ridges.",
            altDescription = "Alpine ridge in soft morning fog",
            urls = sampleUrls,
            user = sampleUser1,
            links =
                PhotoLinks(
                    self = "https://api.unsplash.com/photos/photo_1",
                    html = "https://unsplash.com/photos/photo_1",
                    download = "https://unsplash.com/photos/photo_1/download",
                    downloadLocation = "https://api.unsplash.com/photos/photo_1/download?ixid=fixture",
                ),
            exif =
                Exif(
                    make = "Fujifilm",
                    model = "X-T5",
                    exposureTime = "1/500",
                    aperture = "2.8",
                    focalLength = "35.0",
                    iso = 160,
                ),
            location =
                Location(
                    name = "Serra da Estrela, Portugal",
                    city = "Covilha",
                    country = "Portugal",
                    position = Position(latitude = 40.3219, longitude = -7.6128),
                ),
            tags = listOf(Tag("alpine"), Tag("architecture"), Tag("minimal"), Tag("mist")),
        )

    private val sampleHistoricalValues =
        listOf(
            StatsValue("2026-10-01", 320),
            StatsValue("2026-10-05", 510),
            StatsValue("2026-10-10", 480),
            StatsValue("2026-10-15", 860),
            StatsValue("2026-10-20", 1120),
            StatsValue("2026-10-25", 940),
            StatsValue("2026-10-30", 1380),
        )

    val samplePhotoStats =
        PhotoStats(
            id = samplePhoto.id,
            downloads = StatMetric(total = 14820, historical = HistoricalStats(change = 1840, values = sampleHistoricalValues)),
            views = StatMetric(total = 482900, historical = HistoricalStats(change = 54200, values = sampleHistoricalValues)),
            likes = StatMetric(total = 6420),
        )

    val photoDetailsState =
        PhotoDetailsState(
            photo = samplePhoto,
            stats = samplePhotoStats,
            isFavorite = true,
            isLoading = false,
        )
}

@OptIn(
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class,
)
@Composable
fun PreviewScaffold(
    widthDp: Int = 412,
    heightDp: Int = 892,
    darkTheme: Boolean = false,
    content: @Composable (SharedTransitionScope, AnimatedVisibilityScope) -> Unit,
) {
    ImageFeedTheme(darkTheme = darkTheme, dynamicColor = false) {
        CompositionLocalProvider(
            LocalAdaptiveLayoutInfo provides
                AdaptiveLayoutInfo(
                    windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(widthDp.dp, heightDp.dp)),
                    posture = DevicePosture.Normal,
                    screenWidthDp = widthDp,
                    screenHeightDp = heightDp,
                ),
        ) {
            SharedTransitionLayout {
                AnimatedVisibility(visible = true) {
                    content(this@SharedTransitionLayout, this@AnimatedVisibility)
                }
            }
        }
    }
}
