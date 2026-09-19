# Specification: Material 3 Design Foundation & Theming Infrastructure

**Status:** New / Not Implemented

## Overview
This specification establishes the core Material 3 (Material You) design foundation for the Android application (`androidApp`). Currently, the app defines an inline `MaterialTheme` with three hardcoded colors in `MainActivity.kt`, has no centralized theme architecture, lacks dynamic color support, forces a permanent dark mode without system switching, and omits edge-to-edge window configuration.

This spec delivers an atomic, foundational theme package (`com.example.imagefeed.android.theme`) establishing color schemes, typography, shapes, dynamic color, and root activity edge-to-edge setup without altering individual screen composables yet.

---

## Current State & Deficiencies
1. **Inline Theme Definition**: `MainActivity.kt` defines:
   ```kotlin
   MaterialTheme(
       colorScheme = darkColorScheme(
           primary = Color(0xFF111111),
           background = Color(0xFF0F0F11),
           surface = Color(0xFF1E1E24),
       )
   )
   ```
2. **Missing Theme System Files**: No `Color.kt`, `Type.kt`, `Shape.kt`, or `Theme.kt` exist under `androidApp`.
3. **No Dynamic Color Support**: Android 12+ (API 31+) Wallpaper-based dynamic color (`dynamicDarkColorScheme`, `dynamicLightColorScheme`) is completely absent.
4. **No Light/Dark System Switching**: App forces dark palette regardless of device configuration.
5. **Missing Edge-to-Edge**: `enableEdgeToEdge()` is not called in `MainActivity.onCreate()`, violating Android 15 (targetSdk 37) recommendations and causing inconsistent status and navigation bar scrims.

---

## Architectural Changes & Implementation Plan

### 1. File Structure
Create package `androidApp/src/main/java/com/example/imagefeed/android/theme/`:
*   `Color.kt` — Core color palette definitions (seed colors, dark scheme, light scheme, container variants).
*   `Type.kt` — Material 3 Typography scale definitions (`display*`, `headline*`, `title*`, `body*`, `label*`).
*   `Shape.kt` — Material 3 Shapes scale definitions (`extraSmall`, `small`, `medium`, `large`, `extraLarge`).
*   `Theme.kt` — `ImageFeedTheme` composable with dynamic color and theme mode options.

### 2. Color System (`Color.kt`)
Define the complete set of Material 3 tonal roles for both Dark and Light themes:
*   **Primary / onPrimary / PrimaryContainer / onPrimaryContainer**:
    *   Dark: Elegant neutral-tinted primary reflecting the photography canvas aesthetic.
    *   Light: Deep slate / dark charcoal primary with soft container tints.
*   **Secondary / onSecondary / SecondaryContainer / onSecondaryContainer**:
    *   Accent hues for active filter chips, badge counts, and highlighted selection states.
*   **Tertiary / onTertiary / TertiaryContainer / onTertiaryContainer**:
    *   Warm accent for download triggers, rating indicators, or curated badges.
*   **Surface Containers Hierarchy**:
    *   `surface`: Base background (`0xFF0F0F11` dark / `0xFFFBFBFE` light).
    *   `surfaceContainerLowest`: Deepest background layer (`0xFF0A0A0C` dark / `0xFFFFFFFF` light).
    *   `surfaceContainerLow`: Secondary background backing (`0xFF16161A` dark / `0xFFF5F5F8` light).
    *   `surfaceContainer`: Standard card and feed item surface (`0xFF1E1E24` dark / `0xFFEEEEF2` light).
    *   `surfaceContainerHigh`: Elevated modals, filter sheets, dialogs (`0xFF25252D` dark / `0xFFE8E8EE` light).
    *   `surfaceContainerHighest`: Interactive inputs, search bars, active chips (`0xFF2C2C35` dark / `0xFFE2E2E8` light).
*   **Outlines**:
    *   `outline`: Subtle borders for unselected filter chips and outlined cards.
    *   `outlineVariant`: Soft dividers and card separators.

### 3. Typography System (`Type.kt`)
Configure `androidx.compose.material3.Typography`:
*   `displayLarge` / `displayMedium` / `displaySmall`: For editorial collection headers and splash titles.
*   `headlineLarge` / `headlineMedium` / `headlineSmall`: For screen titles (`Search`, `Collections`, `Photo Details`).
*   `titleLarge` / `titleMedium` / `titleSmall`: For card headers, photographer names, and dialog titles.
*   `bodyLarge` / `bodyMedium` / `bodySmall`: For photo descriptions, bio texts, and EXIF parameters.
*   `labelLarge` / `labelMedium` / `labelSmall`: For button captions, chip labels, navigation items, and photo tags.

### 4. Shape System (`Shape.kt`)
Configure `androidx.compose.material3.Shapes`:
*   `extraSmall`: `RoundedCornerShape(4.dp)` — tooltips, small badges.
*   `small`: `RoundedCornerShape(8.dp)` — compact chips, text inputs, map corners.
*   `medium`: `RoundedCornerShape(12.dp)` — standard photo cards, mosaic collection cards.
*   `large`: `RoundedCornerShape(16.dp)` — inspector sheets, profile cards, user rows.
*   `extraLarge`: `RoundedCornerShape(28.dp)` — full-modal bottom sheets, search bars, pill buttons.

### 5. Unified Theme Wrapper (`Theme.kt`)
```kotlin
@Composable
fun ImageFeedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
)
```
*   Use `dynamicDarkColorScheme(context)` and `dynamicLightColorScheme(context)` when running on Android 12+ (`Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`) and `dynamicColor == true`.
*   Fallback to custom `DarkColorScheme` and `LightColorScheme`.

### 6. Edge-to-Edge Integration in `MainActivity.kt`
*   Invoke `enableEdgeToEdge()` before `setContent`.
*   Replace inline `MaterialTheme(...)` with `ImageFeedTheme`.
*   Bind root `Surface` to `MaterialTheme.colorScheme.background`.
*   Ensure window insets propagate cleanly without visual regressions.

---

## Verification & Acceptance Criteria
1. **Compilation**: `./gradlew :androidApp:assembleDebug` builds cleanly with zero errors.
2. **Lint & Static Analysis**: `./gradlew ktlintCheck detekt` passes without warnings.
3. **Dynamic Theming**: On Android 12+ devices/emulators, system color accents dynamically propagate to primary and container tokens.
4. **Light/Dark Toggle**: Toggling system dark mode shifts background and surfaces between the light and dark palettes seamlessly.
5. **Edge-to-Edge**: Status bar and navigation bar are rendered transparent with appropriate scrims and contrast.