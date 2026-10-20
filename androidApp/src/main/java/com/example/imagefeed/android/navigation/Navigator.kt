package com.example.imagefeed.android.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class Navigator(
    val state: NavigationState,
) {
    companion object {
        const val MAX_BACKSTACK_DEPTH = 8
    }

    private val _reselectEvents = MutableSharedFlow<NavKey>(extraBufferCapacity = 1)
    val reselectEvents = _reselectEvents.asSharedFlow()

    fun navigate(route: NavKey) {
        val matchingTopLevel = state.backStacks.keys.firstOrNull { it::class == route::class }
        if (matchingTopLevel != null) {
            // Switch to top level route
            state.topLevelRoute = matchingTopLevel
            if (route is AppRoute.Search && route.query.isNotEmpty()) {
                val currentStack = state.backStacks[matchingTopLevel]
                if (currentStack != null && currentStack.lastOrNull() != route) {
                    val existingIndex = currentStack.indexOf(route)
                    if (existingIndex > 0) {
                        currentStack.removeAt(existingIndex)
                    }
                    currentStack.add(route)
                    trimStackDepth(currentStack)
                }
            }
        } else {
            // Push sub-route onto current top-level back stack
            val currentStack = state.backStacks[state.topLevelRoute]
            if (currentStack != null && currentStack.lastOrNull() != route) {
                val existingIndex = currentStack.indexOf(route)
                if (existingIndex > 0) {
                    currentStack.removeAt(existingIndex)
                }
                currentStack.add(route)
                trimStackDepth(currentStack)
            }
        }
    }

    private fun trimStackDepth(stack: MutableList<NavKey>) {
        while (stack.size > MAX_BACKSTACK_DEPTH && stack.size > 1) {
            stack.removeAt(1)
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
