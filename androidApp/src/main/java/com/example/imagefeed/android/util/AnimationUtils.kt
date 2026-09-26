package com.example.imagefeed.android.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun Modifier.staggeredEntrance(
    index: Int,
    baseDelayMs: Int = 40,
    stepDelayMs: Int = 25,
): Modifier {
    val alpha = remember { Animatable(0f) }
    val translationY = remember { Animatable(80f) }

    LaunchedEffect(key1 = index) {
        val cappedIndex = index % 6
        val delay = baseDelayMs + (cappedIndex * stepDelayMs)
        kotlinx.coroutines.delay(delay.toLong().milliseconds)

        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350),
            )
        }
        launch {
            translationY.animateTo(
                targetValue = 0f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
            )
        }
    }

    return this.graphicsLayer(
        alpha = alpha.value,
        translationY = translationY.value,
    )
}

@Composable
fun Modifier.bounceClick(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1.0f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "bounceScale",
    )

    val elevation by animateFloatAsState(
        targetValue = if (isPressed && enabled) 8f else 2f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh,
            ),
        label = "bounceShadow",
    )

    return this
        .shadow(
            elevation = elevation.dp,
            shape = MaterialTheme.shapes.medium,
        ).graphicsLayer(
            scaleX = scale,
            scaleY = scale,
        ).clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1300, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "shimmerTranslate",
    )

    val baseColor = MaterialTheme.colorScheme.surfaceContainerLow
    val highlightColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val shimmerColors =
        listOf(
            baseColor,
            highlightColor,
            baseColor,
        )

    val brush =
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(x = translateAnim, y = 0f),
            end = Offset(x = translateAnim + 400f, y = 400f),
        )

    return this.background(brush)
}

@Composable
fun PhotoCardSkeleton(aspectRatio: Float) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .shimmerEffect()
                .padding(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(MaterialTheme.shapes.small)
                    .shimmerEffect(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .shimmerEffect(),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier =
                    Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .shimmerEffect(),
            )
        }
    }
}

@Composable
fun PhotoGridSkeleton() {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
    ) {
        val aspectRatios = listOf(0.7f, 1.2f, 1.5f, 0.8f, 1.0f, 1.3f)
        items(12) { index ->
            PhotoCardSkeleton(aspectRatio = aspectRatios[index % aspectRatios.size])
        }
    }
}

@Composable
fun CollectionMosaicCardSkeleton() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .shimmerEffect()
                .padding(12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val smallShape = MaterialTheme.shapes.small
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(smallShape.copy(topEnd = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)))
                        .shimmerEffect(),
            )
            Column(
                modifier =
                    Modifier
                        .width(110.dp)
                        .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(
                                smallShape.copy(
                                    topStart = CornerSize(0.dp),
                                    bottomStart = CornerSize(0.dp),
                                    bottomEnd = CornerSize(0.dp),
                                ),
                            ).shimmerEffect(),
                )
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(
                                smallShape.copy(
                                    topStart = CornerSize(0.dp),
                                    bottomStart = CornerSize(0.dp),
                                    topEnd = CornerSize(0.dp),
                                ),
                            ).shimmerEffect(),
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier =
                Modifier
                    .width(180.dp)
                    .height(16.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .shimmerEffect(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier =
                Modifier
                    .width(260.dp)
                    .height(12.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .shimmerEffect(),
        )
    }
}

@Composable
fun UserProfileHeaderSkeleton() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .shimmerEffect(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier =
                Modifier
                    .width(140.dp)
                    .height(18.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .shimmerEffect(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier =
                Modifier
                    .width(90.dp)
                    .height(12.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .shimmerEffect(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier =
                Modifier
                    .width(240.dp)
                    .height(12.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .shimmerEffect(),
        )
    }
}
