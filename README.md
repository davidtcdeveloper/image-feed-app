> This project is a proof of concept written entirely with recent models focused on efficiency like Google's Gemini 3.5 Flash and  MAI-Code-1-Flash. The main goal was to evaluate the models and different ways to work with them (Antigravity, Gemini CLI, GitHub Copilot and so on).

# Unsplash Image Feed App (Kotlin Multiplatform)

A modern, fluid, and high-performance Kotlin Multiplatform (KMP) application utilizing the public Unsplash API to deliver a rich, responsive photo discovery experience on Android, iOS, and macOS.

---

## 📱 Project Overview

The Unsplash Image Feed App leverages native platform UI toolkits (Jetpack Compose on Android and SwiftUI on iOS & macOS) bound to a shared multiplatform business/presentation layer. The application fully implements the public (unauthenticated) features of the Unsplash API across 5 key phases:

1. **Photo Details, EXIF & Statistics**: Detailed inspect views featuring camera configuration data (shutter speed, aperture, ISO), geographical tags mapped natively, and trend line charts showing photo popularity.
2. **Advanced Search & Filters**: Universal search bar supporting debounced text queries, visual category picker tabs, and sheet filter controls (orientation, color, content filter).
3. **Curated Collections**: Browse, explore, and navigate composite mosaic collection grids, infinite photogrid detail feeds, and related carousels.
4. **Topics Directory & Category Tabs**: Interactive horizontal sliding capsule/pill indicators with elastic animation transitions to toggle the main feed between general editorial and dedicated active topics.
5. **Photographer Profiles & Insights**: Detailed public photographer details containing a collapsing folding header, portfolios segmented across three dynamic feeds, and dual interactive timeline statistics charts (Views and Downloads) supporting drag-scrubbing gestures.
6. **macOS Native Target**: Native desktop application compiling fully with native macOS architectural targets and window interfaces, reusing 100% of the iOS views, presenters, and models under-the-hood.

---

## 🏗️ Architecture Overview

The codebase is built on the **Shared Presenter Pattern**, maximizing cross-platform code reuse while retaining completely native rendering and scrolling performance.

```
┌─────────────────────────────────────────────────────────────┐
│                       App View Shells                       │
│                                                             │
│   ┌───────────────────────────┐   ┌─────────────────────┐   │
│   │ androidApp (Jetpack Comp) │   │  iosApp (SwiftUI)   │   │
│   └─────────────┬─────────────┘   └──────────┬──────────┘   │
└─────────────────┼────────────────────────────┼──────────────┘
                  │ (Collects StateFlow)       │ (Exposed CommonFlow)
                  ▼                            ▼
┌─────────────────────────────────────────────────────────────┐
│              shared Module (Kotlin Multiplatform)           │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │                    Presenters                       │   │
│   │  (FeedPresenter, UserProfilePresenter, etc.)         │   │
│   └──────────────────────────┬──────────────────────────┘   │
│                              ▼                              │
│   ┌─────────────────────────────────────────────────────┐   │
│   │                 UnsplashRepository                  │   │
│   └──────────────────────────┬──────────────────────────┘   │
│                              ▼                              │
│   ┌─────────────────────────────────────────────────────┐   │
│   │                 UnsplashApiClient                   │   │
│   │       (Ktor HTTP Engine, ContentNegotiation)        │   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

* **`shared` module (`commonMain`)**: Exposes highly-optimized, stateful presenters mapping Ktor API models into declarative Kotlin `State` objects. Uses **Metro** for compile-time Dependency Injection.
* **`androidApp`**: Thin visual representation shell rendered entirely using Jetpack Compose.
* **`iosApp`**: SwiftUI visual shell that binds directly to Shared flows mapped into native Swift variables.

---

## 🔑 Unsplash API Key Configuration

To comply with security best practices, the application loads your API Access Key from a local, git-ignored configuration file at compile-time and injects it into the shared core via `BuildKonfig`.

1. In the root directory of this project, create a file named **`local.properties`**:
   ```bash
   touch local.properties
   ```
2. Insert your Unsplash API Client Access Key inside `local.properties` using the property name shown below:
   ```properties
    unsplash.api.key=YOUR_UNSPLASH_ACCESS_KEY_HERE
    ```

---

## 🛠️ Development Workflow

This project follows a spec-driven implementation process:
- **Planning**: All features and bug fixes must have a corresponding specification in the `specs/` directory.
- **Guidelines**: Refer to `AGENTS.md` and the `ai-rules/` folder for architectural standards and project-specific rules.
- **Tracking**: Implementation progress is tracked in `specs/steps.md`.

---


## 🛠️ How to Build and Run

### 📋 Prerequisites
* **macOS** (Required to compile the iOS app target)
* **Android Studio Koala+** with the **Kotlin Multiplatform Mobile (KMM)** plugin installed
* **Xcode 15+**
* **JDK 17** or newer

### 🤖 Running the Android Application
1. Open the project folder in **Android Studio**.
2. Wait for Gradle sync to complete.
3. Select `androidApp` from the Run Configurations dropdown.
4. Choose an Android Emulator or a connected physical device.
5. Click the green **Run (Play)** button.

Alternatively, compile from the command line:
```bash
./gradlew :androidApp:assembleDebug
```

### 🍎 Running the iOS Application
The Shared library is fully configured to compile for iOS Simulator and Device architectures (`iosX64`, `iosArm64`, `iosSimulatorArm64`). 

To build the KMP library target directly:
```bash
# Compile for iOS Simulator
./gradlew :shared:compileKotlinIosSimulatorArm64

# Generate full debug framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

To run the SwiftUI App:
1. Ensure `xcodegen` is installed. Run `cd iosApp && xcodegen` to generate the Xcode project workspace dynamically.
2. Open `iosApp/iosApp.xcodeproj` in **Xcode**.
3. Select either the `iosApp` (for iPhone/iPad) or `macosApp` (for macOS) scheme in the top toolbar.
4. Hit **Run (⌘R)**.

### 💻 Running the macOS Application
The macOS app is managed within the same Xcode project as the iOS app.
1. Open `iosApp/iosApp.xcodeproj` in **Xcode**.
2. Select the `macosApp` scheme in the top toolbar.
3. Hit **Run (⌘R)**.

### 🧪 Testing & Verification
To run the shared integration and package-level test suite:
```bash
./gradlew :shared:allTests
```

### 🧹 Code Quality & Linting
To ensure code consistency, run the following tools before committing:
- **Kotlin**: `./gradlew ktlintCheck detekt`
- **Swift**: `swiftlint` and `swiftformat --lint .`

---

## 📜 Unsplash API Compliance Guidelines

When modifying user feeds or detail interfaces:
* **No Local Caching**: Never cache Unsplash image files on external servers. Always fetch and display assets utilizing the native URLs returned by the API.
* **ixid Preservation**: Retain all `ixid` tracking parameters in image request strings.
* **Attribution**: Display photographer names and profiles prominently with a UTM backlink pointing to Unsplash (`utm_source=ImageFeedApp&utm_medium=referral`).
* **Download Tracking**: Ensure downloading or saving a photo triggers the designated `download_location` callback to increment the photographer's download stats.
