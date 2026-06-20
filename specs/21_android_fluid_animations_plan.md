# Implementation Plan: Fluid Animations in Android Jetpack Compose

This document establishes a highly technical, production-ready blueprint for implementing fluid UI animations, motion design, and touch feedback in the Android Jetpack Compose codebase. 

The scope covers shared element transitions, staggered grid item entrances, tactile card micro-interactions, and custom shimmering loading placeholders.

---

## 1. Shared Element Transitions (Compose 1.7.0+)

### 1.1 Technical Architecture
Using Compose’s modern `SharedTransitionLayout` APIs, shared elements are tracked and animated between screen boundaries. The implementation relies on three core classes:
1. **`SharedTransitionLayout`**: A layout container that wraps the navigation host and overlay-renders elements during scene transitions.
2. **`SharedTransitionScope`**: Provided by `SharedTransitionLayout`, exposing `Modifier.sharedElement()` and `Modifier.sharedBounds()`.
3. **`AnimatedVisibilityScope`**: Provided by navigation transitions (`AnimatedContent` or `AnimatedVisibility`), coordinating exit/entrance animations.

Since the navigation layer is built on `androidx.navigation3.ui.NavDisplay`, we wrap `NavDisplay` inside `SharedTransitionLayout`. We then thread `SharedTransitionScope` and `AnimatedVisibilityScope` down to individual screens.

### 1.2 Feed Grid Items (`FeedScreen.kt`) to Hero Images (`PhotoDetailsScreen.kt`)
Each photo has a unique `id` (e.g., `photo.id`). We use this ID as the transition key.

#### Code Signature Updates in `MainActivity.kt`:
```kotlin
SharedTransitionLayout {
    NavDisplay(
        backStack = backStackState,
        onBack = { ... },
        modifier = Modifier.padding(innerPadding),
        entryProvider = { navKey ->
            val key = navKey as Screen
            when (key) {
                is Screen.Feed -> NavEntry(key) {
                    FeedScreen(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@NavEntry, // Provided by NavEntry's AnimatedContent context
                        presenter = presenter,
                        ...
                    )
                }
                is Screen.PhotoDetails -> NavEntry(key) {
                    PhotoDetailsScreen(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@NavEntry,
                        photoId = key.photoId,
                        ...
                    )
                }
            }
        }
    )
}
```

#### Shared Modifier in `PhotoCard` (`MainActivity.kt` / `FeedScreen`):
Inside `PhotoCard`, we apply `Modifier.sharedElement` to the `Image` component.
```kotlin
with(sharedTransitionScope) {
    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .sharedElement(
                state = rememberSharedContentState(key = "photo_img_${photo.id}"),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = boundsTransform
            ),
        contentScale = ContentScale.Crop
    )
}
```

#### Shared Modifier in `PhotoDetailsContent` (`PhotoDetailsScreen.kt`):
```kotlin
with(sharedTransitionScope) {
    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .sharedElement(
                state = rememberSharedContentState(key = "photo_img_${photo.id}"),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = boundsTransform
            ),
        contentScale = ContentScale.Crop
    )
}
```

#### Reusable Bounds Transform (Custom Spring Animation):
```kotlin
val boundsTransform = BoundsTransform { _, _ ->
    spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}
```

### 1.3 Collections List (`CollectionsFeedScreen.kt`) to Details (`CollectionDetailScreen.kt`)
We transition the left-aligned cover photo from the `CollectionMosaicCard` to the expansive, blurred `CollectionDetailHeader` backdrop.

- **Transition Key**: `"collection_cover_${collection.id}"`
- **Mechanism**: We use `Modifier.sharedBounds()` on the cover photo box since the target header acts as a larger backdrop with a blurred overlay.
- **Bounds Transform**: A gentle spring (`dampingRatio = Spring.DampingRatioNoBouncy`, `stiffness = Spring.StiffnessMedium`).

---

## 2. Staggered Item Entrance Transitions

### 2.1 Reusable Modifier (`Modifier.staggeredEntrance`)
To slide and fade elements smoothly into view in `LazyVerticalStaggeredGrid` without heavy re-renders, we create a highly optimized custom modifier that uses `Animatable` and computes an index-based staggered delay.

We use a modulo operator (`index % columns`) or (`index % 6`) to cap the maximum delay, preventing later-page items from waiting indefinitely during deep scrolls.

```kotlin
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun Modifier.staggeredEntrance(
    index: Int,
    baseDelayMs: Int = 40,
    stepDelayMs: Int = 25
): Modifier {
    val alpha = remember { Animatable(0f) }
    val translationY = remember { Animatable(80f) } // Slide up from 80px

    LaunchedEffect(key1 = index) {
        // Prevent endless delays by wrapping the index with a rolling modulo
        val cappedIndex = index % 6 
        val delay = baseDelayMs + (cappedIndex * stepDelayMs)
        kotlinx.coroutines.delay(delay.toLong())
        
        // Run fade-in and slide-up in parallel
        kotlinx.coroutines.launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350)
            )
        }
        kotlinx.coroutines.launch {
            translationY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    return this
        .graphicsLayer(
            alpha = alpha.value,
            translationY = translationY.value
        )
}
```

### 2.2 Grid Item Reuse & Animation Keys
To prevent animations from re-triggering when items are recycled during scrolling:
- Ensure all items in `LazyVerticalStaggeredGrid` and `LazyColumn` use stable unique keys (e.g., `key = { photo.id }`).
- This ensures the Compose runtime identifies that the composable node is reused rather than newly spawned, preserving the animation state.

---

## 3. Card Micro-interactions

To provide immediate physical tactile feedback, card taps trigger springy press-scale transformations (scale down on press, snap back on release) and a subtle elevation lift.

### 3.1 Press-Scale and Elevation Modifier (`Modifier.bounceClick`)
We leverage `MutableInteractionSource` to capture touch states and apply spring-driven scale and elevation transformations.

```kotlin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.bounceClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Scale spring animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounceScale"
    )
    
    // Subtle shadow lift animation
    val elevation by animateFloatAsState(
        targetValue = if (isPressed && enabled) 8f else 2f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "bounceShadow"
    )

    return this
        .shadow(
            elevation = elevation.dp,
            shape = RoundedCornerShape(12.dp)
        )
        .graphicsLayer(
            scaleX = scale,
            scaleY = scale
        )
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Suppress standard ripple to let spring scale dominate
            enabled = enabled,
            onClick = onClick
        )
}
```

### 3.2 Integration Locations
This modifier replaces standard `.clickable {}` in:
- `PhotoCard` (`MainActivity.kt`)
- `CollectionMosaicCard` (`CollectionsFeedScreen.kt`)
- `CollectionRowLayout` (`UserProfileScreen.kt`)
- Portfolio grids (`UserProfileScreen.kt`)

---

## 4. Shimmer Skeleton Loading

To eliminate visual disruption caused by circular progress indicators, we replace them with high-fidelity shimmering skeleton templates matching the structural layouts.

### 4.1 Custom Shimmer Brush (`Modifier.shimmerEffect`)
We build an animated gradient brush that sweeps a highlight across components.

```kotlin
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        Color(0xFF16161B),
        Color(0xFF25252F),
        Color(0xFF16161B)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim, y = 0f),
        end = Offset(x = translateAnim + 400f, y = 400f)
    )

    return this.background(brush)
}
```

### 4.2 Photo Card Skeleton Placeholder (`PhotoCardSkeleton.kt`)
Used to fill out the grid while photos are fetching.

```kotlin
@Composable
fun PhotoCardSkeleton(aspectRatio: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(12.dp))
            .shimmerEffect()
    )
}
```

### 4.3 Full Skeleton Grid Wrapper (`PhotoGridSkeleton.kt`):
Replaces `CircularProgressIndicator` entirely inside feed and profile screens:
```kotlin
@Composable
fun PhotoGridSkeleton() {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        val aspectRatios = listOf(0.7f, 1.2f, 1.5f, 0.8f, 1.0f, 1.3f)
        items(12) { index ->
            PhotoCardSkeleton(aspectRatio = aspectRatios[index % aspectRatios.size])
        }
    }
}
```

---

## 5. Exact Classes and Lines of Code to Modify

All code changes reside in the worktree: `/Users/davidtiagoconceicao/Developer/image-feed-app-animations/`

### 5.1 `androidApp/.../MainActivity.kt`
- **Locations**:
  - `MainActivity.onCreate()` (Line 140): Wrap `NavDisplay` container inside `SharedTransitionLayout` to establish the root motion scope.
  - `FeedScreen` signature (Line 451): Add parameters `sharedTransitionScope: SharedTransitionScope` and `animatedVisibilityScope: AnimatedVisibilityScope`.
  - `FeedScreen` loading branch (Line 559): Replace `<CircularProgressIndicator>` with `<PhotoGridSkeleton />`.
  - `PhotoCard` signature (Line 605): Add parameters `sharedTransitionScope: SharedTransitionScope`, `animatedVisibilityScope: AnimatedVisibilityScope`, and `index: Int`.
  - `PhotoCard` Card definition (Line 634): Remove `.clip(RoundedCornerShape(12.dp)).clickable { onClick() }` and replace with `.bounceClick(onClick = onClick).staggeredEntrance(index = index)`.
  - `PhotoCard` Image component (Line 644): Add transition modifier `with(sharedTransitionScope) { Modifier.sharedElement(state = rememberSharedContentState("photo_${photo.id}"), animatedVisibilityScope = animatedVisibilityScope) }`.

### 5.2 `androidApp/.../PhotoDetailsScreen.kt`
- **Locations**:
  - `PhotoDetailsScreen` signature (Line 84): Add `sharedTransitionScope: SharedTransitionScope` and `animatedVisibilityScope: AnimatedVisibilityScope`.
  - `PhotoDetailsContent` signature (Line 181): Add the transition scopes.
  - `PhotoDetailsContent` Image component (Line 220): Attach `Modifier.sharedElement(rememberSharedContentState("photo_${photo.id}"), animatedVisibilityScope)`.

### 5.3 `androidApp/.../CollectionsFeedScreen.kt`
- **Locations**:
  - `CollectionsFeedScreen` loading branch (Line 122): Replace standard `<CircularProgressIndicator>` with `<CollectionGridSkeleton />` (shimmer elements).
  - `CollectionMosaicCard` signature (Line 160): Accept `sharedTransitionScope: SharedTransitionScope`, `animatedVisibilityScope: AnimatedVisibilityScope`, and `index: Int`.
  - `CollectionMosaicCard` Card component (Line 182): Replace `.clickable { onClick() }` with `.bounceClick(onClick = onClick).staggeredEntrance(index)`.
  - `CollectionMosaicCard` cover photo Image (Line 208): Apply `.sharedBounds(rememberSharedContentState("col_cover_${collection.id}"), animatedVisibilityScope)` to transition mosaic card cover background seamlessly to the details backdrop.

### 5.4 `androidApp/.../CollectionDetailScreen.kt`
- **Locations**:
  - `CollectionDetailScreen` signature (Line 71): Add transition scopes.
  - `CollectionDetailHeader` signature (Line 234): Add transition scopes.
  - `CollectionDetailHeader` backdrop Image (Line 263): Attach `.sharedBounds(rememberSharedContentState("col_cover_${collectionId}"), animatedVisibilityScope)`.
  - Photos items block (Line 202): Update `PhotoCard` parameters to supply index, animation scopes, and add staggered entrance logic.

### 5.5 `androidApp/.../UserProfileScreen.kt`
- **Locations**:
  - `PortfolioTabContent` staggered grid (Line 493): supply `index` to `PhotoCard` items to activate staggered item slide-ups.
  - `CollectionRowLayout` Card wrapper (Line 605): Replace `.clickable` with `.bounceClick(onClick = onClick)` to ensure consistent bouncy card interaction inside photographer profile.
