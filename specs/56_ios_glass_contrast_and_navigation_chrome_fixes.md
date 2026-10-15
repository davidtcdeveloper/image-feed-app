# iOS Glass Contrast, Navigation Chrome & Legibility Fixes Spec

**Status:** Proposed

## Overview
This specification addresses critical contrast bugs, muddy navigation bar chrome, and navigation bar/tab bar leaks identified during visual inspection of the iOS client:
1. **Search "POPULAR TOPICS" Contrast Bug**: Under "POPULAR TOPICS" in `SearchView.swift`, the pill buttons use `.glassCapsule(style: .ultraThin)` which renders light text over a light translucent fill on a white canvas background, washing out topic labels in light mode.
2. **Bottom Tab Bar Wash-Out**: Selected and unselected tab items wash out against light content because of hardcoded `.tint(.white)` combined with `.toolbarColorScheme(.dark, for: .tabBar)`.
3. **Heavy / Muddy Navigation Bar Chrome**: Hardcoded `.toolbarColorScheme(.dark, for: .navigationBar)` on light canvas backgrounds forces a dark/muddy translucent rectangle over the top safe area across `ContentView.swift`, `CollectionsFeedView.swift`, `SearchView.swift`, `UserProfileView.swift`, and `CollectionDetailView.swift`.
4. **Navigation Tab Bar Leak on Pushed Views**: Pushed screens (`UserProfileView`, `SearchView`, `CollectionDetailView`) do not hide the primary tab bar, leaking the tab switcher into deep navigation flows and stealing vertical real estate.
5. **Feed Author Chip Contrast**: Author badges on masonry photo cards in `PhotoCard` lack contrast over bright photos.

---

## Technical Root Cause Analysis

### 1. Search Popular Topics Pills
In [`SearchView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/SearchView.swift#L379-L390):
* Topic buttons apply `.glassCapsule(style: .ultraThin)` with `.glassVibrancy(.primary)`.
* On light mode canvases (`GlassTheme.canvasBackgroundColor`), an ultra-thin material pill over white background yields a near-white translucent surface. Combined with vibrancy resolution or white text inherited from parent modifiers, labels become virtually unreadable.
* **Fix**: Provide high-contrast styling by setting `.foregroundStyle(.primary)` and a subtle darker tint / high-contrast border or force high contrast using `GlassTheme.surfaceBackgroundColor` with specular border.

### 2. Tab Bar White-on-Gray Wash-Out
In [`ContentView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L30-L36):
* `TabView` explicitly declares `.tint(.white)` and `.toolbarColorScheme(.dark, for: .tabBar)`.
* In Light Mode, white selected icons and light gray unselected icons wash out against bright underlying imagery or light content backgrounds.
* **Fix**: Ensure tab bar tint respects semantic color schemes, or adapt `.tint(Color.primary)` so the active tab is always legible and crisp in both Light and Dark modes.

### 3. Muddy Navigation Bar Chrome
Across:
* [`ContentView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L260-L262)
* [`CollectionsFeedView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/CollectionsFeedView.swift#L129-L131)
* [`SearchView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/SearchView.swift#L227-L229)
* [`UserProfileView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/UserProfileView.swift#L101-L103)
* [`CollectionDetailView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/CollectionDetailView.swift#L243-L245)
* Each screen attaches `.toolbarColorScheme(.dark, for: .navigationBar)` regardless of system color scheme. Over a light background, this forces Apple's material shader to render dark gray chrome rather than natural glass.
* **Fix**: Remove forced `.toolbarColorScheme(.dark, for: .navigationBar)` so navigation bars dynamically adapt to system Light/Dark appearance and let photography scroll naturally underneath.

### 4. Tab Bar Leaking into Pushed Views
In [`ContentView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift):
* Pushed destinations (`SearchView`, `UserProfileView`, `CollectionDetailView`) inside `NavigationStack` do not declare `.toolbar(.hidden, for: .tabBar)`.
* In iOS Human Interface Guidelines (HIG), pushed detail views or modal search flows should hide the bottom tab bar to maximize vertical browsing space.
* **Fix**: Attach `.toolbar(.hidden, for: .tabBar)` across pushed views.

### 5. Feed Author Badge Contrast
In [`ContentView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L500-L520):
* `PhotoCard` overlays `.glassCapsule(style: .ultraThin)` directly over photo pixels. Over bright or white photos, the capsule loses boundary definition.
* **Fix**: Enforce `.environment(\.colorScheme, .dark)` on image-overlaid micro-pills with a subtle dark backing scrim (`Color.black.opacity(0.35)`) and 0.5pt specular border, guaranteeing WCAG AAA contrast regardless of photo brightness.

---

## Architectural Invariants & Code Hygiene
1. **Dual-Platform Build Policy**: Verify both Android and iOS targets build without errors.
2. **Zero Compiler Warning Policy**: Clean builds with no warnings.
3. **Living Code Policy**: Reference living files via Markdown links.

---

## Acceptance Criteria
- [ ] Popular Topics pill buttons in `SearchView` have crisp, high-contrast text in both Light and Dark modes.
- [ ] Tab bar icons and text are sharp and legible across all scroll positions and color modes.
- [ ] Top navigation bars remove forced dark scheme overrides, rendering clean Apple glass.
- [ ] Pushed views (`UserProfileView`, `SearchView`, `CollectionDetailView`) hide the bottom tab bar.
- [ ] Feed author capsules on `PhotoCard` maintain high contrast and legibility over bright and dark imagery.
