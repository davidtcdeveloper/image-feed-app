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
import com.example.imagefeed.android.LocalPaneWidthDp
import com.example.imagefeed.android.adaptive.AdaptiveLayoutInfo
import com.example.imagefeed.android.adaptive.DevicePosture
import com.example.imagefeed.android.adaptive.LocalAdaptiveLayoutInfo
import com.example.imagefeed.android.designsystem.ProvideAppSharedTransition
import com.example.imagefeed.android.theme.ImageFeedTheme
import com.example.imagefeed.details.model.PhotoStats
import com.example.imagefeed.model.CollectionSummary
import com.example.imagefeed.model.Exif
import com.example.imagefeed.model.HistoricalStats
import com.example.imagefeed.model.Location
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoCollection
import com.example.imagefeed.model.PhotoLinks
import com.example.imagefeed.model.PhotoUrls
import com.example.imagefeed.model.Position
import com.example.imagefeed.model.PreviewPhoto
import com.example.imagefeed.model.ProfileImage
import com.example.imagefeed.model.Social
import com.example.imagefeed.model.StatMetric
import com.example.imagefeed.model.StatsValue
import com.example.imagefeed.model.Tag
import com.example.imagefeed.model.Topic
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserLinks
import com.example.imagefeed.presentation.collection.CollectionDetailState
import com.example.imagefeed.presentation.collections.CollectionsFeedState
import com.example.imagefeed.presentation.details.PhotoDetailsState
import com.example.imagefeed.presentation.favorites.FavoritesState
import com.example.imagefeed.presentation.feed.FeedState
import com.example.imagefeed.presentation.profile.UserProfileState
import com.example.imagefeed.presentation.search.SearchState

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
            downloads =
                StatMetric(
                    total = 14820,
                    historical = HistoricalStats(change = 1840, values = sampleHistoricalValues),
                ),
            views =
                StatMetric(
                    total = 482900,
                    historical = HistoricalStats(change = 54200, values = sampleHistoricalValues),
                ),
            likes = StatMetric(total = 6420),
        )

    val photoDetailsState =
        PhotoDetailsState(
            photo = samplePhoto,
            stats = samplePhotoStats,
            isFavorite = true,
            isLoading = false,
        )

    val sampleUser2 =
        sampleUser1.copy(
            id = "user_2",
            username = "jonas_berg",
            name = "Jonas Berg",
            location = "Oslo, Norway",
        )

    val samplePhoto2 =
        samplePhoto.copy(
            id = "photo_2",
            width = 4000,
            height = 3000,
            color = "#2D6A4F",
            blurHash = "L48y}x00~qof009F-;IU00?bM{ay",
            description = "Pine canopy mist above fjord cliffs.",
            altDescription = "Pine forest in mist",
            user = sampleUser2,
        )

    val samplePhoto3 =
        samplePhoto.copy(
            id = "photo_3",
            width = 3200,
            height = 4200,
            color = "#B45309",
            blurHash = "LEHV6nWB2yk8pyo0adR*.7kCMdnj",
            description = "Warm sunset limestone facade in Lisbon.",
            altDescription = "Warm architectural facade",
            user = sampleUser1,
        )

    val samplePhoto4 =
        samplePhoto.copy(
            id = "photo_4",
            width = 4200,
            height = 2800,
            color = "#334155",
            blurHash = "L35|?{M{00t7~qt7IUWB00Rj-;ay",
            description = "Minimalist concrete courtyard shadow study.",
            altDescription = "Concrete courtyard",
            user = sampleUser2,
        )

    val samplePhotos = listOf(samplePhoto, samplePhoto2, samplePhoto3, samplePhoto4)

    val sampleTopics =
        listOf(
            Topic(
                id = "topic_nature",
                slug = "nature",
                title = "Nature",
                totalPhotos = 14200,
                coverPhoto = samplePhoto2,
            ),
            Topic(
                id = "topic_architecture",
                slug = "architecture",
                title = "Architecture",
                totalPhotos = 9800,
                coverPhoto = samplePhoto3,
            ),
            Topic(
                id = "topic_wallpapers",
                slug = "wallpapers",
                title = "Wallpapers",
                totalPhotos = 18600,
                coverPhoto = samplePhoto,
            ),
        )

    val feedState =
        FeedState(
            photos = samplePhotos,
            topics = sampleTopics,
            selectedTopicSlug = "editorial",
            isLoading = false,
        )

    private val samplePreviewPhotos =
        listOf(
            PreviewPhoto(id = "prev_1", blurHash = samplePhoto.blurHash, urls = sampleUrls),
            PreviewPhoto(id = "prev_2", blurHash = samplePhoto2.blurHash, urls = sampleUrls),
            PreviewPhoto(id = "prev_3", blurHash = samplePhoto3.blurHash, urls = sampleUrls),
        )

    val sampleCollections =
        listOf(
            PhotoCollection(
                id = "col_1",
                title = "Nordic Light & Mist",
                description = "Quiet alpine horizons and soft coastal morning fog.",
                totalPhotos = 48,
                coverPhoto = samplePhoto,
                user = sampleUser1,
                previewPhotos = samplePreviewPhotos,
            ),
            PhotoCollection(
                id = "col_2",
                title = "Brutalist Geometry",
                description = "Monolithic concrete angles, travertine, and daylight shadows.",
                totalPhotos = 32,
                coverPhoto = samplePhoto3,
                user = sampleUser2,
                previewPhotos = samplePreviewPhotos.reversed(),
            ),
        )

    val favoritesState = FavoritesState(photos = samplePhotos, isLoading = false)

    val collectionDetailState =
        CollectionDetailState(
            collection = sampleCollections.first(),
            photos = samplePhotos,
            related = sampleCollections.drop(1),
            hasReachedEnd = true,
        )

    val userProfileState =
        UserProfileState(
            user = sampleUser1,
            portfolioPhotos = samplePhotos,
            portfolioReachedEnd = true,
        )

    val collectionsFeedState =
        CollectionsFeedState(
            collections = sampleCollections,
            isLoading = false,
        )

    val sampleCollectionSummaries =
        sampleCollections.map { col ->
            CollectionSummary(
                id = col.id,
                title = col.title,
                description = col.description,
                totalPhotos = col.totalPhotos,
                coverPhoto = col.coverPhoto,
                user = col.user,
            )
        }

    val searchDiscoveryState =
        SearchState(
            query = "",
            searchHistory = listOf("alpine mist", "minimalist architecture"),
        )

    val searchResultsState =
        SearchState(
            query = "alpine",
            photos = samplePhotos,
            collections = sampleCollectionSummaries,
            users = listOf(sampleUser1, sampleUser2),
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
            LocalPaneWidthDp provides widthDp.dp,
        ) {
            SharedTransitionLayout {
                AnimatedVisibility(visible = true) {
                    ProvideAppSharedTransition(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedVisibility,
                    ) {
                        content(this@SharedTransitionLayout, this@AnimatedVisibility)
                    }
                }
            }
        }
    }
}
