# Android Material 3 Design Rules

## Purpose
Enforce strict Material 3 (Material You) design standards across the Android application (`androidApp`). This rule inherits and implements the cross-platform standards defined in `ai-rules/design-principles.md`. All new Compose UI and modifications to existing screens MUST adhere to these rules.

---

## 1. Zero Hardcoded Design Tokens Policy

### Never Use Raw Colors
*   **PROHIBITED**: `Color(0xFF...)`, `Color.White`, `Color.Black`, `Color.Gray`, `Color.LightGray`, or ad-hoc RGB colors in UI layouts.
*   **MANDATORY**: Reference `MaterialTheme.colorScheme.*` for all container backgrounds, text colors, icons, borders, and dividers.
*   **Exception**: Domain-specific content colors (such as Unsplash photographer badge colors or specific camera sensor colors) defined as explicit domain color sets.

### Semantic Surface Containers
Material 3 uses tonal surface elevation instead of drop shadows or arbitrary shades of grey:
*   `MaterialTheme.colorScheme.surface`: Root screen background and base surface.
*   `MaterialTheme.colorScheme.surfaceContainerLowest`: Deepest background layer behind nested cards.
*   `MaterialTheme.colorScheme.surfaceContainerLow`: Secondary canvas backing.
*   `MaterialTheme.colorScheme.surfaceContainer`: Default for standard cards (`PhotoCard`, `CollectionCard`, `UserRowCard`).
*   `MaterialTheme.colorScheme.surfaceContainerHigh`: Elevated modals, bottom sheets, filter dialogs.
*   `MaterialTheme.colorScheme.surfaceContainerHighest`: Interactive inputs, search bars, active filter chips, tag pills.

---

## 2. Typography Standards

*   **PROHIBITED**: Ad-hoc font sizes (e.g. `fontSize = 14.sp`, `fontSize = 20.sp`, `fontWeight = FontWeight.Bold`) directly on `Text` composables.
*   **MANDATORY**: Apply `style = MaterialTheme.typography.*`:
    *   `displayLarge` / `displayMedium` / `displaySmall`: Hero collection banners and splash displays.
    *   `headlineLarge` / `headlineMedium` / `headlineSmall`: Screen titles and major section headers.
    *   `titleLarge` / `titleMedium` / `titleSmall`: Card headers, photographer names, dialog headings.
    *   `bodyLarge` / `bodyMedium` / `bodySmall`: Photo descriptions, user biographies, metadata paragraphs.
    *   `labelLarge` / `labelMedium` / `labelSmall`: Button labels, chip texts, tags, navigation captions.
*   **Why**: Respects user device-level accessibility font scaling and ensures visual consistency.

---

## 3. Shape Standards

*   **PROHIBITED**: Ad-hoc `RoundedCornerShape(16.dp)`, `RoundedCornerShape(18.dp)` scattered randomly in composables.
*   **MANDATORY**: Use semantic shapes from `MaterialTheme.shapes.*`:
    *   `extraSmall` (`4.dp`): Badges, tooltips.
    *   `small` (`8.dp`): Chips, tag pills, inline map views.
    *   `medium` (`12.dp`): Standard photo cards, mosaic items.
    *   `large` (`16.dp`): Dialogs, inspector panels, user summary cards.
    *   `extraLarge` (`28.dp`): Modal bottom sheets, search bars, pill buttons.

---

## 4. Component Standards

*   **Chips**: Use official `FilterChip`, `SuggestionChip`, `AssistChip`, or `InputChip`. NEVER construct custom `Box(modifier = Modifier.background(...).clickable { ... })` for tags or filter options.
*   **Search**: Use Material 3 `SearchBar` or `DockedSearchBar` (`androidx.compose.material3.SearchBar`) with built-in search animations and suggestions content.
*   **Pull-to-Refresh**: Use Material 3 `PullToRefreshBox` with `PullToRefreshDefaults` for list and grid refreshes.
*   **Buttons**: Prefer `FilledTonalButton` or standard `Button` with theme-derived colors. Avoid hardcoding button background colors to pure white or black.
*   **Cards**: Use `Card`, `ElevatedCard`, or `OutlinedCard` with `CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)`.
*   **App Bars**: Connect `TopAppBarDefaults.pinnedScrollBehavior()` or `enterAlwaysScrollBehavior()` to list scroll states.

---

## 5. Platform Integration & Modern UX

*   **Edge-to-Edge**: The app must call `enableEdgeToEdge()` in `MainActivity.onCreate()`. All screens must respect `WindowInsets.safeDrawing`, `Scaffold` inner padding, or status/navigation bar insets.
*   **Dynamic Color**: The UI must render legibly and with high contrast under both Dynamic Color (Android 12+ wallpaper palettes) and fallback custom palettes.
*   **Light & Dark Theme**: Never force dark mode unconditionally. Support `isSystemInDarkTheme()` and verify that light theme maintains proper text/surface contrast ratios.
*   **Haptics**: Incorporate subtle haptic feedback (`LocalHapticFeedback.current`) on significant interactions: pull-to-refresh activation, filter selection toggles, and photo download triggers.
