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

### 4. Presenter Coroutine Lifecycle Hardening
*   **Decision:** Keep the shared presenter architecture but make each presenter own a lifecycle-aware scope and cancel work in `clear()`.
*   **Reasoning:** The shared presenters should remain the integration layer between KMP state logic and platform UIs, but they need explicit teardown boundaries to avoid state emissions after the UI has moved on. We use a shared `PresenterScope` plus `CoroutineContext.isActive()` and `MutableStateFlow.updateIfActive(...)` helpers so presenters can guard state updates without repeating boilerplate checks around every emission.

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
*   `specs/16_clickable_image_labels.md`
*   `specs/17_build_warnings_resolution.md`
*   `specs/18_linting_tooling_plan.md`
*   `specs/19_koin_to_metro_migration_plan.md`
*   `specs/20_testing_strategy.md`
*   `specs/21_google_maps_android_integration.md`
*   `specs/21_android_fluid_animations_plan.md`
*   `specs/21_ios_fluid_animations_plan.md`
*   `specs/22_coroutine_lifecycle_scope_management.md`
*   `specs/23_adaptive_layout_tablets_foldables.md`
*   `specs/24_material_3_design_foundation.md`
*   `specs/25_material_3_screen_tokenization.md`
*   `specs/26_material_3_ux_modernization.md`
*   `specs/27_ios_glass_design_foundation.md`
*   `specs/28_ios_chrome_navigation_modernization.md`
*   `specs/29_ios_cards_attribution_glass_redesign.md`
*   `specs/30_ios_interactive_sheet_and_sensory_experience.md`
*   `specs/31_readme_and_navigation_documentation.md`
*   `specs/32_agent_rules_modernization.md`
*   `specs/33_cross_platform_parity_and_token_hardening.md`
*   `specs/34_agent_tooling_and_verification_rules.md`
*   `specs/35_android_navigation3_lifecycle_and_transition_scope.md`
*   `specs/36_android_navigation3_multiple_backstacks_and_navigator.md`
*   `specs/37_android_navigation3_predictive_back_and_motion.md`
*   `specs/38_android_navigation3_adaptive_list_detail_scenes.md`
*   `specs/39_skills_and_rules_migration.md`
*   `specs/40_living_code_references_and_anti_staleness_rules.md`
*   `specs/41_readme_and_agents_parity_alignment.md`
*   `specs/42_mvi_foundation_and_transition_guidelines.md`
*   `specs/43_mvi_pilot_photo_details_migration.md`
*   `specs/44_mvi_core_screens_migration.md`
*   `specs/45_mvi_architecture_cleanup_and_finalization.md`
*   `specs/46_modularization_foundation_core_extraction.md`
*   `specs/47_modularization_pilot_feature_details.md`
*   `specs/48_modularization_remaining_features_extraction.md`
*   `specs/49_modularization_umbrella_and_linking_optimization.md`
*   `specs/50_image_pipeline_and_blurhash_caching.md`
*   `specs/51_lazy_grid_motion_and_scroll_optimization.md`
*   `specs/52_macos_title_and_ios_sheet_navigation_fixes.md`
*   `specs/53_android_material3_expressive_core_friction_fixes.md`
*   `specs/54_android_material3_expressive_photo_details_and_dynamic_theming.md`
*   `specs/55_android_material3_expressive_floating_navigation_and_bento_discovery.md`
*   `specs/56_ios_glass_contrast_and_navigation_chrome_fixes.md`
*   `specs/57_ios_immersive_photo_detail_and_interactive_sparkline.md`
*   `specs/58_ios_floating_glass_capsule_and_profile_refinements.md`
*   `specs/59_cross_platform_localization_architecture.md`
*   `specs/60_android_shimmer_skeleton_rendering_optimization.md`
*   `specs/61_android_media_card_hot_path_optimization.md`
*   `specs/62_android_scroll_chrome_and_horizontal_row_hygiene.md`
*   `specs/63_android_photo_details_and_chart_rendering_optimization.md`
*   `specs/64_android_release_optimization_and_baseline_profiles.md`
*   `specs/65_memory_optimization_phase1_caches_grids_and_scopes.md`
*   `specs/66_memory_optimization_phase2_maps_thumbnails_and_blurhash.md`
*   `specs/67_memory_optimization_phase3_network_navigation_di_and_ui_churn.md`
*   `specs/68_ios_swiftui_performance_phase1_algorithmic_and_layout_architecture.md`
*   `specs/69_ios_swiftui_performance_phase2_scroll_dynamics_and_state_isolation.md`
*   `specs/70_ios_swiftui_performance_phase3_gpu_rendering_and_image_pipeline.md`
*   `specs/researches/modularization_evaluation.md`
*   `specs/researches/swiftui_performance_optimization_plan.md`
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

### Step 23: Coroutine Lifecycle & Metro DI Hardening
1.  **Audit current presenter scopes:**
    *   Review every shared presenter for self-created `CoroutineScope` instances and identify which ones are retained by Metro graph singletons.
2.  **Introduce shared lifecycle abstractions:**
    *   Add a `DispatcherProvider` plus a presenter cleanup contract in `shared/commonMain` so coroutines can be cancelled deterministically.
3.  **Refactor Metro bindings for screen-scoped instances:**
    *   Replace app-scoped presenter singletons with factory-based creation for feed, search, collections, random-photo, and detail/profile flows.
4.  **Wire cleanup into Android and iOS layers:**
    *   Update platform wrappers to create presenters per screen and invoke cleanup when the UI lifecycle ends.
5.  **Verify cancellation behavior:**
    *   Add shared tests with a test dispatcher to confirm that in-flight work is cancelled and state emissions stop once the presenter is cleared.
    *   Track the implementation plan in `specs/22_coroutine_lifecycle_scope_management.md`.

### Step 24: July 2026 Dependency Assessment & Update Plan
1.  **Dependency Audit:**
    *   Compare currently declared versions catalog against latest stable versions in July 2026.
2.  **Define Mitigation & Integration Risks:**
    *   Assess impact of upgrading Gradle 9.6.1, AGP 9.3.0, Ktor 3.5.1, and Google Maps dependencies.
3.  **Reference Specification:**
    *   Track the assessment table and step-by-step update plan in `specs/14_dependency_updates.md`.

### Step 25: Android Tablets & Foldables Adaptive Layout
1.  **Define Window Size & Posture Infrastructure:**
    *   Introduce `AdaptiveLayoutInfo` observing `WindowWidthSizeClass` (Compact, Medium, Expanded) and Jetpack WindowManager `FoldingFeature` (Book, TableTop, Flat).
    *   Add responsive grid column calculation utilities for Photos, Collections, and Search User/Collection items in `AdaptiveUtils.kt`.
    *   Fix CDN image request resolution calculation using container/column width instead of hardcoding `screenWidthDp / 2`.
2.  **Modernize Navigation Shell in `MainActivity`:**
    *   Provide start-docked `NavigationRail` for Medium and Expanded screens ($\ge 600\text{dp}$), replacing the bottom `NavigationBar`.
    *   Retain bottom `NavigationBar` on Compact screens (< 600dp).
3.  **Modernize Collections, Search, and Profile Grids:**
    *   Migrate `CollectionsFeedScreen` from `LazyColumn` to adaptive multi-column grid (`calculateCollectionGridColumns`).
    *   Update `SearchScreen` and `UserProfileScreen` to eliminate 1-column hardcoding on wide displays.
4.  **Implement Responsive Dual-Pane Photo Details:**
    *   Side-by-side Dual-Pane layout on Medium/Expanded widths and Book posture (Left: Photo Canvas; Right: Inspector sidebar with stats, map, EXIF, and tags).
    *   Horizontal split in TableTop posture (Top: Photo; Bottom: Controls & metadata deck).
    *   Hinge bounds avoidance to prevent crease bisecting.
5.  **Reference Specification:**
    *   Track the detailed layout blueprint and checklist in `specs/23_adaptive_layout_tablets_foldables.md`.

### Step 26: Material 3 Design Foundation (Completed)
1.  **Introduce Theme Architecture Package:**
    *   Create package `com.example.imagefeed.android.theme` containing `Color.kt`, `Type.kt`, `Shape.kt`, and `Theme.kt`.
    *   Define full Material 3 tonal palettes for Dark and Light modes including multi-tiered surface containers (`surfaceContainerLowest` through `surfaceContainerHighest`).
    *   Configure `Typography` using official Material 3 typography scale tokens (`display*`, `headline*`, `title*`, `body*`, `label*`).
    *   Configure `Shapes` scale (`extraSmall` to `extraLarge`).
2.  **Enable Dynamic Color & System Theme Switching:**
    *   Implement `ImageFeedTheme` composable with dynamic color support for Android 12+ (`dynamicDarkColorScheme`/`dynamicLightColorScheme`) and `isSystemInDarkTheme()` resolution.
3.  **Root Activity Setup:**
    *   Call `enableEdgeToEdge()` in `MainActivity.onCreate()` for transparent system bars and consistent status/navigation bar insets.
    *   Replace inline `MaterialTheme` in `MainActivity.kt` with `ImageFeedTheme`.
4.  **Reference Specification:**
    *   Track blueprint in `specs/24_material_3_design_foundation.md`.

### Step 27: Material 3 Screen Tokenization (Completed)
1.  **Refactor Design Tokens Across Screens:**
    *   Systematically eliminate hardcoded hex colors (`Color(0xFF0F0F11)`, `Color(0xFF1E1E24)`, `Color(0xFF2C2C35)`) and ad-hoc `fontSize` across `MainActivity.kt`, `SearchScreen.kt`, `PhotoDetailsScreen.kt`, `UserProfileScreen.kt`, `CollectionsFeedScreen.kt`, and `CollectionDetailScreen.kt`.
    *   Bind cards and surfaces to semantic tokens (`MaterialTheme.colorScheme.surfaceContainer`, etc.).
2.  **Standardize Typography & Shapes:**
    *   Replace hardcoded `fontSize = ...sp` with `MaterialTheme.typography.*` to support system dynamic font scaling across all 6 screens.
    *   Replace ad-hoc `RoundedCornerShape(...)` with `MaterialTheme.shapes.*`.
    *   Standardize photographer attribution names to `MaterialTheme.typography.titleSmall`.
3.  **Standardize Component Variants & Scrim Policies:**
    *   Replace custom tag boxes in `PhotoDetailsScreen.kt` with Material 3 `SuggestionChip`.
    *   Align filter chips in `SearchScreen.kt` to standard outlined M3 `FilterChipDefaults.filterChipColors()`.
    *   Establish explicit on-media scrim typography policy (`Color.White` primary / `Color.White.copy(alpha = 0.8f)` secondary) and domain color swatch exemption.
    *   Standardize TopAppBars and cards to ensure full light/dark theme contrast.
4.  **Reference Specification:**
    *   Track blueprint in `specs/25_material_3_screen_tokenization.md`.

### Step 28: Material 3 UX Modernization (Completed)
1.  **Implement Modern SearchBar (Single Source of Truth):**
    *   Extend `SearchState` and `UnifiedSearchPresenter` with `isSearchActive: Boolean = false`, avoiding local `remember` variables in Compose.
    *   Replace ad-hoc `TextField` in `SearchScreen.kt` with Material 3 `SearchBar` on compact screens and `DockedSearchBar` on medium/expanded screens.
    *   Bind `BackHandler(enabled = state.isSearchActive)` to collapse search before navigation pops.
2.  **Implement Native Pull-to-Refresh:**
    *   Add `fun refresh()` and `isRefreshing` to `UnifiedSearchPresenter`.
    *   Add Material 3 `PullToRefreshBox` with `PullToRefreshDefaults` on Feed (`MainActivity`), Collections Feed, and Search Results.
3.  **Configure App Bar Scroll Behaviors:**
    *   Attach `TopAppBarDefaults.pinnedScrollBehavior()` to Feed (`FeedScreen`) to maintain sticky category tabs while elevating with surfaceContainer tint.
    *   Attach `TopAppBarDefaults.enterAlwaysScrollBehavior()` to Collections (`CollectionsFeedScreen`).
4.  **Haptic & Motion Enhancements:**
    *   Add tactile haptic feedback on pull-to-refresh activation, filter toggling, and photo downloads.
    *   Opt-in to predictive back gestures via `android:enableOnBackInvokedCallback="true"` in `AndroidManifest.xml`.
5.  **Reference Specification:**
    *   Track blueprint in `specs/26_material_3_ux_modernization.md`.

### Step 29: iOS Glass Design Foundation & SwiftUI Material System (Completed)
1.  **Implement Reusable Glass Primitives:**
    *   Create `GlassTheme.swift` with `GlassBackgroundModifier`, semantic convenience extensions (`.glassBackground(...)`, `.glassCapsule()`, `.glassCard()`).
    *   Configure specular edge gradients (`LinearGradient` with `0.5pt` stroke) and soft ambient shadows.
2.  **Accessibility & Fallbacks:**
    *   Add `@Environment(\.accessibilityReduceTransparency)` fallback support to degrade gracefully to high-contrast opaque semantic colors when enabled.
3.  **Reference Specification:**
    *   Track blueprint in `specs/27_ios_glass_design_foundation.md`.

### Step 30: iOS Translucent Chrome & Floating Category Bar (Completed)
1.  **Modernize Navigation and Tab Chrome:**
    *   Remove legacy `UITabBarAppearance.configureWithOpaqueBackground()` from `ContentView.swift`.
    *   Apply `.toolbarBackground(.ultraThinMaterial, for: .navigationBar, .tabBar)` with `.toolbarBackground(.visible)` across feed tabs, collections, and search.
2.  **Edge-to-Edge Layout & Floating Frosted Glass Category Bar:**
    *   Promote feed photo `ScrollView` to full-bleed with top content margins so imagery scrolls fluidly under the top chrome.
    *   Elevate the category picker into a floating frosted glass capsule strip backed by `GlassTheme` with edge fade masking and declarative `.sensoryFeedback`.
3.  **Glass Action Controls:**
    *   Refactor toolbar icons and custom back buttons into circular glass controls conforming to `GlassTheme` with specular borders and macOS platform guards.
4.  **Reference Specification:**
    *   Track blueprint in `specs/28_ios_chrome_navigation_modernization.md`.

### Step 31: iOS Cards Attribution & Glass Redesign (Completed)
1.  **Floating Glass Attribution Capsule:**
    *   Replace full-width `LinearGradient` bottom scrim in `PhotoCard` and `CollectionPhotoGridCard` with an inset floating glass attribution capsule using `GlassTheme` with isolated tap routing.
2.  **Glassmorphic Metric & Info Cards:**
    *   Refactor `MetricCard`, `CollectionMosaicCard` badges/curator info, and profile header social chips with `GlassTheme` tokens and Dynamic Type vibrancy.
3.  **Reference Specification:**
    *   Track blueprint in `specs/29_ios_cards_attribution_glass_redesign.md`.

### Step 32: iOS Interactive Inspector Sheet & Sensory Experience (Completed)
1.  **Full-Canvas Image View & Detented Sheet:**
    *   Refactor compact-width `PhotoDetailsView` to maintain full-canvas photo presentation fitting the exact screen width with proportional height (`containerWidth / aspectRatio`), pinch/pan zoom (1.0x-4.0x), and double-tap toggle.
    *   Configure `matchedGeometryEffect` hero geometry source ownership (`PhotoDetailsView` as active source, `PhotoCard` yielding during presentation) to prevent thumbnail-scale shrinkage.
    *   Implement interactive non-modal frosted glass inspector sheet (`.presentationDetents([.fraction(0.35), .fraction(0.70), .large])` and `.presentationBackgroundInteraction(.enabled(upThrough: .fraction(0.70)))`).
    *   Add floating glass action bar with photographer attribution, download trigger, and inspector toggle button.
    *   Ensure sheet dismisses prior to navigation push for tag and profile clicks.
2.  **Contrast & Accessibility Hardening:**
    *   Layer `.regularMaterial` with `Color(hex: "0A0A0C").opacity(0.82)` dark scrim in `.presentationBackground` for $\ge 7:1$ WCAG AAA contrast over bright images.
    *   Anchor sheet in `.preferredColorScheme(.dark)` and `.environment(\.colorScheme, .dark)` to fix Light Mode white-on-white collision.
    *   Replace static white/gray colors across `PhotoInspectorView` and `ExifRowView` with semantic `.glassVibrancy(.primary)` and `.glassVibrancy(.secondary)`.
    *   Add `@Environment(\.accessibilityReduceTransparency)` fallback for sheet background.
    *   Wrap iOS-specific sheet modifiers and size class queries in `#if os(iOS)` guards for macOS compatibility.
3.  **iOS 17 Motion & Sensory Feedback:**
    *   Implement native `.scrollTransition` (scale `0.96`, opacity `0.85`) across feed cards (`PhotoCard`, `CollectionMosaicCard`) and grid views.
    *   Add declarative `.sensoryFeedback` for tab switching, download completion, and random shuffle actions.
4.  **Reference Specification:**
    *   Track blueprint in `specs/30_ios_interactive_sheet_and_sensory_experience.md`.

### Step 33: README Modernization, Feature Catalog & Screen Navigation Architecture (Completed)
1.  **Audit & Modernize Project Documentation:**
    *   Update `README.md` to reflect recent cross-platform developments, including compile-time Metro DI (`@DependencyGraph`), lifecycle-aware `PresenterScope` (`updateIfActive`), Android Jetpack Navigation 3, Material 3 design tokens with dynamic color, and iOS frosted glass foundations (`GlassTheme`).
2.  **Add Structured Features Catalog:**
    *   Document all user-facing capabilities across the six core destinations: Photo Discovery & Topics, Unified Multi-Facet Search, Photo Details & EXIF & Maps, Curated Collections, Photographer Profiles & Insights, and macOS native desktop support.
3.  **Document Screen Navigation Architecture & Hierarchy:**
    *   Diagram the application's navigation flows using Mermaid, visualizing transitions between root shells (NavigationRail, NavigationBar, TabView, SplitView) and primary/detail destinations.
    *   Detail deep cross-screen interaction paths: hero animations, tag searches, attribution taps, related collections, and shake-to-randomize.
4.  **Reference Specification:**
    *   Track blueprint in `specs/31_readme_and_navigation_documentation.md`.

### Step 34: Agent Guidelines & Rule Modernization (Completed)
1.  **Modernize AGENTS.md & ai-rules:**
    *   Replace obsolete Koin references with Metro DI (`dev.zacsweers.metro`, `ApplicationGraph`, `MetroHelper`).
    *   Document Jetpack Navigation 3 (`androidx.navigation3`, `NavBackStack`, `NavDisplay`, `NavKey`) on Android.
    *   Explicitly include `macosApp` as a declarative UI shell target.
    *   Replace `cd` command patterns with non-`cd` commands for XcodeGen/xcodebuild.
    *   Clarify active `.swiftformat` configuration.
    *   Document `.presentationBackgroundInteraction` and high-contrast dark scrims in `apple-design.md`.
2.  **Synchronize XcodeGen Configuration:**
    *   Update `iosApp/project.yml` pre-build scripts for `iosApp` and `macosApp` to include `OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED` shell check, aligning with `project.pbxproj`.
3.  **Reference Specification:**
    *   Track blueprint in `specs/32_agent_rules_modernization.md`.

### Step 35: Cross-Platform Parity, Token Hardening & Navigation Reconciliation (Completed)
1.  **Reconcile Search Navigation:**
    *   Wire in-app `CollectionDetails` navigation on both Android (`SearchScreen.kt` -> `Screen.CollectionDetails`) and iOS (`SearchView.swift` -> `CollectionPathItem`) instead of external browser links.
2.  **Harmonize iOS Search State & Pull-to-Refresh:**
    *   Bind `viewModel.isRefreshing` to `.refreshable` in `SearchView.swift` and connect search query submission to `viewModel.submitSearch`.
3.  **Harden iOS Design Tokens:**
    *   Eliminate remaining raw hex values (`#0F0F11`, `#1E1E24`, `#2C2C35`) and ad-hoc `.font(.system(size: ...))` across `SearchView`, `CollectionsFeedView`, `UserProfileView`, `CollectionDetailView`, and `PhotoDetailsView`.
4.  **Enforce Unsplash Attribution & Compliance Parity:**
    *   Add floating attribution capsules to iOS photo search results and route photographer attribution clicks in `CollectionDetailView` to in-app `UserProfileView`.
5.  **Dynamic CDN Resizing on iOS:**
    *   Implement column-aware item width calculations across all iOS photo grids to match Android's dynamic sizing.
6.  **Standardize Controls & Hero Animations:**
    *   Adopt `GlassToolbarButton` / `GlassTheme` on `PhotoDetailsView` floating buttons and expand `matchedGeometryEffect` where applicable.
7.  **Reference Specification:**
    *   Track blueprint in `specs/33_cross_platform_parity_and_token_hardening.md`.

### Step 36: Agent Tooling, Sandbox Execution & Verification Modernization (Completed)
1.  **Sandbox Execution & Environment Permissions:**
    *   Document the requirement for `BypassSandbox: true` when running host build/test/lint commands (`./gradlew`, `xcodebuild`, `xcodegen`, `swiftlint`, `swiftformat`) in `AGENTS.md` and `ai-rules/build-and-deps.md`.
2.  **Diagnostics & Compiler Warning Resolution:**
    *   Remove nonexistent `analyze_file` tool reference from `AGENTS.md` and replace with deterministic compiler verification sequences across Kotlin, Swift, and static analysis tools.
3.  **Swift Formatting & Baseline Cleanup:**
    *   Document `swiftformat .` as the primary Swift auto-formatting command in `AGENTS.md` and `ai-rules/build-and-deps.md`.
    *   Format existing Swift sources across `iosApp/` to establish a clean zero-violation baseline for `swiftformat --lint .`.
4.  **Dynamic Rule Loading Routing:**
    *   Update `ai-rules/rule-loading.md` to include `build-and-deps.md` for iOS/macOS UI tasks, and add routing paths for build diagnostics, linting, and test authoring.
5.  **Testing Strategy & Scaffolding Standards:**
    *   Expand `ai-rules/testing.md` to document the canonical presenter test harness (`TestPresenterScopeFactory`, `TestDispatcherProvider`, `FakeUnsplashRepository`) and clarify platform test target boundaries.
6.  **Tooling & MCP Integration Guidelines:**
    *   Specify the requirement to run `xcodegen generate --spec iosApp/project.yml` prior to running Xcode CLI or Xcode MCP build tools whenever project files or settings are modified.
7.  **Reference Specification:**
    *   Track blueprint in `specs/34_agent_tooling_and_verification_rules.md`.

### Step 37: Android Navigation 3 Lifecycle & Transition Scope Hardening (Completed)
1.  **Shared Element Hero Transition Scope Fix:**
    *   Pass `sharedTransitionScope = this` directly to `NavDisplay`.
    *   Remove artificial `AnimatedVisibility(visible = true)` wrapping blocks across all 6 entries in `MainActivity.kt`.
    *   Thread `LocalNavAnimatedContentScope.current` as `animatedVisibilityScope` to screen composables (`FeedScreen`, `CollectionsFeedScreen`, `CollectionDetailScreen`, `SearchScreen`, `PhotoDetailsScreen`, `UserProfileScreen`) so hero transitions animate.
2.  **Back-Stack Entry Scoped Presenter Lifecycle:**
    *   Equip `NavDisplay` with `rememberViewModelStoreNavEntryDecorator()` alongside `rememberSaveableStateHolderNavEntryDecorator()`.
    *   Implement Android platform wrapper `PresenterViewModel<P>` and `rememberEntryPresenter` helper.
    *   Migrate screen presenters to entry-scoped ViewModels so that state, search queries, active filters, and scroll offsets persist when covered by detail screens and survive device rotation.
    *   Remove `by lazy` presenter fields and root `DisposableEffect` from `MainActivity.kt`.
3.  **Click Debouncing & Back Handling Hygiene:**
    *   Wrap navigation callbacks with `dropUnlessResumed` to drop rapid concurrent click events.
    *   Replace dead `finish()` logic in `NavDisplay.onBack` with `backStackState.removeLastOrNull()`.
    *   Remove unused legacy `navigation-compose` (Navigation 2) from catalog and Gradle build files.
4.  **Reference Specification:**
    *   Track blueprint in `specs/35_android_navigation3_lifecycle_and_transition_scope.md`.

### Step 38: Android Navigation 3 Multiple Back Stacks & Navigator Architecture (Completed)
1.  **Multiple Back Stacks & Navigation State Holder:**
    *   Implement `rememberAppNavigationState` and `NavigationState` managing independent `NavBackStack<NavKey>` per top-level route (`Feed`, `Collections`, `Search`).
    *   Enforce "Exit Through Home" back behavior (`AppRoute.Feed` remains the root anchor).
2.  **Decoupled Navigator Controller & Reselect Interaction:**
    *   Create `Navigator` with `navigate`, `goBack`, and `onReselect` methods.
    *   Emit `reselectEvents` flow and wire `animateScrollToItem(0)` in `FeedScreen`, `CollectionsFeedScreen`, and `SearchScreen` on tab reselection.
    *   Eliminate `private var backStack` field in `MainActivity` and wire shake detector directly to `navigator`.
3.  **Entry Provider DSL & Consolidated Chrome:**
    *   Consolidate `NavigationRail` and `NavigationBar` item rendering using a shared `TOP_LEVEL_DESTINATIONS` registry.
    *   Refactor the 170-line inline entry provider into modular `EntryProviderScope<NavKey>` extensions (`feedSection`, `collectionsSection`, `searchSection`, `photoDetailsSection`, `userProfileSection`).
4.  **Navigator Unit Test Suite:**
    *   Implement JVM unit tests in `androidApp/src/test` verifying multi-stack isolation, top-level switching, exit-through-home, and reselection events.
5.  **Reference Specification:**
    *   Track blueprint in `specs/36_android_navigation3_multiple_backstacks_and_navigator.md`.

### Step 39: Android Navigation 3 Motion & Predictive Back Navigation (Completed)
1.  **NavigationEvent Migration in SearchScreen:**
    *   Migrate legacy `BackHandler` in `SearchScreen` to modern `NavigationBackHandler` with `rememberNavigationEventState`.
    *   Intercept gesture back to deactivate search without prematurely popping the back stack.
2.  **Material 3 NavDisplay Transition Specifications:**
    *   Configure `transitionSpec` (coordinated slide-and-fade for forward navigation).
    *   Configure `popTransitionSpec` (reverse slide-and-fade for backward navigation).
    *   Configure `predictivePopTransitionSpec` with edge-aware touch tracking.
    *   Add destination-level metadata overrides where modal-style animations are desired.
3.  **Reference Specification:**
    *   Track blueprint in `specs/37_android_navigation3_predictive_back_and_motion.md`.

### Step 40: Android Navigation 3 Adaptive List-Detail Scenes Architecture (Completed)
1.  **Material 3 Adaptive Navigation 3 Integration:**
    *   Integrate `androidx.compose.material3.adaptive:adaptive-navigation3` dependency.
    *   Instantiate `rememberListDetailSceneStrategy` with zero horizontal partition spacing.
2.  **Adaptive Pane Role Modeling:**
    *   Annotate `Feed` and `Collections` as list panes with empty-state detail placeholders.
    *   Annotate `PhotoDetails` and `CollectionDetails` as detail panes.
    *   Coordinate responsive layout density between the navigation container split and `PhotoDetailsScreen`'s internal dual-pane inspector.
3.  **Reference Specification:**
    *   Track blueprint in `specs/38_android_navigation3_adaptive_list_detail_scenes.md`.

### Step 41: Universal Agent Guidelines, Skills & Rules Migration (Completed)
1.  **Skills Packaging & Multi-Harness Discovery:**
    *   Create dedicated workspace skills in `.agents/skills/` using the universal Agent Skills standard (`SKILL.md` with YAML frontmatter):
        - `kmp-architecture/SKILL.md` (shared boundaries, Presenter pattern, Metro DI, coroutine scopes).
        - `verification-and-linting/SKILL.md` (build commands, toolchain verification, linting/formatting runbooks).
        - `kmp-testing/SKILL.md` (shared presenter test scaffolding, fake repositories, testing execution).
        - `android-m3-design/SKILL.md` (Material 3 tokens, Navigation 3, predictive back, adaptive layouts).
        - `ios-glass-design/SKILL.md` (GlassTheme tokens, materials, detented sheets, sensory feedback).
    *   Create root `GEMINI.md` pointer document directing Android Studio / Gemini to load `AGENTS.md`.
    *   Create `skills -> .agents/skills` symlink for OpenCode CLI and root-level scanning harnesses.
2.  **Invariants Consolidation & Context Optimization in `AGENTS.md`:**
    *   Consolidate critical architecture boundaries, security, commit format, and hygiene policies directly into a lean `AGENTS.md` (< 70 lines) to eliminate prompt bloat.
    *   Embed a markdown-linked Skills Catalog in `AGENTS.md` as a universal fallback for harnesses without native dynamic skill discovery.
3.  **Deprecation of `ai-rules/` & Routing Streamlining:**
    *   Remove `ai-rules/rule-loading.md` and completely delete the legacy `ai-rules/` directory to keep the project clean.
    *   Update `AGENTS.md` reference indexes to point to workspace skills and active specifications.
4.  **Reference Specification:**
    *   Track blueprint in `specs/39_skills_and_rules_migration.md`.

### Step 42: Living Code References & Anti-Staleness Rules for Agent Skills (Completed)
1.  **Core Invariant in `AGENTS.md`:**
    *   Add the `Living Code & Anti-Staleness Policy` under `Core Invariants -> Quality & Hygiene Standards`.
    *   Prohibit verbatim boilerplate code templates and multi-line implementation blocks in all model instructions and skills.
    *   Mandate living code references (clickable markdown links) and declarative token/invariant definitions.
    *   Enforce the < 4 lines micro-snippet rule for compiler flags, CLI commands, and formulas.
2.  **Refactoring Existing Skills:**
    *   Remove 40-line `FeedPresenterTest` boilerplate in `kmp-testing/SKILL.md` and replace with a living file link to `shared/src/commonTest/kotlin/com/example/imagefeed/presentation/FeedPresenterTest.kt`.
    *   Remove raw overlay and sheet boilerplate in `ios-glass-design/SKILL.md`, pointing to `iosApp/iosApp/GlassTheme.swift` modifiers instead.
    *   Streamline `kmp-architecture/SKILL.md` to reference shared state extension definitions declaratively.
3.  **Reference Specification:**
    *   Track blueprint in `specs/40_living_code_references_and_anti_staleness_rules.md`.

### Step 43: Documentation Parity & Alignment with AGENTS.md (Completed)
1.  **Dual-Platform Build Commands Synchronization:**
    *   Align `README.md` iOS command-line verification with `AGENTS.md` and `verification-and-linting/SKILL.md` (`xcodegen generate --spec iosApp/project.yml` and `xcodebuild ... CODE_SIGNING_ALLOWED=NO`).
2.  **Swift Formatting Toolchain Parity:**
    *   Add `swiftformat .` to `README.md` Code Quality section to match Kotlin auto-formatting.
3.  **Unsplash API Compliance Hardening:**
    *   Document the Retina display scaling (`UIScreen.main.scale`) requirement in `README.md` Unsplash compliance guidelines.
4.  **Living Code References in Architecture & Navigation:**
    *   Update Android Navigation 3 route references from `Screen.*` to `AppRoute.*`.
    *   Correct iOS navigation description to `FeedPathItem` and `CollectionPathItem` structs (wrapping `ItemType` enums).
    *   Expand presenter architecture list to include `CollectionsFeedPresenter` and `RandomPhotoPresenter`.

### Step 44: Shared MVI Foundation & Architecture Transition Guidelines (Completed)
1.  **Multiplatform Lifecycle ViewModel Dependency:**
    *   Add `androidx-lifecycle-viewmodel` to `gradle/libs.versions.toml` and configure as `implementation` dependency in `shared/build.gradle.kts` under `commonMain.dependencies`.
2.  **Base MVI / UDF Contracts:**
    *   Implement `UiState`, `UiIntent`, and `UiEffect` marker interfaces under `shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/`.
    *   Implement abstract base class `MviViewModel<S : UiState, I : UiIntent, E : UiEffect>` providing `state: StateFlow<S>`, `iosState: CommonFlow<S>`, `effects: Flow<E>`, `iosEffects: CommonFlow<E>`, `dispatch(intent: I)`, and `teardown()` lifecycle hook.
    *   Author unit tests in `MviViewModelTest.kt` validating atomic state mutations, buffered effect channels, and teardown hooks.
3.  **Active Transition Architecture Guidelines:**
    *   Update `AGENTS.md` and `.agents/skills/kmp-architecture/SKILL.md` to formally document the active transition state where legacy Presenters and MVI ViewModels coexist during ongoing migration.
4.  **Reference Specification:**
    *   Track blueprint in `specs/42_mvi_foundation_and_transition_guidelines.md`.
5.  **Reference Specification:**
    *   Track blueprint in `specs/41_readme_and_agents_parity_alignment.md`.

### Step 45: MVI Pilot Migration - Photo Details (Completed)
1.  **Shared MVI Contracts & Implementation:**
    *   Implement `PhotoDetailsState`, `PhotoDetailsIntent`, and `PhotoDetailsEffect` under `shared/src/commonMain/kotlin/com/example/imagefeed/presentation/details/`.
    *   Implement `PhotoDetailsViewModel` extending `MviViewModel` with Metro `@AssistedInject` and `@AssistedFactory`.
2.  **Metro DI Graph Integration:**
    *   Provide `photoDetailsViewModelFactory` in `ApplicationGraph` and `getPhotoDetailsViewModel(photoId)` in `MetroHelper`.
3.  **Platform Presentation Shell Migration:**
    *   Migrate Android `PhotoDetailsScreen.kt` and `NavigationSections.kt` to collect state and effects from `PhotoDetailsViewModel` and dispatch intents.
    *   Migrate iOS `PhotoDetailsViewModel.swift` to wrap shared `PhotoDetailsViewModel`, observe `iosState` and `iosEffects`, forward intents, and clear on `deinit`.
4.  **Unit Testing:**
    *   Author comprehensive `PhotoDetailsViewModelTest.kt` verifying state transitions, intent dispatching, error side-effects, and retry flows.
5.  **Reference Specification:**
    *   Track blueprint in `specs/43_mvi_pilot_photo_details_migration.md`.

### Step 46: MVI Core Features Migration (Completed)
1.  **Shared MVI Feature Contracts & ViewModels:**
    *   Implement `CollectionDetailState/Intent/Effect/ViewModel` under `shared/src/commonMain/kotlin/com/example/imagefeed/presentation/collection/`.
    *   Implement `CollectionsFeedState/Intent/Effect/ViewModel` under `shared/src/commonMain/kotlin/com/example/imagefeed/presentation/collections/`.
    *   Implement `UserProfileState/Intent/Effect/ViewModel` under `shared/src/commonMain/kotlin/com/example/imagefeed/presentation/profile/`.
    *   Implement `RandomPhotoState/Intent/Effect/ViewModel` under `shared/src/commonMain/kotlin/com/example/imagefeed/presentation/random/`.
    *   Implement `FeedState/Intent/Effect/ViewModel` under `shared/src/commonMain/kotlin/com/example/imagefeed/presentation/feed/`.
    *   Implement `UnifiedSearchState/Intent/Effect/ViewModel` under `shared/src/commonMain/kotlin/com/example/imagefeed/presentation/search/`.
2.  **Metro DI Bindings:**
    *   Add bindings and accessors for all modern ViewModels and assisted factories to `ApplicationGraph` and `MetroHelper`.
3.  **Platform Presentation Shells:**
    *   Migrate Android screens (`CollectionDetailScreen`, `CollectionsFeedScreen`, `FeedScreen`, `SearchScreen`, `UserProfileScreen`) to collect MVI state/effects and dispatch intents.
    *   Migrate iOS Swift ViewModels (`CollectionDetailViewModel`, `CollectionsFeedViewModel`, `FeedViewModel`, `SearchViewModel`, `UserProfileViewModel`) to wrap shared `MviViewModel` instances.
4.  **Unit Testing & Verification:**
    *   Author unit tests for all ViewModels in `shared/src/commonTest/kotlin/com/example/imagefeed/presentation/`.
    *   Verify dual-platform compilation and linters.
5.  **Reference Specification:**
    *   Track blueprint in `specs/44_mvi_core_screens_migration.md`.

### Step 47: MVI Architecture Cleanup & Finalization (Completed)
1.  **Delete Legacy Presenter Infrastructure:**
    *   Decommissioned `PresenterScope.kt`, `PresenterFactories.kt`, `TestPresenterScopeFactory.kt`, `PresenterViewModel.kt`, and all obsolete `*Presenter.kt` implementations and presenter tests.
2.  **Metro Graph Cleanliness:**
    *   Removed `DispatcherProvider`, `PresenterScopeFactory`, and all legacy presenter factory bindings from `AppModule`, `ApplicationGraph`, and `MetroHelper`.
3.  **Android UI Shell Standard ViewModel Scoping:**
    *   Migrated navigation entry ViewModels from `rememberEntryPresenter` to standard Android Compose `viewModel(key = ...) { ... }`.
4.  **Agent Documentation Alignment:**
    *   Updated `AGENTS.md`, `GEMINI.md`, and skills (`kmp-architecture`, `kmp-testing`, `ios-glass-design`, `android-m3-design`) to establish Shared MVI / UDF as the sole architectural standard and eliminated the transition state.
5.  **Reference Specification:**
    *   Track blueprint in `specs/45_mvi_architecture_cleanup_and_finalization.md`.

### Step 48: Modularization Phase 1 - Foundation & Core Extraction (Completed)
1.  **Module Directory & Build Script Scaffolding:**
    *   Register `:core:model`, `:core:mvi`, and `:core:network` in `settings.gradle.kts` (eliminating centralized `:core:data` under Option B).
    *   Create KMP build scripts using `com.android.kotlin.multiplatform.library` and Apple Silicon targets (`iosArm64`, `iosSimulatorArm64`, `macosArm64`).
2.  **Extract Core Infrastructure Modules:**
    *   Extract `:core:model` (universal domain entities and serialization DTOs: `Photo`, `Topic`, `PhotoCollection`, `CollectionSummary`, `User`, `SearchResponse`, deferring feature-specific models to their owning features).
    *   Extract `:core:mvi` (presentation primitives: `MviViewModel`, `UiState`, `UiIntent`, `UiEffect`, `CommonFlow`).
    *   Extract `:core:network` (Ktor 3 HTTP engine configuration, logging, `BuildKonfig` API key injection, dedicated `PhotoDownloadTracker`, and pagination helper `CollectionsExt`).
    *   Retain legacy `UnsplashApiClient` and `UnsplashRepository` temporarily in `:shared` to service un-migrated ViewModels without introducing an intermediate `:core:data` module.
3.  **Update Assembly Umbrella & Validate:**
    *   Configure `:shared` to depend on `:core:network`, `:core:mvi`, and `:core:model` with `api` scope and export core symbols in native framework.
    *   Verify dual-platform builds and test suites.
4.  **Reference Specification & Research:**
    *   Track blueprint in `specs/46_modularization_foundation_core_extraction.md` and `specs/researches/modularization_evaluation.md`.

### Step 49: Modularization Phase 2 - Pilot Feature Migration (`:feature:details`) (Completed)
1.  **Scaffold Feature Module:**
    *   Register `:feature:details` in `settings.gradle.kts` and configure `build.gradle.kts` with Metro compiler plugin and dependencies on `:core:model`, `:core:mvi`, and `:core:network`.
2.  **Migrate Photo Details Vertical Slice:**
    *   Relocate `PhotoDetailsViewModel`, `PhotoDetailsState`, `PhotoDetailsIntent`, `PhotoDetailsEffect` to `:feature:details`.
    *   Implement dedicated `PhotoDetailsRepository` contract, `internal class PhotoDetailsRepositoryImpl`, and `internal class PhotoDetailsApiClient`.
    *   Migrate `PhotoStats` (and metric sub-types) from `Photo.kt` into `:feature:details`.
    *   Delegate download tracking directly to `PhotoDownloadTracker` from `:core:network`.
    *   Relocate unit tests to `feature/details/src/commonTest/kotlin/com/example/imagefeed/presentation/details/PhotoDetailsViewModelTest.kt` and replace the 17-method monolithic fake with lean `FakePhotoDetailsRepository`.
3.  **Validate Metro DI & Apple Framework Linking:**
    *   Verify Metro compile-time code generation for assisted factory (`PhotoDetailsViewModel.Factory`) across Gradle module boundaries.
    *   Export `:feature:details` from `:shared` native framework and verify Swift views in `iosApp` and `macosApp` link without errors while keeping internal data classes unexported.
4.  **Reference Specification & Research:**
    *   Track blueprint in `specs/47_modularization_pilot_feature_details.md` and `specs/researches/modularization_evaluation.md`.

### Step 50: Modularization Phase 3 - Remaining Feature Extractions (Completed)
1.  **Scaffold Vertical Feature Modules:**
    *   Register and configure `:feature:feed`, `:feature:search`, `:feature:collections`, and `:feature:profile` in `settings.gradle.kts` with dependencies on `:core:model`, `:core:mvi`, and `:core:network`.
2.  **Migrate Vertical Feature Slices & Tests:**
    *   Migrate feed and random photo logic to `:feature:feed` with dedicated `FeedRepository`, `FeedApiClient`, and `FakeFeedRepository`.
    *   Migrate multi-facet search logic to `:feature:search` with dedicated `SearchRepository`, `SearchApiClient`, and `FakeSearchRepository`.
    *   Migrate collection feed and detail logic to `:feature:collections` with dedicated `CollectionsRepository`, `CollectionsApiClient`, and `FakeCollectionsRepository`.
    *   Migrate photographer profile logic to `:feature:profile` with dedicated `ProfileRepository`, `ProfileApiClient`, `FakeProfileRepository`, and feature-owned `UserStats` model.
    *   Delegate all download tracking calls to `PhotoDownloadTracker` from `:core:network`.
    *   Correct all test paths and move test suites into each feature module's `commonTest`.
3.  **Decommission Legacy Data Layer & Root Metro Graph:**
    *   Completely delete `UnsplashRepository.kt` and `UnsplashApiClient.kt` from `:shared`.
    *   Aggregate all feature modules into `:shared`'s `ApplicationGraph` and `MetroHelper`.
    *   Export feature ViewModels in `:shared` native framework for Swift/HIG consumption.
4.  **Reference Specification & Research:**
    *   Track blueprint in `specs/48_modularization_remaining_features_extraction.md` and `specs/researches/modularization_evaluation.md`.

### Step 51: Modularization Phase 4 - Umbrella Framework & Linking Optimization (Completed)
1.  **Selective Binary Export & Internal Visibility Benefits:**
    *   Restrict Apple framework `export(...)` declarations in `:shared` strictly to public feature ViewModels and core model/MVI contracts.
    *   Keep internal networking engines (`:core:network`), feature repository implementations, and feature API clients unexported (enforced via `internal` visibility) to minimize Objective-C bridging stubs and reduce link times.
2.  **Toolchain Caching & CI Matrix Optimization:**
    *   Verify `org.gradle.parallel=true`, `org.gradle.caching=true`, and `org.gradle.configuration-cache=true`.
    *   Partition CI pipeline tasks across fast Linux runners (unit tests across all 8 modular libraries, static analysis, Android build) and macOS runners (Apple framework compilation, native tests).
3.  **Reference Specification & Research:**
    *   Track blueprint in `specs/49_modularization_umbrella_and_linking_optimization.md` and `specs/researches/modularization_evaluation.md`.

### Step 52: Android Image Pipeline & BlurHash Decoding Cache (Completed)
1.  **Coil 3 Application ImageLoader Configuration:**
    *   Implement `SingletonImageLoader.Factory` in `ImageFeedApplication.kt`.
    *   Configure `MemoryCache` with 25% heap sizing and strong reference caching.
    *   Configure `DiskCache` with 100 MB max size in `cacheDir` in compliance with Unsplash hotlinking rules.
2.  **BlurHash Asynchronous Offloading & LRU Caching:**
    *   Implement an in-memory `LruCache<String, Bitmap>` in `BlurHashDecoder.kt` preventing duplicate decoding across list item recycling.
    *   Offload trigonometric decoding to `Dispatchers.Default` via `decodeAsync(...)`.
    *   Provide synchronous `getCached(...)` inspection for zero-flicker placeholder rendering during initial composition.
3.  **Screen Integration:**
    *   Migrate placeholder loading across `FeedScreen.kt`, `CollectionsFeedScreen.kt`, `CollectionDetailScreen.kt`, and `PhotoDetailsScreen.kt`.
4.  **Reference Specification:**
    *   Track blueprint in `specs/50_image_pipeline_and_blurhash_caching.md`.

### Step 53: Android Lazy Grid Motion, Modifier Overhead & Scroll Optimization (Completed)
1.  **Motion & Modifier Performance Hardening:**
    *   Scope `Modifier.staggeredEntrance` strictly to initial screen appearance batch (`index < 6`), bypassing translation and alpha springs for scrolled/recycled items.
    *   Remove `.shadow()` dynamic elevation from `Modifier.bounceClick` and migrate scale animation to Draw-phase lambda `graphicsLayer { ... }`.
2.  **Scroll State & Pagination Prefetching:**
    *   Replace inline `derivedStateOf { listState.layoutInfo... }` with coroutine `snapshotFlow` and `distinctUntilChanged()`.
3.  **Sister Screen Alignment:**
    *   Align `CollectionsFeedScreen.kt`, `SearchScreen.kt`, `CollectionDetailScreen.kt`, and `UserProfileScreen.kt`.
4.  **Reference Specification:**
    *   Track blueprint in `specs/51_lazy_grid_motion_and_scroll_optimization.md`.

### Step 52: macOS Window Title & iOS Navigation Bar Sheet Overlap Fixes (Completed)
1.  **macOS Collection Detail Window Title:**
    *   Add `.navigationTitle(viewModel.collection?.title ?? "Collection")` and iOS inline title display mode to `CollectionDetailView.swift`.
2.  **iOS Photo Details Navigation Bar Overlap & Focus:**
    *   Hide navigation bar chrome on iOS via `.toolbar(.hidden, for: .navigationBar)` and add empty `.navigationTitle("")` in `PhotoDetailsView.swift` to prevent background bar from overlapping bottom sheet and stealing gestures.
3.  **Reference Specification:**
    *   Track blueprint in `specs/52_macos_title_and_ios_sheet_navigation_fixes.md`.

### Step 53: Android Material 3 Expressive Core Friction Fixes & Screen Hygiene (Completed)
1.  **Profile Tab Row Wrapping Fix & Morphing Badges:**
    *   Replace fixed 4-slot `SecondaryTabRow` with horizontally scrollable `LazyRow` pill chips and inline count `Badge`s in `UserProfileScreen.kt`.
    *   Eliminate mid-word text wrapping like `"COLLECTIO / NS (6)"` across dynamic font scales.
2.  **Attribution Scoping & Social Assist Chips (Unsplash Guard):**
    *   Add `showAttribution: Boolean = true` parameter to `PhotoCard` and `PortfolioTabContent`.
    *   Pass `false` in creator's own portfolio grid (`ProfileTab.PORTFOLIO`) on `UserProfileScreen.kt`, while strictly keeping `showAttribution = true` on `ProfileTab.LIKES` (`likedPhotos`) to comply with Unsplash API terms.
    *   Convert custom uppercase social badges (`"INSTAGRAM: @..."`) to compact Material 3 `AssistChip`s with leading icons.
3.  **Icon Semantic Alignment & Header Streamlining:**
    *   Remove redundant Search action icon from Feed top bar in `FeedScreen.kt`; migrate title to collapsible `headlineMedium`.
    *   Replace confusing stats icons in `PhotoDetailsScreen.kt` (`Icons.Default.Info` -> `Icons.Default.Visibility`, `Icons.Default.PlayArrow` -> `Icons.Default.FileDownload`).
    *   Replace download CTA icon (`Icons.Default.Done` -> `Icons.Default.Download`).
4.  **Reference Specification:**
    *   Track blueprint in `specs/53_android_material3_expressive_core_friction_fixes.md`.

### Step 54: Android Material 3 Expressive Photo Details & Content-Driven Dynamic Theming (Completed)
1.  **Lightweight Native Dynamic Tonal Generation:**
    *   Implement `generatePhotoColorScheme` in `Theme.kt` seeded from Unsplash `photo.color` using native HSL/tonal calculations (zero external dependencies).
    *   Scope `PhotoDetailsScreen.kt` with dynamic `ColorScheme` tinting surface containers, chips, and button accents.
2.  **Visual EXIF 2x2 Bento Box Grid:**
    *   Reconstruct camera and lens metadata into a 2x2 mini-bento grid with micro-icons (Aperture, Shutter/`exposureTime`, Focal Length/`focalLength`, ISO) in `SurfaceContainerLow` containers.
3.  **Interactive Color Palette Swatches & Floating Action Bar:**
    *   Render interactive 5-swatch color palette row derived from photo seed color with tap-to-copy hex and haptics.
    *   Anchor primary download action into a floating bottom action toolbar (Download RAW/Full, Share) with Unsplash download tracking (`TrackDownload`). Anchor inside details sheet on wide-screen dual-pane layouts.
4.  **Glowing Bezier Analytics Curve:**
    *   Upgrade views chart to smooth cubic Bezier path (`Path.cubicTo`) with multi-stop area gradient fill and min/max reference bounds, bound to `state.stats`.
5.  **Reference Specification:**
    *   Track blueprint in `specs/54_android_material3_expressive_photo_details_and_dynamic_theming.md`.

### Step 55: Android Material 3 Expressive Floating Navigation Chrome & Bento Discovery (Completed)
1.  **Floating Translucent Pill Navigation Bar:**
    *   Replace docked `NavigationBar` on phones in `MainActivity.kt` with a floating pill container (`height = 68.dp`, `shape = CircleShape`, `SurfaceContainerHigh` at ~88% alpha, subtle gradient border).
    *   Configure edge-to-edge content underlay with compensatory bottom padding so photography scrolls visibly underneath.
    *   Preserve start-docked `NavigationRail` for tablets and foldables (`AdaptiveLayoutInfo.kt`).
2.  **Curated Collections Proportional Bento Previews:**
    *   Formalize collection preview card to `1.4f` aspect ratio (62% hero, 38% stacked pair) with 28dp squircle corners in `CollectionsFeedScreen.kt`, preserving Coil 3 BlurHash caching.
    *   Upgrade category topic tabs in `FeedScreen.kt` to visual image capsules with dark scrims.
3.  **Dark Photographic Gradient Suggestions & Color Exploration Dial:**
    *   Upgrade search suggestions in `SearchScreen.kt` to rich dark photographic gradient cards with bold typography.
    *   Implement horizontal color dot selector in `SearchScreen.kt` dispatching `UnifiedSearchIntent.SelectColor(color)`.
4.  **Reference Specification:**
    *   Track blueprint in `specs/55_android_material3_expressive_floating_navigation_and_bento_discovery.md`.

### Step 56: iOS Glass Contrast, Navigation Chrome & Legibility Fixes (Completed)
1.  **Search Screen & Tab Bar Contrast Fixes:**
    *   Update `SearchSuggestionsView` in `SearchView.swift` to ensure popular topic pills have crisp, high-contrast labels across Light and Dark modes.
    *   Fix Tab Bar wash-out by removing hardcoded white tinting on light backgrounds in `ContentView.swift`.
2.  **Navigation Chrome Normalization & Pushed Sheet Hygiene:**
    *   Remove forced `.toolbarColorScheme(.dark, for: .navigationBar)` from `ContentView.swift`, `CollectionsFeedView.swift`, `SearchView.swift`, `UserProfileView.swift`, and `CollectionDetailView.swift`.
    *   Attach `.toolbar(.hidden, for: .tabBar)` to pushed views (`UserProfileView`, `SearchView`, `CollectionDetailView`).
3.  **Feed Author Badge Dark Vibrancy:**
    *   Apply dark vibrancy and backing scrim to `PhotoCard` author capsule in `ContentView.swift`.
4.  **Reference Specification:**
    *   Track blueprint in `specs/56_ios_glass_contrast_and_navigation_chrome_fixes.md`.

### Step 57: iOS Immersive Photo Detail Canvas & Interactive Sparkline (Completed)
1.  **Ambient Backdrop Layer:**
    *   Implement full-bleed ambient blurred backdrop using downsampled photo with dark scrim on `PhotoDetailsView.swift`, eliminating top white void and providing contrast for Spec 52's promoted top overlay buttons.
2.  **Interactive Views Sparkline Scrubbing:**
    *   Add touch gesture dragging, closest-date mapping, glowing `PointMark` indicator, and haptic feedback to `HistoricalStatsChart`.
3.  **Tactile Glass Orbs for Action Buttons:**
    *   Modernize `GlassToolbarButton` in `GlassTheme.swift` to 40x40 single-layer `.thinMaterial` circles with hierarchical SF Symbols.
4.  **Reference Specification:**
    *   Track blueprint in `specs/57_ios_immersive_photo_detail_and_interactive_sparkline.md`.

### Step 58: iOS Floating Glass Tab Capsule & Profile Refinements (Completed)
1.  **Floating Specular Glass Tab Capsule & Navigation Suppression:**
    *   Implement `FloatingTabCapsule` with sliding indicator (`matchedGeometryEffect`), spring physics, and `.regularMaterial` glass.
    *   Suppress `FloatingTabCapsule` when navigation path is not empty (`path.isEmpty == false`) so it never hovers over pushed child screens.
2.  **Profile Social Chips & Tab Picker Wrapping Fix:**
    *   Convert verbose text buttons in `UserProfileView.swift` to compact icon-led glass chips.
    *   Refactor `ProfileTabPicker` to eliminate label truncation on compact iPhone viewports.
    *   Upgrade popular search topics into visual cards with bold typography.
3.  **Reference Specification:**
    *   Track blueprint in `specs/58_ios_floating_glass_capsule_and_profile_refinements.md`.

### Step 59: Cross-Platform Localization Architecture (Phase 1: English Extraction & Foundation) (Completed)
1.  **Shared Domain Error Modeling & ViewModel Decoupling:**
    *   Define `sealed class AppError` in `:core:model` (or `:core:mvi`) and central exception mapper `Throwable.toAppError()` in `:core:network`.
    *   Refactor all 7 KMP feature ViewModels (`FeedViewModel`, `CollectionsFeedViewModel`, `CollectionDetailViewModel`, `UnifiedSearchViewModel`, `PhotoDetailsViewModel`, `UserProfileViewModel`, `RandomPhotoViewModel`) to emit typed `AppError` in state and effect flows instead of hardcoded English fallback strings.
    *   Purge hardcoded mock search terms from default `SearchState.searchHistory`.
    *   Update `commonTest` suites to assert typed `AppError` instances with 100% test pass rate (`./gradlew allTests`).
2.  **Android Resource Architecture (`:androidApp`):**
    *   Create `res/values/strings.xml` and `res/values/plurals.xml` covering the unified 85-key catalog.
    *   Replace hardcoded literals across all Compose screens with `stringResource()` and `pluralStringResource()`.
    *   Replace `formatStatValue` in `PhotoDetailsScreen.kt` with Android ICU `CompactDecimalFormat` (API 29+ safe, avoiding API 34 crash).
    *   Format sparkline timeline dates using `java.time.LocalDate` and localized `FormatStyle.MEDIUM`.
    *   Localize Toast errors in `MainActivity.kt` on device shake.
3.  **Apple Platforms Architecture (`:iosApp` & `macosApp`):**
    *   Configure `iosApp/project.yml` with `developmentLanguage: en` and `knownRegions: [en]`.
    *   Create `iosApp/iosApp/Localizable.xcstrings` String Catalog with all 85 keys and plural format variations.
    *   Implement Swift extension on `shared.AppError` for localized string resolution.
    *   Adopt native Swift Foundation metric formatting (`.formatted(.number.notation(.compactName))`) and date formatting (`ISO8601DateFormatter`).
    *   Replace hardcoded `chevron.left` back buttons with auto-mirroring `chevron.backward`.
4.  **Agent Instructions & Skill Governance:**
    *   Author `.agents/skills/localization/SKILL.md` defining string extraction patterns, plural syntax differences, and format token conventions.
    *   Update `AGENTS.md` and `GEMINI.md` with the Zero Hardcoded Strings Policy and skill link while preserving root brevity (< 75 lines).
5.  **Reference Specification:**
    *   Track blueprint in `specs/59_cross_platform_localization_architecture.md`.

### Step 60: Android Shimmer & Skeleton Rendering Optimization (Completed)
1.  **Draw-Phase Shimmer:**
    *   Replace per-element composition-phase `shimmerEffect` with a draw-phase modifier driven by one shared infinite transition per skeleton screen.
    *   Remove nested shimmering containers and make the sweep density-independent.
2.  **Reference Specification:**
    *   Track blueprint in `specs/60_android_shimmer_skeleton_rendering_optimization.md`.

### Step 61: Android Media Card Hot-Path Optimization (Completed)
1.  **PhotoCard & CollectionMosaicCard:**
    *   Hoist grid-invariant item width, remember `ImageRequest`/URL and scrim brush, gate `sharedElement`/`sharedBounds` per item.
    *   Memoize `calculate*GridColumns` helpers; decide on `staggeredEntrance` as `Modifier.Node` (low priority).
2.  **Reference Specification:**
    *   Track blueprint in `specs/61_android_media_card_hot_path_optimization.md`.

### Step 62: Android Scroll-Driven Chrome & Horizontal Row Hygiene (Completed)
1.  **Feed Chrome & Rows:**
    *   Remove composition-phase `contentOffset` read for the topic strip background.
    *   Key the unkeyed Search colour `LazyRow`; document verified rows left unchanged; profile nested-grid virtualization tracked and handled in Step 65.
2.  **Reference Specification:**
    *   Track blueprint in `specs/62_android_scroll_chrome_and_horizontal_row_hygiene.md`.

### Step 63: Android Photo Details Layout & Chart Rendering Optimization (Completed)
1.  **Details & Charts:**
    *   Keep pane-width awareness for `BoxWithConstraints` (window-level `LocalAdaptiveLayoutInfo` is not a safe replacement); remember the details `ImageRequest`.
    *   Cache chart geometry in draw cache, derive hover index, fix the 300dp hover bug, eliminate dead `animProgress` per zero-dead-code policy, cache `CompactDecimalFormat`.
2.  **Reference Specification:**
    *   Track blueprint in `specs/63_android_photo_details_and_chart_rendering_optimization.md`.

### Step 64: Android Release Optimization, Baseline Profiles & Macrobenchmark (Completed)
1.  **Release Build & Profiles (Phased):**
    *   Phase A: Created `proguard-rules.pro` with keep rules for serialization DTOs, `NavKey`, Metro graph, Maps, Ktor, OkHttp, and Coil; enabled R8 minification (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`) for release; verified release assembly.
    *   Phase B: Added `:benchmark` macrobenchmark module using AGP 9.4.1 + `androidx.baselineprofile` 1.5.0; generated baseline profiles on Android emulator; committed `androidApp/src/main/baseline-prof.txt`; integrated `androidx.profileinstaller`; recorded startup and frame timing benchmark metrics.
2.  **Reference Specification:**
    *   Track blueprint in `specs/64_android_release_optimization_and_baseline_profiles.md`.

### Step 65: Runtime Memory Optimization Phase 1 — Image Caches, Grid Virtualization & ViewModel Scope Leaks (Planned)
1.  **Android Profile Grid Virtualization:**
    *   Replace nested `LazyVerticalStaggeredGrid` inside `Column(Modifier.verticalScroll)` + `.heightIn(max = 2000.dp)` in `UserProfileScreen.kt` with a single full-screen `LazyVerticalStaggeredGrid` using `StaggeredGridItemSpan.FullLine` header and tab items, dismantling legacy nested composables.
2.  **iOS/macOS `KFImage` Downsampling & Bounded `NSCache`:**
    *   Evaluate `.placeholder` lazily on cache misses, downsample images via `ImageIO` (`CGImageSourceCreateThumbnailAtIndex`), and cache decoded images in a byte-cost-bounded `NSCache` with memory-warning eviction.
3.  **Android Coil Tuning & Memory Trim Handling:**
    *   Lower `MemoryCache` `maxSizePercent` from `0.25` to `0.15`, enable `allowRgb565(true)`, remove the 100 MB local `DiskCache` for Unsplash hotlink compliance, and implement `onTrimMemory(level)` in `ImageFeedApplication.kt` to trim Coil's cache and call `BlurHashDecoder.clearCache()`.
4.  **ViewModel Scope Hygiene:**
    *   Make `iosState` and `iosEffects` `by lazy` in `MviViewModel.kt`, and deterministically close/clear `RandomPhotoViewModel` and its `Closeable` watcher across all paths in `FeedViewModel.swift`.
5.  **Reference Specification:**
    *   Track blueprint in `specs/65_memory_optimization_phase1_caches_grids_and_scopes.md`.

### Step 66: Runtime Memory Optimization Phase 2 — Maps, Thumbnail Downsizing & BlurHash Cache Optimization (Planned)
1.  **Static Map Previews:**
    *   Enable Google Maps Lite Mode (`liteMode(true)`) and `remember` `MapStyleOptions` in `PhotoDetailsScreen.kt`; replace live `MapKit.Map` in `MapCardView` (`PhotoDetailsView.swift`) with a cached `MKMapSnapshotter` static snapshot.
2.  **Thumbnail Sizing & Duplicate Query Parameter Fixes:**
    *   Add `PhotoUrls.getOptimalUrl(targetWidthPx, quality)` and single-parameter `getOptimalUrl(targetWidthPx)` in `:core:model`, replace oversized `urls.regular` loads in small collection/preview cards across Android and iOS, and fix duplicate `&w=` query parameters appended onto `urls.small` in `CollectionsFeedScreen.kt` and `CollectionsFeedView.swift`.
3.  **BlurHash Painter & Byte-Bounded Cache:**
    *   Return a remembered `BitmapPainter?` from `rememberBlurHashPlaceholder` across all Android call sites, quantize placeholder heights into discrete buckets, and bound `BlurHashDecoder.cache` by byte size (`sizeOf` / `allocationByteCount`).
4.  **Reference Specification:**
    *   Track blueprint in `specs/66_memory_optimization_phase2_maps_thumbnails_and_blurhash.md`.

### Step 67: Runtime Memory Optimization Phase 3 — Network Buffering, Navigation Hygiene, DI Scoping & UI Frame Churn (Planned)
1.  **Ktor Network & Metro DI Scoping:**
    *   Replace `LogLevel.ALL` and `prettyPrint = true` with `LogLevel.NONE`, `prettyPrint = false`, and a shared `Json` instance in `HttpClientFactory.kt`.
    *   Define `@Scope @HiddenFromObjC annotation class AppScope` in `:core:network` with Metro plugin and annotate all 5 feature repository `@Provides` methods (`FeedModule`, `CollectionsModule`, `SearchModule`, `ProfileModule`, `PhotoDetailsModule`) with `@SingleIn(AppScope::class)`.
2.  **Navigation Backstack & Inactive Tab State Hygiene:**
    *   Ensure `selectedPhotoForHero = nil` is cleared on all navigation transitions in `ContentView.swift`, bound Android detail backstack depth and `UnifiedSearchViewModel` keys (`NavigationSections.kt`, `Navigator.kt`), and cap inactive tab list retention in `UnifiedSearchState` and `UserProfileState`.
3.  **UI Frame & Emission Churn Elimination:**
    *   Guard Swift `@Observable` property/array reassignments, throttle `activeCapsuleMinimized` and `scrollResetTask` in `ContentView.swift`, replace $O(N^2)$ `firstIndex(where:)` masonry lookups with $O(1)$ indexed column items in `AdaptiveLayoutHelper.swift`, share a single `InfiniteTransition` across shimmer skeletons (`AnimationUtils.kt`), and `remember`/cache chart data and `Canvas` objects (`PhotoDetailsScreen.kt`, `UserProfileScreen.kt`).
4.  **Reference Specification:**
    *   Track blueprint in `specs/67_memory_optimization_phase3_network_navigation_di_and_ui_churn.md`.

### Step 68: SwiftUI Performance Phase 1 — Algorithmic & Layout Architecture (Planned)
1.  **$O(1)$ Masonry Indexing & Column Bucket Memoization:**
    *   Introduce `IndexedItem<T>` and single-pass `partitionIntoColumns` in `AdaptiveLayoutHelper.swift`.
    *   Memoize column buckets inside `@Observable` ViewModels (`FeedViewModel`, `CollectionsFeedViewModel`, `SearchViewModel`, `CollectionDetailViewModel`, `UserProfileViewModel`) and invalidate only on backing array mutation.
    *   Eliminate $O(N^2)$ `firstIndex(where:)` calls in `SearchView.swift`, `UserProfileView.swift`, and `CollectionDetailView.swift`, and replace `lastIndex(where:)` in `CollectionsFeedView.swift` with an $O(1)$ index threshold check.
2.  **Root `GeometryReader` Removal & Lazy ViewModel Lifecycle:**
    *   Remove the root `GeometryReader` wrapping `CollectionsFeedView.swift`.
    *   Defer KMP ViewModel instantiation out of `View.init` across `PhotoDetailsView.swift`, `SearchView.swift`, `UserProfileView.swift`, and `CollectionDetailView.swift` using lazy `@State` initialization.
3.  **Reference Specification:**
    *   Track blueprint in `specs/68_ios_swiftui_performance_phase1_algorithmic_and_layout_architecture.md`.

### Step 69: SwiftUI Performance Phase 2 — Scroll Dynamics, Native Geometry Tracking & State Isolation (Planned)
1.  **Isolated Scroll Capsule Coordinator & Transition Guards:**
    *   Extract `CapsuleScrollCoordinator` (`@Observable`) so scroll-driven minimization only invalidates `FloatingTabCapsule` without re-evaluating `ContentView.body` or `PhotosFeedTabView.body`.
    *   Guard `withAnimation` state flips and only schedule `scrollResetTask` when `isMinimized == true`.
    *   Use native `.onScrollGeometryChange` on `#available(iOS 18.0, macOS 15.0, *)` with bucket quantization and a quantized iOS 17 `PreferenceKey` fallback.
2.  **Search Input Deduplication & Pagination Motion Cleanup:**
    *   Eliminate duplicate `updateQuery` dispatches in `SearchView.swift` while preserving KMP `UnifiedSearchViewModel`'s 300ms coroutine debounce (avoiding a 600ms double-debounce).
    *   Remove whole-grid `.animation(.spring(), value: viewModel.photos)` from `SearchView.swift` L164.
3.  **Reference Specification:**
    *   Track blueprint in `specs/69_ios_swiftui_performance_phase2_scroll_dynamics_and_state_isolation.md`.

### Step 70: SwiftUI Performance Phase 3 — GPU Compositor, Single-Pass Shimmer & Image Pipeline (Planned)
1.  **Generic `KFImage<Placeholder: View>` & Cross-Platform `ImageMemoryCache`:**
    *   Replace `AnyView` placeholder type erasure with generic `KFImage<Placeholder: View>` evaluated lazily only on cache misses.
    *   Implement byte-cost-bounded `ImageMemoryCache` (`NSCache<NSURL, PlatformImage>` supporting `UIImage` on iOS and `NSImage` on macOS) with off-main-thread `ImageIO` thumbnail downsampling (`CGImageSourceCreateThumbnailAtIndex` + `kCGImageSourceShouldCacheImmediately`) and iOS memory-warning eviction.
2.  **GPU Material & Offscreen Pass Optimization:**
    *   Introduce `GlassStyle.lightweight` (single-pass translucent dark scrim + specular border, `hasShadow: false`) in `GlassTheme.swift` for recurring masonry attribution and photo-count capsules (`PhotoCard`, `SearchPhotoGridCard`, `CollectionPhotoGridCard`, `CollectionMosaicCard`), and remove redundant `.ultraThinMaterial` overlays stacked over solid hex color image placeholders.
    *   Refactor `ShimmerModifier` in `AnimationPrimitives.swift` from double-pass `.mask(content)` to single-pass `.blendMode(.sourceAtop)` with `.compositingGroup()` and `accessibilityReduceMotion` support.
    *   Optimize `ambientBackdrop` in `PhotoDetailsView.swift` with `maxPixelSize: 160`, `.blur(radius: 28)`, and `.drawingGroup()`.
    *   Update `.agents/skills/ios-glass-design/SKILL.md` to codify the tiered `GlassStyle.lightweight` policy upon implementation.
3.  **Reference Specification:**
    *   Track blueprint in `specs/70_ios_swiftui_performance_phase3_gpu_rendering_and_image_pipeline.md`.


