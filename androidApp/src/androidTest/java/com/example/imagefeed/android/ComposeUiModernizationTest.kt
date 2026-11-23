package com.example.imagefeed.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.imagefeed.android.adaptive.AdaptiveLayoutInfo
import com.example.imagefeed.android.adaptive.DevicePosture
import com.example.imagefeed.android.adaptive.LocalAdaptiveLayoutInfo
import com.example.imagefeed.android.theme.ConnectedToggleGroup
import com.example.imagefeed.android.theme.ExpressiveLoadingIndicator
import com.example.imagefeed.android.theme.ImageFeedTheme
import com.example.imagefeed.model.Exif
import com.example.imagefeed.model.Photo
import com.example.imagefeed.model.PhotoLinks
import com.example.imagefeed.model.PhotoUrls
import com.example.imagefeed.model.ProfileImage
import com.example.imagefeed.model.Tag
import com.example.imagefeed.model.User
import com.example.imagefeed.model.UserLinks
import com.example.imagefeed.presentation.search.SearchFilters
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeUiModernizationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun textFieldStateSearchBarInputAndSubmit() {
        var submittedQuery = ""
        composeTestRule.setContent {
            ImageFeedTheme {
                val searchBarState = rememberSearchBarState()
                val textFieldState = rememberTextFieldState()
                SearchBar(
                    state = searchBarState,
                    inputField = {
                        SearchBarDefaults.InputField(
                            textFieldState = textFieldState,
                            searchBarState = searchBarState,
                            onSearch = { submittedQuery = it },
                            modifier = Modifier.testTag("search_input"),
                            placeholder = { Text("Search photos") },
                        )
                    },
                )
            }
        }

        composeTestRule.onNodeWithTag("search_input").performTextInput("mountains")
        composeTestRule.onNodeWithTag("search_input").performImeAction()
        composeTestRule.runOnIdle {
            assertEquals("mountains", submittedQuery)
        }
    }

    @Test
    fun connectedToggleGroupUpdatesSelection() {
        var appliedFilters: SearchFilters? = null
        composeTestRule.setContent {
            ImageFeedTheme {
                var selectedOrderBy by remember { mutableStateOf("relevant") }
                var selectedOrientation by remember { mutableStateOf<String?>(null) }
                Column {
                    ConnectedToggleGroup(
                        options = listOf("relevant" to "Relevant", "latest" to "Latest"),
                        selected = selectedOrderBy,
                        onSelect = {
                            selectedOrderBy = it
                            appliedFilters = SearchFilters(orderBy = selectedOrderBy, orientation = selectedOrientation)
                        },
                    )
                    ConnectedToggleGroup(
                        options =
                            listOf(
                                null to "All",
                                "landscape" to "Landscape",
                                "portrait" to "Portrait",
                            ),
                        selected = selectedOrientation,
                        onSelect = {
                            selectedOrientation = it
                            appliedFilters = SearchFilters(orderBy = selectedOrderBy, orientation = selectedOrientation)
                        },
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Latest").performClick()
        composeTestRule.onNodeWithText("Landscape").performClick()
        composeTestRule.runOnIdle {
            assertEquals(SearchFilters(orderBy = "latest", orientation = "landscape"), appliedFilters)
        }
        composeTestRule.onNodeWithText("Latest").assertIsOn()
        composeTestRule.onNodeWithText("Landscape").assertIsOn()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun photoInspectorPaneRendersExifGridAndFlexBoxTags() {
        var clickedTag = ""
        val samplePhoto =
            Photo(
                id = "test_photo",
                createdAt = "2026-11-23T00:00:00Z",
                width = 4000,
                height = 3000,
                color = "#224466",
                blurHash = null,
                description = "Alpine ridge",
                altDescription = "Alpine ridge",
                urls = PhotoUrls(raw = "", full = "", regular = "", small = "", thumb = ""),
                user =
                    User(
                        id = "u1",
                        username = "tester",
                        name = "Test Photographer",
                        profileImage = ProfileImage(small = "", medium = "", large = ""),
                        links = UserLinks(self = "", html = "", photos = ""),
                    ),
                links =
                    PhotoLinks(
                        self = "",
                        html = "",
                        download = "",
                        downloadLocation = "",
                    ),
                exif =
                    Exif(
                        make = "Fujifilm",
                        model = "X-T5",
                        exposureTime = "1/500",
                        aperture = "2.8",
                        focalLength = "35",
                        iso = 160,
                    ),
                tags = listOf(Tag("mountains"), Tag("alpine")),
            )

        composeTestRule.setContent {
            ImageFeedTheme {
                CompositionLocalProvider(
                    LocalAdaptiveLayoutInfo provides
                        AdaptiveLayoutInfo(
                            windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(412.dp, 915.dp)),
                            posture = DevicePosture.Normal,
                            screenWidthDp = 412,
                            screenHeightDp = 915,
                        ),
                ) {
                    PhotoInspectorPane(
                        photo = samplePhoto,
                        stats = null,
                        onUserClick = {},
                        onTagClick = { clickedTag = it },
                        onShowSnackbar = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("f/2.8").assertIsDisplayed()
        composeTestRule.onNodeWithText("1/500s").assertIsDisplayed()
        composeTestRule.onNodeWithText("35mm").assertIsDisplayed()
        composeTestRule.onNodeWithText("160").assertIsDisplayed()
        composeTestRule.onNodeWithText("MOUNTAINS").performClick()
        composeTestRule.runOnIdle {
            assertEquals("mountains", clickedTag)
        }
    }

    @Test
    fun expressiveLoadingIndicatorRenders() {
        composeTestRule.setContent {
            ImageFeedTheme {
                ExpressiveLoadingIndicator(modifier = Modifier.testTag("expressive_loader"))
            }
        }
        composeTestRule.onNodeWithTag("expressive_loader").assertIsDisplayed()
    }
}
