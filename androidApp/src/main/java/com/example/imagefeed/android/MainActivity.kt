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
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
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

class MainActivity : ComponentActivity() {
    private val presenter: FeedPresenter by inject()
    private val collectionsPresenter: CollectionsFeedPresenter by inject()
    private val repository: UnsplashRepository by inject()
    
    private var sensorManager: SensorManager? = null
    private var shakeDetector: ShakeDetector? = null
    private var navController: NavHostController? = null
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
                    val controller = rememberNavController()
                    navController = controller

                    val navBackStackEntry by controller.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val showBottomBar = currentRoute in listOf("feed", "collections", "search")

                    Scaffold(
                        bottomBar = {
                            if (showBottomBar) {
                                NavigationBar(
                                    containerColor = Color(0xFF0F0F11),
                                    contentColor = Color.White
                                ) {
                                    NavigationBarItem(
                                        selected = currentRoute == "feed",
                                        onClick = {
                                            if (currentRoute != "feed") {
                                                controller.navigate("feed") {
                                                    popUpTo("feed") { inclusive = false }
                                                    launchSingleTop = true
                                                }
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
                                        selected = currentRoute == "collections",
                                        onClick = {
                                            if (currentRoute != "collections") {
                                                controller.navigate("collections") {
                                                    popUpTo("feed") { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
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
                                        selected = currentRoute == "search",
                                        onClick = {
                                            if (currentRoute != "search") {
                                                controller.navigate("search") {
                                                    popUpTo("feed") { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
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
                        NavHost(
                            navController = controller,
                            startDestination = "feed",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("feed") {
                                FeedScreen(
                                    presenter = presenter,
                                    onPhotoClick = { photo ->
                                        controller.navigate("photo_details/${photo.id}")
                                    },
                                    onUserClick = { user ->
                                        controller.navigate("user_profile/${user.username}")
                                    },
                                    onSearchClick = {
                                        controller.navigate("search")
                                    },
                                    onRandomClick = {
                                        handleShake() // reuse same fetch random logic
                                    }
                                )
                            }
                            composable("collections") {
                                CollectionsFeedScreen(
                                    presenter = collectionsPresenter,
                                    onCollectionClick = { coll ->
                                        controller.navigate("collection_details/${coll.id}")
                                    },
                                    onSearchClick = {
                                        controller.navigate("search")
                                    }
                                )
                            }
                            composable(
                                route = "collection_details/{collectionId}",
                                arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val collectionId = backStackEntry.arguments?.getString("collectionId") ?: ""
                                CollectionDetailScreen(
                                    collectionId = collectionId,
                                    onBack = { controller.popBackStack() },
                                    onPhotoClick = { photo ->
                                        controller.navigate("photo_details/${photo.id}")
                                    },
                                    onCollectionClick = { id ->
                                        controller.navigate("collection_details/$id")
                                    }
                                )
                            }
                            composable("search") {
                                SearchScreen(
                                    onBack = { controller.popBackStack() },
                                    onPhotoClick = { photo ->
                                        controller.navigate("photo_details/${photo.id}")
                                    },
                                    onUserClick = { user ->
                                        controller.navigate("user_profile/${user.username}")
                                    }
                                )
                            }
                            composable(
                                route = "photo_details/{photoId}",
                                arguments = listOf(navArgument("photoId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val photoId = backStackEntry.arguments?.getString("photoId") ?: ""
                                PhotoDetailsScreen(
                                    photoId = photoId,
                                    onBack = { controller.popBackStack() },
                                    onUserClick = { username ->
                                        controller.navigate("user_profile/$username")
                                    }
                                )
                            }
                            composable(
                                route = "user_profile/{username}",
                                arguments = listOf(navArgument("username") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val username = backStackEntry.arguments?.getString("username") ?: ""
                                UserProfileScreen(
                                    username = username,
                                    onBack = { controller.popBackStack() },
                                    onPhotoClick = { photo ->
                                        controller.navigate("photo_details/${photo.id}")
                                    },
                                    onCollectionClick = { col ->
                                        controller.navigate("collection_details/${col.id}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleShake() {
        if (isFetchingRandom) return
        val controller = navController ?: return
        
        isFetchingRandom = true
        Toast.makeText(this, "🎲 Shaking up a random photo...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                val randomPhotos = repository.getRandomPhotos(count = 1)
                val randomPhoto = randomPhotos.firstOrNull()
                if (randomPhoto != null) {
                    controller.navigate("photo_details/${randomPhoto.id}")
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
    val context = LocalContext.current
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
                            "UNSPLASH FEED",
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
    val context = LocalContext.current
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

            // Dynamic bottom overlay containing credentials to comply with Unsplash terms
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
