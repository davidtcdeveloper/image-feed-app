package com.example.imagefeed.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowInsets
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.imagefeed.android.designsystem.ExpressiveNavigationRail
import com.example.imagefeed.android.navigation.AppRoute
import com.example.imagefeed.android.navigation.TOP_LEVEL_DESTINATIONS
import com.example.imagefeed.android.preview.PreviewScaffold
import com.example.imagefeed.android.preview.UiPreviewFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.foundation.layout.WindowInsets as ComposeWindowInsets

private const val COMPACT_WIDTH_DP = 412
private const val COMPACT_HEIGHT_DP = 892
private const val EXPANDED_WIDTH_DP = 900
private const val EXPANDED_HEIGHT_DP = 700
private const val STATUS_BAR_TOP_DP = 48
private const val CUTOUT_HORIZONTAL_DP = 24
private const val SEARCH_BAR_VERTICAL_PADDING_DP = 8
private const val COMPACT_HORIZONTAL_PADDING_DP = 12
private const val EXPANDED_HORIZONTAL_PADDING_DP = 16
private const val MIN_EXPECTED_DOCKED_WIDTH_DP = 370f
private const val BOUNDS_TOLERANCE_DP = 1f
private const val PX_TOLERANCE = 4f

@RunWith(AndroidJUnit4::class)
class SearchScreenInsetsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun searchBarAndDiscoveryHeaderClearStatusBarInsetsInCompactLayout() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val photosTabLabel = context.getString(R.string.search_tab_photos)
        val popularTopicsLabel = context.getString(R.string.search_section_popular_topics)

        composeTestRule.setContent {
            val density = LocalDensity.current
            val statusBarTopPx = with(density) { STATUS_BAR_TOP_DP.dp.roundToPx() }
            val simulatedInsets =
                WindowInsetsCompat
                    .Builder()
                    .setInsets(
                        WindowInsetsCompat.Type.statusBars(),
                        Insets.of(0, statusBarTopPx, 0, 0),
                    ).build()

            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowInsets(simulatedInsets),
            ) {
                PreviewScaffold(
                    widthDp = COMPACT_WIDTH_DP,
                    heightDp = COMPACT_HEIGHT_DP,
                    darkTheme = false,
                ) { _, _ ->
                    SearchScreenContent(
                        state = UiPreviewFixtures.searchDiscoveryState,
                        onQueryChange = {},
                        onSearchSubmit = {},
                        onSetSearchActive = {},
                        onSelectColor = {},
                        onSelectSearchType = {},
                        onApplyFilters = {},
                        onRefresh = {},
                        onLoadMore = {},
                        onRemoveHistoryEntry = {},
                        onClearHistory = {},
                        onBack = {},
                        onPhotoClick = {},
                        onUserClick = {},
                        onCollectionClick = {},
                    )
                }
            }
        }

        val searchBarBounds = composeTestRule.onNodeWithTag(SEARCH_BAR_TAG).assertIsDisplayed().getUnclippedBoundsInRoot()
        val photosTabBounds = composeTestRule.onNodeWithText(photosTabLabel).assertIsDisplayed().getUnclippedBoundsInRoot()
        val popularTopicsBounds =
            composeTestRule.onNodeWithText(popularTopicsLabel).assertIsDisplayed().getUnclippedBoundsInRoot()

        val expectedMinSearchBarTop = (STATUS_BAR_TOP_DP + SEARCH_BAR_VERTICAL_PADDING_DP - BOUNDS_TOLERANCE_DP).dp
        assertTrue(
            "Expected SearchBar top (${searchBarBounds.top}) to clear status bar + padding ($expectedMinSearchBarTop)",
            searchBarBounds.top >= expectedMinSearchBarTop,
        )
        assertTrue(
            "Expected SearchBar left (${searchBarBounds.left}) >= $COMPACT_HORIZONTAL_PADDING_DP.dp",
            searchBarBounds.left >= (COMPACT_HORIZONTAL_PADDING_DP - BOUNDS_TOLERANCE_DP).dp,
        )
        assertTrue(
            "Expected Photos tab top (${photosTabBounds.top}) below SearchBar bottom (${searchBarBounds.bottom})",
            photosTabBounds.top > searchBarBounds.bottom,
        )
        assertTrue(
            "Expected Popular Topics top (${popularTopicsBounds.top}) below Photos tab bottom (${photosTabBounds.bottom})",
            popularTopicsBounds.top > photosTabBounds.bottom,
        )
    }

    @Test
    fun searchBarClearsTopAndHorizontalCutoutInsetsInExpandedResultsState() {
        composeTestRule.setContent {
            val density = LocalDensity.current
            val statusBarTopPx = with(density) { STATUS_BAR_TOP_DP.dp.roundToPx() }
            val cutoutHorizontalPx = with(density) { CUTOUT_HORIZONTAL_DP.dp.roundToPx() }
            val simulatedInsets =
                WindowInsetsCompat
                    .Builder()
                    .setInsets(
                        WindowInsetsCompat.Type.statusBars(),
                        Insets.of(0, statusBarTopPx, 0, 0),
                    ).setInsets(
                        WindowInsetsCompat.Type.displayCutout(),
                        Insets.of(cutoutHorizontalPx, 0, cutoutHorizontalPx, 0),
                    ).build()

            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowInsets(simulatedInsets),
            ) {
                PreviewScaffold(
                    widthDp = EXPANDED_WIDTH_DP,
                    heightDp = EXPANDED_HEIGHT_DP,
                    darkTheme = true,
                ) { _, _ ->
                    SearchScreenContent(
                        state = UiPreviewFixtures.searchResultsState,
                        onQueryChange = {},
                        onSearchSubmit = {},
                        onSetSearchActive = {},
                        onSelectColor = {},
                        onSelectSearchType = {},
                        onApplyFilters = {},
                        onRefresh = {},
                        onLoadMore = {},
                        onRemoveHistoryEntry = {},
                        onClearHistory = {},
                        onBack = {},
                        onPhotoClick = {},
                        onUserClick = {},
                        onCollectionClick = {},
                    )
                }
            }
        }

        val searchBarBounds = composeTestRule.onNodeWithTag(SEARCH_BAR_TAG).assertIsDisplayed().getUnclippedBoundsInRoot()
        val expectedMinTop = (STATUS_BAR_TOP_DP + SEARCH_BAR_VERTICAL_PADDING_DP - BOUNDS_TOLERANCE_DP).dp
        val expectedMinLeft = (CUTOUT_HORIZONTAL_DP + EXPANDED_HORIZONTAL_PADDING_DP - BOUNDS_TOLERANCE_DP).dp

        assertTrue(
            "Expected SearchBar top (${searchBarBounds.top}) >= $expectedMinTop",
            searchBarBounds.top >= expectedMinTop,
        )
        assertTrue(
            "Expected SearchBar left (${searchBarBounds.left}) >= $expectedMinLeft",
            searchBarBounds.left >= expectedMinLeft,
        )
    }

    @Test
    fun expandedDockedSearchBarMatchesCollapsedSearchBarBoundsAndWidth() {
        composeTestRule.setContent {
            val density = LocalDensity.current
            val statusBarTopPx = with(density) { STATUS_BAR_TOP_DP.dp.roundToPx() }
            val simulatedInsets =
                WindowInsetsCompat
                    .Builder()
                    .setInsets(
                        WindowInsetsCompat.Type.statusBars(),
                        Insets.of(0, statusBarTopPx, 0, 0),
                    ).build()

            var isSearchActive by remember { mutableStateOf(false) }

            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowInsets(simulatedInsets),
            ) {
                PreviewScaffold(
                    widthDp = EXPANDED_WIDTH_DP,
                    heightDp = EXPANDED_HEIGHT_DP,
                    darkTheme = false,
                ) { _, _ ->
                    SearchScreenContent(
                        state = UiPreviewFixtures.searchDiscoveryState.copy(isSearchActive = isSearchActive),
                        onQueryChange = {},
                        onSearchSubmit = {},
                        onSetSearchActive = { isSearchActive = it },
                        onSelectColor = {},
                        onSelectSearchType = {},
                        onApplyFilters = {},
                        onRefresh = {},
                        onLoadMore = {},
                        onRemoveHistoryEntry = {},
                        onClearHistory = {},
                        onBack = {},
                        onPhotoClick = {},
                        onUserClick = {},
                        onCollectionClick = {},
                    )
                }
            }
        }

        val collapsedNode = composeTestRule.onNodeWithTag(SEARCH_BAR_TAG).assertIsDisplayed().fetchSemanticsNode()
        val collapsedBounds = composeTestRule.onNodeWithTag(SEARCH_BAR_TAG).getUnclippedBoundsInRoot()
        assertTrue(
            "Expected collapsed SearchBar width (${collapsedBounds.width}) > $MIN_EXPECTED_DOCKED_WIDTH_DP.dp",
            collapsedBounds.width.value > MIN_EXPECTED_DOCKED_WIDTH_DP,
        )

        composeTestRule.onNodeWithTag(SEARCH_BAR_TAG).performClick()
        composeTestRule.waitForIdle()

        val expandedNode =
            composeTestRule.onNodeWithTag(EXPANDED_SEARCH_BAR_TAG).assertIsDisplayed().fetchSemanticsNode()
        assertEquals(collapsedNode.positionOnScreen.y, expandedNode.positionOnScreen.y, PX_TOLERANCE)
        assertEquals(collapsedNode.positionOnScreen.x, expandedNode.positionOnScreen.x, PX_TOLERANCE)
        assertEquals(collapsedNode.size.width.toFloat(), expandedNode.size.width.toFloat(), PX_TOLERANCE)
    }

    @Test
    fun expandedFullScreenSearchBarClearsStatusBarInsetsInCompactLayout() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val collapseDesc = context.getString(R.string.search_action_collapse_accessibility)
        var systemStatusBarTopDp = 0f

        composeTestRule.setContent {
            val density = LocalDensity.current
            val rootInsets = ViewCompat.getRootWindowInsets(LocalView.current)
            val realStatusBarPx = rootInsets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
            systemStatusBarTopDp = with(density) { realStatusBarPx.toDp().value }
            val statusBarTopPx = with(density) { STATUS_BAR_TOP_DP.dp.roundToPx() }
            val simulatedInsets =
                WindowInsetsCompat
                    .Builder()
                    .setInsets(
                        WindowInsetsCompat.Type.statusBars(),
                        Insets.of(0, statusBarTopPx, 0, 0),
                    ).build()

            var isSearchActive by remember { mutableStateOf(false) }

            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowInsets(simulatedInsets),
            ) {
                PreviewScaffold(
                    widthDp = COMPACT_WIDTH_DP,
                    heightDp = COMPACT_HEIGHT_DP,
                    darkTheme = false,
                ) { _, _ ->
                    SearchScreenContent(
                        state = UiPreviewFixtures.searchDiscoveryState.copy(isSearchActive = isSearchActive),
                        onQueryChange = {},
                        onSearchSubmit = {},
                        onSetSearchActive = { isSearchActive = it },
                        onSelectColor = {},
                        onSelectSearchType = {},
                        onApplyFilters = {},
                        onRefresh = {},
                        onLoadMore = {},
                        onRemoveHistoryEntry = {},
                        onClearHistory = {},
                        onBack = {},
                        onPhotoClick = {},
                        onUserClick = {},
                        onCollectionClick = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(SEARCH_BAR_TAG).performClick()
        composeTestRule.waitForIdle()

        val collapseButtonBounds =
            composeTestRule
                .onNode(hasContentDescription(collapseDesc) and hasAnyAncestor(isDialog()))
                .assertIsDisplayed()
                .getUnclippedBoundsInRoot()
        assertTrue(
            "Expected expanded collapse button top (${collapseButtonBounds.top}) >= $systemStatusBarTopDp.dp",
            collapseButtonBounds.top.value >= systemStatusBarTopDp - BOUNDS_TOLERANCE_DP,
        )
    }

    @Test
    fun searchScreenWithConsumedRailStartInsetsDoesNotDoublePadStartCutout() {
        composeTestRule.setContent {
            val density = LocalDensity.current
            val statusBarTopPx = with(density) { STATUS_BAR_TOP_DP.dp.roundToPx() }
            val cutoutHorizontalPx = with(density) { CUTOUT_HORIZONTAL_DP.dp.roundToPx() }
            val simulatedInsets =
                WindowInsetsCompat
                    .Builder()
                    .setInsets(
                        WindowInsetsCompat.Type.statusBars(),
                        Insets.of(0, statusBarTopPx, 0, 0),
                    ).setInsets(
                        WindowInsetsCompat.Type.displayCutout(),
                        Insets.of(cutoutHorizontalPx, 0, cutoutHorizontalPx, 0),
                    ).build()

            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowInsets(simulatedInsets),
            ) {
                PreviewScaffold(
                    widthDp = EXPANDED_WIDTH_DP,
                    heightDp = EXPANDED_HEIGHT_DP,
                    darkTheme = false,
                ) { _, _ ->
                    Row(modifier = Modifier.fillMaxSize()) {
                        ExpressiveNavigationRail(
                            destinations = TOP_LEVEL_DESTINATIONS,
                            selectedRoute = AppRoute.Search(),
                            isRandomLoading = false,
                            onRandomClick = {},
                            onDestinationClick = { _, _ -> },
                            modifier = Modifier.testTag("nav_rail"),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .consumeWindowInsets(
                                        ComposeWindowInsets.safeDrawing.only(WindowInsetsSides.Start),
                                    ),
                        ) {
                            SearchScreenContent(
                                state = UiPreviewFixtures.searchDiscoveryState,
                                onQueryChange = {},
                                onSearchSubmit = {},
                                onSetSearchActive = {},
                                onSelectColor = {},
                                onSelectSearchType = {},
                                onApplyFilters = {},
                                onRefresh = {},
                                onLoadMore = {},
                                onRemoveHistoryEntry = {},
                                onClearHistory = {},
                                onBack = {},
                                onPhotoClick = {},
                                onUserClick = {},
                                onCollectionClick = {},
                            )
                        }
                    }
                }
            }
        }

        val railBounds = composeTestRule.onNodeWithTag("nav_rail").assertIsDisplayed().getUnclippedBoundsInRoot()
        val searchBarBounds = composeTestRule.onNodeWithTag(SEARCH_BAR_TAG).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(
            "Expected SearchBar top (${searchBarBounds.top}) to clear status bar",
            searchBarBounds.top >= (STATUS_BAR_TOP_DP + SEARCH_BAR_VERTICAL_PADDING_DP - BOUNDS_TOLERANCE_DP).dp,
        )
        assertEquals(
            (railBounds.right + EXPANDED_HORIZONTAL_PADDING_DP.dp).value,
            searchBarBounds.left.value,
            BOUNDS_TOLERANCE_DP,
        )
    }
}
