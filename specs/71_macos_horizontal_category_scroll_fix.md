# macOS Horizontal Category Tab Scrolling & Desktop Navigation Spec

**Status:** Approved

## Overview

This specification defines the root cause analysis and architectural solution for the horizontal scrolling defect in the top category tab bar ([`FeedCategoryBar`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L597-L654)) on macOS (`macosApp`), as well as consistent horizontal scroll ergonomics across secondary horizontal chip strips ([`ProfileTabPicker`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/UserProfileView.swift#L280-L346)).

---

## Problem Description

On macOS, the top topic/category bar rendered at the top of [`PhotosFeedTabView`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L280-L595) via [`FeedCategoryBar`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L597-L654) contains `"Editorial"` plus the list of Unsplash topics (`Topic`). When the total width of topic pills exceeds the window width, users on macOS cannot reliably scroll the bar horizontally to reach off-screen categories.

---

## Root Cause Analysis

1. **AppKit `NSScrollView` Pointer Semantics & Hidden Scroller**:
   * On iOS (`UIScrollView`), touch-dragging anywhere on [`FeedCategoryBar`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L605) pans the horizontal scroll view.
   * On macOS (`NSScrollView`), pointer click-and-drag gestures do **not** pan scroll views, and `ScrollView(.horizontal, showsIndicators: false)` hides the horizontal scrollbar thumb. Users with a standard mouse have no visual or interactive affordance to scroll horizontally.

2. **Vertical Mouse Wheel Event Forwarding to Outer Vertical `ScrollView`**:
   * [`FeedCategoryBar`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L405-L407) is mounted inside `.safeAreaInset(edge: .top, spacing: 0)` of the outer vertical photo grid `ScrollView` ([`ContentView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L348-L408)).
   * Standard mouse scroll wheels emit vertical scroll deltas (`scrollingDeltaY` with `scrollingDeltaX == 0`).
   * Because the category `ScrollView` is configured strictly for the `.horizontal` axis, AppKit's underlying `NSScrollView` ignores purely vertical wheel events and forwards them up the responder chain to the parent vertical feed `ScrollView`, scrolling the photo masonry grid instead of the category bar.

3. **`LazyHStack` Sizing & Masking Inside `NSScrollView`**:
   * [`FeedCategoryBar`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L606-L649) wraps a small, bounded list of topic capsules (~10–15 items) in a `LazyHStack` and applies `.mask` and `.glassBackground(..., shape: Capsule())` directly to the `ScrollView`.
   * On macOS, combining `LazyHStack` deferred width measurement with offscreen masking/clipping on the `ScrollView` container can cause the underlying `NSScrollView` `documentView` to under-report its scrollable content width or interfere with hit-testing. Switching to an eager `HStack` ensures full content geometry is known immediately on layout pass.

4. **Missing Programmatic Scroll-to-Selection (`ScrollViewReader`) & Step Controls**:
   * Selecting a partially visible category pill near the leading or trailing edge updates `selectedTopicSlug` ([`ContentView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L574-L578)) without scrolling the selected pill into view (`scrollTo(_:anchor:)`).
   * Desktop users lack leading/trailing chevron step buttons (`<` / `>`) to page or step through topics with a single click.

---

## Proposed Solution

### 1. Eager `HStack` + `ScrollViewReader` Auto-Centering (Cross-Platform)
* In [`FeedCategoryBar`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L597-L654):
  * Replace `LazyHStack(spacing: 8)` with an eager `HStack(spacing: 8)` so the full horizontal content width is computed deterministically on both iOS and macOS.
  * Wrap the horizontal `ScrollView` content in a `ScrollViewReader`.
  * Assign explicit `.id("editorial")` to the Editorial button and `.id(topic.slug)` to each topic [`CategoryTabButton`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L656-L682).
  * Observe `selectedTopicSlug` via `.onChange(of: selectedTopicSlug)` and animate `proxy.scrollTo(newSlug, anchor: .center)` using `.spring(response: 0.35, dampingFraction: 0.8)` so selecting any partially visible tab smoothly centers it and reveals adjacent tabs.

### 2. macOS Leading & Trailing Glass Chevron Step Controls (`#if os(macOS)`)
* In [`FeedCategoryBar`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L597-L654), when compiled for `#if os(macOS)` and `!topics.isEmpty`:
  * Provide compact leading (`chevron.backward`) and trailing (`chevron.forward`) step buttons integrated at the ends of the category bar capsule (or flanking the scroll region) styled with `GlassTheme` vibrancy tokens (`.glassVibrancy(.primary)` / `.glassVibrancy(.secondary)`).
  * Clicking the leading or trailing chevron steps/scrolls horizontally through the ordered slug list (`["editorial"] + topics.map(\.slug)`) via `ScrollViewReader` (jumping a visible page window of ~3–4 topics or stepping to the previous/next topic and centering it) so desktop mouse users have a discoverable 1-click affordance.
  * Localize button accessibility labels (`"Previous categories"` / `"Next categories"`) in [`Localizable.xcstrings`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/Localizable.xcstrings) using RTL-safe directional SF Symbols (`chevron.backward` and `chevron.forward`).

### 3. macOS Vertical Mouse-Wheel to Horizontal Scroll Translation (`#if os(macOS)`)
* Implement a reusable `#if os(macOS)` helper modifier or `NSViewRepresentable` bridge (e.g., `MacOSHorizontalScrollWheelModifier`) applied to [`FeedCategoryBar`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L597-L654) (and [`ProfileTabPicker`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/UserProfileView.swift#L280-L346)):
  * Locate the backing `NSScrollView` (or monitor `.scrollWheel` events while the pointer hovers over the horizontal bar) and translate vertical wheel deltas (`event.scrollingDeltaY` when `abs(event.scrollingDeltaY) > abs(event.scrollingDeltaX)`) into horizontal `contentView` origin offsets clamped to `0 ... max(0, documentView.width - contentView.width)`.
  * Consume the translated wheel event when hovering over the category bar so it does not leak to the parent vertical masonry `ScrollView`.
  * Preserve native horizontal trackpad two-finger swipe gestures (`scrollingDeltaX`) without interference.
  * Ensure any event monitor or observer is deterministically cleaned up in `onDisappear` / `deinit`.

### 4. Container Clipping & Mask Hygiene
* Apply the edge-fade `.mask` directly to the inner `ScrollView` viewport rather than the outer `.glassBackground` border/shadow wrapper in [`FeedCategoryBar`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L638-L649), ensuring `NSScrollView` hit-testing and macOS chevron controls remain crisp and unobstructed.

---

## Architectural Invariants & Compliance

1. **Apple Glass Design & HIG ([`ios-glass-design`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/.agents/skills/ios-glass-design/SKILL.md))**:
   * All controls and surfaces in [`FeedCategoryBar`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L597-L654) must use [`GlassTheme`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/GlassTheme.swift) tokens (`.glassBackground`, `.glassVibrancy`, `accessibilityReduceTransparency` fallbacks) and adapt cleanly to both Light and Dark mode.
2. **Localization & RTL Safety ([`localization`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/.agents/skills/localization/SKILL.md))**:
   * Use auto-mirroring directional icons (`chevron.backward`, `chevron.forward`) and register any new accessibility labels in [`Localizable.xcstrings`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/Localizable.xcstrings).
3. **Dual-Platform Build & Zero Warning Policy**:
   * Verify compilation with zero warnings across both `iosApp` and `macosApp` targets via `xcodegen` and `xcodebuild`.

---

## Implementation Steps

1. **Refactor `FeedCategoryBar` in [`ContentView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L597-L654)**:
   * Replace `LazyHStack` with `HStack` inside a `ScrollViewReader`.
   * Attach `.id("editorial")` and `.id(topic.slug)` to each [`CategoryTabButton`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L656-L682).
   * Add `.onChange(of: selectedTopicSlug)` to animate `proxy.scrollTo(selectedTopicSlug, anchor: .center)`.
2. **Add macOS Horizontal Scroll Wheel Support & Step Chevrons (`#if os(macOS)`)**:
   * Implement vertical-to-horizontal scroll wheel translation on macOS when hovering over [`FeedCategoryBar`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L597-L654) and [`ProfileTabPicker`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/UserProfileView.swift#L280-L346).
   * Add leading and trailing glass chevron buttons on `#if os(macOS)` in [`FeedCategoryBar`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L597-L654) to scroll horizontally across topics on click.
3. **Update String Catalog ([`Localizable.xcstrings`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/Localizable.xcstrings))**:
   * Add localized entries for the macOS category scroll button accessibility labels (`"Previous categories"`, `"Next categories"`).
4. **Verify Formatting, Linting & Dual-Platform Builds**:
   * Run `swiftformat --lint .` and `swiftlint`.
   * Generate Xcode project via `xcodegen generate --spec iosApp/project.yml`.
   * Build `macosApp` (`-scheme macosApp -destination 'platform=macOS'`) and `iosApp` (`-scheme iosApp -destination 'generic/platform=iOS Simulator'`).

---

## Acceptance Criteria

- [ ] `FeedCategoryBar` uses an eager `HStack` wrapped in `ScrollViewReader` with stable item IDs (`"editorial"` and `topic.slug`) and smoothly scrolls the selected category into view on selection.
- [ ] On macOS, hovering over `FeedCategoryBar` (or `ProfileTabPicker`) and scrolling with a standard vertical mouse scroll wheel translates vertical wheel deltas into horizontal scrolling of the tab bar without scrolling the underlying vertical photo grid.
- [ ] On macOS, native two-finger horizontal trackpad swipes scroll `FeedCategoryBar` smoothly across the full width of loaded topics.
- [ ] On macOS, leading and trailing chevron buttons (`chevron.backward` / `chevron.forward`) allow mouse users to scroll through categories with a single click, using `GlassTheme` vibrancy and localized accessibility labels.
- [ ] iOS category bar appearance, touch scrolling, and `matchedGeometryEffect` selection capsule animation remain intact with zero regressions.
- [ ] Both `iosApp` and `macosApp` targets compile cleanly with zero warnings, and `swiftformat --lint .` / `swiftlint` pass with zero violations.
