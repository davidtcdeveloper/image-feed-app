# Implementation Steps & Technical Reasoning

This document outlines the detailed steps to execute the project implementation, along with the reasoning behind the architectural and tech stack decisions.

---

## Technical Reasoning

### 1. BuildKonfig for Secure API Key Management
*   **Decision:** Read the API Access Key from a local file (`local.properties`) and inject it using the `BuildKonfig` gradle plugin.
*   **Reasoning:** Placing API keys directly in source code is a major security risk, leading to accidental commits to public repositories. By loading it via a gradle properties/properties file during compilation, we can exclude `local.properties` from Git (`.gitignore`). `BuildKonfig` automatically generates Kotlin code containing the key in the shared module (`BuildKonfig.UNSPLASH_API_KEY`), which is fully accessible to both Android and iOS targets natively.

### 2. Shared Presentation (Presenter/StateHolder Pattern)
*   **Decision:** Implement the state machine and pagination logic in the KMP `shared` module using Kotlin `Flow` and `StateFlow`.
*   **Reasoning:** Having duplicate logic for pagination, offset calculation, network retries, and loading states on Android and iOS often leads to inconsistent behavior and double the maintenance cost. By managing the state inside Kotlin:
    *   State flows from Ktor -> Repository -> Presenter -> UI.
    *   The UI layers remain strictly visual (declarative shells) rendering the state.
    *   iOS 17's Swift 5.9 `@Observable` can observe the Kotlin Flow stream via a lightweight adapter class. This combines the benefit of shared logic with the native UI-binding syntax in Swift.

### 3. Dynamic Image Dimensions
*   **Decision:** Pass the UI container width to the image loader (Coil on Android, Kingfisher on iOS) and dynamically modify the Unsplash image request parameters (`w` parameter).
*   **Reasoning:** Unsplash photos are high resolution (several megapixels). Downloading full-size photos on a mobile screen causes high memory usage, laggy scrolling, and huge cellular data consumption. Appending `&w=X` (where X is the screen or card width in pixels) and `&q=80&auto=format` instructs the Unsplash/Imgix CDN to downscale and optimize the image on-the-fly. This delivers fast, fluid image loads while fully complying with Unsplash's hotlinking terms.

---

## Detailed Step-by-Step Execution Plan

### Step 1: KMP Project Setup & Initial Gradle Config
1.  Initialize the root project structure with:
    *   `build.gradle.kts` (root script targeting kotlin-multiplatform and android application plugins)
    *   `settings.gradle.kts` (defining shared, androidApp, and iosApp modules)
    *   `gradle.properties` (defining Kotlin, Gradle, and dependency versions)
    *   `local.properties` (with a placeholder key: `unsplash.api.key=YOUR_ACCESS_KEY_HERE`)
2.  Add `.gitignore` to exclude `.gradle`, `.idea`, `build/`, `local.properties`, Xcode user settings, and build outputs.

### Step 2: Shared Module Configuration (`shared`)
1.  Configure `shared/build.gradle.kts` with target declarations:
    *   `androidTarget()`
    *   `iosArm64()`, `iosSimulatorArm64()` (iOS simulator & device support)
2.  Configure dependencies:
    *   Ktor Client (Core, Android, Darwin engines)
    *   Kotlinx Serialization (JSON compiler plugin)
    *   Kotlinx Coroutines
    *   BuildKonfig plugin:
        ```kotlin
        buildkonfig {
            packageName = "com.example.imagefeed"
            defaultConfigs {
                buildConfigField("UNSPLASH_API_KEY", findProperty("unsplash.api.key") as? String ?: "")
            }
        }
        ```

### Step 3: Network Data Source & Models
1.  Implement target serializable models:
    *   `Photo` (contains `id`, `width`, `height`, `description`, `blur_hash`, `urls`, `user`)
    *   `PhotoUrls` (contains `raw`, `full`, `regular`, `small`, `thumb`)
    *   `User` (contains `name`, `username`, `profile_image`)
2.  Implement `UnsplashApiClient` in `commonMain`:
    *   Use Ktor to construct a client instance with JSON deserialization.
    *   Append headers: `Authorization: Client-ID ...` and `Accept-Version: v1`.
    *   Add a method `fetchEditorialFeed(page: Int, perPage: Int): List<Photo>`.

### Step 4: Shared Presenter (`FeedPresenter`)
1.  Define the `UnsplashRepository` interface and its implementation.
2.  Create `FeedPresenter` in `commonMain`:
    *   Keep track of the current page (start at 1).
    *   Expose `StateFlow<FeedState>`.
    *   Method `refresh()`: Reset page to 1, clear existing items, and fetch first page.
    *   Method `loadNextPage()`: Fetch the next page if not already loading. Append photos to the state list.
    *   Gracefully catch network errors and emit error message states.

### Step 5: Android Client (`androidApp`)
1.  Configure `androidApp/build.gradle.kts` to target SDK 34 and include Jetpack Compose dependencies.
2.  Implement DI module for Koin in Android (providing `FeedPresenter` and platform configurations).
3.  Create the feed UI using Compose:
    *   Use `LazyVerticalStaggeredGrid` for a dynamic layout.
    *   Add `PullToRefreshBox` for feed refreshment.
    *   Use Coil to load images, passing `photo.urls.raw + "&w=" + calculatedWidth` and rendering a custom BlurHash placeholder.

### Step 6: iOS Client (`iosApp`)
1.  Open Xcode workspace or set up the Swift package definition in `iosApp`.
2.  Implement the Kotlin-to-Swift Flow observer class:
    *   Subscribe to `FeedPresenter.state` flow within an async task inside a Swift Class conforming to `@Observable` / `ObservableObject`.
3.  Create the SwiftUI layout:
    *   Design a two-column staggered grid (placing alternating elements in left/right stacks to simulate a waterfall layout).
    *   Integrate Kingfisher to retrieve images dynamically with custom size constraints.
    *   Add native iOS 17 pull-to-refresh mechanics.

### Step 7: Gradle Version Catalog Migration
1. Create `gradle/libs.versions.toml` to list all plugins, versions, and libraries.
2. Update the root `build.gradle.kts` file to declare plugins using the `alias` method.
3. Update `shared/build.gradle.kts` to reference versions and libraries from `libs.versions.toml`.
4. Update `androidApp/build.gradle.kts` to reference versions and libraries from `libs.versions.toml`.
5. Run `./gradlew tasks` or equivalent to verify dependencies compile successfully.

### Step 8: Gradle and Tooling Upgrade
1. Evaluate and prepare requirements for Gradle 9.x / AGP 9.x and Kotlin 2.4.x.
2. Upgrade the Gradle wrapper distribution URL.
3. Update standard Kotlin and AGP dependency definitions in `gradle/libs.versions.toml`.
4. Adjust Kotlin DSL build logic inside module-level scripts to ensure full compatibility.

### Step 9: Ecosystem Dependency Upgrade
1. Review all other core platform dependencies for new stable versions.
2. Formulate comprehensive coordinate migration plans for breaking library releases (Ktor 3.x, Koin 4.x, and Coil 3.x).



