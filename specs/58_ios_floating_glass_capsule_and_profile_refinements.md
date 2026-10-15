# iOS Floating Glass Tab Capsule & Profile Refinements Spec

**Status:** Proposed

## Overview
This specification details the visionOS-inspired floating tab capsule and profile screen enhancements:
1. **Floating Specular Glass Tab Capsule**: Replace the system-docked `TabView` bar with a floating `.regularMaterial` capsule featuring a sliding indicator (`matchedGeometryEffect`), active tab spring physics, and high-contrast dynamic typography.
2. **Auto-Minimizing Tab Capsule on Scroll**: Smoothly scale down and translate the floating capsule when the user scrolls downward through the masonry feed, smoothly expanding back when scrolling upward or resting.
3. **Profile Social Links Polish**: Replace verbose uppercase text buttons (`INSTAGRAM: @SANDISK`) in `UserProfileView.swift` with compact, icon-led glass chips.
4. **Visual Topic Exploration Cards in Search**: Upgrade flat text pills in `SearchView.swift` into visual topic cards with subtle curated photography backgrounds.

---

## Technical Root Cause & Design Solution

### 1. visionOS Floating Specular Glass Tab Capsule
In [`ContentView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift):
* Currently relies on standard `TabView` with `UITabBarAppearance`.
* A photography application benefits significantly from full-bleed content extending down to the bottom home indicator edge, with chrome floating cleanly above.
* **Solution**:
  * Build `FloatingTabCapsule` with `Photos` and `Collections` options.
  * Use `.regularMaterial` with `LinearGradient` specular border (`white.opacity(0.40)` to `white.opacity(0.05)`).
  * Animate active tab indicator via `matchedGeometryEffect(id: "ActiveTabIndicator", in: animation)` with a responsive spring (`response: 0.35, dampingFraction: 0.75`).
  * Ensure Dynamic Type and VoiceOver accessibility remain fully functional.

### 2. Profile Social Chips Modernization
In [`UserProfileView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/UserProfileView.swift#L223-L239):
* `SocialButton` outputs verbose labels (`"INSTAGRAM: @..."`) causing horizontal overflow and mid-text wrapping on smaller screens.
* **Solution**:
  * Transform into icon-led chips:
    * Instagram: SF Symbol `camera` or custom glyph with `@handle`
    * Twitter/X: SF Symbol `bubble.left` with `@handle`
    * Website: SF Symbol `globe` with `"Website"`
  * Encase in a compact glass capsule.

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
- [ ] Floating glass tab capsule floats above full-bleed content with smooth sliding indicator animation.
- [ ] Tab switching triggers light sensory feedback.
- [ ] User profile social links render as compact, icon-led glass chips without text wrapping.
- [ ] Popular topics in Search feature visual cards with bold typography.
