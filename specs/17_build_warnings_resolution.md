# Specification 17: Build Warnings Resolution

This document outlines the plan to address compile-time warnings and build configurations across the shared Kotlin Multiplatform module, Android app, and iOS/macOS apps.

---

## 1. Collected Warnings & Root Causes

### A. Android Compiler Warnings (Compose & Platform Deprecations)

| Component | File / Location | Warning Description | Root Cause / Fix |
| :--- | :--- | :--- | :--- |
| **Material 3 App Bar** | [CollectionDetailScreen.kt](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/CollectionDetailScreen.kt#L127) | `centerAlignedTopAppBarColors` is deprecated. | Replace with modern API `TopAppBarDefaults.topAppBarColors()`. |
| **Material 3 App Bar** | [CollectionsFeedScreen.kt](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/CollectionsFeedScreen.kt#L88) | `centerAlignedTopAppBarColors` is deprecated. | Replace with modern API `TopAppBarDefaults.topAppBarColors()`. |
| **Material 3 App Bar** | [MainActivity.kt](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L476) | `centerAlignedTopAppBarColors` is deprecated. | Replace with modern API `TopAppBarDefaults.topAppBarColors()`. |
| **Material 3 App Bar** | [PhotoDetailsScreen.kt](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt#L97) | `centerAlignedTopAppBarColors` is deprecated. | Replace with modern API `TopAppBarDefaults.topAppBarColors()`. |
| **Material 3 App Bar** | [UserProfileScreen.kt](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L115) | `centerAlignedTopAppBarColors` is deprecated. | Replace with modern API `TopAppBarDefaults.topAppBarColors()`. |
| **Material Icons** | [MainActivity.kt](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L194) | `Icons.Filled.List` is deprecated. | Replace with `Icons.AutoMirrored.Filled.List`. |
| **Material Icons** | [SearchScreen.kt](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L119) | `Icons.Filled.List` is deprecated. | Replace with `Icons.AutoMirrored.Filled.List`. |
| **Material Icons** | [PhotoDetailsScreen.kt](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt#L251) | `Icons.Filled.ArrowForward` is deprecated. | Replace with `Icons.AutoMirrored.Filled.ArrowForward`. |
| **Tabs & Indicators** | [MainActivity.kt](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L488) | `ScrollableTabRow` is deprecated. | Replace with `SecondaryScrollableTabRow`. |
| **Tabs & Indicators** | [SearchScreen.kt](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L127) | `TabRow` is deprecated. | Replace with `SecondaryTabRow`. |
| **Tabs & Indicators** | [SearchScreen.kt](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt#L133) | `Modifier.tabIndicatorOffset` is deprecated. | Use `TabIndicatorScope.tabIndicatorOffset` extension. |
| **Tabs & Indicators** | [UserProfileScreen.kt](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L381) | `TabRow` is deprecated. | Replace with `SecondaryTabRow`. |
| **Tabs & Indicators** | [UserProfileScreen.kt](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L387) | `Modifier.tabIndicatorOffset` is deprecated. | Use `TabIndicatorScope.tabIndicatorOffset` extension. |
| **Haptics / OS API** | [UserProfileScreen.kt](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L804) | `VIBRATOR_SERVICE` is deprecated. | Retrieve `Vibrator` via modern `VibratorManager` on API 31+. |
| **Haptics / OS API** | [UserProfileScreen.kt](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L805) | `vibrate(Long)` is deprecated. | Use `VibrationEffect.createOneShot()` to trigger haptics. |

### B. Shared Kotlin Multiplatform Module Warnings
*   **Result:** `BUILD SUCCESSFUL` with **0 warnings** on iOS simulator compiles.

### C. iOS & macOS Endpoint / Xcode Warnings & Errors

| Target | File / Area | Warning Description | Root Cause / Fix |
| :--- | :--- | :--- | :--- |
| **Both** | Gradle Framework Linkage | Architecture mismatch build error (KMP doesn't support `ios_x64` / `macos_x64`). | Exclude `x86_64` from Simulator/macOS architectures in XcodeGen configuration `project.yml`. |
| **Both** | `Assets.xcassets` | App icon set has an unassigned child (`icon-1024x1024@1x.png`). | Delete the duplicate, invalid `iphone` idiom 1024x1024 icon mapping from `Contents.json`. |
| **Both** | Build Phase Scripts | Run script build phase warning (missing outputs/run check). | Add `alwaysRun: true` to the `Compile Kotlin Shared Framework` script in `project.yml`. |

---

## 2. Detailed Technical Plan

### A. Android Code Fixes

1.  **Migrate App Bars:**
    Replace `TopAppBarDefaults.centerAlignedTopAppBarColors(...)` with `TopAppBarDefaults.topAppBarColors(...)`.
2.  **Migrate Icons:**
    Change `Icons.Default.List` to `Icons.AutoMirrored.Filled.List` and `Icons.Default.ArrowForward` to `Icons.AutoMirrored.Filled.ArrowForward`.
3.  **Migrate Tabs & Indicators:**
    *   Change `ScrollableTabRow` to `SecondaryScrollableTabRow`.
    *   Change `TabRow` to `SecondaryTabRow`.
    *   Replace `tabIndicatorOffset` calls:
        ```kotlin
        // Before
        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal])
        // After
        modifier = Modifier.tabIndicatorOffset(activeTab.ordinal)
        ```
4.  **Modernize Vibrator API:**
    Retrieve the service based on Android version and trigger via `VibrationEffect`:
    ```kotlin
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(20)
    }
    ```

### B. iOS & macOS XcodeGen Configurations

1.  **Exclude `x86_64` in `project.yml`:**
    Add `EXCLUDED_ARCHS` to exclude `x86_64` on simulator and macOS targets since Gradle KMP focuses purely on Apple Silicon targets:
    ```yaml
    # under iosApp.settings.base
    EXCLUDED_ARCHS[sdk=iphonesimulator*]: x86_64
    
    # under macosApp.settings.base
    EXCLUDED_ARCHS[sdk=macosx*]: x86_64
    ```
2.  **Silence Script Phase Warnings in `project.yml`:**
    Add `alwaysRun: true` to the `Compile Kotlin Shared Framework` build phase definition so Xcode doesn't complain about missing outputs.
3.  **Resolve Unassigned AppIcon Child:**
    Edit `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/Contents.json` to remove the redundant `iphone` 1024x1024 image element, keeping the valid `ios-marketing` 1024x1024 image element.

---

## 3. Verification Steps

1.  **Regenerate Xcode Project:**
    Run `cd iosApp && xcodegen` to apply the project configuration changes.
2.  **Verify Android Build:**
    Run `./gradlew clean :androidApp:assembleDebug` and verify there are no deprecated Compose/Platform warnings.
3.  **Verify iOS Build:**
    Run `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination "generic/platform=iOS Simulator" clean build` and verify it compiles without architecture errors and without script/asset warnings.
4.  **Verify macOS Build:**
    Run `xcodebuild -project iosApp/iosApp.xcodeproj -scheme macosApp -destination "generic/platform=macOS" clean build` and verify it compiles successfully.
