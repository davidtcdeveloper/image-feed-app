# Design Systems & Navigation

Both platform shells remain thin, declarative view layers rendering shared MVI state using platform-idiomatic design languages.

---

## 1. Android: Material 3 Expressive & Jetpack Navigation 3

### Navigation Architecture
* **Type-Safe Routes**: [`AppRoute`](../androidApp/src/main/java/com/example/imagefeed/android/navigation/Routes.kt) defines sealed `NavKey` destinations (`Feed`, `Collections`, `CollectionDetails(collectionId)`, `Favorites`, `Search(query)`, `PhotoDetails(photoId)`, `UserProfile(username)`).
* **Multiple Backstacks & Depth Bound**: [`Navigator.kt`](../androidApp/src/main/java/com/example/imagefeed/android/navigation/Navigator.kt) manages per-tab backstacks (`TOP_LEVEL_DESTINATIONS`: Photos, Collections, Favorites, Search), preserves scroll/state on tab switching, trims sub-route stack depth to `MAX_BACKSTACK_DEPTH = 8`, and emits reselection events (`reselectEvents`) for scroll-to-top behavior.
* **Resumed Navigation Guard**: [`NavigationSections.kt`](../androidApp/src/main/java/com/example/imagefeed/android/navigation/NavigationSections.kt) wraps navigation callbacks in `dropUnlessResumed` / `dropUnlessResumedWithArg` (`favoritesSection` registers `ListDetailSceneStrategy.listPane` for [`FavoritesScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/FavoritesScreen.kt) with quick unbookmarking and non-blocking snackbar feedback).
* **Adaptive Scenes & Foldables**: [`AdaptiveLayoutInfo.kt`](../androidApp/src/main/java/com/example/imagefeed/android/adaptive/AdaptiveLayoutInfo.kt) inspects `WindowSizeClass` and `WindowInfoTracker` `FoldingFeature`:
  * Compact width (< 600dp) uses bottom navigation (`NavigationBar`), switching to `NavigationRail` on Medium/Expanded widths ($\ge 600\text{dp}$).
  * `ListDetailSceneStrategy` (`listPane` + `detailPane` + `PhotoDetailPlaceholder`) activates dual-pane layouts on wide screens and Book/TableTop foldable postures while avoiding hinge creases.

### Material 3 Design & Motion
* Dynamic tonal color palettes and semantic surface container tokens are defined in [`Theme.kt`](../androidApp/src/main/java/com/example/imagefeed/android/theme/Theme.kt) alongside [`Color.kt`](../androidApp/src/main/java/com/example/imagefeed/android/theme/Color.kt), [`Shape.kt`](../androidApp/src/main/java/com/example/imagefeed/android/theme/Shape.kt), and [`Type.kt`](../androidApp/src/main/java/com/example/imagefeed/android/theme/Type.kt).
* Hero photo transitions wrap navigation sections in `SharedTransitionLayout` paired with `LocalNavAnimatedContentScope`, plus predictive back support via `androidx.navigationevent.compose`.

---

## 2. Apple (`iosApp` & `macosApp`): Liquid Glass Design System & Multi-Target UI

### Shared Apple Source Tree
* Both the `iosApp` (iOS 17+) and `macosApp` (macOS 14+) targets compile the same SwiftUI codebase under `iosApp/iosApp/` configured in [`iosApp/project.yml`](../iosApp/project.yml).
* Any edit to `iosApp/iosApp/**` must conditionally handle `#if os(iOS)` / `#elseif os(macOS)` differences (`UIKit` vs. `AppKit`, `UIScreen` vs. `NSScreen`, `UIScrollView` vs. `NSScrollView`) and build cleanly on both Xcode schemes after running `xcodegen generate --spec iosApp/project.yml`.

### Tiered GlassTheme System
* [`GlassTheme.swift`](../iosApp/iosApp/GlassTheme.swift) centralizes specular border gradients, shadows, high-contrast WCAG AAA dark scrim (`highContrastScrimColor`), semantic cross-platform canvas/surface colors, and `.glassBackground(...)`.
* **Tiered Performance Policy**:
  * Elevated navigation chrome ([`FloatingTabCapsule.swift`](../iosApp/iosApp/FloatingTabCapsule.swift), `FeedCategoryBar`, `SearchTabBar`, detail action orbs) uses live blur styles (`.ultraThin` / `.regular`) with specular hairline borders.
  * High-frequency masonry overlays (photographer attribution pills, collection count badges) use `GlassStyle.lightweight` (single-pass translucent dark scrim + specular border, `hasShadow: false`) to prevent GPU compositor bottlenecks during scrolling.
* **Accessibility Fallbacks**: `GlassBackgroundModifier` and `FloatingTabCapsule` automatically fall back to solid semantic surfaces and crisp borders when `@Environment(\.accessibilityReduceTransparency)` or `@Environment(\.colorSchemeContrast) == .increased` is active.

### Apple Navigation & Layout Ergonomics
* **iOS Shell**: [`ContentView.swift`](../iosApp/iosApp/ContentView.swift) pairs `NavigationStack` flows with the floating glass `FloatingTabCapsule` (`Photos` | `Collections` | `Favorites`), [`FavoritesView.swift`](../iosApp/iosApp/FavoritesView.swift) (`FavoritesTabView` reusing `PhotoCard` with destructive `"Remove Bookmark"` context menus), and quick search/random entry points.
* **macOS Shell**: [`MacOSContentView.swift`](../iosApp/iosApp/MacOSContentView.swift) uses `NavigationSplitView` sidebar navigation (`Photos`, `Collections`, `Favorites`), desktop shortcuts (`⌘F`, `⌘S`), and horizontal mouse-wheel delta translation + chevron buttons (`chevron.backward` / `chevron.forward`) on horizontal category/tab strips.
* **Full-Bleed Floating Bars**: `FeedCategoryBar` (`ContentView.swift`) and `SearchTabBar` ([`SearchView.swift`](../iosApp/iosApp/SearchView.swift)) dock via `.safeAreaInset(edge: .top, spacing: 0)` inside `44pt` frosted capsules with `CategoryTabButton` `matchedGeometryEffect` selection pills and `.sensoryFeedback(.selection, trigger:)`.
* **Photo Details & Frame-0 Hero Seeding**: [`PhotoDetailsView.swift`](../iosApp/iosApp/PhotoDetailsView.swift) accepts `initialPhoto: Photo?` so `matchedGeometryEffect` hero geometry and the canvas render on Frame 0 while `PhotoDetailsViewModel` loads lazily in `.task`, pairing explicit-toggle bookmark haptics (`favoriteFeedbackTrigger`) and `GlassToastPill` feedback with bookmark actions in `compactFloatingBar` and [`PhotoInspectorView.swift`](../iosApp/iosApp/PhotoInspectorView.swift). iOS compact layout presents `PhotoInspectorView` in a non-modal detented bottom sheet, while iPad/macOS renders a side-by-side dual pane.
