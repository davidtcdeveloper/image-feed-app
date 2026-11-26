package com.example.imagefeed.android

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.imagefeed.android.details.FloatingActionToolbar
import com.example.imagefeed.android.details.PHOTO_CANVAS_IMAGE_TAG
import com.example.imagefeed.android.details.PHOTO_TOOLBAR_BOOKMARK_TAG
import com.example.imagefeed.android.details.PHOTO_TOOLBAR_TAG
import com.example.imagefeed.android.details.PhotoCanvasPane
import com.example.imagefeed.android.details.PhotoInspectorPane
import com.example.imagefeed.android.details.PhotoZoomScaleKey
import com.example.imagefeed.android.preview.PreviewScaffold
import com.example.imagefeed.android.preview.UiPreviewFixtures
import com.example.imagefeed.model.color.PhotoColorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

private const val DEFAULT_ZOOM_SCALE = 1f
private const val EXPECTED_DOUBLE_TAP_SCALE = 2.5f
private const val ZOOM_TOLERANCE = 0.05f
private const val TALL_CONTENT_HEIGHT_DP = 1600
private const val MAX_COMPACT_TOOLBAR_HEIGHT_DP = 80

@RunWith(AndroidJUnit4::class)
class PhotoDetailsImmersiveTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun hasZoomScaleNear(expected: Float): SemanticsMatcher =
        SemanticsMatcher("PhotoZoomScale ~= $expected") { node ->
            val actual = node.config.getOrElseNullable(PhotoZoomScaleKey) { null }
            actual != null && abs(actual - expected) < ZOOM_TOLERANCE
        }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Test
    fun compactPhotoCanvasDoubleTapZoomsAndResets() {
        val samplePhoto = UiPreviewFixtures.samplePhoto

        composeTestRule.setContent {
            PreviewScaffold(widthDp = 412, heightDp = 892, darkTheme = false) { sharedScope, visibilityScope ->
                PhotoCanvasPane(
                    sharedTransitionScope = sharedScope,
                    animatedVisibilityScope = visibilityScope,
                    photo = samplePhoto,
                    painter = ColorPainter(Color.DarkGray),
                    aspectRatio = samplePhoto.width.toFloat() / samplePhoto.height.toFloat(),
                    isDualPane = false,
                    onUserClick = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag(PHOTO_CANVAS_IMAGE_TAG)
            .assertIsDisplayed()
            .assert(hasZoomScaleNear(DEFAULT_ZOOM_SCALE))

        composeTestRule
            .onNodeWithTag(PHOTO_CANVAS_IMAGE_TAG)
            .performTouchInput { doubleClick() }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(PHOTO_CANVAS_IMAGE_TAG)
            .assert(hasZoomScaleNear(EXPECTED_DOUBLE_TAP_SCALE))

        composeTestRule
            .onNodeWithTag(PHOTO_CANVAS_IMAGE_TAG)
            .performTouchInput { doubleClick() }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(PHOTO_CANVAS_IMAGE_TAG)
            .assert(hasZoomScaleNear(DEFAULT_ZOOM_SCALE))
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Test
    fun compactPhotoCanvasYieldsVerticalScrollToParentAtDefaultScale() {
        val samplePhoto = UiPreviewFixtures.samplePhoto
        lateinit var scrollState: ScrollState

        composeTestRule.setContent {
            PreviewScaffold(widthDp = 412, heightDp = 892, darkTheme = false) { sharedScope, visibilityScope ->
                scrollState = rememberScrollState()
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                ) {
                    PhotoCanvasPane(
                        sharedTransitionScope = sharedScope,
                        animatedVisibilityScope = visibilityScope,
                        photo = samplePhoto,
                        painter = ColorPainter(Color.DarkGray),
                        aspectRatio = samplePhoto.width.toFloat() / samplePhoto.height.toFloat(),
                        isDualPane = false,
                        onUserClick = {},
                    )
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(TALL_CONTENT_HEIGHT_DP.dp),
                    )
                }
            }
        }

        composeTestRule.runOnIdle {
            assertEquals(0, scrollState.value)
        }

        // Swipe inside the upper/middle photo canvas (above CompactAttributionOverlay) so Image's
        // transformable(canPan = { scale > 1f }) directly yields vertical drag to parent scroll.
        composeTestRule
            .onNodeWithTag(PHOTO_CANVAS_IMAGE_TAG)
            .performTouchInput {
                swipeUp(
                    startY = height * 0.6f,
                    endY = height * 0.1f,
                )
            }
        composeTestRule.waitForIdle()

        var scrollAfterDefaultSwipe = 0
        composeTestRule.runOnIdle {
            scrollAfterDefaultSwipe = scrollState.value
            assertTrue(
                "Expected parent scroll to advance at 1.0x zoom, was $scrollAfterDefaultSwipe",
                scrollAfterDefaultSwipe > 0,
            )
        }

        // Bring the photo canvas cleanly back into the viewport after the fling before testing
        // double-tap zoom and zoomed-pan blocking of parent scroll.
        composeTestRule
            .onNodeWithTag(PHOTO_CANVAS_IMAGE_TAG)
            .performScrollTo()
        composeTestRule.waitForIdle()

        var scrollBeforeZoomedSwipe = 0
        composeTestRule.runOnIdle {
            scrollBeforeZoomedSwipe = scrollState.value
        }

        // Double-tap to zoom in; vertical swipe inside the zoomed canvas should pan without scrolling parent.
        composeTestRule
            .onNodeWithTag(PHOTO_CANVAS_IMAGE_TAG)
            .assertIsDisplayed()
            .performTouchInput { doubleClick() }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(PHOTO_CANVAS_IMAGE_TAG)
            .assert(hasZoomScaleNear(EXPECTED_DOUBLE_TAP_SCALE))
            .performTouchInput {
                swipeUp(
                    startY = height * 0.6f,
                    endY = height * 0.2f,
                )
            }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertEquals(scrollBeforeZoomedSwipe, scrollState.value)
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun horizontalFloatingToolbarTracksFullAndRawDownloadsAndTogglesBookmarkSemantics() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Use an unresolvable URI scheme so startActivity never launches a browser over the test Activity.
        val samplePhoto =
            UiPreviewFixtures.samplePhoto.copy(
                urls =
                    UiPreviewFixtures.samplePhoto.urls.copy(
                        full = "imagefeed-test-noop://full",
                        raw = "imagefeed-test-noop://raw",
                    ),
            )
        var trackedDownloads = 0
        var downloadErrorCount = 0

        composeTestRule.setContent {
            PreviewScaffold(widthDp = 412, heightDp = 240, darkTheme = false) { _, _ ->
                var isFavorite by remember { mutableStateOf(false) }
                FloatingActionToolbar(
                    photo = samplePhoto,
                    isFavorite = isFavorite,
                    onTrackDownload = { trackedDownloads++ },
                    onToggleFavorite = { isFavorite = !isFavorite },
                    onShowSnackbar = { downloadErrorCount++ },
                )
            }
        }

        val fullLabel = context.getString(R.string.details_download_full)
        val rawLabel = context.getString(R.string.details_download_raw)
        val shareDesc = context.getString(R.string.content_desc_share)
        val bookmarkAddDesc = context.getString(R.string.details_bookmark_add)
        val bookmarkRemoveDesc = context.getString(R.string.details_bookmark_remove)

        composeTestRule.onNodeWithTag(PHOTO_TOOLBAR_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText(fullLabel).assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText(rawLabel).assertIsDisplayed().performClick()
        composeTestRule.onNodeWithContentDescription(shareDesc).assertIsDisplayed()

        composeTestRule.runOnIdle {
            assertEquals(2, trackedDownloads)
            assertEquals(2, downloadErrorCount)
        }

        composeTestRule
            .onNodeWithTag(PHOTO_TOOLBAR_BOOKMARK_TAG)
            .assertIsDisplayed()
            .assertIsOff()
        composeTestRule.onNodeWithContentDescription(bookmarkAddDesc).assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(PHOTO_TOOLBAR_BOOKMARK_TAG)
            .assertIsOn()
        composeTestRule.onNodeWithContentDescription(bookmarkRemoveDesc).assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(PHOTO_TOOLBAR_BOOKMARK_TAG)
            .assertIsOff()
    }

    @Test
    fun photoInspectorRendersCoreModelBackedSwatchPillsAndCopiesHex() {
        val samplePhoto = UiPreviewFixtures.samplePhoto
        val expectedSwatches = PhotoColorEngine.deriveTonalSwatches(samplePhoto.color)
        assertTrue(expectedSwatches.isNotEmpty())
        var lastSnackbar = ""

        composeTestRule.setContent {
            PreviewScaffold(widthDp = 412, heightDp = 892, darkTheme = false) { _, _ ->
                PhotoInspectorPane(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    photo = samplePhoto,
                    stats = null,
                    showPhotographerHeader = false,
                    onUserClick = {},
                    onTagClick = {},
                    onShowSnackbar = { lastSnackbar = it },
                )
            }
        }

        val firstHex = expectedSwatches.first().hex
        composeTestRule
            .onNodeWithText(firstHex)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.runOnIdle {
            assertTrue("Expected snackbar to mention $firstHex, was '$lastSnackbar'", lastSnackbar.contains(firstHex))
        }
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Test
    fun photoDetailsScreenCompactRendersSatinTopBarCanvasAndFloatingToolbar() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val title = context.getString(R.string.details_title)
        val backDesc = context.getString(R.string.content_desc_back)
        val openWebDesc = context.getString(R.string.details_action_open_web)

        composeTestRule.setContent {
            PreviewScaffold(widthDp = 412, heightDp = 892, darkTheme = false) { sharedScope, visibilityScope ->
                PhotoDetailsScreen(
                    sharedTransitionScope = sharedScope,
                    animatedVisibilityScope = visibilityScope,
                    state = UiPreviewFixtures.photoDetailsState,
                    onRetry = {},
                    onTrackDownload = {},
                    onToggleFavorite = {},
                    onBack = {},
                    onUserClick = {},
                    onTagClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(backDesc).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(openWebDesc).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PHOTO_CANVAS_IMAGE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PHOTO_TOOLBAR_TAG).assertIsDisplayed()
        val toolbarBounds = composeTestRule.onNodeWithTag(PHOTO_TOOLBAR_TAG).getUnclippedBoundsInRoot()
        assertTrue(
            "Expected compact HorizontalFloatingToolbar height (<= $MAX_COMPACT_TOOLBAR_HEIGHT_DP.dp), " +
                "was ${toolbarBounds.height}",
            toolbarBounds.height <= MAX_COMPACT_TOOLBAR_HEIGHT_DP.dp,
        )
    }
}
