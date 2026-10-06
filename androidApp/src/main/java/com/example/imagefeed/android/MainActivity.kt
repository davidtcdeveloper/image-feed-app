package com.example.imagefeed.android

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import com.example.imagefeed.android.adaptive.LocalAdaptiveLayoutInfo
import com.example.imagefeed.android.adaptive.ProvideAdaptiveLayoutInfo
import com.example.imagefeed.android.navigation.AppRoute
import com.example.imagefeed.android.navigation.Navigator
import com.example.imagefeed.android.navigation.TOP_LEVEL_DESTINATIONS
import com.example.imagefeed.android.navigation.collectionsSection
import com.example.imagefeed.android.navigation.feedSection
import com.example.imagefeed.android.navigation.photoDetailsSection
import com.example.imagefeed.android.navigation.rememberAppNavigationState
import com.example.imagefeed.android.navigation.searchSection
import com.example.imagefeed.android.navigation.userProfileSection
import com.example.imagefeed.android.theme.ImageFeedTheme
import com.example.imagefeed.di.MetroHelper
import com.example.imagefeed.repository.UnsplashRepository
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    private val repository: UnsplashRepository by lazy { MetroHelper.graph.repository }

    private var sensorManager: SensorManager? = null
    private var shakeDetector: ShakeDetector? = null
    private var onNavigateToRandom: ((AppRoute) -> Unit)? = null
    private var isFetchingRandom = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Setup shake sensor
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        shakeDetector =
            ShakeDetector {
                handleShake()
            }

        setContent {
            ImageFeedTheme {
                ProvideAdaptiveLayoutInfo(activity = this@MainActivity) {
                    val adaptiveLayoutInfo = LocalAdaptiveLayoutInfo.current
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        val navigationState = rememberAppNavigationState()
                        val navigator = remember(navigationState) { Navigator(navigationState) }
                        onNavigateToRandom = { route -> navigator.navigate(route) }

                        val currentStack = navigationState.backStacks[navigationState.topLevelRoute]
                        val isAtRoot = currentStack?.size == 1
                        val useRail = adaptiveLayoutInfo.useNavigationRail
                        val useBottomBar = !adaptiveLayoutInfo.useNavigationRail && isAtRoot

                        Row(modifier = Modifier.fillMaxSize()) {
                            if (useRail) {
                                NavigationRail(
                                    header = {
                                        IconButton(
                                            onClick = { handleShake() },
                                            modifier = Modifier.padding(top = 8.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Randomize",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    },
                                ) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TOP_LEVEL_DESTINATIONS.forEach { destination ->
                                        val isSelected = navigationState.topLevelRoute == destination.route
                                        NavigationRailItem(
                                            selected = isSelected,
                                            onClick = {
                                                if (isSelected) {
                                                    navigator.onReselect(destination.route)
                                                } else {
                                                    navigator.navigate(destination.route)
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = destination.icon,
                                                    contentDescription = destination.label,
                                                )
                                            },
                                            label = { Text(destination.label) },
                                        )
                                    }
                                }
                            }

                            Scaffold(
                                modifier = Modifier.weight(1f),
                                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                bottomBar = {
                                    if (useBottomBar) {
                                        NavigationBar {
                                            TOP_LEVEL_DESTINATIONS.forEach { destination ->
                                                val isSelected = navigationState.topLevelRoute == destination.route
                                                NavigationBarItem(
                                                    selected = isSelected,
                                                    onClick = {
                                                        if (isSelected) {
                                                            navigator.onReselect(destination.route)
                                                        } else {
                                                            navigator.navigate(destination.route)
                                                        }
                                                    },
                                                    icon = {
                                                        Icon(
                                                            imageVector = destination.icon,
                                                            contentDescription = destination.label,
                                                        )
                                                    },
                                                    label = { Text(destination.label) },
                                                )
                                            }
                                        }
                                    }
                                },
                            ) { innerPadding ->
                                @OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3AdaptiveApi::class)
                                SharedTransitionLayout {
                                    val sharedTransitionScope = this
                                    val entries =
                                        navigationState.toDecoratedEntries(
                                            entryProvider =
                                                entryProvider {
                                                    feedSection(
                                                        sharedTransitionScope = sharedTransitionScope,
                                                        navigator = navigator,
                                                        onRandomClick = { handleShake() },
                                                    )
                                                    collectionsSection(
                                                        sharedTransitionScope = sharedTransitionScope,
                                                        navigator = navigator,
                                                    )
                                                    searchSection(
                                                        sharedTransitionScope = sharedTransitionScope,
                                                        navigator = navigator,
                                                    )
                                                    photoDetailsSection(
                                                        sharedTransitionScope = sharedTransitionScope,
                                                        navigator = navigator,
                                                    )
                                                    userProfileSection(
                                                        sharedTransitionScope = sharedTransitionScope,
                                                        navigator = navigator,
                                                    )
                                                },
                                        )

                                    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
                                    val directive =
                                        remember(windowAdaptiveInfo) {
                                            calculatePaneScaffoldDirective(windowAdaptiveInfo)
                                                .copy(horizontalPartitionSpacerSize = 0.dp)
                                        }
                                    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

                                    NavDisplay(
                                        entries = entries,
                                        sceneStrategies = listOf(listDetailStrategy),
                                        onBack = {
                                            if (!navigator.goBack()) {
                                                finish()
                                            }
                                        },
                                        sharedTransitionScope = sharedTransitionScope,
                                        modifier =
                                            Modifier
                                                .padding(innerPadding)
                                                .consumeWindowInsets(innerPadding),
                                        transitionSpec = {
                                            (
                                                slideInHorizontally(
                                                    initialOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
                                                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                                ) + fadeIn(animationSpec = tween(350))
                                            ) togetherWith
                                                (
                                                    slideOutHorizontally(
                                                        targetOffsetX = { fullWidth -> -(fullWidth * 0.15f).toInt() },
                                                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                                    ) + fadeOut(animationSpec = tween(200))
                                                )
                                        },
                                        popTransitionSpec = {
                                            (
                                                slideInHorizontally(
                                                    initialOffsetX = { fullWidth -> -(fullWidth * 0.15f).toInt() },
                                                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                                ) + fadeIn(animationSpec = tween(350))
                                            ) togetherWith
                                                (
                                                    slideOutHorizontally(
                                                        targetOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
                                                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                                    ) + fadeOut(animationSpec = tween(200))
                                                )
                                        },
                                        predictivePopTransitionSpec = { swipeEdge ->
                                            val isEdgeLeft = swipeEdge == NavigationEvent.EDGE_LEFT
                                            (
                                                slideInHorizontally(
                                                    initialOffsetX = { fullWidth ->
                                                        if (isEdgeLeft) -(fullWidth * 0.15f).toInt() else (fullWidth * 0.15f).toInt()
                                                    },
                                                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                                ) + fadeIn(animationSpec = tween(350))
                                            ) togetherWith
                                                (
                                                    slideOutHorizontally(
                                                        targetOffsetX = { fullWidth ->
                                                            if (isEdgeLeft) (fullWidth * 0.15f).toInt() else -(fullWidth * 0.15f).toInt()
                                                        },
                                                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                                    ) + fadeOut(animationSpec = tween(200))
                                                )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { accelerometer ->
            sensorManager?.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        sensorManager?.unregisterListener(shakeDetector)
        super.onPause()
    }

    private fun handleShake() {
        if (isFetchingRandom) return
        isFetchingRandom = true
        lifecycleScope.launch {
            try {
                val photos = repository.getRandomPhotos(count = 1)
                val randomPhoto = photos.firstOrNull() ?: return@launch
                onNavigateToRandom?.invoke(AppRoute.PhotoDetails(randomPhoto.id))
            } catch (e: Exception) {
                Toast
                    .makeText(
                        this@MainActivity,
                        "Failed to fetch random photo: ${e.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
            } finally {
                isFetchingRandom = false
            }
        }
    }
}

class ShakeDetector(
    private val onShake: () -> Unit,
) : SensorEventListener {
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
                val speed =
                    sqrt(
                        (x - lastX) * (x - lastX) + (y - lastY) * (y - lastY) + (z - lastZ) * (z - lastZ),
                    ) / diffTime * 10000
                if (speed > shakeThreshold) {
                    onShake()
                }
                lastX = x
                lastY = y
                lastZ = z
            }
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor,
        accuracy: Int,
    ) {}
}
