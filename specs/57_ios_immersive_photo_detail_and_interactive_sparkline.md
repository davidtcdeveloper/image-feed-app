# iOS Immersive Photo Detail Canvas & Interactive Sparkline Spec

**Status:** Implemented

## Overview
This specification addresses layout voids on the photo preview screen and upgrades static components to interactive tactile experiences:
1. **Photo Detail Canvas & Top Void**: On `PhotoDetailsView`, the photo is centered on an opaque/light background while the bottom sheet covers the bottom fraction, leaving a large empty void above the image with floating navigation buttons.
2. **Ambient Color Bleed Backdrop**: Replace the empty canvas background with an ambient full-bleed backdrop featuring a scaled, heavily blurred version of the photo with a subtle dark scrim.
3. **Interactive 30-Day Historical Views Sparkline**: Transform the static SwiftChart in `HistoricalStatsChart` into an interactive scrubbing experience with touch gestures, glowing indicator point, Catmull-Rom smoothing, and declarative sensory haptic ticks.
4. **Modernized Tactile Glass Orbs**: Refactor `GlassToolbarButton` to 40x40 single-layer `.thinMaterial` tactile orbs with `.hierarchical` SF Symbol rendering, eliminating dated double borders and heavy drop shadows.

---

## Technical Root Cause & Design Solution

### 1. Photo Detail Canvas & Ambient Backdrop
In [`PhotoDetailsView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/PhotoDetailsView.swift#L300-L394):
* `compactPhotoLayout` renders `GlassTheme.canvasBackgroundColor` (white in light mode) behind the photo.
* When the detented sheet is partially collapsed (detent `.fraction(0.35)`), the photo sits in the center, creating an uninspired white void in the upper portion of the viewport.
* **Solution**:
  * Layer a full-bleed background image using a downsampled photo URL (`photo.urls.small` or `thumb`) with `.scaleEffect(1.3)`, `.blur(radius: 60)`, and `Color.black.opacity(0.35)`.
  * This guarantees photographic continuity, eliminates the empty white space, provides contrast for the top overlay navigation buttons promoted in Spec 52 (`chevron.left` and `info.circle`), and avoids GPU fill-rate penalties.

### 2. Interactive Historical Views Sparkline
In [`PhotoDetailsView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/PhotoDetailsView.swift#L715-L742):
* `HistoricalStatsChart` renders a static line and area chart without touch response.
* **Solution**:
  * Implement drag gesture inspection via `chartOverlay` with geometry proxy.
  * When scrubbing horizontally, identify the closest `StatsValue` by date, display a glowing `PointMark` indicator with highlighted metric callout, and trigger light sensory haptics on index change.
  * Reset selection gracefully on gesture release.

### 3. Tactile Glass Orbs for Action Buttons
In [`GlassTheme.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/GlassTheme.swift#L325-L351):
* `GlassToolbarButton` currently uses a 34x34 circle with `.glassBackground(style: .ultraThin, showBorder: true, hasShadow: true)` which gives a double-ringed appearance.
* **Solution**:
  * Refactor to 40x40 tactile glass orbs with `.thinMaterial`, 0.75pt specular rim gradient border, and SF Symbols configured with `.symbolRenderingMode(.hierarchical)`.

---

## Architectural Invariants & Code Hygiene
1. **Unsplash API Compliance**: Photo hotlinking preserved, display scales accounted for, download tracking active.
2. **Dual-Platform Build Policy**: Verify both Android (`:androidApp:assembleDebug`) and iOS (`iosApp.xcodeproj`) build cleanly.
3. **Living Code Policy**: Reference living files via Markdown links.

---

## Acceptance Criteria
- [x] Photo detail screen displays full-bleed ambient blurred backdrop behind the active photo.
- [x] Upper empty white void is eliminated on compact layouts.
- [x] Historical Views chart supports smooth horizontal drag scrubbing with haptic ticks and glowing indicator point.
- [x] Navigation and toolbar buttons render as modern 40x40 tactile glass orbs with hierarchical symbols.
