package com.example.imagefeed.android.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NavigatorTest {
    private fun createTestNavigator(
        startRoute: AppRoute = AppRoute.Feed,
        topLevelRoutes: Set<AppRoute> = setOf(AppRoute.Feed, AppRoute.Collections, AppRoute.Search()),
    ): Pair<Navigator, NavigationState> {
        val topLevelState = mutableStateOf(startRoute)
        val backStacks =
            topLevelRoutes.associateWith { key ->
                NavBackStack<NavKey>(key)
            }
        val state =
            NavigationState(
                startRoute = startRoute,
                topLevelRouteState = topLevelState,
                backStacks = backStacks,
            )
        return Navigator(state) to state
    }

    @Test
    fun tabSwitchingUpdatesTopLevelRoute() {
        val (navigator, state) = createTestNavigator()
        assertEquals(AppRoute.Feed, state.topLevelRoute)

        navigator.navigate(AppRoute.Collections)
        assertEquals(AppRoute.Collections, state.topLevelRoute)

        navigator.navigate(AppRoute.Search())
        assertEquals(AppRoute.Search(), state.topLevelRoute)

        navigator.navigate(AppRoute.Feed)
        assertEquals(AppRoute.Feed, state.topLevelRoute)
    }

    @Test
    fun subRoutesArePushedToCurrentStackWithoutLeaking() {
        val (navigator, state) = createTestNavigator()

        // Switch to Search
        navigator.navigate(AppRoute.Search())
        assertEquals(AppRoute.Search(), state.topLevelRoute)

        // Push photo detail onto search stack
        val photoRoute = AppRoute.PhotoDetails("test_photo_1")
        navigator.navigate(photoRoute)

        val searchStack = state.backStacks[AppRoute.Search()]
        val feedStack = state.backStacks[AppRoute.Feed]
        val collectionsStack = state.backStacks[AppRoute.Collections]

        assertEquals(listOf(AppRoute.Search(), photoRoute), searchStack?.toList())
        assertEquals(listOf(AppRoute.Feed), feedStack?.toList())
        assertEquals(listOf(AppRoute.Collections), collectionsStack?.toList())
    }

    @Test
    fun exitThroughHomeBackNavigationSemantics() {
        val (navigator, state) = createTestNavigator()

        // Navigate to Collections and push collection details
        navigator.navigate(AppRoute.Collections)
        val collDetails = AppRoute.CollectionDetails("col_123")
        navigator.navigate(collDetails)

        // Pop detail: should remain on Collections tab with root entry
        val poppedDetail = navigator.goBack()
        assertTrue(poppedDetail)
        assertEquals(AppRoute.Collections, state.topLevelRoute)
        assertEquals(listOf(AppRoute.Collections), state.backStacks[AppRoute.Collections]?.toList())

        // Next pop: at Collections root, should navigate back to Feed (startRoute)
        val poppedToStart = navigator.goBack()
        assertTrue(poppedToStart)
        assertEquals(AppRoute.Feed, state.topLevelRoute)

        // At startRoute root: goBack should return false (system exit)
        val poppedRoot = navigator.goBack()
        assertFalse(poppedRoot)
        assertEquals(AppRoute.Feed, state.topLevelRoute)
    }

    @Test
    fun subRouteOnStartRoutePopsBeforeRootExit() {
        val (navigator, state) = createTestNavigator()

        val photoRoute = AppRoute.PhotoDetails("feed_photo")
        navigator.navigate(photoRoute)
        assertEquals(2, state.backStacks[AppRoute.Feed]?.size)

        val popped = navigator.goBack()
        assertTrue(popped)
        assertEquals(1, state.backStacks[AppRoute.Feed]?.size)
        assertEquals(AppRoute.Feed, state.backStacks[AppRoute.Feed]?.lastOrNull())

        val poppedRoot = navigator.goBack()
        assertFalse(poppedRoot)
    }

    @Test
    fun reselectEventIsEmitted() =
        runTest {
            val (navigator, _) = createTestNavigator()

            var receivedEvent: NavKey? = null
            backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
                receivedEvent = navigator.reselectEvents.first()
            }

            navigator.onReselect(AppRoute.Feed)

            assertEquals(AppRoute.Feed, receivedEvent)
        }

    @Test
    fun getTopLevelRoutesInUseBehavior() {
        val (navigator, state) = createTestNavigator()

        // At feed: only startRoute in use
        assertEquals(listOf(AppRoute.Feed), state.getTopLevelRoutesInUse())

        // At collections: [startRoute, collections]
        navigator.navigate(AppRoute.Collections)
        assertEquals(listOf(AppRoute.Feed, AppRoute.Collections), state.getTopLevelRoutesInUse())

        // At search: [startRoute, search]
        navigator.navigate(AppRoute.Search())
        assertEquals(listOf(AppRoute.Feed, AppRoute.Search()), state.getTopLevelRoutesInUse())
    }

    @Test
    fun navigatingToSearchWithQuerySwitchesToSearchTabAndPushesToSearchStack() {
        val (navigator, state) = createTestNavigator()

        val searchQuery = AppRoute.Search(query = "mountains")
        navigator.navigate(searchQuery)

        // Top level route switches to Search root
        assertEquals(AppRoute.Search(), state.topLevelRoute)

        // Search stack contains root Search and query Search
        val searchStack = state.backStacks[AppRoute.Search()]
        assertEquals(listOf(AppRoute.Search(), searchQuery), searchStack?.toList())

        // Feed stack is untouched
        val feedStack = state.backStacks[AppRoute.Feed]
        assertEquals(listOf(AppRoute.Feed), feedStack?.toList())
    }

    @Test
    fun searchWithQueryPopsBackToSearchRootThenToFeed() {
        val (navigator, state) = createTestNavigator()

        // Push photo onto feed
        val photoRoute = AppRoute.PhotoDetails("photo_1")
        navigator.navigate(photoRoute)

        // Navigate to search tag from photo
        val searchQuery = AppRoute.Search(query = "nature")
        navigator.navigate(searchQuery)

        assertEquals(AppRoute.Search(), state.topLevelRoute)
        assertEquals(listOf(AppRoute.Search(), searchQuery), state.backStacks[AppRoute.Search()]?.toList())
        assertEquals(listOf(AppRoute.Feed, photoRoute), state.backStacks[AppRoute.Feed]?.toList())

        // First pop: pops query from search stack, stays on Search root
        val poppedQuery = navigator.goBack()
        assertTrue(poppedQuery)
        assertEquals(AppRoute.Search(), state.topLevelRoute)
        assertEquals(listOf(AppRoute.Search()), state.backStacks[AppRoute.Search()]?.toList())

        // Second pop: at Search root, transitions back to Feed tab
        val poppedToFeed = navigator.goBack()
        assertTrue(poppedToFeed)
        assertEquals(AppRoute.Feed, state.topLevelRoute)
        assertEquals(listOf(AppRoute.Feed, photoRoute), state.backStacks[AppRoute.Feed]?.toList())

        // Third pop: pops photo on Feed
        val poppedPhoto = navigator.goBack()
        assertTrue(poppedPhoto)
        assertEquals(listOf(AppRoute.Feed), state.backStacks[AppRoute.Feed]?.toList())

        // Fourth pop: at Feed root, triggers exit
        val poppedExit = navigator.goBack()
        assertFalse(poppedExit)
    }
}
