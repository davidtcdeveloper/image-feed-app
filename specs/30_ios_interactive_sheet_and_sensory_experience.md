# Specification: iOS Interactive Photo Inspector, Scroll Transitions & Sensory Experience

**Status:** New / Not Implemented

## Overview
This specification modernizes the photo detail inspection workflow and interaction fidelity on iOS 17+. Currently, `PhotoDetailsView` on iPhone pushes a long vertical scrollable view where the photo immediately scrolls offscreen when inspecting metrics, EXIF data, maps, or related tags. Furthermore, feed items scroll with static rigidity, and haptic feedback is triggered imperatively via UIKit generators (`UIImpactFeedbackGenerator`).

This spec introduces a modern full-canvas photo viewer paired with an interactive frosted glass bottom sheet (`.presentationDetents`), incorporates native iOS 17 `.scrollTransition` physics to feed items, and adopts declarative SwiftUI `.sensoryFeedback`.

---

## Current State & Deficiencies
1. **Photo Scrolls Offscreen on Mobile**:
   *   In `PhotoDetailsView.swift`, when `isDualPane` is false (iPhone), the view is wrapped in a standard `ScrollView`. As the user scrolls down to view camera specs, historical charts, or location maps, the photograph itself disappears off the top of the screen.
   *   Modern media apps (e.g., Apple Photos, Apple Maps) allow the user to view the full media canvas while sliding a frosted inspection deck over it.
2. **Rigid Scroll Presentation**:
   *   Feed items in `PhotosFeedTabView` and `CollectionsFeedView` move as rigid blocks without perspective scaling, depth shifts, or smooth edge transitions during scrolling.
3. **Imperative Haptic Feedback**:
   *   Category buttons in `ContentView.swift` imperatively allocate `UIImpactFeedbackGenerator(style: .light)` inside button action closures:
       ```swift
       #if os(iOS)
       let generator = UIImpactFeedbackGenerator(style: .light)
       generator.impactOccurred()
       #endif
       ```
   *   This does not leverage SwiftUI's declarative lifecycle-managed haptic engine.

---

## Architectural Changes & Implementation Plan

### 1. Interactive Glass Photo Inspector Sheet (iPhone)
*   Refactor `PhotoDetailsView` for compact widths (iPhone):
    *   **Canvas Layer**: Display the high-resolution photo in a full-canvas, zoomable viewport (`GeometryReader` with interactive pinch/pan or double-tap to zoom).
    *   **Floating Action Bar**: Keep high-resolution download and photographer link accessible on top of the image in a floating glass pill.
    *   **Interactive Sheet (`.sheet`)**:
        *   Present `PhotoInspectorView` within a modern detented sheet:
            ```swift
            .sheet(isPresented: $showInspector) {
                PhotoInspectorView(photo: photo, viewModel: viewModel, ...)
                    .presentationDetents([.fraction(0.30), .fraction(0.65), .large])
                    .presentationDragIndicator(.visible)
                    .presentationBackground(.ultraThinMaterial)
                    .presentationCornerRadius(24)
            }
            ```
        *   Users can swipe up to inspect camera EXIF, location maps, and view charts while keeping the photo visible in the background.

### 2. Native iOS 17 Scroll Transitions (`.scrollTransition`)
*   In `PhotosFeedTabView` and `CollectionsFeedView`, apply native scroll transitions to feed cards:
    ```swift
    .scrollTransition(topLeading: .interactive, bottomTrailing: .interactive) { content, phase in
        content
            .scaleEffect(phase.isIdentity ? 1.0 : 0.95)
            .opacity(phase.isIdentity ? 1.0 : 0.82)
    }
    ```
*   Provides subtle depth and physics without expensive custom geometry listeners or third-party packages.

### 3. Declarative Sensory Feedback (`.sensoryFeedback`)
*   Replace imperative haptics with SwiftUI declarative modifiers:
    *   **Topic Selection**:
        ```swift
        .sensoryFeedback(.selection, trigger: viewModel.selectedTopicSlug)
        ```
    *   **Tab Switching**:
        ```swift
        .sensoryFeedback(.selection, trigger: selectedTab)
        ```
    *   **Download Trigger**:
        ```swift
        .sensoryFeedback(.success, trigger: downloadTriggered)
        ```
*   Ensures consistent system haptics that automatically respect user accessibility settings.

### 4. Implementation Checklist
- [ ] Refactor compact-width `PhotoDetailsView` to maintain full-canvas photo presentation with interactive detented glass inspector sheet (`.presentationDetents`).
- [ ] Add pinch-to-zoom / double-tap zoom capability to full-screen photo detail canvas.
- [ ] Implement `.scrollTransition` on `PhotoCard` and `CollectionMosaicCard`.
- [ ] Replace imperative `UIImpactFeedbackGenerator` calls with declarative `.sensoryFeedback`.
- [ ] Test sheet dragging gestures, scroll fluidness, and haptic responses on iOS 17+ Simulator and devices.
