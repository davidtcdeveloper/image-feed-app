package com.example.imagefeed.android.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class Navigator(
    val state: NavigationState,
) {
    private val _reselectEvents = MutableSharedFlow<NavKey>(extraBufferCapacity = 1)
    val reselectEvents = _reselectEvents.asSharedFlow()

    fun navigate(route: NavKey) {
        val matchingTopLevel = state.backStacks.keys.firstOrNull { it::class == route::class }
        if (matchingTopLevel != null) {
            // Switch to top level route
            state.topLevelRoute = matchingTopLevel
            if (route is AppRoute.Search && route.query.isNotEmpty()) {
                if (state.backStacks[matchingTopLevel]?.lastOrNull() != route) {
                    state.backStacks[matchingTopLevel]?.add(route)
                }
            }
        } else {
            // Push sub-route onto current top-level back stack
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun onReselect(route: NavKey) {
        _reselectEvents.tryEmit(route)
    }

    fun goBack(): Boolean {
        val currentStack = state.backStacks[state.topLevelRoute] ?: return false
        val currentRoute = currentStack.lastOrNull()

        return if (currentRoute == state.topLevelRoute) {
            if (state.topLevelRoute != state.startRoute) {
                state.topLevelRoute = state.startRoute
                true
            } else {
                false // At the root of startRoute; delegate to system/activity exit
            }
        } else {
            currentStack.removeLastOrNull()
            true
        }
    }
}
