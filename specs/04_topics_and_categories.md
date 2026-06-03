# Implementation Step 4: Topics Directory & Home Feed Horizontal Category Navigation

This specification details the creation of horizontal topic navigation bar on the home screen feed, a full Topics Directory, and topic-specific photo feeds, following Unsplash public API standards.

---

## 1. Unsplash API Specifications

### Endpoints Used
1.  **List Topics:** `GET /topics`
    *   **Description:** Retrieves a paginated list of public curated topics/categories.
    *   **Parameters:** `page`, `per_page`, `ids` (filter by list of IDs), `order_by` (featured/latest/position/oldest; default: `position`)
2.  **Get a Topic:** `GET /topics/:id_or_slug`
    *   **Description:** Retrieves full topic details including descriptive blurbs and status.
    *   **Parameters:** `id_or_slug` (String, Path, Required)
3.  **Get a Topic's Photos:** `GET /topics/:id_or_slug/photos`
    *   **Description:** Retrieves photos submitted to and approved for a specific topic.
    *   **Parameters:** `id_or_slug` (String, Path, Required), `page`, `per_page`, `orientation`, `order_by`

---

## 2. Shared Domain & Presentation Layer (`shared/commonMain`)

### Data Models (`com.example.imagefeed.model`)
Implement the topic data structures:

```kotlin
@Serializable
data class Topic(
    val id: String,
    val slug: String,
    val title: String,
    val description: String? = null,
    @SerialName("published_at") val publishedAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("total_photos") val totalPhotos: Int,
    val status: String, // e.g. "open"
    @SerialName("cover_photo") val coverPhoto: Photo,
    @SerialName("owners") val owners: List<User>
)
```

### Presenter Layer (`com.example.imagefeed.presentation`)
Support a unified feed presenter where the home feed can easily toggle between "Editorial" and active topic categories:

```kotlin
data class HomeFeedState(
    val topics: List<Topic> = emptyList(),
    val selectedTopicSlug: String = "editorial", // "editorial" represents standard feed
    val photos: List<Photo> = emptyList(),
    val isLoadingPhotos: Boolean = false,
    val isLoadingTopics: Boolean = false,
    val error: String? = null,
    val page: Int = 1,
    val hasReachedEnd: Boolean = false
)

class HomeFeedPresenter(private val repository: UnsplashRepository) {
    private val _state = MutableStateFlow(HomeFeedState())
    val state = _state.asStateFlow()

    fun loadTopics() { ... } // Loads horizontal navigation categories on start
    fun selectTopic(slug: String) { ... } // Resets photos and loads topic photos
    fun loadNextPage() { ... } // Correctly routes next page load to editorial or topic photo API
}
```

---

## 3. UI/UX Design & Platform-Specific Best Practices

### A. iOS SwiftUI Implementation (`iosApp`)
*   **Horizontal Category Bar:**
    *   A custom scrolling tab bar (`ScrollView(.horizontal, showsIndicators: false)`) sitting just below the top navigation bar.
    *   Features a sliding capsule indicator background that matches the active tab's width and dynamically slides using SwiftUI’s `.matchedGeometryEffect` when selected.
    *   Light haptic ticks (`selectionChanged`) trigger as the user taps different tabs.
*   **Topics Directory:** A dedicated category grid utilizing SwiftUI's `LazyVGrid(columns: [GridItem(.adaptive(minimum: 150))])`. Each card displays the topic name over a blurred cover-photo background.
*   **Skeleton Loading:** Swapping categories triggers a soft shimmer skeletal loading state (using a linear gradient offset animation) instead of a hard loading spinner, providing a fluid transition experience.

### B. Android Jetpack Compose Implementation (`androidApp`)
*   **Horizontal Scroll Category Row:**
    *   Constructed using a Compose `ScrollableTabRow` with a custom indicator.
    *   Active tabs are highlighted with a sleek sliding indicator that matches Material 3 guidelines.
    *   Tapping a tab triggers a subtle scale animation on the text (`scale` animate float).
*   **Topics List Page:** A grid utilizing standard `Card` containing the topic's title and description. A "Featured" badge is placed on the top-right using a custom translucent tag.
*   **Paging Cache:** Cached photos are maintained inside memory for previously loaded categories, preventing redundant network calls when clicking back and forth between two categories.
*   **Swipe to Refresh:** Integrates Material 3 `PullToRefresh` to refresh photos for the currently selected topic or editorial feed.

---

## 4. Compliance & Verification Checklist

*   [ ] **Attribution Standards:** Every topic photo card must preserve the photographer’s details and profile hyperlinks.
*   [ ] **Dynamic Scaling:** Ensure images requested for the horizontal category tabs or grid previews are requested with small dimensions (e.g. `w=120`, `w=400`) to preserve cellular data.
*   [ ] **Referral parameters:** Subtitle credit text links to Unsplash using standard parameters (`?utm_source=ImageFeedApp&utm_medium=referral`).
