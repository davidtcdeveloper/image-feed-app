---
name: android-m3-design
description: >-
  Design and implement Jetpack Compose UI for Android following Material 3 guidelines,
  design tokens, Jetpack Navigation 3, adaptive layouts, and predictive back animations.
---

# Android Material 3, Navigation 3 & Adaptive UI Runbook

## Purpose & Triggers

This skill guides the implementation, styling, and architecture of Android Jetpack Compose UI within `androidApp`.
Use this skill when:
- Designing or modifying Compose UI screens, cards, dialogs, or components.
- Applying Material 3 (Material You) design tokens, dynamic color, or typography scales.
- Working with Jetpack Navigation 3 (`androidx.navigation3`), entry decorators, or back stacks.
- Configuring shared element hero transitions or predictive back gestures.
- Implementing responsive/adaptive layouts for foldables, tablets, and large screens.

---

## 1. Zero Hardcoded Design Tokens Policy

All Android UI components MUST reference semantic tokens from `MaterialTheme`. Hardcoded colors, arbitrary font sizes, and manual corner radii are strictly prohibited.

### A. Semantic Colors & Surface Hierarchy
Never use raw color literals (`Color(0xFF...)`, `Color.White`, `Color.Black`, `Color.Gray`, etc.). Material 3 uses tonal surface elevation instead of drop shadows or arbitrary grays:
- `MaterialTheme.colorScheme.surface`: Base root screen canvas.
- `MaterialTheme.colorScheme.surfaceContainerLowest`: Deepest recessed background behind nested cards.
- `MaterialTheme.colorScheme.surfaceContainerLow`: Secondary background canvas.
- `MaterialTheme.colorScheme.surfaceContainer`: Default for standard cards (`PhotoCard`, `CollectionCard`, `UserRowCard`).
- `MaterialTheme.colorScheme.surfaceContainerHigh`: Elevated modals, bottom sheets, filter dialogs.
- `MaterialTheme.colorScheme.surfaceContainerHighest`: Interactive inputs, search bars, active filter chips, tag pills.

*Exception*: Domain-specific content colors (such as Unsplash average photo background colors or camera sensor metadata tags).

### D. `designsystem/` Package (`Matte → Satin → Float`)
Organize shared UI surface primitives under `androidApp/src/main/java/com/example/imagefeed/android/designsystem/`:
- **Matte (`SurfaceTokens.kt`)**: Use `MatteSurface` and `SurfaceTokens` for opaque/tonal masonry cards, inspector panels, and discovery tiles with zero live backdrop blur. Photo cards ([`PhotoCard.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/components/PhotoCard.kt)) use a compact Matte corner attribution capsule (`SurfaceTokens.matteScrimPillColor()` + `outlineVariant` border) rather than full-width bottom scrims.
- **Satin (`SatinHeaderScaffold.kt`)**: Apply `Modifier.satinProgressiveBlur` for top-edge headers, strictly gated on `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU` (API 33+) and disabled under reduced motion or `LocalInspectionMode.current`, degrading cleanly on API 29–32 to a multi-stop vertical `MaterialTheme.colorScheme.surface` alpha scrim.
- **Shared Transitions (`SharedTransitionLocals.kt`)**: Use `LocalAppSharedTransition` (`ProvideAppSharedTransition`) and `SatinOcclusionController` instead of deep manual scope parameter threading.
- **Float (`FloatNavigationChrome.kt`, `ExpressiveShapes.kt`)**: Centralizes floating navigation chrome and expressive shape tokens.

### B. Typography Scale
Never use ad-hoc font sizes (e.g., `fontSize = 14.sp`, `fontSize = 20.sp`). Apply `style = MaterialTheme.typography.*` to support system-wide Dynamic Type font scaling:
- `displayLarge` / `displayMedium` / `displaySmall`: Hero banners and splash displays.
- `headlineLarge` / `headlineMedium` / `headlineSmall`: Screen titles and major section headers.
- `titleLarge` / `titleMedium` / `titleSmall`: Card headers, photographer names, dialog headings.
- `bodyLarge` / `bodyMedium` / `bodySmall`: Descriptions, bios, EXIF metadata paragraphs.
- `labelLarge` / `labelMedium` / `labelSmall`: Button labels, chip text, tag pills, navigation captions.

### C. Shape Scale
Never hardcode arbitrary radii (e.g., `RoundedCornerShape(16.dp)`). Use semantic shapes from `MaterialTheme.shapes.*`:
- `extraSmall` (`4.dp`): Badges, tooltips.
- `small` (`8.dp`): Chips, tag pills, inline map views.
- `medium` (`12.dp`): Standard photo cards, mosaic items.
- `large` (`16.dp`): Dialogs, inspector panels, user summary cards.
- `extraLarge` (`28.dp`): Modal bottom sheets, search bars, pill buttons.
- **Expressive Polygon Policy**: `PolygonShape` and `MorphPolygonShape` ([`ExpressiveShapes.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/designsystem/ExpressiveShapes.kt), [`ExpressiveShapeTokens`](../../../androidApp/src/main/java/com/example/imagefeed/android/theme/Shape.kt)) are permitted strictly for micro-controls, badges, and empty-state backdrops (`ApertureRandomizeButton`, `PhotoDetailPlaceholder` soft-scallop container); never clip photographs into polygons.

---

## 2. Component Guidelines

- **Theme & Motion**: Wrap root UI in `MaterialExpressiveTheme` ([`Theme.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/theme/Theme.kt)) with `MotionScheme.expressive()`, and drive navigation and spatial/effects animations via `MaterialTheme.motionScheme` physics springs rather than fixed easing tweens.
- **Chips & Connected Toggles**: Use official Material 3 `FilterChip`, `SuggestionChip`, `AssistChip`, `InputChip`, or M3 1.5.0 `ConnectedToggleGroup` ([`ExpressiveComponents.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/theme/ExpressiveComponents.kt)) for mutually-exclusive options. Never construct custom clickable `Box` layouts for tags or filter options.
- **Loading & Progress Indicators**: Use themed M3 1.5.0 `ExpressiveLoadingIndicator` (primary loading states) and `ExpressiveWavyProgressIndicator` (inline/pagination loading) from [`ExpressiveComponents.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/theme/ExpressiveComponents.kt) instead of generic `CircularProgressIndicator`.
- **Search**: Use `SearchBar` or `DockedSearchBar` (`androidx.compose.material3.SearchBar`) with `TextFieldState`-backed `SearchBarDefaults.InputField` (Compose 1.13+; value-based `BasicTextField` overloads are deprecated — see [`SearchScreen.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/SearchScreen.kt)).
- **Pull-to-Refresh**: Use Material 3 `PullToRefreshBox` with `PullToRefreshDefaults`.
- **Buttons**: Prefer `FilledTonalButton` or `Button` with theme colors. Avoid hardcoding button background fills.
- **Cards**: Use `Card`, `ElevatedCard`, or `OutlinedCard` with `CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)`.
- **App Bars**: Connect `TopAppBarDefaults.pinnedScrollBehavior()` or `enterAlwaysScrollBehavior()` to list scroll states.

---

## 3. Jetpack Navigation 3 Architecture

The application uses **Jetpack Navigation 3** (`androidx.navigation3`). Legacy Navigation 2 (`NavController`, `NavHost` with XML/Compose string routes) is strictly prohibited.

### Key Architectural Patterns
1. **Immutable Back Stacks**: Navigation state is driven by `NavBackStack<NavKey>`.
2. **Navigation State Holder**: Manage independent stacks per top-level route (`Feed`, `Collections`, `Favorites`, `Search` — see [`Navigator.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/navigation/Navigator.kt)) with an "Exit Through Home" anchor.
3. **Entry-Scoped Lifecycle**:
   - Equip `NavDisplay` with `rememberViewModelStoreNavEntryDecorator()` and `rememberSaveableStateHolderNavEntryDecorator()`.
   - Scope ViewModels with standard Compose `viewModel(key = ...) { ... }` so ViewModel state survives backgrounding and screen rotations.
4. **Predictive Back & Gestures**:
   - Enable `android:enableOnBackInvokedCallback="true"` in `AndroidManifest.xml`.
   - Intercept back events in sub-components (such as active search) with `NavigationBackHandler` and `rememberNavigationEventState` before popping the stack.
5. **Shared Element Transitions**:
   - Wrap navigation with `SharedTransitionLayout`.
   - Thread `LocalNavAnimatedContentScope.current` as `animatedVisibilityScope` to screen composables for hero transitions.

---

## 4. Adaptive Layouts for Foldables & Tablets

Layouts must adapt dynamically across device form factors:
- **Window Size Classes**:
  - `Compact` (< 600dp): Floating tonal `ShortNavigationBar` capsule (`FloatingBottomNavigationBar` in [`FloatNavigationChrome.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/designsystem/FloatNavigationChrome.kt)), single-column or 2-column phone layout.
  - `Medium` (600–839dp) & `Expanded` ($\ge$ 840dp): Start-docked collapsible `WideNavigationRail` (`ExpressiveNavigationRail` with safe-drawing insets), multi-column adaptive grids.
- **Adaptive Grids & Non-Lazy Clusters**: Use `StaggeredGridCells.Adaptive` and column calculation utilities from [`AdaptiveUtils.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/AdaptiveUtils.kt), which read quantized `LocalPaneWidthDp.current` (provided at Navigation 3 pane boundaries via `ProvidePaneWidth`, falling back to `LocalAdaptiveLayoutInfo.current.screenWidthDp`) so column counts and Retina CDN widths adapt accurately inside `ListDetailSceneStrategy` panes. Use stable Compose 1.13 non-lazy `Grid` and `FlexBox` for bounded EXIF bento, topic tile, and tag clusters (see [`PhotoInspectorPane.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/details/PhotoInspectorPane.kt) and [`SearchDiscoveryView.kt`](../../../androidApp/src/main/java/com/example/imagefeed/android/search/SearchDiscoveryView.kt)).
- **Dual-Pane Detail Views**:
  - Medium/Expanded widths & Book posture: Side-by-side dual-pane layout (Left: Photo Canvas; Right: Inspector sidebar with stats, map, EXIF, tags).
  - TableTop posture: Horizontal split (Top: Photo; Bottom: Controls & metadata).
  - Hinge avoidance: Check folding features to avoid bisecting content across the physical crease.

---

## 5. Edge-to-Edge & Sensory Experience

- **Edge-to-Edge**: Invoke `enableEdgeToEdge()` in `MainActivity.onCreate()`. Respect `WindowInsets.safeDrawing`, `Scaffold` inner padding, and system bar insets.
- **Light & Dark Theme Parity**: Never force dark mode unconditionally. Support `isSystemInDarkTheme()` and dynamic color theming (Android 12+), verifying WCAG AA contrast.
- **Haptic Feedback**: Integrate subtle `LocalHapticFeedback.current` on pull-to-refresh activation, filter chip toggles, and photo download completions. Never trigger haptics on passive scrolling.
