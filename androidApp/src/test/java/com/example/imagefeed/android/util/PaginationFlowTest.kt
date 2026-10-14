package com.example.imagefeed.android.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PaginationFlowTest {
    data class MockLayoutInfo(
        val totalItems: Int,
        val lastVisibleIndex: Int,
    )

    @Test
    fun paginationTriggersExactlyOnceWhenCrossingThreshold() =
        runTest {
            val layoutInfoFlow = MutableStateFlow(MockLayoutInfo(totalItems = 20, lastVisibleIndex = 5))
            var isLoading = false
            var hasReachedEnd = false
            val loadNextPageDispatches = mutableListOf<Unit>()

            val job =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    layoutInfoFlow
                        .map { info ->
                            info.totalItems > 0 && info.lastVisibleIndex >= info.totalItems - 6
                        }.distinctUntilChanged()
                        .filter { shouldLoad -> shouldLoad && !isLoading && !hasReachedEnd }
                        .collect {
                            loadNextPageDispatches.add(Unit)
                        }
                }

            // Initially index 5 of 20 -> should not trigger
            assertEquals(0, loadNextPageDispatches.size)

            // User scrolls to index 14 of 20 (>= 20 - 6 = 14) -> threshold crossed!
            layoutInfoFlow.value = MockLayoutInfo(totalItems = 20, lastVisibleIndex = 14)
            assertEquals(1, loadNextPageDispatches.size)

            // User scrolls a bit more to index 15 of 20 -> still above threshold, distinctUntilChanged prevents duplicate
            layoutInfoFlow.value = MockLayoutInfo(totalItems = 20, lastVisibleIndex = 15)
            assertEquals(1, loadNextPageDispatches.size)

            // Loading state toggles to true
            isLoading = true
            layoutInfoFlow.value = MockLayoutInfo(totalItems = 20, lastVisibleIndex = 16)
            assertEquals(1, loadNextPageDispatches.size)

            // New page loads: total items increases to 40, last visible index is 16
            isLoading = false
            layoutInfoFlow.value = MockLayoutInfo(totalItems = 40, lastVisibleIndex = 16)
            // Now 16 is NOT >= 40 - 6, so condition becomes false
            assertEquals(1, loadNextPageDispatches.size)

            // User scrolls to index 34 of 40 (>= 40 - 6 = 34) -> threshold crossed again!
            layoutInfoFlow.value = MockLayoutInfo(totalItems = 40, lastVisibleIndex = 34)
            assertEquals(2, loadNextPageDispatches.size)

            job.cancel()
        }

    @Test
    fun paginationSuppressedWhenHasReachedEndIsTrue() =
        runTest {
            val layoutInfoFlow = MutableStateFlow(MockLayoutInfo(totalItems = 20, lastVisibleIndex = 10))
            val isLoading = false
            val hasReachedEnd = true
            val loadNextPageDispatches = mutableListOf<Unit>()

            val job =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    layoutInfoFlow
                        .map { info ->
                            info.totalItems > 0 && info.lastVisibleIndex >= info.totalItems - 6
                        }.distinctUntilChanged()
                        .filter { shouldLoad -> shouldLoad && !isLoading && !hasReachedEnd }
                        .collect {
                            loadNextPageDispatches.add(Unit)
                        }
                }

            // Cross threshold while hasReachedEnd is true
            layoutInfoFlow.value = MockLayoutInfo(totalItems = 20, lastVisibleIndex = 18)
            assertEquals(0, loadNextPageDispatches.size)

            job.cancel()
        }

    @Test
    fun paginationSuppressedWhenIsLoadingIsTrue() =
        runTest {
            val layoutInfoFlow = MutableStateFlow(MockLayoutInfo(totalItems = 20, lastVisibleIndex = 10))
            val isLoading = true
            val hasReachedEnd = false
            val loadNextPageDispatches = mutableListOf<Unit>()

            val job =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    layoutInfoFlow
                        .map { info ->
                            info.totalItems > 0 && info.lastVisibleIndex >= info.totalItems - 6
                        }.distinctUntilChanged()
                        .filter { shouldLoad -> shouldLoad && !isLoading && !hasReachedEnd }
                        .collect {
                            loadNextPageDispatches.add(Unit)
                        }
                }

            // Cross threshold while isLoading is true
            layoutInfoFlow.value = MockLayoutInfo(totalItems = 20, lastVisibleIndex = 18)
            assertEquals(0, loadNextPageDispatches.size)

            job.cancel()
        }
}
