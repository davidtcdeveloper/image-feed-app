# Specification: iOS Interactive Photo Inspector, Scroll Transitions & Sensory Experience

**Status:** Implemented

## Overview
This specification modernizes the photo detail inspection workflow and interaction fidelity on iOS 17+. Currently, `PhotoDetailsView` on iPhone pushes a long vertical scrollable view where the photo immediately scrolls offscreen when inspecting metrics, EXIF data, maps, or related tags. Furthermore, feed items scroll with static rigidity, and while topic switching adopted declarative sensory feedback in Step 30, tactile feedback remains missing across tab switching, downloads, and random shuffle actions.

This spec introduces a modern full-canvas photo viewer paired with an interactive, non-modal frosted glass bottom sheet (`.presentationDetents`, `.presentationBackgroundInteraction`), incorporates native iOS 17 `.scrollTransition` physics across all card grids, and completes the adoption of declarative SwiftUI `.sensoryFeedback` while maintaining full macOS (`macosApp`) compilation and Accessibility Reduce Transparency compliance.

---

## Current State & Deficiencies

1. **Photo Scrolls Offscreen on Mobile**:
   * In `PhotoDetailsView.swift`, when `isDualPane` is false (iPhone / compact width), the view is wrapped in a standard `ScrollView`. As the user scrolls down to view camera specs, historical charts, or location maps, the photograph itself disappears off the top of the screen.
   * Modern media apps (e.g., Apple Photos, Apple Maps) allow the user to view the full media canvas while sliding a frosted inspection deck over it without obscuring the canvas completely.
2. **Legacy Layout Artifact in Inspector**:
   * In `PhotoInspectorView` (within `PhotoDetailsView.swift`), when `showPhotographerHeader` is false, the photographer row uses an ad-hoc negative top padding (`.padding(.top, -30)`) designed to overlap the photo in the old vertical stack. When hosted in an interactive sheet, this negative margin collides with the drag indicator and rounded sheet boundaries.
3. **Rigid Scroll Presentation Across Card Grids**:
   * Feed cards and collection items across `PhotosFeedTabView`, `CollectionsFeedView`, `SearchView`, `CollectionDetailView`, and `UserProfileView` move as rigid blocks without perspective scaling, depth shifts, or smooth edge transitions during scrolling.
4. **Incomplete Sensory Feedback Coverage**:
   * While topic switching was migrated to `.sensoryFeedback(.selection, trigger: viewModel.selectedTopicSlug)` in Step 30, tactile feedback is absent from:
     * Root tab switching in `ContentView.swift` (`selectedTab`).
     * High-resolution photo download button triggers in `PhotoDetailsView.swift`.
     * Random photo shuffle actions (shake gesture or toolbar shuffle button).

---

## Architectural Changes & Implementation Plan

### 1. Interactive Glass Photo Inspector Sheet (iPhone / Compact Width)

* **Refactor Compact `PhotoDetailsView`**:
  * **Full-Canvas Viewport & Proportional Sizing**:
    * **Full-Width Sizing**: Render the image fitting the exact screen/container width (`containerWidth = geo.size.width`), computing height proportionally (`calculatedHeight = containerWidth / aspectRatio`) centered within the canvas viewport.
    * **Hero Geometry Source Management**: Ensure `PhotoDetailsView` acts as the active geometry source (`isSource: true`) for `matchedGeometryEffect(id: "photo-img-\(photoId)", in: heroNamespace)` while `PhotoCard` yields source status (`isHeroSource: selectedPhotoForHero?.id != photo.id`). This prevents thumbnail-scale downscaling during modal presentation.
    * **Scale Boundaries**: Clamp zoom scale between `1.0x` (identity) and `4.0x`.
    * **Double-Tap Action**: Double-tapping toggles between `1.0x` and `2.5x` with spring animation.
    * **Pan Boundaries**: Restrict pan translation based on current scale and canvas height (`containerWidth * (scale - 1) / 2` and `max(0, (calculatedHeight * scale - screenHeight) / 2)`) so the image cannot be dragged indefinitely offscreen.
    * **Hero Animation Protection**: Ensure scale and offset state reset to identity (`scale = 1.0`, `offset = .zero`) upon dismissing the screen so `matchedGeometryEffect(id: "photo-img-\(photoId)", in: heroNamespace)` animates smoothly back to the grid item without transform jumps.
  * **Floating Glass Action Bar**:
    * Position a floating glass capsule over the bottom or top-trailing area of the canvas containing:
      * Quick photographer attribution avatar and name.
      * High-resolution download button with tactile feedback.
      * Inspector toggle button (`Image(systemName: "info.circle")`) to allow reopening the inspector sheet if dismissed.
  * **Interactive Non-Modal Sheet (`.sheet`)**:
    * Present `PhotoInspectorView` within a detented sheet configured for non-modal background interaction, anchored in dark color scheme with high-contrast frosted glass backing:
      ```swift
      #if os(iOS)
      .sheet(isPresented: $showInspector) {
          PhotoInspectorView(
              photo: photo,
              viewModel: viewModel,
              onUserSelect: { username in
                  showInspector = false
                  onUserSelect(username)
              },
              onTagSelect: { tag in
                  showInspector = false
                  onTagSelect(tag)
              },
              onDownload: {
                  downloadFeedbackTrigger += 1
              }
          )
          .environment(\.colorScheme, .dark)
          .preferredColorScheme(.dark)
          .presentationDetents([.fraction(0.35), .fraction(0.70), .large])
          .presentationBackgroundInteraction(.enabled(upThrough: .fraction(0.70)))
          .presentationDragIndicator(.visible)
          .presentationCornerRadius(24)
          .presentationBackground {
              if reduceTransparency {
                  Color(hex: "0F0F11")
              } else {
                  ZStack {
                      Color(hex: "0A0A0C").opacity(0.82)
                      Rectangle().fill(.regularMaterial)
                  }
              }
          }
      }
      #endif
      ```
    * **Contrast-Safe Sheet Scrim Foundation**: Combining `Rectangle().fill(.regularMaterial)` with an underlying `Color(hex: "0A0A0C").opacity(0.82)` dark scrim prevents bright canvas photos (e.g. snow, white architecture) from bleeding through and washing out text at the 35% and 70% detents, locking contrast to $\ge 7:1$ (exceeding WCAG AAA).
    * **Color Scheme Isolation Fix**: Attaching `.preferredColorScheme(.dark)` and `.environment(\.colorScheme, .dark)` to the sheet prevents it from defaulting to system Light Mode, eliminating white-on-white text collisions.
    * **Simultaneous Canvas Interaction**: `.presentationBackgroundInteraction(.enabled(upThrough: .fraction(0.70)))` ensures the user can pinch, pan, or tap the photo behind the sheet while the inspector rests at the compact (`35%`) or medium (`70%`) detent.
    * **Clean Navigation Handoff**: When a user taps a tag or photographer profile from within `PhotoInspectorView`, the sheet explicitly sets `showInspector = false` before invoking `onTagSelect` / `onUserSelect`. This prevents the parent `NavigationStack` from pushing views behind an active modal sheet.
    * **Inspector Layout Clean-Up**: Remove the legacy `.padding(.top, -30)` hack in `PhotoInspectorView`, standardizing on unified header presentation inside the sheet.
    * **Multiplatform Compatibility**: Wrap all sheet modifiers and iOS-only APIs in `#if os(iOS)` to preserve clean builds for `macosApp`.

### 2. Universal Native iOS 17 Scroll Transitions (`.scrollTransition`)

* Apply native SwiftUI `.scrollTransition` across all card grids to introduce fluid entry and exit dynamics without custom geometry listeners:
  ```swift
  .scrollTransition(topLeading: .interactive, bottomTrailing: .interactive) { content, phase in
      content
          .scaleEffect(phase.isIdentity ? 1.0 : 0.96)
          .opacity(phase.isIdentity ? 1.0 : 0.85)
  }
  ```
* **Coverage Scope**:
  * `PhotoCard` in `PhotosFeedTabView` (Feed).
  * `CollectionMosaicCard` in `CollectionsFeedView`.
  * Grid photo cards in `SearchView`, `CollectionDetailView`, and `UserProfileView`.
* May be implemented via a reusable ViewModifier (e.g. `FeedCardScrollTransition`) to guarantee identical physics and curve parameters across all screens.

### 3. Declarative Sensory Feedback (`.sensoryFeedback`)

* Extend declarative SwiftUI `.sensoryFeedback` to complete haptic coverage:
  * **Tab Switching (`ContentView.swift`)**:
    ```swift
    TabView(selection: $selectedTab) { ... }
        .sensoryFeedback(.selection, trigger: selectedTab)
    ```
  * **Download Trigger (`PhotoDetailsView.swift`)**:
    * Introduce a feedback trigger counter in `PhotoDetailsView`:
      ```swift
      @State private var downloadFeedbackTrigger = 0
      ```
    * Increment `downloadFeedbackTrigger += 1` inside the download action button.
    * Bind `.sensoryFeedback(.success, trigger: downloadFeedbackTrigger)`.
  * **Random Photo Shuffle (`ContentView.swift`)**:
    * Trigger `.sensoryFeedback(.impact(weight: .medium), trigger: shakeFeedbackTrigger)` on random photo shuffle.
* Ensures system-governed haptics that automatically honor the user's Accessibility vibration preferences and compile cleanly across targets.

### 4. Accessibility, Contrast & Platform Guidelines Compliance

* **High-Contrast Grounded Sheet Scrim**: In accordance with `ai-rules/apple-design.md`, sheet materials over dynamic photography must guarantee legibility. A dark base scrim (`0A0A0C` at 82% opacity) layered with `.regularMaterial` eliminates translucency washouts.
* **Semantic Vibrancy Hierarchy**:
  * Title, photographer name, and description: `.glassVibrancy(.primary)` / `.foregroundStyle(.white)`.
  * Subtext, handles, timestamps, section headers, and EXIF row labels: `.glassVibrancy(.secondary)` / `.foregroundStyle(.white.opacity(0.68))` (replacing static `.foregroundColor(.gray)`).
  * Tag badges and EXIF values: `.glassVibrancy(.primary)`.
* **Floating Pill Contrast Scrim**: `compactFloatingBar` pill uses an underlying dark tint (`Color.black.opacity(0.4)`) beneath the glass capsule modifier to remain legible over white or daylight canvas pixels.
* **Reduce Transparency**: All sheet and floating bar materials inspect `@Environment(\.accessibilityReduceTransparency)` and fall back to solid dark surface `Color(hex: "0F0F11")` when enabled.
* **macOS Compilation**: Ensure that all iOS 16/17 sheet detent modifiers, `UIImpactFeedbackGenerator` references (if any), and UIKit-specific types are enclosed in `#if os(iOS)` blocks so `macosApp` builds without compiler errors or warnings.

---

## Implementation Checklist

- [x] **Photo Canvas & Gestures**:
  - [x] Ensure photo fits full container width (`containerWidth = geo.size.width`) and adjusts height proportionally (`containerWidth / aspectRatio`).
  - [x] Configure hero geometry source ownership (`PhotoDetailsView` as active source, `PhotoCard` yielding during presentation) to prevent thumbnail-scale shrinkage.
  - [x] Implement full-canvas zoomable image container in `PhotoDetailsView` for compact widths with scale clamping (`1.0x` - `4.0x`), pan translation, and double-tap zoom toggle.
  - [x] Add zoom state reset logic on view dismissal to guarantee smooth, glitch-free `matchedGeometryEffect` hero transitions.
  - [x] Implement floating glass action pill over canvas with photographer info, download button, and inspector toggle button (`info.circle`).
  - [x] Add dark contrast backing tint to floating action pill for high-luminosity canvas photos.
- [x] **Interactive Detented Sheet & Contrast**:
  - [x] Refactor compact `PhotoDetailsView` to present `PhotoInspectorView` in `.sheet(isPresented: $showInspector)`.
  - [x] Configure `.presentationDetents([.fraction(0.35), .fraction(0.70), .large])`.
  - [x] Configure `.presentationBackgroundInteraction(.enabled(upThrough: .fraction(0.70)))` for concurrent photo interaction.
  - [x] Configure `.presentationDragIndicator(.visible)` and `.presentationCornerRadius(24)`.
  - [x] Anchor sheet in `.preferredColorScheme(.dark)` and `.environment(\.colorScheme, .dark)` to fix Light Mode white-on-white collision.
  - [x] Layer `.regularMaterial` with `Color(hex: "0A0A0C").opacity(0.82)` dark scrim in `.presentationBackground` for $\ge 7:1$ WCAG AAA contrast over bright images.
  - [x] Add `@Environment(\.accessibilityReduceTransparency)` fallback for sheet background (`Color(hex: "0F0F11")`).
  - [x] Clean up `PhotoInspectorView` legacy `-30pt` negative top padding and unify header presentation.
  - [x] Ensure tag selection and user profile selection dismiss the sheet before pushing onto the parent `NavigationPath`.
  - [x] Wrap all sheet detents and iOS-specific modifiers in `#if os(iOS)`.
- [x] **Typography & Semantic Vibrancy**:
  - [x] Replace static `.foregroundColor(.white)` on title, photographer name, and description with `.glassVibrancy(.primary)`.
  - [x] Replace static `.foregroundColor(.gray)` on handles, section headers, and EXIF labels with `.glassVibrancy(.secondary)` (`.foregroundStyle(.white.opacity(0.68))`).
  - [x] Standardize `ExifRowView` to use semantic vibrancy styles for label and value.
  - [x] Update `HistoricalStatsChart` with contrast-safe line and fill styles.
- [x] **Native Scroll Transitions**:
  - [x] Implement `.scrollTransition` (scale `0.96`, opacity `0.85`) on `PhotoCard` and `CollectionMosaicCard`.
  - [x] Apply scroll transitions to photo grid cards in `SearchView`, `CollectionDetailView`, and `UserProfileView`.
- [x] **Declarative Sensory Feedback**:
  - [x] Attach `.sensoryFeedback(.selection, trigger: selectedTab)` on root `TabView` in `ContentView.swift`.
  - [x] Add `@State private var downloadFeedbackTrigger = 0` and wire `.sensoryFeedback(.success, trigger: downloadFeedbackTrigger)` on `PhotoDetailsView`.
  - [x] Attach `.sensoryFeedback` on random photo shuffle trigger in `ContentView.swift`.
- [x] **Verification**:
  - [x] Verify dual-platform compilation: `./gradlew :shared:compileKotlinIosSimulatorArm64` and `xcodebuild` (iOS Simulator + macOS).
  - [x] Verify non-modal gesture interaction (pinch/pan canvas while sheet is at 35% and 70% detents).
  - [x] Verify high-contrast appearance over pure white and pitch black photos in both Light and Dark mode.
  - [x] Verify Accessibility Reduce Transparency appearance.
