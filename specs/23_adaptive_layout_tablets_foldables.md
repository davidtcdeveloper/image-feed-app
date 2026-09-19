# Implementation Step 25: Adaptive Layouts for Android Tablets, Foldables & iPads

This specification outlines the UI responsive layout strategies to support large screen form factors (Android tablets, Chromebooks, foldable devices, and Apple iPads). It transitions mobile-first layouts into highly adaptive, screen-aware, and posture-aware native interfaces.

---

## 1. Responsive Screen Class & Posture Definitions

To support diverse form factors, the applications adapt to both physical width breakpoints and physical hardware postures:

### A. Width Breakpoints (Window Size Classes)

| Screen Width (Dp) | Size Class | Device Form Factor | Navigation Shell | Photo Grid Columns | Collection Columns |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **< 600 Dp** | Compact | Phones, Folded Cover Displays | Bottom `NavigationBar` | 2 Columns | 1 Column |
| **600 Dp to 840 Dp** | Medium | Unfolded Foldables (Portrait), Small Tablets | Start `NavigationRail` | 3 Columns | 2 Columns |
| **> 840 Dp** | Expanded | Large Tablets, Unfolded Foldables (Landscape) | Start `NavigationRail` | 4 to 5 Columns | 2 to 3 Columns |

### B. Foldable Postures (Jetpack WindowManager)

| Posture | Hinge Orientation | Device State | Layout Strategy |
| :--- | :--- | :--- | :--- |
| **Normal / Flat** | N/A | Fully flat or standard tablet | Standard dual-pane split or multi-column grid |
| **Book Posture** | Vertical | Half-opened ($\sim 90^\circ - 120^\circ$) | **Vertical Split**: Left pane for media canvas, Right pane for metadata/inspector; hinge area excluded |
| **TableTop Posture** | Horizontal | Half-opened ($\sim 90^\circ - 120^\circ$) | **Horizontal Split**: Top screen for photo viewer canvas; Bottom screen for controls, stats, and map deck; hinge area excluded |

---

## 2. Android Jetpack Compose Implementation (`androidApp`)

### A. Adaptive Navigation Architecture
On screens with width $< 600\text{dp}$, the app displays the standard bottom `NavigationBar`.
On screens with width $\ge 600\text{dp}$ (Tablets & Unfolded Foldables), the bottom bar is replaced with a start-docked **`NavigationRail`**:
- Vertical item stack: `Photos`, `Collections`, `Search`.
- Header / Top action: Randomizer shake button or app glyph.
- Frees $\sim 80\text{dp}$ of vertical real estate on landscape tablets while keeping navigation within natural thumb reach.

### B. Uniform Multi-Column Grids
1. **Editorial Feed (`MainActivity.kt`)**:
   - `calculateGridColumns()` adapts from 2 columns (<600dp) to 3 (600-840dp) and 4-5 (>840dp).
   - Image resolution request fix: Replace hardcoded `screenWidthDp / 2` with `(screenWidthDp / columnCount).dp.roundToPx()` via `calculatePhotoItemWidthPx()` to download appropriately sized CDN images without bandwidth or GPU waste.
2. **Curated Collections (`CollectionsFeedScreen.kt`)**:
   - Migrate from `LazyColumn` (which causes wide horizontal card stretching on tablets) to `LazyVerticalStaggeredGrid` with `calculateCollectionGridColumns()`:
     - 1 column on Compact (<600dp).
     - 2 columns on Medium (600-900dp).
     - 2 to 3 columns on Expanded (>900dp).
3. **Search Results (`SearchScreen.kt`)**:
   - Migrate Users and Collections results from `StaggeredGridCells.Fixed(1)` to adaptive column layouts (`Adaptive(280.dp)` for users, `Adaptive(320.dp)` for collections).
   - Constrain filter `ModalBottomSheet` width on wide screens (`maxWidth = 560.dp`).
4. **User Profile (`UserProfileScreen.kt`)**:
   - Header adopts a balanced horizontal presentation on expanded screens with constrained width (`maxWidth = 680.dp`).
   - Collections tab switches to adaptive multi-column grid (`calculateCollectionGridColumns()`).

### C. Photo Details: Responsive Dual-Pane & Posture-Aware Canvas
For `PhotoDetailsScreen.kt`:
1. **Compact (<600dp)**: Preserves existing single-column vertical scroll.
2. **Medium / Expanded ($\ge 600\text{dp}$) & Book Posture**:
   - Side-by-side **Dual-Pane** layout:
     - **Left Pane (55-60% width)**: High-resolution photo canvas with interactive pinch-zoom, pan gestures, and floating photographer attribution badge.
     - **Right Pane (40-45% width)**: Scrollable sidebar with photographer bio, download button, stats summary, historical views Canvas chart, dark-themed Google Map, EXIF camera specs, and clickable tags.
   - **Hinge Awareness**: When in Book posture with a vertical separating hinge, the panes align strictly to each side of the hinge boundary (`FoldingFeature.bounds`), preventing visual distortion across the physical fold.
3. **TableTop Posture**:
   - **Top Pane**: Immersive photo display centered above the horizontal fold.
   - **Bottom Pane**: Interactive control deck (download action, analytics chart, map, camera specs) resting flat on the table surface.
   - **Hinge Awareness**: Hinge height excluded to prevent content rendering inside the physical fold crease.

---

## 3. iOS SwiftUI Implementation (`iosApp`)

In SwiftUI, grids divide items dynamically based on the current horizontal size class (`UserInterfaceIdiom` or `horizontalSizeClass`).

### A. Dynamic N-Column Division
Calculate column partition count dynamically using `AdaptiveLayoutHelper`:
*   `ContentView.swift` (Main Photos Feed tab)
*   `SearchView.swift` (Search results waterfall layout)
*   `UserProfileView.swift` (UserProfile custom segmented feeds)
*   `CollectionDetailView.swift` (Collections photos feed)

### B. Curated Collections Adaptive Grid
In `CollectionsFeedView.swift`:
*   Replace single-column `LazyVStack` with an adaptive 2-to-3 column grid on iPad / Mac desktop using `LazyVGrid` or dynamic column partition.

### C. Adaptive Navigation Split View & Photo Details on iPad
*   Leverage `NavigationSplitView` or dual-pane presentation on iPad regular width.
*   Dual-pane photo details for iPad landscape: left pane photo canvas with interactive zoom, right pane inspector sidebar.

---

## 4. Verification & Responsive Checks

*   [x] **Adaptive Grid Breakpoints:** Verified column calculations for Compact (1-2), Medium (2-3), and Expanded (3-5).
*   [x] **Navigation Shell Adaptation:** Bottom NavigationBar on Compact (<600dp), docked NavigationRail with quick-action header on Medium/Expanded (>=600dp).
*   [x] **Foldable Book Posture Support:** Dual-pane split respecting the vertical crease bounds (`FoldingFeature.bounds.width()`).
*   [x] **Foldable TableTop Posture Support:** Top photo viewport and bottom metadata/controls deck with horizontal fold crease exclusion (`FoldingFeature.bounds.height()`).
*   [x] **Accurate CDN Image Sizing:** Verified `calculatePhotoItemWidthPx()` scales image width dynamically based on active column count.
*   [x] **iOS Adaptive Grid & Dual-Pane:** Curated Collections multi-column support and iPad dual-pane photo inspector layout.
*   [x] **Build & Static Analysis:** Verified `:androidApp:assembleDebug` with zero compiler warnings, `:shared:allTests` passing, and Xcode simulator build succeeding.
