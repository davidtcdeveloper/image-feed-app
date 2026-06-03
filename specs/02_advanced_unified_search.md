# Implementation Step 2: Advanced Unified Search, Filters, & Search Suggestions

This specification details the development of a complete unified search system allowing users to search for Photos, Collections, and Users, with advanced filters for color, orientation, sorting, and localization using public unauthenticated API endpoints.

---

## 1. Unsplash API Specifications

### Endpoints Used
1.  **Search Photos:** `GET /search/photos`
    *   **Description:** Search across Unsplash photos using query strings and rich filters.
    *   **Parameters:**
        *   `query` (String, Query, Required)
        *   `page` (Int, Query, Optional, default: 1)
        *   `per_page` (Int, Query, Optional, default: 10)
        *   `order_by` (String, Query, Optional; values: `relevant`, `latest`; default: `relevant`)
        *   `collections` (String, Query, Optional; comma-separated collection IDs)
        *   `content_filter` (String, Query, Optional; values: `low`, `high`; default: `low`)
        *   `color` (String, Query, Optional; values: `black_and_white`, `black`, `white`, `yellow`, `orange`, `red`, `purple`, `magenta`, `green`, `teal`, `blue`)
        *   `orientation` (String, Query, Optional; values: `landscape`, `portrait`, `squarish`)
        *   `lang` (String, Query, Optional; e.g., `en`, `es`, `fr`, etc.)
2.  **Search Collections:** `GET /search/collections`
    *   **Description:** Search for matching photo collections.
    *   **Parameters:** `query`, `page`, `per_page`
3.  **Search Users:** `GET /search/users`
    *   **Description:** Search for photographer profiles.
    *   **Parameters:** `query`, `page`, `per_page`

---

## 2. Shared Domain & Presentation Layer (`shared/commonMain`)

### Presenter Layer (`com.example.imagefeed.presentation`)
A single `UnifiedSearchPresenter` encapsulates all three dimensions (Photos, Collections, Users) and their states:

```kotlin
enum class SearchTab { PHOTOS, COLLECTIONS, USERS }

data class SearchFilters(
    val orderBy: String = "relevant",
    val color: String? = null,
    val orientation: String? = null,
    val contentFilter: String = "low"
)

data class SearchState(
    val query: String = "",
    val activeTab: SearchTab = SearchTab.PHOTOS,
    val filters: SearchFilters = SearchFilters(),
    val photos: List<Photo> = emptyList(),
    val collections: List<CollectionSummary> = emptyList(),
    val users: List<UserSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchHistory: List<String> = emptyList(),
    val suggestions: List<String> = emptyList()
)

class UnifiedSearchPresenter(private val repository: UnsplashRepository) {
    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    fun updateQuery(newQuery: String) { ... } // Includes 300ms debounce
    fun setTab(tab: SearchTab) { ... }
    fun applyFilters(filters: SearchFilters) { ... }
    fun loadNextPage() { ... }
    fun clearHistory() { ... }
}
```

---

## 3. UI/UX Design & Platform-Specific Best Practices

### Search Screen UX Architecture
*   **Search Box & Suggestions:** Instant feedback showing matching search suggestions as the user types. Below the input, a search history list with individual deletion options is visible.
*   **Three-Way Sliding Header:** Swipe gestures or tapping high-contrast tabs (**Photos**, **Collections**, **Users**) slide the content body horizontally.
*   **Interactive Filters Drawer:** Sliding from the side or emerging as an expandable sheet, allowing visual manipulation of filters.

### A. iOS SwiftUI Implementation (`iosApp`)
*   **Dynamic Focus & Navigation:** Native integration using `.searchable` and `.searchSuggestions`. Focused state slides down the recent history panel with soft fade transitions.
*   **Interactive Bottom Sheet Drawer:** Expandable custom modal sheet with custom background glassmorphism. It uses SwiftUI's native `.presentationDetents([.medium, .large])` to control depth.
*   **Visual Filter Selectors:**
    *   *Orientation Selector:* Horizontal segmented graphics visually illustrating Aspect Ratio icons (horizontal, vertical, square).
    *   *Color Swatches:* A grid of circular shapes with the native color representable. Selecting a circle adds a thin high-contrast white ring border and triggers a brief bounce spring scale animation.
*   **Spring Grid Updates:** Re-filtering uses SwiftUI’s `.animation(.spring(), value: state.photos)` to automatically reposition photos in the grid.

### B. Android Jetpack Compose Implementation (`androidApp`)
*   **Search Bar Transition:** Embed a Material 3 `SearchBar` or `DockedSearchBar` that expands full-screen on mobile devices when active.
*   **Modal Bottom Sheet Filters:** Launch a Material 3 `ModalBottomSheet` displaying filters. Use native state to track drag-to-dismiss capabilities.
*   **Visual Filter Components:**
    *   *Orientation Option:* Multi-select chips with visual icons indicating orientation formats.
    *   *Color Chips:* Horizontal scrolling list of circular `Box` components wrapped in `clickable` modifiers. Selected colors are adorned with a checkmark vector animation.
*   **Debounced Flow:** Use Kotlin Coroutines `Flow` operator `.debounce(300)` combined with `.distinctUntilChanged()` inside the shared module to automatically throttle rapid user keystrokes before hitting Ktor.

---

## 4. Compliance & Verification Checklist

*   [ ] **API Rate Throttling:** Ensure that debouncing prevents spamming searches which would exhaust demo-mode rate limits (50/hr).
*   [ ] **Dynamic Image URL constraints:** All images resulting from searches must construct optimized sizes matching the device columns.
*   [ ] **Photographer Attribution:** Ensure that grid views for both Photo Search and Collection cover images display names and profile icons linked directly with proper referral parameters.
