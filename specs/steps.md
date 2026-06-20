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

## Current Specification Inventory

The `specs/` folder currently contains the following implementation and design documents:

*   `specs/01_photo_details_and_statistics.md`
*   `specs/02_advanced_unified_search.md`
*   `specs/03_collections_and_curation.md`
*   `specs/04_topics_and_categories.md`
*   `specs/05_photographer_profiles_and_insights.md`
*   `specs/06_adaptive_layout_tablets.md`
*   `specs/07_gradle_version_catalog_migration.md`
*   `specs/08_gradle_and_tooling_upgrade.md`
*   `specs/09_dependency_updates_plan.md`
*   `specs/10_navigation_3_upgrade_spec.md`
*   `specs/11_macos_entry_point_integration.md`
*   `specs/12_photo_detail_description_fix.md`
*   `specs/13_macos_back_button_duplication_fix.md`
*   `specs/14_dependency_updates.md`
*   `specs/15_gradle_dsl_modernization.md`
*   `specs/17_build_warnings_resolution.md`
*   `specs/18_linting_tooling_plan.md`
*   `specs/19_koin_to_metro_migration_plan.md`
*   `specs/20_testing_strategy.md`
*   `specs/21_android_fluid_animations_plan.md`
*   `specs/21_ios_fluid_animations_plan.md`
*   `specs/implementation_plan.md`
*   `specs/steps.md`

These are the spec files that the implementation notes and planning references should align with.

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

### Step 10: Launch Icon Implementation
1. Design a custom launcher icon representing visual discovery and photography (stylized camera aperture over a vibrant abstract landscape).
2. Implement an Android Adaptwive Icon:
   * Define XML vector drawables for foreground and background layers.
   * Update `AndroidManifest.xml` to reference the new adaptive launcher icon.
3. Implement iOS and macOS Launch Icons:
   * Generate high-resolution icon assets (AppIcon.appiconset).
   * Configure Xcode resource catalog to set the app icon.

### Step 11: macOS Entry Point Integration & iOS UI Reuse
1.  **Configure Multiplatform targets in Gradle:**
    *   Update `shared/build.gradle.kts` to register native targets: `macosX64()` and `macosArm64()`.
    *   Introduce an intermediate `appleMain` source set that both `iosMain` and `macosMain` depend on. Move Darwin-specific code (Ktor client engine, Darwin serializers) to `appleMain` to prevent duplication.
2.  **Define the macOS Target in XcodeGen:**
    *   Edit `iosApp/project.yml` to define a new target `macosApp` with `type: application`, `platform: macOS`, and `deploymentTarget: "14.0"`.
    *   Point `sources` to the existing `iosApp` folder so the same codebase is compiled for macOS.
    *   Verify SPM configuration includes Kingfisher support for the macOS target.
3.  **Refactor UIKit / iOS dependencies:**
    *   Replace hardcoded references to `UIScreen.main.bounds` in image loaders with a responsive `GeometryReader` container or frame property injectors.
    *   Replace iOS-specific `RoundedCorner` drawing logic using `UIBezierPath` and `UIRectCorner` with native SwiftUI `UnevenRoundedRectangle` or simple `.clipShape(RoundedRectangle)`.
    *   Wrap touch-specific elements (such as `UIImpactFeedbackGenerator` haptics) and iOS-specific shake notification overlays with `#if os(iOS)` / `#if os(macOS)` compiler conditions.
4.  **Implement Desktop Navigation & Container Shells:**
    *   Introduce a macOS-appropriate root shell (`MacOSContentView`) using a sidebar navigation schema (`NavigationSplitView`) to select between "Photos" and "Collections".
    *   Embed the exact same `PhotosFeedTabView` and `CollectionsFeedTabView` views within the master-detail segments.
5.  **Enable Alternative Shuffle (Shake) Triggers on Desktop:**
    *   Add key bindings (`.keyboardShortcut("s", modifiers: .command)`) and a visible "Shuffle" toolbar icon as desktop alternatives to the physical device shake gesture.
6.  **Verify compilation & lifecycle integration:**
    *   Run `xcodegen` and open the generated Xcode project.
    *   Build and run the `macosApp` target to verify KMP framework linking, Koin DI initiation, image downloading, and adaptive grid scaling.

### Step 12: Photo Detail Description Fix
1.  **Document the Swift/KMP binding mismatch:**
    *   The shared Kotlin model exposes the Unsplash caption as `description`, but the generated Swift binding exposes it as `description_` to avoid colliding with Swift's `description` API.
    *   The iOS/macOS view must use `photo.description_ ?? photo.altDescription` instead of `photo.description`.
2.  **Implement the fix in the shared Apple UI path:**
    *   Update `iosApp/iosApp/PhotoDetailsView.swift` so the detail screen renders the real Unsplash caption text rather than the Swift object dump.
    *   Keep the existing `altDescription` fallback for cases where the main caption is blank.
3.  **Track the fix in its own spec:**
    *   The implementation notes for this change live in `specs/12_photo_detail_description_fix.md`.

### Step 13: macOS Back Button Duplication Fix
1.  **Document the duplicated return-arrow issue on macOS:**
    *   The custom toolbar back button is intentionally rendered in the shared iOS/macOS detail screens, but the native system back affordance was still visible on macOS because the hide logic was scoped to iOS only.
    *   This produced two arrows stacked on top of each other in the top navigation area.
2.  **Apply the navigation fix in the shared Apple UI path:**
    *   Update `iosApp/iosApp/PhotoDetailsView.swift`, `iosApp/iosApp/CollectionDetailView.swift`, and `iosApp/iosApp/UserProfileView.swift` so the system back button is hidden on all platforms.
    *   Keep the custom toolbar button as the single return control.
3.  **Track the fix in its own spec:**
    *   The implementation notes for this change live in `specs/13_macos_back_button_duplication_fix.md`.

### Step 14: Dependency Updates & Modernization
1.  **Update Version Catalog:**
    *   Modify `gradle/libs.versions.toml` to use the latest versions for Ktor (3.5.0), Koin (4.2.1), Coil (3.4.0), and other core libraries.
2.  **Ktor 3 Migration:**
    *   Update network client configuration in `shared/commonMain` to comply with Ktor 3 APIs.
3.  **Coil 3 Integration:**
    *   Switch from Coil 2 to Coil 3 in `androidApp` and potentially share image loading logic if moving towards a full KMP Compose approach.
4.  **iOS/macOS Kingfisher Removal:**
    *   Remove Kingfisher from SPM in `iosApp/project.yml` and revert `KFImage.swift` to use SwiftUI's native `AsyncImage` for a zero-dependency, SPM-free setup.
5.  **Verification:**
    *   Validate the build across all platforms (Android, iOS, macOS) and ensure network requests and image rendering remain functional.

### Step 15: Gradle & KMP Modernization (AGP 9.0+)
1.  **Fully Activate AGP 9.0+ Features:**
    *   Set `android.newDsl=true` and `android.builtInKotlin=true` in `gradle.properties`.
2.  **Migrate Android App:**
    *   Remove `org.jetbrains.kotlin.android` plugin; rely on AGP's built-in Kotlin support.
3.  **Migrate Shared Library:**
    *   Replace `com.android.library` with `com.android.kotlin.multiplatform.library`.
    *   Migrate from `androidTarget()` to the `android { ... }` block inside `kotlin { ... }`.
4.  **Target Optimization:**
    *   Focus exclusively on Apple Silicon (`macosArm64()`, `iosArm64()`, `iosSimulatorArm64()`) and remove Intel-based `x64` targets to eliminate architecture mismatch warnings.
5.  **Verification:**
    *   Run `./gradlew clean help` to verify sync and initial build logic.

### Step 16: Clickable Photo Details Labels / Tags Navigation
1.  **Android: Update `Screen.Search` parameterization:**
    *   Change `Screen.Search` from a `data object` to a `data class Screen.Search(val query: String = "")`.
    *   Update references to `Screen.Search` in tab clicks to `Screen.Search()`.
    *   Add an `onTagClick: (String) -> Unit` callback parameter to `PhotoDetailsScreen` and wire it to add a new `Screen.Search(query = tag)` destination to the backstack.
    *   Add an `initialQuery: String` parameter to `SearchScreen` and trigger `presenter.updateQuery(initialQuery)` using `LaunchedEffect(initialQuery)`.
2.  **iOS/macOS: Add query arguments to `SearchViewModel` & `SearchView`:**
    *   Add `initialQuery` parameter to `SearchViewModel` initializer to immediately perform a search.
    *   Update `SearchView` to receive `initialQuery` and initialize `searchText` and `viewModel` with it to synchronize the search bar and the presenter states.
    *   Add `onTagSelect: (String) -> Void` to `PhotoDetailsView` and modify tag labels to be interactive button elements that invoke this callback.
    *   Update navigation destinations in `ContentView` to map the `.search` path type using `item.id` as the query parameters.
3.  **Verification:**
    *   Build and run Android, iOS, and macOS applications. Click tags on various photo details screen and verify automatic query navigation and results display.

### Step 17: Build Warnings & Deprecations Resolution (Completed)
1.  **Android deprecation refactoring:**
    *   Migrate `centerAlignedTopAppBarColors` to `topAppBarColors`.
    *   Migrate `Icons.Default.List` and `Icons.Default.ArrowForward` to auto-mirrored versions.
    *   Migrate `ScrollableTabRow` and `TabRow` to `SecondaryScrollableTabRow` and `SecondaryTabRow`, utilizing modern `tabIndicatorOffset` calls.
    *   Migrate haptic feedback logic to use the modern `VibratorManager` API on S+ and `VibrationEffect` on O+.
2.  **iOS/macOS target config updates:**
    *   Exclude `x86_64` targets from Xcode simulator and macOS schemes in `project.yml`.
    *   Remove unassigned child warning from `AppIcon.appiconset/Contents.json`.
    *   Silence build script phase warnings by enabling `alwaysRun: true` in `project.yml`.
3.  **Verification:**
    *   Run `xcodegen` and clean build Android, iOS, and macOS to verify all warnings have been cleared.

### Step 18: Linting & Static Analysis Toolchain Setup
1.  **Establish the baseline:**
    *   Add ktlint, detekt, SwiftLint, and SwiftFormat configuration files without immediately turning every rule into a hard failure.
    *   Run the tools in report-only mode to capture the current violation baseline for Kotlin and Swift.
2.  **Analyze pre-existing issues:**
    *   Classify violations into auto-fixable, low-risk, and high-risk categories before changing code.
    *   Fix the obvious, safe issues first.
    *   Do not lower rule severity or relax the rules to make existing issues pass; the project quality bar must remain strict.
3.  **Enforce the toolchain progressively:**
    *   Enable Kotlin style and static-analysis rules in Gradle and wire them into CI.
    *   Enable SwiftLint/SwiftFormat for Apple targets and make local developer commands available.
    *   Update `AGENTS.md` so agents run the lint/static-analysis tools after changes and fix any reported issues before marking work done.
4.  **Verify consistency:**
    *   Re-run the toolchain after remediation to confirm the project now produces stable, consistent lint feedback for both Kotlin and Swift.

### Step 19: Transitioning from Koin to Metro Dependency Injection
1.  **Gradle & Catalogs Integration:**
    *   Add Metro dependency `dev.zacsweers.metro` (version `1.2.1`) to `gradle/libs.versions.toml`.
    *   Apply the Metro plugin in root `build.gradle.kts` and `shared/build.gradle.kts`.
2.  **Core Scopes & Root Dependency Graph:**
    *   Define `@Scope annotation class AppScope` inside the shared module.
    *   Create `NetworkModule` providing `HttpClient`, and bind interfaces via `@ContributesBinding`.
    *   Define `@DependencyGraph` interface representing the complete application dependencies.
3.  **Refactor Presenters for Constructor & Assisted Injection:**
    *   Migrate un-parameterized presenters (`FeedPresenter`, `RandomPhotoPresenter`) to `@Inject constructor(...)`.
    *   Migrate parameterized presenters (`PhotoDetailsPresenter`, `CollectionDetailPresenter`, `UserProfilePresenter`) to use `@Inject` and `@Assisted` constructor parameters alongside respective `@AssistedFactory` interfaces.
4.  **Establish Coexistence Bridge & Expose to Platforms:**
    *   Implement `MetroHelper` in KMP and delegate Koin's `commonModule` dependencies to the generated Metro Graph (`MetroHelper.graph`).
    *   Gradually shift both Android (`MainActivity`) and iOS views/ViewModels to obtain instances directly from `MetroHelper`, replacing Koin's `by inject()`.
5.  **Remove Koin & Set Diagnostics:**
    *   Remove Koin initialization block from both `ImageFeedApplication.kt` and `iOSApp.swift`.
    *   Remove all Koin dependencies and libraries from Gradle version catalogs.
    *   Set `desugaredProviderSeverity = "ERROR"` in `metro` configuration to enforce strict static checking.

### Step 20: Shared Integration Coverage for High-Value App Flows
1.  **Establish the baseline:**
    *   Add `shared/src/commonTest` coverage for the shared presenter layer, since it is the main integration seam between repository data and app behavior.
2.  **Test user-visible flows, not internals:**
    *   Validate initial feed loading, refresh behavior, and topic selection through the real presenter state flow and a fake repository.
    *   Avoid assertions about private implementation details, helper calls, or internal state transitions.
3.  **Keep the suite package-oriented:**
    *   Add future tests for search, photo detail loading, and collection browsing in the same shared integration style rather than one-off class probes.
4.  **Verify locally and in CI:**
    *   Run `./gradlew :shared:allTests` as the standard quality gate for shared behavior changes.

### Step 21: Google Maps Android Integration Plan
1.  **Define Gradle Configuration:**
    *   Add Google Play Services Maps (`play-services-maps`) and Google Maps Compose wrapper (`maps-compose`) to `libs.versions.toml`.
    *   Declare dependencies in `androidApp/build.gradle.kts`.
2.  **Ensure Secure API Key Loading:**
    *   Configure `androidApp/build.gradle.kts` to load `google.maps.api.key` from local git-ignored properties.
    *   Expose it using Gradle `manifestPlaceholders`.
3.  **Configure Android Manifest:**
    *   Add the meta-data element referencing the placeholder into `AndroidManifest.xml`.
4.  **Implement Inline Map UI:**
    *   Refactor `PhotoDetailsScreen.kt` to load a `GoogleMap` element showing the captured image coordinates inline.
    *   Configure dark-styled styling properties to maintain dark mode consistency.
5.  **Verify compilation & navigation:**
    *   Clean build the Android app and verify the layout and map launching mechanics.

### Step 22: Fluid Animations Implementation (Completed)
1.  **Android Jetpack Compose Motion (Completed):**
    *   Configure `SharedTransitionLayout` wrapping the navigation container.
    *   Implement staggered grid entrance effects and bounce-scale clickable interactions.
    *   Design and deploy linear-gradient shimmering skeleton placeholders.
    *   Reference the detailed blueprint in `specs/21_android_fluid_animations_plan.md`.
2.  **iOS SwiftUI Fluid Transitions (Completed):**
    *   Implement overlay-based hero transitions using `matchedGeometryEffect` to zoom grid cards seamlessly.
    *   Build staggered bottom-up transitions for loaded feed grids using index-delayed spring curves.
    *   Develop springy press-scale feedbacks on interactive card buttons.
    *   Replace standard activity views with shimmering layout outlines in `ContentView`, `CollectionsFeedView`, and `UserProfileView`.
    *   Reference the detailed blueprint in `specs/21_ios_fluid_animations_plan.md`.

