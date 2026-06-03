package com.example.imagefeed.android

import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun calculateGridColumns(): StaggeredGridCells {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    
    return when {
        screenWidth >= 840 -> StaggeredGridCells.Adaptive(240.dp) // Large Tablets / Landscape
        screenWidth >= 600 -> StaggeredGridCells.Adaptive(180.dp) // Portrait Tablets
        else -> StaggeredGridCells.Fixed(2)                       // Handheld Mobile Phones
    }
}
