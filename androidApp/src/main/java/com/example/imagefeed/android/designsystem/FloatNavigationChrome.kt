package com.example.imagefeed.android.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemColors
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailState
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.imagefeed.android.R
import com.example.imagefeed.android.navigation.AppRoute
import com.example.imagefeed.android.navigation.TOP_LEVEL_DESTINATIONS
import com.example.imagefeed.android.navigation.TopLevelDestination
import com.example.imagefeed.android.preview.PreviewScaffold
import kotlinx.coroutines.launch

/**
 * Layout and elevation tokens for Material 3 Expressive Float navigation chrome (Spec 84).
 */
object FloatNavigationChrome {
    val HorizontalMargin = 16.dp
    val BottomMargin = 12.dp
    val MaxBottomBarWidth = 520.dp
    val BottomBarTonalElevation = 3.dp
    val BottomBarShadowElevation = 6.dp
    val RailHeaderStartPadding = 24.dp
    val RailHeaderTopPadding = 8.dp
    val RailHeaderSpacing = 12.dp
}

@Composable
private fun floatingBottomBarItemColors(): NavigationItemColors =
    ShortNavigationBarItemDefaults.colors(
        selectedIndicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColorTopIconPosition = MaterialTheme.colorScheme.onSurface,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

/**
 * Floating M3 Expressive [ShortNavigationBar] capsule for compact widths (< 600dp),
 * grounded in semantic tonal surface tokens for Light and Dark mode parity.
 */
@Composable
fun FloatingBottomNavigationBar(
    destinations: List<TopLevelDestination>,
    selectedRoute: AppRoute,
    onDestinationClick: (route: AppRoute, isReselected: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemColors = floatingBottomBarItemColors()
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = FloatNavigationChrome.HorizontalMargin,
                    end = FloatNavigationChrome.HorizontalMargin,
                    bottom = FloatNavigationChrome.BottomMargin,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = FloatNavigationChrome.MaxBottomBarWidth),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = FloatNavigationChrome.BottomBarTonalElevation,
            shadowElevation = FloatNavigationChrome.BottomBarShadowElevation,
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                ShortNavigationBar(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    arrangement = ShortNavigationBarArrangement.EqualWeight,
                ) {
                    destinations.forEach { destination ->
                        val isSelected = selectedRoute == destination.route
                        ShortNavigationBarItem(
                            selected = isSelected,
                            onClick = { onDestinationClick(destination.route, isSelected) },
                            icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                            label = {
                                Text(
                                    text = stringResource(destination.labelRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = itemColors,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationRailHeader(
    isExpanded: Boolean,
    isRandomLoading: Boolean,
    onToggleExpand: () -> Unit,
    onRandomClick: () -> Unit,
) {
    val toggleDescription =
        stringResource(
            if (isExpanded) R.string.nav_rail_collapse_accessibility else R.string.nav_rail_expand_accessibility,
        )
    val stateLabel =
        stringResource(
            if (isExpanded) R.string.nav_rail_state_expanded else R.string.nav_rail_state_collapsed,
        )
    Column(
        modifier =
            Modifier.padding(
                start = FloatNavigationChrome.RailHeaderStartPadding,
                top = FloatNavigationChrome.RailHeaderTopPadding,
            ),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(FloatNavigationChrome.RailHeaderSpacing),
    ) {
        IconButton(
            onClick = onToggleExpand,
            modifier = Modifier.semantics { stateDescription = stateLabel },
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                contentDescription = toggleDescription,
            )
        }
        ApertureRandomizeButton(
            isLoading = isRandomLoading,
            onClick = onRandomClick,
        )
    }
}

/**
 * Collapsible M3 Expressive [WideNavigationRail] for Medium/Expanded widths (>= 600dp),
 * with safe-drawing insets and an [ApertureRandomizeButton] header trigger.
 */
@Composable
fun ExpressiveNavigationRail(
    destinations: List<TopLevelDestination>,
    selectedRoute: AppRoute,
    isRandomLoading: Boolean,
    onRandomClick: () -> Unit,
    onDestinationClick: (route: AppRoute, isReselected: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    railState: WideNavigationRailState =
        rememberWideNavigationRailState(initialValue = WideNavigationRailValue.Collapsed),
) {
    val scope = rememberCoroutineScope()
    val isExpanded = railState.targetValue == WideNavigationRailValue.Expanded

    WideNavigationRail(
        modifier = modifier,
        state = railState,
        colors =
            WideNavigationRailDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
        header = {
            NavigationRailHeader(
                isExpanded = isExpanded,
                isRandomLoading = isRandomLoading,
                onToggleExpand = { scope.launch { railState.toggle() } },
                onRandomClick = onRandomClick,
            )
        },
    ) {
        destinations.forEach { destination ->
            val isSelected = selectedRoute == destination.route
            WideNavigationRailItem(
                selected = isSelected,
                onClick = { onDestinationClick(destination.route, isSelected) },
                railExpanded = isExpanded,
                icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                label = {
                    Text(
                        text = stringResource(destination.labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun CompactFloatBottomBarPreviewContent(darkTheme: Boolean) {
    PreviewScaffold(widthDp = 412, heightDp = 160, darkTheme = darkTheme) { _, _ ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.BottomCenter,
        ) {
            FloatingBottomNavigationBar(
                destinations = TOP_LEVEL_DESTINATIONS,
                selectedRoute = AppRoute.Feed,
                onDestinationClick = { _, _ -> },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 160, name = "Float Bottom Bar - Compact Light")
@Composable
fun CompactFloatBottomBarLightPreview() {
    CompactFloatBottomBarPreviewContent(darkTheme = false)
}

@Preview(showBackground = true, widthDp = 412, heightDp = 160, name = "Float Bottom Bar - Compact Dark")
@Composable
fun CompactFloatBottomBarDarkPreview() {
    CompactFloatBottomBarPreviewContent(darkTheme = true)
}

@Preview(showBackground = true, widthDp = 640, heightDp = 640, name = "Wide Navigation Rail - Expanded")
@Composable
fun ExpandedNavigationRailPreview() {
    val expandedState = rememberWideNavigationRailState(initialValue = WideNavigationRailValue.Expanded)
    PreviewScaffold(widthDp = 640, heightDp = 640, darkTheme = false) { _, _ ->
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
        ) {
            ExpressiveNavigationRail(
                destinations = TOP_LEVEL_DESTINATIONS,
                selectedRoute = AppRoute.Feed,
                isRandomLoading = false,
                onRandomClick = {},
                onDestinationClick = { _, _ -> },
                railState = expandedState,
            )
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface),
            )
        }
    }
}
