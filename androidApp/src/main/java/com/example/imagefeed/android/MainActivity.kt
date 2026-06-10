package com.example.imagefeed.android

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.compose.LocalPlatformContext
import com.example.imagefeed.android.util.BlurHashDecoder
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.User
import com.example.imagefeed.presentation.FeedPresenter
import com.example.imagefeed.presentation.FeedState
import com.example.imagefeed.presentation.CollectionsFeedPresenter
import com.example.imagefeed.repository.UnsplashRepository
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.math.sqrt

@Serializable
sealed class Screen : NavKey {
    @Serializable
    data object Feed : Screen()

    @Serializable
    data object Collections : Screen()

    @Serializable
    data class CollectionDetails(val collectionId: String) : Screen()

    @Serializable
    data class Search(val query: String = "") : Screen()

    @Serializable
    data class PhotoDetails(val photoId: String) : Screen()

    @Serializable
    data class UserProfile(val username: String) : Screen()
}

class MainActivity : ComponentActivity() {
    private val presenter: FeedPresenter by inject()
    private val collectionsPresenter: CollectionsFeedPresenter by inject()
    private val repository: UnsplashRepository by inject()
    
    private var sensorManager: SensorManager? = null
    private var shakeDetector: ShakeDetector? = null
    private var backStack: NavBackStack<NavKey>? = null
    private var isFetchingRandom = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup shake sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector {
            handleShake()
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF111111),
                    background = Color(0xFF0F0F11),
                    surface = Color(0xFF1E1E24)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val backStackState = rememberNavBackStack(Screen.Feed)
                    backStack = backStackState

                    val navigateToTab = { targetScreen: Screen ->
                        if (targetScreen is Screen.Feed) {
                            while (backStackState.size > 1) {
                                backStackState.removeAt(backStackState.size - 1)
                            }
                        } else {
                            val targetIndex = backStackState.indexOfFirst {
                                (targetScreen is Screen.Search && it is Screen.Search) ||
                                (targetScreen is Screen.Collections && it is Screen.Collections) ||
                                (targetScreen is Screen.Feed && it is Screen.Feed)
                            }
                            if (targetIndex != -1) {
                                while (backStackState.size > targetIndex + 1) {
                                    backStackState.removeAt(backStackState.size - 1)
                                }
                            } else {
                                while (backStackState.size > 2) {
                                    backStackState.removeAt(backStackState.size - 1)
                                }
                                if (backStackState.size > 1) {
                                    backStackState[1] = targetScreen
                                } else {
                                    backStackState.add(targetScreen)
                                }
                            }
                        }
                    }

                    val currentScreen = backStackState.lastOrNull() as? Screen ?: Screen.Feed
                    val showBottomBar = currentScreen is Screen.Feed || currentScreen is Screen.Collections || currentScreen is Screen.Search

                    Scaffold(
                        bottomBar = {
                            if (showBottomBar) {
                                NavigationBar(
                                    containerColor = Color(0xFF0F0F11),
                                    contentColor = Color.White
                                ) {
                                    NavigationBarItem(
                                        selected = currentScreen is Screen.Feed,
                                        onClick = {
                                            if (currentScreen !is Screen.Feed) {
                                                navigateToTab(Screen.Feed)
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.Home,
                                                contentDescription = "Photos"
                                            )
                                        },
                                        label = { Text("Photos") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.Black,
                                            selectedTextColor = Color.White,
                                            unselectedIconColor = Color.Gray,
                                            unselectedTextColor = Color.Gray,
                                            indicatorColor = Color.White
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = currentScreen is Screen.Collections,
                                        onClick = {
                                            if (currentScreen !is Screen.Collections) {
                                                navigateToTab(Screen.Collections)
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.List,
                                                contentDescription = "Collections"
                                            )
                                        },
                                        label = { Text("Collections") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.Black,
                                            selectedTextColor = Color.White,
                                            unselectedIconColor = Color.Gray,
                                            unselectedTextColor = Color.Gray,
                                            indicatorColor = Color.White
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = currentScreen is Screen.Search,
                                        onClick = {
                                            if (currentScreen !is Screen.Search) {
                                                navigateToTab(Screen.Search())
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Search"
                                            )
                                        },
                                        label = { Text("Search") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.Black,
                                            selectedTextColor = Color.White,
                                            unselectedIconColor = Color.Gray,
                                            unselectedTextColor = Color.Gray,
                                            indicatorColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavDisplay(
                            backStack = backStackState,
                            onBack = {
                                if (backStackState.size > 1) {
                                    backStackState.removeAt(backStackState.size - 1)
                                } else {
                                    finish()
                                }
                            },
                            modifier = Modifier.padding(innerPadding),
                            entryProvider = { navKey ->
                                val key = navKey as Screen
                                when (key) {
                                    is Screen.Feed -> NavEntry(key) {
                                        FeedScreen(
                                            presenter = presenter,
                                            onPhotoClick = { photo ->
                                                backStackState.add(Screen.PhotoDetails(photo.id))
                                            },
                                            onUserClick = { user ->
                                                backStackState.add(Screen.UserProfile(user.username))
                                            },
                                            onSearchClick = {
                                                backStackState.add(Screen.Search())
                                            },
                                            onRandomClick = {
                                                handleShake()
                                            }
                                        )
                                    }
                                    is Screen.Collections -> NavEntry(key) {
                                        CollectionsFeedScreen(
                                            presenter = collectionsPresenter,
                                            onCollectionClick = { coll ->
                                                backStackState.add(Screen.CollectionDetails(coll.id))
                                            },
                                            onSearchClick = {
                                                backStackState.add(Screen.Search())
                                            }
                                        )
                                    }
                                    is Screen.CollectionDetails -> NavEntry(key) {
                                        CollectionDetailScreen(
                                            collectionId = key.collectionId,
                                            onBack = {
                                                if (backStackState.size > 1) {
                                                    backStackState.removeAt(backStackState.size - 1)
                                                }
                                            },
                                            onPhotoClick = { photo ->
                                                backStackState.add(Screen.PhotoDetails(photo.id))
                                            },
                                            onCollectionClick = { id ->
                                                backStackState.add(Screen.CollectionDetails(id))
                                            }
                                        )
                                    }
                                    is Screen.Search -> NavEntry(key) {
                                        SearchScreen(
                                            initialQuery = key.query,
                                            onBack = {
                                                if (backStackState.size > 1) {
                                                    backStackState.removeAt(backStackState.size - 1)
                                                }
                                            },
                                            onPhotoClick = { photo ->
                                                backStackState.add(Screen.PhotoDetails(photo.id))
                                            },
                                            onUserClick = { user ->
                                                backStackState.add(Screen.UserProfile(user.username))
                                            }
                                        )
                                    }
                                    is Screen.PhotoDetails -> NavEntry(key) {
                                        PhotoDetailsScreen(
                                            photoId = key.photoId,
                                            onBack = {
                                                if (backStackState.size > 1) {
                                                    backStackState.removeAt(backStackState.size - 1)
                                                }
                                            },
                                            onUserClick = { username ->
                                                backStackState.add(Screen.UserProfile(username))
                                            },
                                            onTagClick = { tag ->
                                                backStackState.add(Screen.Search(query = tag))
                                            }
                                        )
                                    }
                                    is Screen.UserProfile -> NavEntry(key) {
                                        UserProfileScreen(
                                            username = key.username,
                                            onBack = {
                                                if (backStackState.size > 1) {
                                                    backStackState.removeAt(backStackState.size - 1)
                                                }
                                            },
                                            onPhotoClick = { photo ->
                                                backStackState.add(Screen.PhotoDetails(photo.id))
                                            },
                                            onCollectionClick = { col ->
                                                backStackState.add(Screen.CollectionDetails(col.id))
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun handleShake() {
        if (isFetchingRandom) return
        val backStackState = backStack ?: return
        
        isFetchingRandom = true
        Toast.makeText(this, "🎲 Shaking up a random photo...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                val randomPhotos = repository.getRandomPhotos(count = 1)
                val randomPhoto = randomPhotos.firstOrNull()
                if (randomPhoto != null) {
                    backStackState.add(Screen.PhotoDetails(randomPhoto.id))
                } else {
                    Toast.makeText(this@MainActivity, "No photos found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Fetch failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isFetchingRandom = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.registerListener(
            shakeDetector,
            sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_UI
        )
    }

    override fun onPause() {
        sensorManager?.unregisterListener(shakeDetector)
        super.onPause()
    }
}

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {
    private var lastUpdate: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private val shakeThreshold = 800 // High sensitivity but comfortable threshold

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val curTime = System.currentTimeMillis()

            if (curTime - lastUpdate > 100) {
                val diffTime = curTime - lastUpdate
                lastUpdate = curTime
                val speed = sqrt((x - lastX) * (x - lastX) + (y - lastY) * (y - lastY) + (z - lastZ) * (z - lastZ)) / diffTime * 10000
                if (speed > shakeThreshold) {
                    onShake()
                }
                lastX = x
                lastY = y
                lastZ = z
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    presenter: FeedPresenter,
    onPhotoClick: (Photo) -> Unit,
    onUserClick: (User) -> Unit,
    onSearchClick: () -> Unit,
    onRandomClick: () -> Unit
) {
    val state by presenter.state.collectAsStateWithLifecycle(initialValue = FeedState())
    val context = LocalPlatformContext.current
    val listState = rememberLazyStaggeredGridState()

    // Infinite scrolling logic
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = listState.layoutInfo.totalItemsCount
            if (lastVisibleItem == null || totalItems == 0) false
            else lastVisibleItem.index >= totalItems - 6
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            presenter.loadNextPage()
        }
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "FEED",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    },
                    actions = {
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = onRandomClick) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Randomize",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF0F0F11)
                    )
                )

                val activeIndex = if (state.selectedTopicSlug == "editorial") {
                    0
                } else {
                    val idx = state.topics.indexOfFirst { it.slug == state.selectedTopicSlug }
                    if (idx >= 0) idx + 1 else 0
                }

                ScrollableTabRow(
                    selectedTabIndex = activeIndex,
                    containerColor = Color(0xFF0F0F11),
                    contentColor = Color.White,
                    edgePadding = 12.dp
                ) {
                    Tab(
                        selected = state.selectedTopicSlug == "editorial",
                        onClick = { presenter.selectTopic("editorial") },
                        text = {
                            Text(
                                "Editorial",
                                fontWeight = if (state.selectedTopicSlug == "editorial") FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                    state.topics.forEach { topic ->
                        val isSelected = state.selectedTopicSlug == topic.slug
                        Tab(
                            selected = isSelected,
                            onClick = { presenter.selectTopic(topic.slug) },
                            text = {
                                Text(
                                    topic.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.photos.isEmpty() && state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (state.photos.isEmpty() && state.error != null) {
                ErrorView(error = state.error!!, onRetry = { presenter.refresh() })
            } else {
                // Main content: Photo Feed
                LazyVerticalStaggeredGrid(
                    columns = calculateGridColumns(),
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp
                ) {
                    items(state.photos, key = { it.id }) { photo ->
                        PhotoCard(
                            photo = photo,
                            onClick = {
                                onPhotoClick(photo)
                            },
                            onUserClick = {
                                onUserClick(photo.user)
                            }
                        )
                    }

                    if (state.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoCard(
    photo: Photo,
    onClick: () -> Unit,
    onUserClick: () -> Unit
) {
    val context = LocalPlatformContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    
    // Calculate image width based on half screen size to resize request
    val screenWidthDp = configuration.screenWidthDp
    val itemWidthPx = with(density) { (screenWidthDp / 2).dp.roundToPx() }
    
    val aspectRatio = photo.width.toFloat() / photo.height.toFloat()

    // Decode BlurHash placeholder in background
    val placeholderBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, photo.blurHash) {
        value = BlurHashDecoder.decode(photo.blurHash, 32, (32 / aspectRatio).toInt())
    }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(photo.urls.raw + "&w=" + itemWidthPx + "&q=80&auto=format")
            .crossfade(true)
            .build(),
        placeholder = placeholderBitmap?.let { BitmapPainter(it.asImageBitmap()) }
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painter,
                contentDescription = photo.altDescription ?: photo.description,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio),
                contentScale = ContentScale.Crop
            )

            // Dynamic bottom overlay containing photographer attribution
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUserClick() }
                ) {
                    val userProfileImagePainter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(photo.user.profileImage.small)
                            .crossfade(true)
                            .build()
                    )
                    
                    Image(
                        painter = userProfileImagePainter,
                        contentDescription = "User profile",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = photo.user.name,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorView(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Error Loading Feed",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            color = Color.LightGray,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) {
            Text("Retry")
        }
    }
}
