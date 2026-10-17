# iOS Floating Glass Tab Capsule & Profile Refinements Spec

**Status:** Implemented

## Overview
This specification details the visionOS-inspired floating tab capsule and profile screen enhancements:
1. **Floating Specular Glass Tab Capsule**: Replace the system-docked `TabView` bar with a floating `.regularMaterial` capsule featuring a sliding indicator (`matchedGeometryEffect`), active tab spring physics, and high-contrast dynamic typography.
2. **Auto-Minimizing Tab Capsule on Scroll**: Smoothly scale down and translate the floating capsule when the user scrolls downward through the masonry feed, smoothly expanding back when scrolling upward or resting.
3. **Profile Social Links Polish**: Replace verbose uppercase text buttons (`INSTAGRAM: @SANDISK`) in `UserProfileView.swift` with compact, icon-led glass chips.
4. **Visual Topic Exploration Cards in Search**: Upgrade flat text pills in `SearchView.swift` into visual topic cards with subtle curated photography backgrounds.

---

## Technical Root Cause & Design Solution

### 1. visionOS Floating Specular Glass Tab Capsule & Navigation Suppression
In [`ContentView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift):
* Currently relies on standard `TabView` with `UITabBarAppearance`.
* A photography application benefits significantly from full-bleed content extending down to the bottom home indicator edge, with chrome floating cleanly above.
* **Navigation Suppression on Pushed Screens**:
  * In SwiftUI, `.toolbar(.hidden, for: .tabBar)` only hides the native `UITabBar`; it **does not** hide custom floating overlays.
  * `PhotosFeedTabView` and `CollectionsFeedTabView` manage their own navigation stacks (`path: NavigationPath`).
  * `FloatingTabCapsule` must be suppressed (hidden via opacity/transition or conditional rendering) whenever `path.isEmpty == false` or when a sheet/search is actively presented, ensuring it never hovers over pushed screens ([`UserProfileView`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/UserProfileView.swift), [`CollectionDetailView`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/CollectionDetailView.swift), [`SearchView`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/SearchView.swift)).
* **Design & Geometry**:
  * Build `FloatingTabCapsule` with `Photos` and `Collections` options.
  * Use `.regularMaterial` with `LinearGradient` specular border (`white.opacity(0.40)` to `white.opacity(0.05)`).
  * Animate active tab indicator via `matchedGeometryEffect(id: "ActiveTabIndicator", in: animation)` with a responsive spring (`response: 0.35, dampingFraction: 0.75`).
  * Ensure Dynamic Type and VoiceOver accessibility remain fully functional.

### 2. Profile Social Chips & Tab Picker Wrapping
In [`UserProfileView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/UserProfileView.swift#L223-L282):
* `SocialButton` (lines 223–239) outputs verbose labels (`"INSTAGRAM: @..."`) causing horizontal overflow and mid-text wrapping on smaller screens.
* `ProfileTabPicker` (lines 241–282) divides width across 4 fixed columns (`HStack(spacing: 0)`), causing labels like `"COLLECTIONS (6)"` to truncate on compact iPhone viewports (e.g. SE / 13 mini).
* **Solution**:
  * Transform social links into icon-led chips:
    * Instagram: SF Symbol `camera` or custom glyph with `@handle`
    * Twitter/X: SF Symbol `bubble.left` with `@handle`
    * Website: SF Symbol `globe` with `"Website"`
    * Encase in a compact glass capsule.
  * Refactor `ProfileTabPicker` to support horizontal scrolling (`ScrollView(.horizontal, showsIndicators: false)`) with decoupled count badges, matching the Material 3 fix in Android Spec 53.

### 3. Visual Search Topic Cards
In [`SearchView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/SearchView.swift):
* Popular topics are rendered as plain text capsules.
* **Solution**:
  * Convert topics into visual cards featuring subtle dark photographic gradients and bold title typography for increased visual energy.

---

## Architectural Invariants & Code Hygiene
1. **Dual-Platform Build Policy**: Verify both Android and iOS targets build without errors.
2. **Zero Compiler Warning Policy**: Clean builds with no warnings.
3. **Living Code Policy**: Reference living files via Markdown links.

---

## Acceptance Criteria
- [x] Floating glass tab capsule floats above full-bleed content with smooth sliding indicator animation.
- [x] Floating tab capsule automatically hides when child views are pushed (`path.isEmpty == false`), preventing overlap on detail screens.
- [x] Tab switching triggers light sensory feedback.
- [x] User profile social links render as compact, icon-led glass chips without text wrapping.
- [x] User profile tab picker eliminates label truncation on compact iOS viewports.
- [x] Popular topics in Search feature visual cards with bold typography.
