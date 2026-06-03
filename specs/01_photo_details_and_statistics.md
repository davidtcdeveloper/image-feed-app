# Implementation Step 1: Photo Details, EXIF Metadata, & Interactive Statistics

This specification outlines the architecture, user experience, and design requirements to support full photo details, EXIF data rendering, location mapping, interactive statistics, and a "Shake-to-Randomize" photo generator using only public, unauthenticated Unsplash endpoints.

---

## 1. Unsplash API Specifications

### Endpoints Used
1.  **Get a Photo:** `GET /photos/:id`
    *   **Description:** Retrieves full detailed metadata including EXIF, tags, and location data.
    *   **Parameters:** `id` (String, Path, Required)
2.  **Get Photo Statistics:** `GET /photos/:id/statistics`
    *   **Description:** Retrieves views and downloads totals along with a historical 30-day timeline.
    *   **Parameters:** `id` (String, Path, Required), `resolution` (default: "days"), `quantity` (default: 30)
3.  **Get a Random Photo:** `GET /photos/random`
    *   **Description:** Retrieves one or more random photos matching optional constraints.
    *   **Parameters:** `orientation` (landscape/portrait/squarish), `count` (1-30), `query` (filter terms)

---

## 2. Shared Domain & Presentation Layer (`shared/commonMain`)

### Data Models (`com.example.imagefeed.model`)
Extend serialization-ready models to parse full photo response payloads:

```kotlin
@Serializable
data class Exif(
    val make: String? = null,
    val model: String? = null,
    @SerialName("exposure_time") val exposureTime: String? = null,
    val aperture: String? = null,
    @SerialName("focal_length") val focalLength: String? = null,
    val iso: Int? = null
)

@Serializable
data class Location(
    val city: String? = null,
    val country: String? = null,
    val position: Position? = null
)

@Serializable
data class Position(
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class Tag(
    val title: String
)

@Serializable
data class StatsValue(
    val date: String,
    val value: Int
)

@Serializable
data class HistoricalStats(
    val change: Int,
    val average: Int,
    val values: List<StatsValue>
)

@Serializable
data class StatMetric(
    val total: Int,
    val historical: HistoricalStats
)

@Serializable
data class PhotoStats(
    val id: String,
    val downloads: StatMetric,
    val views: StatMetric
)
```

### Presenter Layer (`com.example.imagefeed.presentation`)
Create the `PhotoDetailsPresenter` and `RandomPhotoPresenter`:

```kotlin
data class PhotoDetailsState(
    val photo: Photo? = null,
    val stats: PhotoStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class PhotoDetailsPresenter(
    private val repository: UnsplashRepository,
    private val photoId: String
) {
    private val _state = MutableStateFlow(PhotoDetailsState())
    val state = _state.asStateFlow()
    
    fun loadDetails() { ... }
}
```

---

## 3. UI/UX Design & Platform-Specific Best Practices

### User Flow & Layout
*   **Hero Transition:** Tapping a photo on the feed initiates a shared element hero animation that transitions the card smoothly into the detail view.
*   **Segmented Sliding Tabs:** The detail screen features a smooth segment picker: **Info & EXIF**, **Location**, and **Interactive Stats**.

### A. iOS SwiftUI Implementation (`iosApp`)
*   **EXIF Cards:** A grid of cards using SwiftUI’s `ContainerRelativeShape` showing glassmorphism (translucent background blur) elements containing camera shutter speeds, ISO, and focal length.
*   **Location Map:** Integration of `Map` (MapKit) in SwiftUI, centering on coordinates returned by the API if present, with a custom pin displaying the photographer's profile photo.
*   **Animated Line Charts:** Render a custom smooth Bezier curve line chart of 30-day views/downloads using SwiftUI's `Charts` framework (`LineMark` and `AreaMark` with customized gradient fills).
*   **Haptic Randomizer:** Listen for the native device shake motion using standard UIWindow event responders. Upon detection, trigger a haptic pulse (`UIImpactFeedbackGenerator`) and load a new randomized full-screen photo with cross-fade transition.

### B. Android Jetpack Compose Implementation (`androidApp`)
*   **Hero Shared Elements:** Implement native Jetpack Compose `SharedTransitionLayout` to fluidly scale the feed image to the top of the detail screen.
*   **Glassmorphism EXIF Grid:** Use Compose `Card` with alpha modifiers and a custom Blur background shader to represent camera details clearly in dark theme.
*   **Interactive Charts:** Draw an animated 30-day views/downloads timeline graph utilizing Jetpack Compose `Canvas` or `Compose-Charts` (e.g., Vico) featuring floating coordinate markers when the user drags their finger across the graph.
*   **Location integration:** Integrate a lightweight Google Maps `Map` component inside a Compose wrapper utilizing custom dark-styled vector pins.
*   **Physical Randomizer:** Integrate the system `SensorEventListener` detecting physical accelerometer fluctuations. Execute a dual vibration effect and load a sliding transition random photo.

---

## 4. Compliance & Verification Checklist

*   [ ] **Attribution Compliance:** Display photographer info on the details screen, including their profile image, link to their profile, and standard referral headers.
*   [ ] **Hotlinking Preservation:** Verify that raw image URL requests preserve the `ixid` parameter when appending size constraints (`&w=`, `&dpr=`).
*   [ ] **Download Tracking:** Trigger `apiClient.trackDownload()` whenever an image is saved to the native photo library, ensuring full download statistics alignment on Unsplash.
