# Implementation Step 3: Collections Feed, Curation Details, & Related Collections

This specification describes the architecture and UI designs for browsing, selecting, and inspecting curated collections of Unsplash photos, including secondary related collections exploration without authentication requirements.

---

## 1. Unsplash API Specifications

### Endpoints Used
1.  **List Collections:** `GET /collections`
    *   **Description:** Retrieves a paginated list of public curated photo collections.
    *   **Parameters:** `page` (Int, default: 1), `per_page` (Int, default: 10)
2.  **Get a Collection:** `GET /collections/:id`
    *   **Description:** Retrieves detailed info for a single collection.
    *   **Parameters:** `id` (String, Path, Required)
3.  **Get a Collection's Photos:** `GET /collections/:id/photos`
    *   **Description:** Retrieves all photos within a collection.
    *   **Parameters:** `id` (String, Path, Required), `page`, `per_page`, `orientation`
4.  **List Related Collections:** `GET /collections/:id/related`
    *   **Description:** Retrieves up to 3 collections related to the target collection.
    *   **Parameters:** `id` (String, Path, Required)

---

## 2. Shared Domain & Presentation Layer (`shared/commonMain`)

### Data Models (`com.example.imagefeed.model`)
Implement the collection structures:

```kotlin
@Serializable
data class PhotoCollection(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("published_at") val publishedAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("total_photos") val totalPhotos: Int,
    val private: Boolean,
    @SerialName("cover_photo") val coverPhoto: Photo,
    val user: User,
    val links: CollectionLinks
)

@Serializable
data class CollectionLinks(
    val self: String,
    val html: String,
    val photos: String,
    val related: String
)
```

### Presenter Layer (`com.example.imagefeed.presentation`)
Define presenters for collections feed and details:

```kotlin
// Collections List Presenter
data class CollectionsFeedState(
    val collections: List<PhotoCollection> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val page: Int = 1,
    val hasReachedEnd: Boolean = false
)

class CollectionsFeedPresenter(private val repository: UnsplashRepository) { ... }

// Collection Detail Presenter
data class CollectionDetailState(
    val collection: PhotoCollection? = null,
    val photos: List<Photo> = emptyList(),
    val related: List<PhotoCollection> = emptyList(),
    val isLoadingPhotos: Boolean = false,
    val isHeaderLoading: Boolean = false,
    val error: String? = null
)

class CollectionDetailPresenter(
    private val repository: UnsplashRepository,
    private val collectionId: String
) { ... }
```

---

## 3. UI/UX Design & Platform-Specific Best Practices

### A. iOS SwiftUI Implementation (`iosApp`)
*   **Collection Card Design (Mosaic Preview):**
    *   Displays a compound layout: Large cover photo on the left, flanked by a vertical stack of two smaller recent photo thumbnails on the right. This mosaic grid simulates the native Unsplash experience.
    *   Rounded corners, subtle drop shadows, and photographer attribution overlaid with dynamic fonts.
*   **Curator Header:** Translucent profile icon of the creator. Tapping it navigates directly to the Curator's profile.
*   **Grid Scroll Parallax:** On the Collection Detail page, the collection header has a parallax scaling stretch animation as the user pulls down, showing the title, description, and total count over a blurred representation of the cover photo.
*   **Related Horizontal Carousel:** At the bottom, related collections are represented as cards in a horizontal scroll (`ScrollView(.horizontal)` with paging snapping mechanics).

### B. Android Jetpack Compose Implementation (`androidApp`)
*   **Mosaic Preview Card:**
    *   Constructed using Compose `Row` containing a `SubcomposeAsyncImage` cover photo (weighted at 2f) and a `Column` with two thumbnail images (weighted at 1f each), separated by thin 4.dp spacer margins.
    *   Tapping the card expands the layout into the Collection Detail using standard `AnimatedContent` transition.
*   **Sticky Category Header:** In the detail page, as the user scrolls up, the collection title and count stick dynamically to the top bar utilizing `LazyColumn`'s `stickyHeader` capability, changing background transparency.
*   **Staggered Detail Photo Grid:** Load the collection's photos in a `LazyVerticalStaggeredGrid`. Tap-to-expand details using safe transitions.
*   **Infinite pre-fetching:** Trigger page pre-fetching 5 items before the end of the scroll target.

---

## 4. Compliance & Verification Checklist

*   [ ] **Curator Attribution:** Display curator name, profile image, and referral UTM links back to Unsplash itself.
*   [ ] **Hotlinking Compliance:** All mosaic thumbnails and grids must be directly hotlinked to Unsplash CDN (`urls.raw`) with appended sizing keys (`&w=`, `&q=80`).
*   [ ] **Download Stats:** Clicking any photo inside the collection detail and saving it must trigger download tracking (`photo.links.download_location`).
