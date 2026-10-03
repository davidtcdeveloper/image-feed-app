# Specification: Android Navigation 3 Adaptive List-Detail Scenes Architecture

**Status:** Approved

## 1. Overview & Objectives

The application currently fulfills tablet and foldable responsiveness at the individual screen level ([Spec 23](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/23_adaptive_layout_tablets_foldables.md)):
- `AdaptiveLayoutInfo` dynamically swaps between bottom `NavigationBar` (< 600dp) and start-docked `NavigationRail` (>= 600dp).
- Grids scale from 2 to 5 columns.
- `PhotoDetailsScreen` embeds an internal dual-pane split on screens >= 600dp (Left: zoomable photo canvas; Right: inspector sidebar with stats, EXIF, and Google Map).

However, at the **navigation level**, transitions remain single-pane. When a user on an Expanded tablet (e.g., Pixel Tablet or Samsung Galaxy Tab S9, width >= 840dp) taps a photo in the feed, `NavDisplay` replaces the entire feed with the detail screen.

As prescribed by `AGENTS.md` ("Side-by-side split panes should be used for detailed views on large screens") and the official Navigation 3 **Material List-Detail Recipe**, Navigation 3 introduces `ListDetailSceneStrategy` (`androidx.compose.material3.adaptive.navigation3`). This allows `NavDisplay` to display multiple back-stack entries concurrently across adaptive panes.

This specification outlines the architecture, pane role modeling, and integration strategy to elevate Android navigation to adaptive multi-pane scenes.

---

## 2. Technical Architecture & Design Considerations

```mermaid
flowchart TD
    subgraph Compact Screen "< 600dp (Phone)"
        PhoneFeed["List Pane (Feed / Collections)"]
        PhoneDetail["Detail Pane (PhotoDetails)"]
        PhoneFeed -.->|Tap Photo (Pushes Detail)| PhoneDetail
    end

    subgraph Expanded Screen ">= 840dp (Tablet / Foldable Flat)"
        subgraph Split Scene
            TabList["List Pane (Feed Grid)"]
            TabDetail["Detail Pane (Photo Canvas + Inspector)"]
        end
    end
```

### A. Material 3 Adaptive Navigation 3 Dependency
To utilize `ListDetailSceneStrategy`, the project requires the Material 3 Adaptive Navigation 3 integration library:
- Artifact: `androidx.compose.material3.adaptive:adaptive-navigation3`
- Catalog Entry:
  ```toml
  androidx-material3-adaptive-navigation3 = { group = "androidx.compose.material3.adaptive", name = "adaptive-navigation3", version = "1.1.0-beta01" }
  ```

### B. Defining Pane Roles via Metadata
Navigation 3 assigns pane roles to destinations through `NavEntry` metadata:
1. **Primary List Pane (`ListDetailSceneStrategy.listPane`)**:
   - `AppRoute.Feed`: Primary list pane displaying the photo feed.
   - `AppRoute.Collections`: Primary list pane displaying curated collections.
   - `detailPlaceholder`: Composable rendered in the secondary pane on wide displays when no detail route is present in the back stack.
2. **Secondary Detail Pane (`ListDetailSceneStrategy.detailPane`)**:
   - `AppRoute.PhotoDetails`: Detail pane displaying the full photo view and metadata.
   - `AppRoute.CollectionDetails`: Detail pane displaying the collection items when opened from the collections list.
3. **Tertiary / Extra Pane (`ListDetailSceneStrategy.extraPane`)**:
   - `AppRoute.UserProfile`: Opened as a supporting inspector pane on very wide displays (> 1200dp) when viewing a photo, or layered as an overlay.

### C. Directive & Partition Configuration
In wide window sizes, horizontal partition spacing can be adjusted to match Material Design token standards:
```kotlin
val windowAdaptiveInfo = currentWindowAdaptiveInfo()
val directive = remember(windowAdaptiveInfo) {
    calculatePaneScaffoldDirective(windowAdaptiveInfo)
        .copy(horizontalPartitionSpacerSize = 0.dp)
}
val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
```

### D. Detail Placeholder Specification
When an Expanded tablet is in dual-pane mode and the back stack contains only the list route (`[AppRoute.Feed]`), the right pane displays a dedicated placeholder:
- A subtly styled Material 3 surface (`surfaceContainerLowest`).
- Centered iconography (`Icons.Outlined.PhotoLibrary`).
- Informative message: *"Select a photo to view high-resolution imagery, EXIF telemetry, and photographer insights."*

### E. Coexistence with Internal Dual-Pane in PhotoDetails
On tablets, `PhotoDetailsScreen` already possesses a dual-pane mode (canvas + sidebar). When embedded into a multi-pane `NavDisplay` scene alongside the Feed:
- On **Medium screens (600–840dp)**: Only one pane is shown at a time (Feed OR PhotoDetails with its internal split).
- On **Expanded screens (840–1200dp)**: The Feed takes 40% width, and PhotoDetails takes 60% width. `PhotoDetailsScreen` collapses into its single-column vertical layout to avoid recursive three-pane crowding.
- On **Ultra-wide displays (>= 1200dp)**: Full 3-pane representation can be enabled.

---

## 3. Implementation Plan & Execution Checklist

- [ ] **Phase 1: Dependency Integration**
  - [ ] Add `androidx-material3-adaptive-navigation3` to `gradle/libs.versions.toml` and `androidApp/build.gradle.kts`.
  - [ ] Verify clean compilation.

- [ ] **Phase 2: Strategy Definition & Pane Roles**
  - [ ] Configure `rememberListDetailSceneStrategy` with custom partition directive.
  - [ ] Annotate `AppRoute.Feed` and `AppRoute.Collections` entries with `ListDetailSceneStrategy.listPane()`.
  - [ ] Design and implement `PhotoDetailPlaceholder` composable for empty detail states.
  - [ ] Annotate `AppRoute.PhotoDetails` with `ListDetailSceneStrategy.detailPane()`.
  - [ ] Annotate `AppRoute.CollectionDetails` with `ListDetailSceneStrategy.detailPane()`.

- [ ] **Phase 3: Responsive Pane Density Adaptation**
  - [ ] Update `PhotoDetailsScreen` to query parent available width before triggering its internal dual-pane split, preventing triple-pane clipping.
  - [ ] Ensure shared element transitions continue to coordinate smoothly across list-to-detail pane layouts.

- [ ] **Phase 4: Full Multi-Platform Verification**
  - [ ] Verify tablet emulator behavior (landscape 10" tablet, portrait foldable).
  - [ ] Verify mobile phone single-pane back-stack operation is completely preserved.
  - [ ] Run `./gradlew ktlintCheck detekt`.
  - [ ] Run `./gradlew :androidApp:compileDebugKotlin`.
  - [ ] Run `./gradlew :androidApp:assembleDebug`.
