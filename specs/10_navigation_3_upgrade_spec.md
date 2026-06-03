# Implementation Step 10: Jetpack Navigation 3 Upgrade Specification

This specification outlines the evaluation, architecture, and structural steps required to upgrade the `androidApp` module to use **Android Jetpack Navigation 3 (Nav3)**. This upgrade transitions the Android application from the legacy, string-based Navigation 2.x API to a state-controlled, type-safe, and Compose-first navigation architecture.

---

## 1. Overview of Jetpack Navigation 3 (Nav3)

Android Jetpack Navigation 3 represents a paradigm shift in how navigation state is managed in Jetpack Compose:
1. **Explicit State Ownership:** Unlike Navigation 2.x, which conceals back stack states inside an opaque `NavController`, Navigation 3 lifts back stack management directly into standard Jetpack Compose state streams (such as a mutable list `mutableStateListOf<Any>()`).
2. **Compile-Time Type Safety:** String-based path interpolation (e.g., `"photo_details/{photoId}"`) is completely eliminated. Routes are defined using standard, strongly-typed Kotlin classes or objects.
3. **Decoupled Screen Rendering:** The UI presentation layer is split from the routing mechanism. Screen rendering is decoupled into separate *Scene Strategies* or an `entryProvider` DSL that maps route objects to composable layouts.
4. **Predictive Back & Animation Control:** Advanced gesture configurations like predictive back are supported natively via built-in `NavDisplay` transition specifications.

---

## 2. Current Architecture & Migration Scope

Currently, the Android app manages navigation inside `androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt` using standard `androidx.navigation:navigation-compose:2.7.7`:

```kotlin
// Legacy Navigation 2.x Setup in MainActivity.kt
val controller = rememberNavController()
val navBackStackEntry by controller.currentBackStackEntryAsState()
val currentRoute = navBackStackEntry?.destination?.route

Scaffold(
    bottomBar = { /* ... */ }
) { innerPadding ->
    NavHost(navController = controller, startDestination = "feed", modifier = Modifier.padding(innerPadding)) {
        composable("feed") { /* Feed UI with string-based click routes */ }
        composable("photo_details/{photoId}") { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId") ?: ""
            PhotoDetailsScreen(photoId = photoId)
        }
        // ...Other string-based composable routes
    }
}
```

### Architectural Impact of Upgrade
* **Shared Module (`shared`):** **No changes are required.** The Kotlin Multiplatform core (comprising repositories, state flows, and presenters) remains intact, since all navigation transitions and state handling are executed on the declarative UI platform layer.
* **iOS App (`iosApp`):** **No changes are required.** The Swift codebase handles native screen transitions independently utilizing SwiftUI's `NavigationStack` and KMP flows.
* **Android App (`androidApp`):** **Complete rewrite of the navigation container.** Legacy `NavHost`, `composable`, and string-based routing configurations in `MainActivity.kt` are replaced with a type-safe `NavDisplay` bound to a serialized back stack state.

---

## 3. Dependency Updates (`libs.versions.toml`)

To adopt Navigation 3, the project must declare the new Google Jetpack artifacts and Lifecycle integrations in the Gradle Version Catalog (`gradle/libs.versions.toml`):

```toml
[versions]
androidx-navigation3 = "1.2.0-alpha03"
androidx-lifecycle-navigation3 = "2.11.0-beta02"

[libraries]
androidx-navigation3-runtime = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "androidx-navigation3" }
androidx-navigation3-ui = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "androidx-navigation3" }
androidx-lifecycle-viewmodel-navigation3 = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-navigation3", version.ref = "androidx-lifecycle-navigation3" }
```

These modules must then be loaded inside `androidApp/build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
```

---

## 4. Upgrading Step-by-Step

### Step 1: Defining Strongly-Typed Navigation Keys
Remove all string route references and replace them with a Kotlin Serialization-annotated hierarchy. Each screen must implement the `NavKey` interface to enable state saving:

```kotlin
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen : NavKey {
    @Serializable
    data object Feed : Screen()

    @Serializable
    data object Collections : Screen()

    @Serializable
    data class CollectionDetails(val collectionId: String) : Screen()

    @Serializable
    data object Search : Screen()

    @Serializable
    data class PhotoDetails(val photoId: String) : Screen()

    @Serializable
    data class UserProfile(val username: String) : Screen()
}
```

### Step 2: Implementing the Entry Provider Mapping
Map each navigation key to its corresponding screen composable using Navigation 3's type-safe `entryProvider` DSL:

```kotlin
import androidx.navigation3.ui.entryProvider
import androidx.navigation3.ui.entry

val entryProvider = entryProvider {
    entry<Screen.Feed> {
        FeedScreen(
            presenter = presenter,
            onPhotoClick = { photo -> backStack.add(Screen.PhotoDetails(photo.id)) },
            onUserClick = { user -> backStack.add(Screen.UserProfile(user.username)) },
            onSearchClick = { backStack.add(Screen.Search) },
            onRandomClick = { handleShake() }
        )
    }
    
    entry<Screen.Collections> {
        CollectionsFeedScreen(
            presenter = collectionsPresenter,
            onCollectionClick = { col -> backStack.add(Screen.CollectionDetails(col.id)) },
            onSearchClick = { backStack.add(Screen.Search) }
        )
    }
    
    entry<Screen.CollectionDetails> { key ->
        CollectionDetailScreen(
            collectionId = key.collectionId,
            onBack = { backStack.removeLastOrNull() },
            onPhotoClick = { photo -> backStack.add(Screen.PhotoDetails(photo.id)) },
            onCollectionClick = { id -> backStack.add(Screen.CollectionDetails(id)) }
        )
    }
    
    entry<Screen.Search> {
        SearchScreen(
            onBack = { backStack.removeLastOrNull() },
            onPhotoClick = { photo -> backStack.add(Screen.PhotoDetails(photo.id)) },
            onUserClick = { user -> backStack.add(Screen.UserProfile(user.username)) }
        )
    }
    
    entry<Screen.PhotoDetails> { key ->
        PhotoDetailsScreen(
            photoId = key.photoId,
            onBack = { backStack.removeLastOrNull() },
            onUserClick = { username -> backStack.add(Screen.UserProfile(username)) }
        )
    }
    
    entry<Screen.UserProfile> { key ->
        UserProfileScreen(
            username = key.username,
            onBack = { backStack.removeLastOrNull() },
            onPhotoClick = { photo -> backStack.add(Screen.PhotoDetails(photo.id)) },
            onCollectionClick = { col -> backStack.add(Screen.CollectionDetails(col.id)) }
        )
    }
}
```

### Step 3: Setting Up the State-Saveable Back Stack
In `MainActivity.kt`, replace the legacy controller declaration with `rememberNavBackStack`. This preserves the stack structure across system process recreation:

```kotlin
import androidx.navigation3.runtime.rememberNavBackStack

val backStack = rememberNavBackStack(startDestination = Screen.Feed)
val currentScreen = backStack.lastOrNull() ?: Screen.Feed
```

### Step 4: Refactoring MainActivity UI Setup
Incorporate `NavDisplay` and link the bottom navigation state selection explicitly to the current active key:

```kotlin
val showBottomBar = currentScreen in listOf(Screen.Feed, Screen.Collections, Screen.Search)

Scaffold(
    bottomBar = {
        if (showBottomBar) {
            NavigationBar(
                containerColor = Color(0xFF0F0F11),
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = currentScreen is Screen.Feed,
                    onClick = {
                        if (currentScreen !is Screen.Feed) {
                            backStack.clear()
                            backStack.add(Screen.Feed)
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Photos") },
                    label = { Text("Photos") }
                )
                
                NavigationBarItem(
                    selected = currentScreen is Screen.Collections,
                    onClick = {
                        if (currentScreen !is Screen.Collections) {
                            backStack.removeIf { it !is Screen.Feed }
                            backStack.add(Screen.Collections)
                        }
                    },
                    icon = { Icon(Icons.Default.List, contentDescription = "Collections") },
                    label = { Text("Collections") }
                )
                
                NavigationBarItem(
                    selected = currentScreen is Screen.Search,
                    onClick = {
                        if (currentScreen !is Screen.Search) {
                            backStack.removeIf { it !is Screen.Feed }
                            backStack.add(Screen.Search)
                        }
                    },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") }
                )
            }
        }
    }
) { innerPadding ->
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = Modifier.padding(innerPadding),
        entryProvider = entryProvider,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator() // Correctly scopes ViewModels to screen lifecycles
        ),
        transitionSpec = { slideInHorizontally() togetherWith slideOutHorizontally() },
        popTransitionSpec = { slideInHorizontally(initialOffsetX = { -it }) togetherWith slideOutHorizontally(targetOffsetX = { it }) }
    )
}
```

---

## 5. Architectural Benefits & Validation

1. **Elimination of Unsafe Argument Casting:** Previously, fetching values like `collectionId` from `backStackEntry.arguments` required nullable string casts prone to runtime crashes. With Navigation 3, the `entry<Screen.CollectionDetails>` DSL directly yields a strongly-typed `key` object containing the `collectionId` as an immutable property.
2. **Simplified Predictive Back Support:** Transitioning back via Android system gestures uses the custom slide behavior configured in `popTransitionSpec` natively.
3. **Decoupled ViewModel Lifecycles:** Integrating the `rememberViewModelStoreNavEntryDecorator()` ensures each route is backed by a distinct `ViewModelStore`, meaning multiple instances of the same detail screen type (e.g. visiting a user, tapping on an image, and clicking back to another user profile) get distinct state holding boundaries rather than overwriting each other.
4. **Platform Separation Preserved:** iOS remains completely unaffected. Android UI is more robust, less complex, and matches modern Jetpack guidelines.
