package com.example.imagefeed.android

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import com.example.imagefeed.android.adaptive.LocalAdaptiveLayoutInfo
import com.example.imagefeed.android.adaptive.ProvideAdaptiveLayoutInfo
import com.example.imagefeed.android.designsystem.ExpressiveNavigationRail
import com.example.imagefeed.android.designsystem.FloatingBottomNavigationBar
import com.example.imagefeed.android.navigation.AppRoute
import com.example.imagefeed.android.navigation.NavigationState
import com.example.imagefeed.android.navigation.Navigator
import com.example.imagefeed.android.navigation.TOP_LEVEL_DESTINATIONS
import com.example.imagefeed.android.navigation.collectionsSection
import com.example.imagefeed.android.navigation.favoritesSection
import com.example.imagefeed.android.navigation.feedSection
import com.example.imagefeed.android.navigation.photoDetailsSection
import com.example.imagefeed.android.navigation.rememberAppNavigationState
import com.example.imagefeed.android.navigation.searchSection
import com.example.imagefeed.android.navigation.userProfileSection
import com.example.imagefeed.android.theme.ImageFeedTheme
import com.example.imagefeed.android.util.localizedMessage
import com.example.imagefeed.di.MetroHelper
import com.example.imagefeed.presentation.random.RandomPhotoEffect
import com.example.imagefeed.presentation.random.RandomPhotoIntent
import com.example.imagefeed.presentation.random.RandomPhotoViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.sqrt

private const val TRANSITION_SLIDE_FRACTION = 0.15f
private const val SHAKE_THRESHOLD = 800
private const val SHAKE_SAMPLE_INTERVAL_MS = 100L
private const val SHAKE_SPEED_SCALE = 10_000f

class MainActivity : ComponentActivity() {
    private var sensorManager: SensorManager? = null
    private var shakeDetector: ShakeDetector? = null
    private var onShakeDetected: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector { onShakeDetected?.invoke() }

        setContent {
            ImageFeedTheme {
                ProvideAdaptiveLayoutInfo(activity = this@MainActivity) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        MainAppShell(
                            onRegisterShake = { handler -> onShakeDetected = handler },
                            onExitApp = { finish() },
                        )
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
}

@Composable
private fun MainAppShell(
    onRegisterShake: ((() -> Unit)?) -> Unit,
    onExitApp: () -> Unit,
) {
    val adaptiveLayoutInfo = LocalAdaptiveLayoutInfo.current
    val navigationState = rememberAppNavigationState()
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val randomPhotoViewModel: RandomPhotoViewModel =
        viewModel(key = "random_photo_discovery") {
            MetroHelper.getRandomPhotoViewModel(autoFetchOnInit = false)
        }
    val randomPhotoState by randomPhotoViewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    RandomPhotoDiscoveryEffects(
        randomPhotoViewModel = randomPhotoViewModel,
        navigator = navigator,
        snackbarHostState = snackbarHostState,
        onRegisterShake = onRegisterShake,
    )

    val currentStack = navigationState.backStacks[navigationState.topLevelRoute]
    val useRail = adaptiveLayoutInfo.useNavigationRail
    val useBottomBar = !useRail && currentStack?.size == 1
    val onDestinationClick: (AppRoute, Boolean) -> Unit = { route, isReselected ->
        if (isReselected) navigator.onReselect(route) else navigator.navigate(route)
    }
    val onRandomClick: () -> Unit = {
        randomPhotoViewModel.dispatch(RandomPhotoIntent.FetchRandomPhoto())
    }

    Row(modifier = Modifier.fillMaxSize()) {
        if (useRail) {
            ExpressiveNavigationRail(
                destinations = TOP_LEVEL_DESTINATIONS,
                selectedRoute = navigationState.topLevelRoute,
                isRandomLoading = randomPhotoState.isLoading,
                onRandomClick = onRandomClick,
                onDestinationClick = onDestinationClick,
            )
        }
        Scaffold(
            modifier =
                Modifier
                    .weight(1f)
                    .then(
                        if (useRail) {
                            Modifier.consumeWindowInsets(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Start),
                            )
                        } else {
                            Modifier
                        },
                    ),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = if (useBottomBar) Modifier else Modifier.navigationBarsPadding(),
                )
            },
            bottomBar = {
                if (useBottomBar) {
                    FloatingBottomNavigationBar(
                        destinations = TOP_LEVEL_DESTINATIONS,
                        selectedRoute = navigationState.topLevelRoute,
                        onDestinationClick = onDestinationClick,
                    )
                }
            },
        ) { innerPadding ->
            AppNavigationHost(
                navigationState = navigationState,
                navigator = navigator,
                innerPadding = innerPadding,
                useBottomBar = useBottomBar,
                onRandomClick = onRandomClick,
                onExitApp = onExitApp,
            )
        }
    }
}

@Composable
private fun RandomPhotoDiscoveryEffects(
    randomPhotoViewModel: RandomPhotoViewModel,
    navigator: Navigator,
    snackbarHostState: SnackbarHostState,
    onRegisterShake: ((() -> Unit)?) -> Unit,
) {
    val context = LocalContext.current

    DisposableEffect(randomPhotoViewModel) {
        onRegisterShake {
            if (!randomPhotoViewModel.state.value.isLoading) {
                randomPhotoViewModel.dispatch(RandomPhotoIntent.FetchRandomPhoto())
            }
        }
        onDispose {
            onRegisterShake(null)
        }
    }

    LaunchedEffect(randomPhotoViewModel, navigator) {
        var snackbarJob: Job? = null
        randomPhotoViewModel.effects.collect { effect ->
            when (effect) {
                is RandomPhotoEffect.PhotoFetched -> {
                    randomPhotoViewModel.state.value.photo
                        ?.takeIf { it.id == effect.photoId }
                        ?.let { MetroHelper.seedPhotoPreview(it) }
                    navigator.navigate(AppRoute.PhotoDetails(effect.photoId))
                }
                is RandomPhotoEffect.FetchFailed -> {
                    val message = effect.error.localizedMessage(context)
                    snackbarJob?.cancel()
                    snackbarJob =
                        launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(message = message)
                        }
                }
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
@Composable
private fun AppNavigationHost(
    navigationState: NavigationState,
    navigator: Navigator,
    innerPadding: PaddingValues,
    useBottomBar: Boolean,
    onRandomClick: () -> Unit,
    onExitApp: () -> Unit,
) {
    SharedTransitionLayout {
        val sharedTransitionScope = this
        val entries =
            buildNavigationEntries(
                navigationState = navigationState,
                sharedTransitionScope = sharedTransitionScope,
                navigator = navigator,
                onRandomClick = onRandomClick,
            )

        val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
        val directive =
            remember(windowAdaptiveInfo) {
                calculatePaneScaffoldDirective(windowAdaptiveInfo)
                    .copy(horizontalPartitionSpacerSize = 0.dp)
            }
        val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

        val layoutDirection = LocalLayoutDirection.current
        val navDisplayPadding =
            if (useBottomBar) {
                PaddingValues(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                    bottom = 0.dp,
                )
            } else {
                innerPadding
            }

        val context = LocalContext.current
        val isReducedMotion =
            remember(context) {
                try {
                    Settings.Global.getFloat(
                        context.contentResolver,
                        Settings.Global.ANIMATOR_DURATION_SCALE,
                        1f,
                    ) == 0f
                } catch (_: Exception) {
                    false
                }
            }
        val motionScheme = MaterialTheme.motionScheme
        val spatialSpec =
            remember(motionScheme, isReducedMotion) {
                if (isReducedMotion) snap() else motionScheme.defaultSpatialSpec<IntOffset>()
            }
        val enterEffectsSpec =
            remember(motionScheme, isReducedMotion) {
                if (isReducedMotion) snap() else motionScheme.defaultEffectsSpec<Float>()
            }
        val exitEffectsSpec =
            remember(motionScheme, isReducedMotion) {
                if (isReducedMotion) snap() else motionScheme.fastEffectsSpec<Float>()
            }
        val slideFraction = if (isReducedMotion) 0f else TRANSITION_SLIDE_FRACTION

        NavDisplay(
            entries = entries,
            sceneStrategies = listOf(listDetailStrategy),
            onBack = {
                if (!navigator.goBack()) {
                    onExitApp()
                }
            },
            sharedTransitionScope = sharedTransitionScope,
            modifier =
                Modifier
                    .padding(navDisplayPadding)
                    .consumeWindowInsets(navDisplayPadding),
            transitionSpec = {
                slideTransform(
                    forward = true,
                    slideFraction = slideFraction,
                    spatialSpec = spatialSpec,
                    enterEffectsSpec = enterEffectsSpec,
                    exitEffectsSpec = exitEffectsSpec,
                )
            },
            popTransitionSpec = {
                slideTransform(
                    forward = false,
                    slideFraction = slideFraction,
                    spatialSpec = spatialSpec,
                    enterEffectsSpec = enterEffectsSpec,
                    exitEffectsSpec = exitEffectsSpec,
                )
            },
            predictivePopTransitionSpec = { swipeEdge ->
                slideTransform(
                    forward = swipeEdge != NavigationEvent.EDGE_LEFT,
                    slideFraction = slideFraction,
                    spatialSpec = spatialSpec,
                    enterEffectsSpec = enterEffectsSpec,
                    exitEffectsSpec = exitEffectsSpec,
                )
            },
        )
    }
}

private fun slideTransform(
    forward: Boolean,
    slideFraction: Float,
    spatialSpec: FiniteAnimationSpec<IntOffset>,
    enterEffectsSpec: FiniteAnimationSpec<Float>,
    exitEffectsSpec: FiniteAnimationSpec<Float>,
): ContentTransform {
    val directionSign = if (forward) 1 else -1
    return (
        slideInHorizontally(
            initialOffsetX = { fullWidth ->
                directionSign * (fullWidth * slideFraction).toInt()
            },
            animationSpec = spatialSpec,
        ) + fadeIn(animationSpec = enterEffectsSpec)
    ) togetherWith
        (
            slideOutHorizontally(
                targetOffsetX = { fullWidth ->
                    -directionSign * (fullWidth * slideFraction).toInt()
                },
                animationSpec = spatialSpec,
            ) + fadeOut(animationSpec = exitEffectsSpec)
        )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun buildNavigationEntries(
    navigationState: NavigationState,
    sharedTransitionScope: SharedTransitionScope,
    navigator: Navigator,
    onRandomClick: () -> Unit,
) = navigationState.toDecoratedEntries(
    entryProvider =
        entryProvider {
            feedSection(
                sharedTransitionScope = sharedTransitionScope,
                navigator = navigator,
                onRandomClick = onRandomClick,
            )
            collectionsSection(
                sharedTransitionScope = sharedTransitionScope,
                navigator = navigator,
            )
            favoritesSection(
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

class ShakeDetector(
    private val onShake: () -> Unit,
) : SensorEventListener {
    private var lastUpdate: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val curTime = System.currentTimeMillis()

            if (curTime - lastUpdate > SHAKE_SAMPLE_INTERVAL_MS) {
                val diffTime = curTime - lastUpdate
                lastUpdate = curTime
                val deltaX = x - lastX
                val deltaY = y - lastY
                val deltaZ = z - lastZ
                val speed =
                    sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / diffTime * SHAKE_SPEED_SCALE
                if (speed > SHAKE_THRESHOLD) {
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
    ) {
        // Sensor accuracy changes do not affect shake threshold detection.
    }
}
