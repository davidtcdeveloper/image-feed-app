# Specification: README Modernization, Feature Catalog & Screen Navigation Architecture

**Status:** Implemented

## 1. Overview & Purpose
This specification documents the modernization of `README.md` to reflect the comprehensive feature set, architectural enhancements, multiplatform design foundations, and complete screen navigation model of the Unsplash Image Feed App.

As the codebase has evolved through major design overhauls (Android Material 3, iOS Glassmorphic Design Foundation, Adaptive Multi-Column & Foldable Layouts), dependency upgrades (Kotlin 2.4, Gradle 9.6, AGP 9.4, Ktor 3.5, Metro 1.4, Coil 3.5, Jetpack Navigation 3), and UX enhancements (interactive detented sheets, pinch-to-zoom canvas, Google Maps integration, clickable tag navigation, accelerometer shake-to-randomize), the project documentation must provide an accurate, high-fidelity developer guide and feature catalog.

---

## 2. Evaluation of Current `README.md`

### 2.1 Outdated & Incomplete Areas
1. **Missing Features Catalog**: The previous summary briefly listed 5 phases plus macOS, omitting key interactive features such as:
   - Full-canvas pinch/pan photo zoom (1.0x–4.0x) and double-tap toggle.
   - Interactive detented non-modal frosted glass inspector sheet on iOS.
   - Dual-pane responsive inspection and inline Google Maps photo capture geolocation on Android.
   - Clickable photo tags navigating directly to pre-filtered search.
   - Accelerometer shake gesture and desktop shortcut (⌘S) for random photo discovery.
   - Multi-facet search across Photos, Collections, and Users with real-time debouncing and filter sheets.
2. **Missing Navigation Model & Flow Diagram**:
   - No documentation or visual diagram explaining how users navigate between the 6 distinct screens (Feed, Collections, Search, Photo Details, Collection Details, User Profile).
   - No mapping of cross-screen navigation triggers (e.g. tapping photographer attribution capsule -> User Profile; tapping tag chip -> Search; tapping collection card in profile -> Collection Details).
   - Missing explanation of platform-specific root shells (Android `NavigationRail` vs. `NavigationBar` via Navigation 3, iOS `TabView` with translucent glass, macOS `NavigationSplitView` sidebar).
3. **Architectural & Dependency Evolution**:
   - Still contained partial mentions of older patterns, omitting the modern compile-time Metro DI graph, presenter lifecycle scope management (`PresenterScope` / `updateIfActive`), and Android Jetpack Navigation 3.
4. **Tooling & Build Prerequisites**:
   - Prerequisites needed refinement for Kotlin 2.4.x, AGP 9.4.x, and Gradle 9.6.x requirements.

---

## 3. Scope of Work

### 3.1 Feature Overview Section
Add a structured breakdown of application features organized into core user capabilities:
* **Photo Discovery & Topics Feed**: Infinite staggered grid, category selector pill strip, BlurHash decode placeholders, dynamic CDN sizing, shake-to-shuffle.
* **Curated Collections**: Composite mosaic cards, collection detail feeds, related collections carousels.
* **Unified Multi-Facet Search**: Debounced search queries across Photos, Collections, and Users; orientation, color, and relevance filters.
* **Photo Details, EXIF & Map**: Full-canvas image viewer, EXIF camera metadata, photo statistics, inline Google Maps (Android), interactive detented sheet (iOS).
* **Photographer Profiles & Analytics**: Bio, portfolio segmented by Photos / Likes / Collections, interactive drag-scrubbing timeline trend charts (Views & Downloads).
* **Cross-Platform Design & UX**: Android Material 3 with Dynamic Color and NavigationRail; iOS Glassmorphic Design System (`GlassTheme`, translucent chrome, floating capsules, `.scrollTransition`, `.sensoryFeedback`).
* **Desktop Platform (macOS)**: Native desktop target with sidebar navigation (`NavigationSplitView`) and keyboard shortcuts.

### 3.2 Screen Navigation Architecture & Mermaid Diagram
Document all navigation pathways and implement a comprehensive Mermaid diagram illustrating:
* **Primary Navigation Shells**:
  - Android: `NavigationRail` (Medium/Expanded $\ge 600\text{dp}$) vs. `NavigationBar` (Compact < 600dp) driven by Jetpack Navigation 3 (`NavBackStack<NavKey>`).
  - iOS: Translucent `TabView` driven by SwiftUI `NavigationStack(path:)` with strongly typed `FeedPathItem` and `CollectionPathItem`.
  - macOS: `NavigationSplitView` with sidebar selecting between Photos and Collections.
* **Screen Destinations**:
  1. `Photos Feed`
  2. `Collections Feed`
  3. `Search Screen`
  4. `Photo Details`
  5. `Collection Details`
  6. `User Profile`
* **Inter-Screen Linkages & Action Triggers**:
  - Photo card selection -> Photo Details (with Hero zoom animation).
  - Photographer attribution click -> User Profile.
  - Tag chip click -> Search Screen (pre-populated with query).
  - Collection card click -> Collection Details.
  - Related collection click -> Collection Details (nested push).
  - Search result selection -> Photo Details, Collection Details, or User Profile.
  - Shake device / toolbar shuffle button / ⌘S -> Photo Details (Random Photo).

### 3.3 Architecture & Technical Stack Updates
* Update architecture diagram and description reflecting Metro DI (`@DependencyGraph`, `@ContributesBinding`, `@AssistedFactory`), lifecycle-aware `PresenterScope`, Navigation 3, and native platform shells.
* Update prerequisites and verification commands.

---

## 4. Verification Plan
* Validate markdown rendering and Mermaid diagram syntax.
* Verify consistency with actual codebase implementations across `androidApp`, `iosApp`, and `shared`.
