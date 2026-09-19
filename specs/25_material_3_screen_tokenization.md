# Specification: Material 3 Screen Tokenization & Component Standards

**Status:** New / Not Implemented

## Overview
Following the introduction of the Material 3 design foundation (`specs/24_material_3_design_foundation.md`), this specification addresses the systematic replacement of hardcoded colors, ad-hoc text sizes, and custom boxes across all Android screens with official Material 3 semantic tokens and components.

Currently, over 60 locations hardcode hex values like `Color(0xFF0F0F11)`, `Color(0xFF1E1E24)`, and `Color(0xFF2C2C35)`, and zero composables use `MaterialTheme.typography` or `MaterialTheme.shapes`.

---

## Scope & Target Screens
The refactoring is executed across six key screen files:
1. `MainActivity.kt` (Editorial Feed, NavigationBar, NavigationRail)
2. `SearchScreen.kt` (Header, tabs, filter bottom sheet, user and collection rows)
3. `PhotoDetailsScreen.kt` (Photo inspector, stats grid, EXIF cards, map card, photographer profile, tag chips)
4. `UserProfileScreen.kt` (Profile header, stats card, portfolio tabs, photo/collection items)
5. `CollectionsFeedScreen.kt` (Collection mosaic cards, curated metadata, user attribution)
6. `CollectionDetailScreen.kt` (Hero header, sticky collapsing app bar gradient, photo grid items)

---

## Detailed Screen-by-Screen Tokenization Plan

### 1. `MainActivity.kt`
*   **NavigationBar & NavigationRail**:
    *   Replace hardcoded container and icon colors with `NavigationBarDefaults.containerColor` (`surfaceContainer`) and standard M3 indicator colors.
    *   Replace hardcoded `tint = Color.White` / `Color.Gray` with `MaterialTheme.colorScheme.onSurface` and `onSurfaceVariant`.
*   **Editorial Top App Bar & Feed Cards**:
    *   Bind `CenterAlignedTopAppBar` container color to `MaterialTheme.colorScheme.surface`.
    *   Set photo card surfaces to `MaterialTheme.colorScheme.surfaceContainer`.
    *   Replace `fontSize = 14.sp`, `fontWeight = FontWeight.Bold` on photographer name with `MaterialTheme.typography.titleSmall`.
    *   Replace `PhotoGridSkeleton` shimmer base/highlight colors with `surfaceContainerLow` and `surfaceContainerHigh`.

### 2. `SearchScreen.kt`
*   **Search Filter Bottom Sheet (`SearchFiltersSheet`)**:
    *   Replace `ModalBottomSheet(containerColor = Color(0xFF1E1E24))` with `containerColor = MaterialTheme.colorScheme.surfaceContainerHigh`.
    *   Replace hardcoded `FilterChipDefaults.filterChipColors(containerColor = Color(0xFF2C2C35))` with M3 defaults using `secondaryContainer` and `surfaceContainerHighest`.
    *   Replace hardcoded filter headers and section titles with `MaterialTheme.typography.titleSmall` and `bodyMedium`.
*   **Search Results Cards**:
    *   Refactor `UserRowCard` and `CollectionRowCard` cards from `containerColor = Color(0xFF1E1E24)` to `MaterialTheme.colorScheme.surfaceContainer`.
    *   Replace hardcoded text typography with `MaterialTheme.typography.titleMedium` (names/titles) and `MaterialTheme.typography.bodySmall` (subtitles/counts).

### 3. `PhotoDetailsScreen.kt`
*   **Photo Tag Chips**:
    *   Replace custom `Box(modifier = Modifier.background(Color(0xFF22222A), RoundedCornerShape(16.dp)).clickable { ... })` with standard Material 3 `SuggestionChip`:
        ```kotlin
        SuggestionChip(
            onClick = { onTagClick(tag.title) },
            label = {
                Text(
                    text = tag.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                )
            },
            shape = MaterialTheme.shapes.small,
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            border = null,
        )
        ```
*   **Inspector Cards & EXIF / Map**:
    *   Refactor photographer card, EXIF metadata grid, and Google Map wrapper card from `Color(0xFF1E1E24)` to `MaterialTheme.colorScheme.surfaceContainer`.
    *   Replace download trigger button colors `ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)` with `FilledTonalButton` or `Button` using `MaterialTheme.colorScheme.primary` and `onPrimary`.
    *   Standardize stats numbers and labels with `MaterialTheme.typography.headlineSmall` and `MaterialTheme.typography.labelMedium`.

### 4. `UserProfileScreen.kt`
*   **Profile Header & Stats Card**:
    *   Refactor user stats summary card from `Color(0xFF1E1E24)` to `MaterialTheme.colorScheme.surfaceContainer`.
    *   Use `MaterialTheme.typography.headlineMedium` for user display name and `MaterialTheme.typography.bodyMedium` for user bio.
    *   Replace secondary tab row colors with `MaterialTheme.colorScheme.surface` and `primary`.

### 5. `CollectionsFeedScreen.kt` & `CollectionDetailScreen.kt`
*   **Collection Mosaic Cards**:
    *   Update card background to `MaterialTheme.colorScheme.surfaceContainer`.
    *   Update empty mosaic placeholder slots from `Color(0xFF2C2C35)` to `MaterialTheme.colorScheme.surfaceContainerHighest`.
    *   Update photo count pills and curated badges to `MaterialTheme.typography.labelSmall`.
*   **CollectionDetail Header**:
    *   Replace hardcoded collapse gradient `Color(0xEE0F0F11)` with `MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)`.

---

## Verification & Acceptance Criteria
1. **Zero Hardcoded Design Tokens**: Zero remaining occurrences of `Color(0xFF0F0F11)`, `Color(0xFF1E1E24)`, and `Color(0xFF2C2C35)` in UI layouts.
2. **Typography Compliance**: All screen texts utilize `MaterialTheme.typography` styles, ensuring dynamic accessibility font scaling functions properly.
3. **Chip Standards**: All photo and search tags render using Material 3 `SuggestionChip` / `FilterChip`.
4. **Platform Builds**: Verify `:androidApp:assembleDebug` and run `./gradlew ktlintCheck detekt`.
5. **No Regressions on iOS/Shared**: Ensure no shared data models or presenter APIs are inadvertently modified.