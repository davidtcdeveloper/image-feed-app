# Specification: Material 3 Screen Tokenization & Component Standards

**Status:** Implemented

## Overview
Following the introduction of the Material 3 design foundation (`specs/24_material_3_design_foundation.md`), this specification addresses the systematic replacement of hardcoded colors, ad-hoc text sizes, and custom boxes across all Android screens with official Material 3 semantic tokens and components.

This spec covers complete tokenization across all six primary Android screen files, establishing strict typography scale compliance (`MaterialTheme.typography`), semantic shape scaling (`MaterialTheme.shapes`), surface elevation tiers (`surfaceContainerLowest` through `surfaceContainerHighest`), and explicit policies for on-media scrim text and domain color swatches.

---

## Cross-Cutting Policies & Exceptions

### 1. On-Media Typography & Scrim Policy
*   **Context:** Photographic cards and headers (`PhotoCard`, `CollectionMosaicCard`, `CollectionRowCard`, `UserCollectionCard`, `RelatedCollectionCard`, and the compact photo inspector) render text directly over photographic images using dark gradient scrims (`Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = ...)))`).
*   **Policy:** 
    *   Do **NOT** use `MaterialTheme.colorScheme.onSurface` for text placed over dark image scrims, as `onSurface` resolves to dark slate (`0xFF1A1C1E`) in Light Theme and becomes invisible.
    *   On-scrim primary text (titles, photographer names) is standardized to `Color.White` (or dedicated on-media token).
    *   On-scrim secondary text (counts, handles, subtitles) is standardized to `Color.White.copy(alpha = 0.8f)` (or dedicated on-media variant token).
    *   This is documented as an approved media-overlay exception per Material 3 guidelines for full-bleed photography.
    *   *Note on Cross-Platform Parity:* While Android uses this approved Material 3 photographic dark gradient scrim for feed cards, iOS/macOS utilizes an inset floating frosted glass capsule (`.glassCapsule(style: .ultraThin)` per Spec 29 & Spec 33) to adhere to Apple HIG translucency standards. Both platforms route photographer attribution to the in-app user profile.

### 2. Domain-Specific Content Color Exception
*   **Search Filter Swatches:** In `SearchScreen.kt`, the `colorHexes` map defining Unsplash photo color filters (`"yellow"` -> `Color(0xFFFFEB3B)`, `"blue"` -> `Color(0xFF2196F3)`, etc.) represents photographic domain content rather than UI theme tokens.
*   **Policy:** Approved domain-specific content color exception under `ai-rules/material-design.md`.

### 3. Component Standards
*   **Filter Chips:** Standard outlined Material 3 chips via default `FilterChipDefaults.filterChipColors()`. Do not force opaque container backgrounds on unselected chips.
*   **Tag Chips:** Official Material 3 `SuggestionChip` with `MaterialTheme.shapes.small` and `MaterialTheme.typography.labelSmall`.
*   **Typography Scale:** 100% replacement of ad-hoc `fontSize = ...sp` with `MaterialTheme.typography.*` tokens across all screens.

---

## Detailed Screen-by-Screen Tokenization Plan

### 1. `MainActivity.kt`
*   **NavigationBar & NavigationRail**:
    *   Use default container colors (`surfaceContainer`) and standard M3 indicator colors.
    *   Ensure icon and label tints resolve to `MaterialTheme.colorScheme.onSurface` and `onSurfaceVariant`.
*   **Editorial Top App Bar (`CenterAlignedTopAppBar`)**:
    *   Container color bound to `MaterialTheme.colorScheme.surface`.
    *   Title styled with `MaterialTheme.typography.titleMedium` (`FontWeight.Bold`, `letterSpacing = 2.sp`) and `onSurface`.
    *   Action icons bound to `MaterialTheme.colorScheme.onSurface`.
    *   Category tab row: `SecondaryScrollableTabRow` with container `surface`, content `primary`, and tab text `MaterialTheme.typography.labelLarge`.
*   **Feed Cards (`PhotoCard`)**:
    *   Card surface set to `MaterialTheme.colorScheme.surfaceContainer` with `MaterialTheme.shapes.medium`.
    *   Photographer attribution name: Standardize to `MaterialTheme.typography.titleSmall` (replacing `labelMedium`), on-scrim `Color.White`.
*   **Feed Shimmer Skeleton (`PhotoGridSkeleton`)**:
    *   Shimmer base and highlight colors bound to `MaterialTheme.colorScheme.surfaceContainerLow` and `surfaceContainerHigh`.

### 2. `SearchScreen.kt`
*   **Search Header & Input (`TextField`)**:
    *   Container color set to `MaterialTheme.colorScheme.surfaceContainerHighest`.
    *   Text color set to `MaterialTheme.colorScheme.onSurface`.
    *   Placeholder styled with `MaterialTheme.typography.bodyMedium` and `MaterialTheme.colorScheme.onSurfaceVariant`.
    *   Shape bound to `MaterialTheme.shapes.extraLarge`.
    *   Tab row: `SecondaryTabRow` with container `surface` and indicator/content `primary`.
*   **Search Filter Bottom Sheet (`SearchFiltersSheet`)**:
    *   `ModalBottomSheet` container set to `MaterialTheme.colorScheme.surfaceContainerHigh`, content `onSurface`.
    *   Headers & section titles: `MaterialTheme.typography.titleMedium` (header) and `MaterialTheme.typography.labelSmall` (`FontWeight.Bold`, `letterSpacing = 1.sp`, `onSurfaceVariant`).
    *   Filter Chips: Standard outlined M3 chips with `FilterChipDefaults.filterChipColors()`.
    *   Color swatches: Exempt domain color map `colorHexes`.
*   **Search Results Cards**:
    *   `UserRowCard`:
        *   Container `MaterialTheme.colorScheme.surfaceContainer`, shape `MaterialTheme.shapes.medium`.
        *   User name: `MaterialTheme.typography.titleMedium` (`FontWeight.Bold`, `onSurface`).
        *   Handle (`@username`): `MaterialTheme.typography.bodySmall` (`onSurfaceVariant`).
        *   Avatar placeholder background: `MaterialTheme.colorScheme.surfaceContainerHighest`.
    *   `CollectionRowCard`:
        *   Container `MaterialTheme.colorScheme.surfaceContainer`, shape `MaterialTheme.shapes.medium`.
        *   Collection title: `MaterialTheme.typography.titleMedium` (`FontWeight.Bold`, `letterSpacing = 1.sp`, on-scrim `Color.White`), replacing hardcoded `fontSize = 16.sp`.
        *   Curator & photo count: `MaterialTheme.typography.bodySmall` (on-scrim `Color.White.copy(alpha = 0.8f)`), replacing hardcoded `fontSize = 12.sp` and `Color.LightGray`.

### 3. `PhotoDetailsScreen.kt`
*   **Top App Bar (`CenterAlignedTopAppBar`)**:
    *   Container color bound to `MaterialTheme.colorScheme.surface`.
    *   Title: `MaterialTheme.typography.titleMedium` (`FontWeight.Bold`, `letterSpacing = 2.sp`, color `MaterialTheme.colorScheme.onSurface`), eliminating hardcoded `Color.White` and `fontSize = 16.sp` to ensure Light Theme legibility.
    *   Back button and share icons: Tint bound to `MaterialTheme.colorScheme.onSurface`, removing hardcoded `Color.White`.
*   **Mobile Canvas Photographer Floating Pill**:
    *   Shape: `MaterialTheme.shapes.extraLarge` (replacing `RoundedCornerShape(24.dp)`).
    *   Container: `Color.Black.copy(alpha = 0.6f)`.
    *   Photographer name: `MaterialTheme.typography.labelLarge` (`FontWeight.Bold`, on-scrim `Color.White`), replacing `fontSize = 14.sp`.
    *   Photographer username: `MaterialTheme.typography.labelSmall` (on-scrim `Color.White.copy(alpha = 0.8f)`), replacing `fontSize = 11.sp`.
*   **Dual-Pane Inspector Photographer Row**:
    *   Photographer name: `MaterialTheme.typography.titleSmall` (`FontWeight.Bold`, on-scrim `Color.White`), replacing `fontSize = 15.sp`.
    *   Photographer username: `MaterialTheme.typography.bodySmall` (on-scrim `Color.White.copy(alpha = 0.8f)`), replacing `fontSize = 12.sp`.
*   **Photo Tag Chips**:
    *   Official Material 3 `SuggestionChip`:
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
*   **Inspector Cards, EXIF & Map**:
    *   Photographer card, EXIF metadata grid, and Google Map wrapper card set to `MaterialTheme.colorScheme.surfaceContainer` with `MaterialTheme.shapes.medium`.
    *   Download action button: `Button` with `MaterialTheme.colorScheme.primary` and `onPrimary`, label `MaterialTheme.typography.labelLarge`.
    *   Stats numbers and labels: Standardize to `MaterialTheme.typography.headlineSmall` and `MaterialTheme.typography.labelMedium`.
    *   Google Map inline container: `MaterialTheme.shapes.small`.

### 4. `UserProfileScreen.kt`
*   **Profile Header & Stats Card**:
    *   User stats summary card: `MaterialTheme.colorScheme.surfaceContainer` with `MaterialTheme.shapes.medium`.
    *   User display name: `MaterialTheme.typography.headlineMedium`.
    *   User biography: `MaterialTheme.typography.bodyMedium`.
    *   Stats count values: `MaterialTheme.typography.titleMedium` (`FontWeight.Bold`).
    *   Stats count labels: `MaterialTheme.typography.bodySmall` (`onSurfaceVariant`).
    *   Portfolio secondary tab row: container `MaterialTheme.colorScheme.surface`, content `primary`.
*   **User Collections Tab (`UserCollectionCard`)**:
    *   Card container: `MaterialTheme.colorScheme.surfaceContainer`, shape `MaterialTheme.shapes.medium`.
    *   Collection title: `MaterialTheme.typography.titleMedium` (`FontWeight.Bold`, on-scrim `Color.White`), replacing hardcoded `fontSize = 16.sp`.
    *   Curator / photo count subtitle: `MaterialTheme.typography.bodySmall` (on-scrim `Color.White.copy(alpha = 0.8f)`), replacing hardcoded `fontSize = 12.sp` and `Color.LightGray`.

### 5. `CollectionsFeedScreen.kt` & `CollectionDetailScreen.kt`
*   **`CollectionsFeedScreen.kt` (Collection Mosaic Cards)**:
    *   Card background: `MaterialTheme.colorScheme.surfaceContainer`, shape `MaterialTheme.shapes.medium`.
    *   Empty mosaic thumbnail placeholders: `MaterialTheme.colorScheme.surfaceContainerHighest`.
    *   Collection title: `MaterialTheme.typography.titleMedium` (`FontWeight.Bold`, `onSurface`).
    *   Collection description: `MaterialTheme.typography.bodySmall` (`onSurfaceVariant`).
    *   Curator attribution text: `MaterialTheme.typography.labelSmall` and `labelMedium`.
    *   Total photo count pill: background `MaterialTheme.colorScheme.surfaceContainerHighest`, text `MaterialTheme.typography.labelSmall`.
*   **`CollectionDetailScreen.kt` (Header & Related Collections)**:
    *   Collapsing TopAppBar gradient / container: `MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)`.
    *   Collection title: `MaterialTheme.typography.displaySmall` (tablet/foldable) / `MaterialTheme.typography.headlineMedium` (compact).
    *   Collection description: `MaterialTheme.typography.bodyMedium`.
    *   `RelatedCollectionCard`:
        *   Card container: `MaterialTheme.colorScheme.surfaceContainer`, shape `MaterialTheme.shapes.small`.
        *   Title: `MaterialTheme.typography.titleSmall` (`FontWeight.Bold`, on-scrim `Color.White`), replacing hardcoded `fontSize = 12.sp`.
        *   Photo count: `MaterialTheme.typography.labelSmall` (on-scrim `Color.White.copy(alpha = 0.8f)`), replacing hardcoded `fontSize = 10.sp` and `Color.LightGray`.

---

## Verification & Acceptance Criteria
1. **Zero Hardcoded Design Tokens**: Zero remaining raw hex colors (`Color(0xFF...)`) in UI layouts, with explicit exceptions limited to domain color filter swatches and on-media photographic scrim overlays.
2. **100% Typography Scale Compliance**: Zero occurrences of ad-hoc `fontSize = ...sp` across all six screen files. All text elements utilize `MaterialTheme.typography` scale styles.
3. **Chip Standards**: All photo tags render using Material 3 `SuggestionChip`; search filters use standard outlined `FilterChip`.
4. **Theme & Contrast Integrity**: Top app bars, cards, and text maintain compliant contrast ratios in both Light and Dark themes.
5. **Platform Builds**: Verify `:androidApp:assembleDebug` and `./gradlew ktlintCheck detekt`.
6. **No Shared Regressions**: Ensure zero modifications to shared KMP models or presenter APIs.