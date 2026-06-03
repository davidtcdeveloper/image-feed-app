# Implementation Step 5: Public Photographer Profiles, Multi-Tab Portfolios, & Analytics Insights

This specification outlines the architecture, layout, and visual representations for photographer profile views, displaying their portfolio (Photos, Likes, Collections) and visual download/view statistics charts without user authorization requirements.

---

## 1. Unsplash API Specifications

### Endpoints Used
1.  **Get a User's Public Profile:** `GET /users/:username`
    *   **Description:** Retrieves full public details of a photographer.
    *   **Parameters:** `username` (String, Path, Required)
2.  **List a User's Photos:** `GET /users/:username/photos`
    *   **Description:** Get photos uploaded by the photographer.
    *   **Parameters:** `username`, `page`, `per_page`, `order_by`
3.  **List a User's Liked Photos:** `GET /users/:username/likes`
    *   **Description:** Get photos liked by the photographer.
    *   **Parameters:** `username`, `page`, `per_page`, `orientation`
4.  **List a User's Collections:** `GET /users/:username/collections`
    *   **Description:** Get collections created by the photographer.
    *   **Parameters:** `username`, `page`, `per_page`
5.  **Get User Statistics:** `GET /users/:username/statistics`
    *   **Description:** Get consolidated downloads and views timeline statistics.
    *   **Parameters:** `username`, `resolution` (default: "days"), `quantity` (default: 30)

---

## 2. Shared Domain & Presentation Layer (`shared/commonMain`)

### Presenter Layer (`com.example.imagefeed.presentation`)
A detailed `UserProfilePresenter` coordinates user public information, tab states, and statistics requests:

```kotlin
enum class ProfileTab { PORTFOLIO, LIKES, COLLECTIONS, INSIGHTS }

data class UserProfileState(
    val user: User? = null,
    val activeTab: ProfileTab = ProfileTab.PORTFOLIO,
    val portfolioPhotos: List<Photo> = emptyList(),
    val likedPhotos: List<Photo> = emptyList(),
    val collections: List<PhotoCollection> = emptyList(),
    val stats: UserStats? = null,
    val isLoadingContent: Boolean = false,
    val isHeaderLoading: Boolean = false,
    val error: String? = null,
    val page: Int = 1,
    val hasReachedEnd: Boolean = false
)

class UserProfilePresenter(
    private val repository: UnsplashRepository,
    private val username: String
) { ... }
```

---

## 3. UI/UX Design & Platform-Specific Best Practices

### Profile Layout Hierarchy
*   **Sticky Folding Header:** Profile picture, name, location, and biography. Scrolls and shrinks smoothly into a compact toolbar upon scrolling up, maximizing feed screen real estate.
*   **Portfolio Grid Tabs:** Sliding tabs representing **Photos**, **Likes**, **Collections**, and **Insights**.
*   **Deep Link Badges:** Social icons (Instagram, Twitter) supporting deep-linking (launches the respective native apps if installed, otherwise defaults to the web browser).

### A. iOS SwiftUI Implementation (`iosApp`)
*   **Header Shrinking Effect:** Use a coordinate space tracker (`GeometryReader` on scroll offset) to dynamically reduce the profile avatar size (from 80pt to 36pt) and fade out biography details as the user scrolls, creating a beautiful folding navigation bar effect.
*   **Social Deep-Linking:** Use `openURL` environment actions to route social links to protocol schemes:
    *   `instagram://user?username=...`
    *   `twitter://user?screen_name=...`
*   **Interactive Analytics Graphs:** Using SwiftUI `Charts` to draw dual charts:
    *   *Views Chart:* Interactive card with shaded line area demonstrating daily views trends.
    *   *Downloads Chart:* Custom bar chart representation (`BarMark` with rounded corners) indicating the total download metrics.
    *   *Selection Tracking:* Drag gestures over chart elements update a floating annotation overlay showing exact stats for the selected date.

### B. Android Jetpack Compose Implementation (`androidApp`)
*   **Collated Folding Header:** Implement custom Compose `NestedScrollConnection` and a sliding layout header that folds into a small sticky top bar containing the user's name and compact profile picture.
*   **Social Deep-Linking:** Construct specific explicit android intent URIs mapping `Intent.ACTION_VIEW` for native package handlers (e.g. `com.instagram.android` or `com.twitter.android`) falling back gracefully to Chrome custom tabs.
*   **Canvas Analytics Charts:**
    *   Draw an interactive Bezier curve graph representing statistics inside a Compose `Canvas` component.
    *   Calculate points dynamically and add entry animations where the curves draw themselves on load (`animateFloatAsState`).
    *   Provide haptic vibration feedback via local tactile controllers (`LocalHapticFeedback.current`) when dragging a slider along the chart dates.

---

## 4. Compliance & Verification Checklist

*   [ ] **Referral Backlink compliance:** All photos, user profiles, and collection cards must incorporate required UTM referral strings back to Unsplash itself.
*   [ ] **Attribution rules:** Ensure the profile prominently features the photographer's full details and link back to their Unsplash HTML page.
*   [ ] **Dynamic Sizing:** User avatars and photos are loaded using size-restricted query strings to keep layouts highly fluid and light on bandwidth.
