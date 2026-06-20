# Spec 21: Google Maps Android Integration Plan

This specification details the plan to resolve the UI inconsistency between Android and iOS/macOS. Apple targets currently render a native MapKit map within the photo detail screen. Android currently redirects the user to the external Google Maps application. 

This plan details how to add support for an inline, interactive, and dark-styled Google Maps component within `PhotoDetailsScreen.kt` using Jetpack Compose and the official Google Maps Compose library.

---

## 1. Current Inconsistency & Solution Overview

### Apple Platforms
- Renders an inline SwiftUI `Map` (`MapKit`) centered on the location coordinates (`latitude` and `longitude`).
- Displays a custom pin (`Marker`) at the coordinates.
- Opens the system Maps app on tap using a custom URL (`maps://?q=...&ll=lat,lon`).

### Android Platform (Current)
- Displays a location `Card` containing the name of the place and textual coordinates.
- On click, it triggers an external implicit `Intent` to open the Google Maps app (falling back to a web browser URL if the app is missing).
- Has **no inline map** shown to the user on the screen.

### Proposed Android Solution
1. Integrate the Google Play Services Maps SDK and the Jetpack Compose wrapper library (`maps-compose`).
2. Add a `GoogleMap` composable inline inside the location card to match the visual presentation of SwiftUI's map.
3. Keep the API key secure by reading it from `local.properties` (ignored by Git) and injecting it into the manifest via Gradle placeholders.
4. Support dark styling to match the application's dark palette.

---

## 2. Technical Steps

### Step 1: Version Catalog Additions (`gradle/libs.versions.toml`)
We will declare the Google Play Services Maps and Compose libraries:

```toml
[versions]
# Google Maps
play-services-maps = "19.2.0"
maps-compose = "8.3.0"

[libraries]
play-services-maps = { group = "com.google.android.gms", name = "play-services-maps", version.ref = "play-services-maps" }
maps-compose = { group = "com.google.maps.android", name = "maps-compose", version.ref = "maps-compose" }
```

### Step 2: Build Configuration Updates (`androidApp/build.gradle.kts`)
We will read the Google Maps API Key from `local.properties` and expose it via manifest placeholders to prevent credentials leakage:

```kotlin
import java.util.Properties

// Read local properties for secure keys
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val googleMapsApiKey = localProperties.getProperty("google.maps.api.key") ?: ""

configure<com.android.build.api.dsl.ApplicationExtension> {
    // ...
    defaultConfig {
        // ...
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = googleMapsApiKey
    }
}

dependencies {
    // ...
    // Google Maps SDK & Compose Integration
    implementation(libs.play-services-maps)
    implementation(libs.maps-compose)
}
```

### Step 3: Manifest Configuration (`androidApp/src/main/AndroidManifest.xml`)
Inject the API key into the manifest using the Gradle placeholder:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- ... -->
    <application
        android:name=".ImageFeedApplication"
        ... >
        
        <!-- Google Maps API Key Meta-Data -->
        <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="${GOOGLE_MAPS_API_KEY}" />
            
        <!-- ... -->
    </application>
</manifest>
```

### Step 4: Dark Map Style Customization (`androidApp/src/main/res/raw/map_style_dark.json`)
To match the app's dark visual design, we will define a standard dark map style JSON configuration.
This JSON will be stored at `androidApp/src/main/res/raw/map_style_dark.json`. It styles water, land, highways, and labels with a muted dark palette.

```json
[
  {
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#212121"
      }
    ]
  },
  {
    "elementType": "labels.icon",
    "stylers": [
      {
        "visibility": "off"
      }
    ]
  },
  {
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#757575"
      }
    ]
  },
  {
    "elementType": "labels.text.stroke",
    "stylers": [
      {
        "color": "#212121"
      }
    ]
  },
  {
    "featureType": "administrative",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#757575"
      }
    ]
  },
  {
    "featureType": "water",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#0f0f11"
      }
    ]
  }
]
```

### Step 5: Compose Map Integration in `PhotoDetailsScreen.kt`
We will replace the existing static location launcher card with a container hosting the inline `GoogleMap` container when coordinates are available.

```kotlin
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings

// Inside PhotoDetailsContent:
photo.location?.let { location ->
    val lat = location.position?.latitude
    val lon = location.position?.longitude
    if (lat != null && lon != null) {
        val positionLatLng = LatLng(lat, lon)
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(positionLatLng, 12f)
        }

        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            if (!location.name.isNullOrEmpty()) {
                Text(
                    text = location.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        // Keep current action: launch external map app for navigation/details
                        val query = location.name ?: ""
                        val gmmIntentUri = Uri.parse("geo:$lat,$lon?q=${Uri.encode(query)}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            val webMapIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
                            )
                            context.startActivity(webMapIntent)
                        }
                    }
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                        mapToolbarEnabled = false,
                        scrollGesturesEnabled = false, // static feel matching iOS card
                        zoomGesturesEnabled = false,
                        tiltGesturesEnabled = false,
                        rotateGesturesEnabled = false
                    )
                ) {
                    Marker(
                        state = MarkerState(position = positionLatLng),
                        title = location.name ?: "Captured Location"
                    )
                }
            }
        }
    }
}
```

---

## 3. API Key & Security Constraints

- The Google Maps API Key must be configured inside the user's `local.properties` as:
  ```properties
  google.maps.api.key=YOUR_ACTUAL_API_KEY
  ```
- If the key is not defined, gradle will inject an empty string `""`.
- The Google Maps SDK will gracefully fail or display grid lines without crashing the application if the API Key is invalid or empty. This preserves the "Zero Crash" constraint for local developers without access to Google Cloud billing keys.
- **NEVER** commit `local.properties` containing active keys. The `.gitignore` file correctly excludes `local.properties`.

---

## 4. Verification Plan

1. **Gradle Build Verification**:
   - Run `./gradlew :androidApp:assembleDebug` to verify project configuration and dependency resolution.
2. **Layout Integrity**:
   - Verify the location block is aligned and sized identically to the stats and camera specification blocks.
   - Verify the custom dark theme style is loaded successfully.
3. **Implicit Intent Redirection**:
   - Tap on the map inside `PhotoDetailsScreen` and confirm that it triggers the navigation intent to the external Google Maps application with exact query and coordinate parameters.
4. **Lint and Warning Verification**:
   - Run `./gradlew ktlintCheck detekt` to guarantee the changes pass styling guidelines.
