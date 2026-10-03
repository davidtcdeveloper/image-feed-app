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

---

## 2. Component Guidelines

- **Chips**: Use official Material 3 `FilterChip`, `SuggestionChip`, `AssistChip`, or `InputChip`. Never construct custom clickable `Box` layouts for tags or filter options.
- **Search**: Use `SearchBar` or `DockedSearchBar` (`androidx.compose.material3.SearchBar`) with search animations and suggestion content.
- **Pull-to-Refresh**: Use Material 3 `PullToRefreshBox` with `PullToRefreshDefaults`.
- **Buttons**: Prefer `FilledTonalButton` or `Button` with theme colors. Avoid hardcoding button background fills.
- **Cards**: Use `Card`, `ElevatedCard`, or `OutlinedCard` with `CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)`.
- **App Bars**: Connect `TopAppBarDefaults.pinnedScrollBehavior()` or `enterAlwaysScrollBehavior()` to list scroll states.

---

## 3. Jetpack Navigation 3 Architecture

The application uses **Jetpack Navigation 3** (`androidx.navigation3`). Legacy Navigation 2 (`NavController`, `NavHost` with XML/Compose string routes) is strictly prohibited.

### Key Architectural Patterns
1. **Immutable Back Stacks**: Navigation state is driven by `NavBackStack<NavKey>`.
2. **Navigation State Holder**: Manage independent stacks per top-level route (`Feed`, `Collections`, `Search`) with an "Exit Through Home" anchor.
3. **Entry-Scoped Lifecycle**:
   - Equip `NavDisplay` with `rememberViewModelStoreNavEntryDecorator()` and `rememberSaveableStateHolderNavEntryDecorator()`.
   - Use `rememberEntryPresenter` so presenter state survives backgrounding and screen rotations.
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
  - `Compact` (< 600dp): Bottom `NavigationBar`, single-column or 2-column phone layout.
  - `Medium` (600–839dp) & `Expanded` ($\ge$ 840dp): Start-docked `NavigationRail`, multi-column adaptive grids.
- **Adaptive Grids**: Use `StaggeredGridCells.Adaptive` and column calculation utilities from `AdaptiveUtils.kt`. Calculate image CDN widths from container column widths instead of screen dimensions.
- **Dual-Pane Detail Views**:
  - Medium/Expanded widths & Book posture: Side-by-side dual-pane layout (Left: Photo Canvas; Right: Inspector sidebar with stats, map, EXIF, tags).
  - TableTop posture: Horizontal split (Top: Photo; Bottom: Controls & metadata).
  - Hinge avoidance: Check folding features to avoid bisecting content across the physical crease.

---

## 5. Edge-to-Edge & Sensory Experience

- **Edge-to-Edge**: Invoke `enableEdgeToEdge()` in `MainActivity.onCreate()`. Respect `WindowInsets.safeDrawing`, `Scaffold` inner padding, and system bar insets.
- **Light & Dark Theme Parity**: Never force dark mode unconditionally. Support `isSystemInDarkTheme()` and dynamic color theming (Android 12+), verifying WCAG AA contrast.
- **Haptic Feedback**: Integrate subtle `LocalHapticFeedback.current` on pull-to-refresh activation, filter chip toggles, and photo download completions. Never trigger haptics on passive scrolling.
