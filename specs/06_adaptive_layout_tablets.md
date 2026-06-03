# Implementation Step 6: Adaptive Layouts for Android Tablets & iPads

This specification outlines the UI responsive layout strategies to support large screen form factors (Android tablets, Chromebooks, foldable devices, and Apple iPads). It aims to transition the rigid 2-column layouts into highly adaptive, screen-aware, and scalable native interfaces.

---

## 1. Responsive Screen Class Definitions

To support diverse screen form factors, the applications must adapt to the physical width of the device:

| Screen Width (Dp) | Device Type | Orientation | Target Column Count (Grids) |
| :--- | :--- | :--- | :--- |
| **< 600 Dp** | Mobile Phones | Portrait/Landscape | 1 to 2 Columns |
| **600 Dp to 840 Dp** | Tablets, Foldables | Portrait | 3 Columns |
| **> 840 Dp** | Large Tablets, iPads | Landscape | 4 to 5 Columns |

---

## 2. Android Jetpack Compose Implementation (`androidApp`)

Instead of utilizing rigid hardcoded integer values inside `StaggeredGridCells.Fixed(2)`, Android Compose must leverage **WindowSizeClasses** or calculate widths dynamically using `LocalConfiguration`.

### A. Dynamic Grid Scaling
Configure the columns using `StaggeredGridCells.Adaptive` to scale automatically, or calculate column spans dynamically:

```kotlin
@Composable
fun calculateGridColumns(): StaggeredGridCells {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    
    return when {
        screenWidth >= 840 -> StaggeredGridCells.Adaptive(240.dp) // Large Tablets / Landscape
        screenWidth >= 600 -> StaggeredGridCells.Adaptive(180.dp) // Portrait Tablets
        else -> StaggeredGridCells.Fixed(2)                       // Handheld Mobile Phones
    }
}
```

This dynamic calculation should be applied uniformly across:
*   `MainActivity.kt` (Main Photos Feed Grid)
*   `SearchScreen.kt` (Unified Search results photo/user cards)
*   `UserProfileScreen.kt` (Photographer Portfolio feeds)
*   `CollectionDetailScreen.kt` (Collection Photo items grid)

### B. Master-Detail Split Screen Layout
For the **Photo Details Screen** on screens wider than `600dp` (Tablets in landscape):
*   Instead of a stacked scrolling list, utilize a side-by-side **Row** split layout.
*   **Left Pane (60% width):** The high-resolution photo with smooth zoom controls and interactive gesture listeners.
*   **Right Pane (40% width):** Scrollable sidebar housing the photographer's attribution, location maps, EXIF metadata, and expandable Canvas trend analytics charts.

---

## 3. iOS SwiftUI Implementation (`iosApp`)

In SwiftUI, instead of maintaining hardcoded `leftColumnPhotos` and `rightColumnPhotos` calculated with modulo-2 division, the grids must divide photos dynamically based on the current horizontal size class (`UserInterfaceIdiom` or `horizontalSizeClass`).

### A. Dynamic N-Column Division
Calculate the column partition count dynamically:

```swift
struct AdaptiveGrid: View {
    @Environment(\.horizontalSizeClass) var sizeClass
    
    private var columnCount: Int {
        #if os(iOS)
        if UIDevice.current.userInterfaceIdiom == .pad {
            return sizeClass == .regular ? 4 : 3
        }
        #endif
        return 2
    }
    
    func photosForColumn(index: Int, totalColumns: Int, from photos: [Photo]) -> [Photo] {
        photos.enumerated()
            .filter { $0.offset % totalColumns == index }
            .map { $0.element }
    }
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            ForEach(0..<columnCount, id: \.self) { colIndex in
                LazyVStack(spacing: 12) {
                    ForEach(photosForColumn(index: colIndex, totalColumns: columnCount, from: photos), id: \.id) { photo in
                        PhotoCard(photo: photo, ...)
                    }
                }
            }
        }
    }
}
```

Apply this modular calculation inside:
*   `ContentView.swift` (Main Photos Feed tab)
*   `SearchView.swift` (Search results waterfall layout)
*   `UserProfileView.swift` (UserProfile custom segmented feeds)
*   `CollectionDetailView.swift` (Collections photos feed)

### B. Adaptive Navigation Split View
On iPad devices, the app should leverage `NavigationSplitView` instead of `NavigationStack`:
*   On portrait iPads: Sidebar collapsible panel.
*   On landscape iPads: Side-by-side presentation.
*   Selecting an item in the main feed opens the detail view in the detail content pane, keeping the feed visible on the side and delivering an immersive iPadOS-optimized layout.

---

## 4. Verification & Responsive Checks

*   [ ] **Simulator Rotations:** Verify orientation changes on 11-inch/13-inch iPad Simulators and Android Tablet Emulators (e.g., Pixel Tablet).
*   [ ] **Stretching Prevention:** Ensure images are dynamically downsized using exact container widths to preserve aspect ratios without excessive vertical pixel stretching.
*   [ ] **Side-by-side Split Pane Stability:** Ensure map views and analytics line charts maintain accurate boundaries when loaded side-by-side inside split panes.
