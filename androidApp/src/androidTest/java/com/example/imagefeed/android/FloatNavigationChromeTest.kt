package com.example.imagefeed.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.imagefeed.android.designsystem.ApertureRandomizeButton
import com.example.imagefeed.android.designsystem.ExpressiveNavigationRail
import com.example.imagefeed.android.designsystem.FloatingBottomNavigationBar
import com.example.imagefeed.android.designsystem.MorphPolygonShape
import com.example.imagefeed.android.designsystem.PolygonShape
import com.example.imagefeed.android.navigation.AppRoute
import com.example.imagefeed.android.navigation.TOP_LEVEL_DESTINATIONS
import com.example.imagefeed.android.theme.ExpressiveShapeTokens
import com.example.imagefeed.android.theme.ImageFeedTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val OUTLINE_SIZE_PX = 120f
private const val RECT_WIDTH_PX = 200f
private const val RECT_HEIGHT_PX = 100f
private const val ROTATION_DEGREES = 45f
private const val BOUNDS_TOLERANCE_PX = 2f
private const val HALF_PROGRESS = 0.5f

@RunWith(AndroidJUnit4::class)
class FloatNavigationChromeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun floatingBottomNavigationBarRendersAndDispatchesSelectionAndReselection() {
        val clicks = mutableListOf<Pair<AppRoute, Boolean>>()
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeTestRule.setContent {
            ImageFeedTheme {
                var selectedRoute by remember { mutableStateOf<AppRoute>(AppRoute.Feed) }
                FloatingBottomNavigationBar(
                    destinations = TOP_LEVEL_DESTINATIONS,
                    selectedRoute = selectedRoute,
                    onDestinationClick = { route, isReselected ->
                        clicks += route to isReselected
                        selectedRoute = route
                    },
                )
            }
        }

        val photosLabel = context.getString(R.string.nav_tab_photos)
        val collectionsLabel = context.getString(R.string.nav_tab_collections)
        val favoritesLabel = context.getString(R.string.nav_tab_favorites)
        val searchLabel = context.getString(R.string.nav_tab_search)

        composeTestRule.onNodeWithText(photosLabel).assertIsDisplayed().assertIsSelected()
        composeTestRule.onNodeWithText(collectionsLabel).assertIsDisplayed().assertIsNotSelected()
        composeTestRule.onNodeWithText(favoritesLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(searchLabel).assertIsDisplayed()

        composeTestRule.onNodeWithText(photosLabel).performClick()
        composeTestRule.onNodeWithText(collectionsLabel).performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(AppRoute.Feed to true, AppRoute.Collections to false),
                clicks,
            )
        }
        composeTestRule.onNodeWithText(collectionsLabel).assertIsSelected()
    }

    @Test
    fun expressiveNavigationRailTogglesExpansionAndTriggersRandomize() {
        var randomClicks = 0
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeTestRule.setContent {
            ImageFeedTheme {
                ExpressiveNavigationRail(
                    destinations = TOP_LEVEL_DESTINATIONS,
                    selectedRoute = AppRoute.Feed,
                    isRandomLoading = false,
                    onRandomClick = { randomClicks++ },
                    onDestinationClick = { _, _ -> },
                )
            }
        }

        val expandDesc = context.getString(R.string.nav_rail_expand_accessibility)
        val collapseDesc = context.getString(R.string.nav_rail_collapse_accessibility)
        val randomizeDesc = context.getString(R.string.feed_randomize_accessibility)

        composeTestRule.onNodeWithContentDescription(expandDesc).performClick()
        composeTestRule.onNodeWithContentDescription(collapseDesc).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(randomizeDesc).performClick()
        composeTestRule.runOnIdle {
            assertEquals(1, randomClicks)
        }
    }

    @Test
    fun apertureRandomizeButtonAndPlaceholderRenderInBothStates() {
        var clicked = false
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val randomizeDesc = context.getString(R.string.feed_randomize_accessibility)
        val prompt = context.getString(R.string.details_placeholder_prompt)

        composeTestRule.setContent {
            ImageFeedTheme {
                Box(modifier = Modifier.size(160.dp)) {
                    ApertureRandomizeButton(
                        isLoading = true,
                        onClick = { clicked = true },
                        modifier = Modifier.testTag("aperture_button"),
                    )
                }
                PhotoDetailPlaceholder()
            }
        }

        composeTestRule.onNodeWithTag("aperture_button").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithContentDescription(randomizeDesc).assertIsDisplayed()
        composeTestRule.onNodeWithText(prompt).assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertTrue(clicked)
        }
    }

    @Test
    fun polygonAndMorphShapesCreateValidOutlinesAndHandleZeroSize() {
        val density = Density(density = 2f)
        val polygonShape = PolygonShape(ExpressiveShapeTokens.SoftScallopPolygon)
        val morphShape =
            MorphPolygonShape(
                morph =
                    Morph(
                        start = ExpressiveShapeTokens.ApertureRestPolygon,
                        end = ExpressiveShapeTokens.ApertureActivePolygon,
                    ),
                percentage = HALF_PROGRESS,
            )

        val zeroOutline = polygonShape.createOutline(Size.Zero, LayoutDirection.Ltr, density)
        assertEquals(Outline.Rectangle(Rect.Zero), zeroOutline)

        val validSize = Size(OUTLINE_SIZE_PX, OUTLINE_SIZE_PX)
        val polyOutline = polygonShape.createOutline(validSize, LayoutDirection.Ltr, density)
        val morphOutline = morphShape.createOutline(validSize, LayoutDirection.Ltr, density)

        assertTrue(polyOutline is Outline.Generic)
        assertTrue(morphOutline is Outline.Generic)
        assertFalse((polyOutline as Outline.Generic).path.isEmpty)
        assertFalse((morphOutline as Outline.Generic).path.isEmpty)

        val rectSize = Size(RECT_WIDTH_PX, RECT_HEIGHT_PX)
        val rotatedPoly =
            PolygonShape(
                ExpressiveShapeTokens.SquirclePolygon,
                rotationDegrees = ROTATION_DEGREES,
            ).createOutline(rectSize, LayoutDirection.Ltr, density) as Outline.Generic
        val rotatedBounds = rotatedPoly.path.getBounds()
        assertTrue(rotatedBounds.left >= -BOUNDS_TOLERANCE_PX)
        assertTrue(rotatedBounds.top >= -BOUNDS_TOLERANCE_PX)
        assertTrue(rotatedBounds.right <= RECT_WIDTH_PX + BOUNDS_TOLERANCE_PX)
        assertTrue(rotatedBounds.bottom <= RECT_HEIGHT_PX + BOUNDS_TOLERANCE_PX)
    }
}
