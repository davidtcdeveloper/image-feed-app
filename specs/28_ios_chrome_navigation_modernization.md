# Specification: iOS Translucent Navigation Chrome & Floating Glass Category Bar

**Status:** Implemented

## Overview
This specification modernizes the application's top navigation bars, bottom tab bar, and category filtering strip using native Apple translucency, semantic materials, and the centralized `GlassTheme` design token system. 

Currently, `ContentView.swift` forces a rigid, opaque dark tab bar appearance via UIKit (`UITabBarAppearance.configureWithOpaqueBackground()`), navigation bars are forced to solid `#0F0F11`, and the horizontal category selector is set to a solid black strip that clips scrolling content abruptly. Furthermore, toolbar and back buttons use basic unstyled icons and text, and `PhotosFeedTabView` uses a vertical stack layout that isolates the photos grid from scrolling underneath the top chrome.

This spec transitions the shell chrome to modern SwiftUI translucent materials (`.toolbarBackground(.ultraThinMaterial)` and `.toolbarBackground(.visible)`), restructures `PhotosFeedTabView` into an edge-to-edge full-bleed scroll container where content blurs fluidly under both the navigation bar and an elevated floating frosted glass category capsule, refactors toolbar buttons into circular glass controls conforming to `GlassTheme`, integrates declarative sensory feedback, and ensures full compatibility with Accessibility Reduce Transparency and multiplatform (macOS) targets.

---

## Current State & Deficiencies

1. **Opaque TabBar Override**: `ContentView.swift` executes UIKit appearance styling in `init()`:
   ```swift
   let appearance = UITabBarAppearance()
   appearance.configureWithOpaqueBackground()
   appearance.backgroundColor = UIColor(red: 15 / 255, green: 15 / 255, blue: 17 / 255, alpha: 1.0)
   UITabBar.appearance().standardAppearance = appearance
   UITabBar.appearance().scrollEdgeAppearance = appearance
   ```
   This prevents content from blurring naturally beneath the tab bar, causes visual clipping, and clashes with modern iPadOS dynamic tab bar behaviors.
2. **Opaque Navigation Bars**: `PhotosFeedTabView` applies:
   ```swift
   .toolbarBackground(Color(hex: "0F0F11"), for: .navigationBar)
   ```
   This cuts off feed scrolling abruptly at the top safe area boundary instead of letting rich imagery scroll beneath the translucent chrome.
3. **Restricted Scroll View Hierarchy & Solid Category Bar**: In `PhotosFeedTabView.swift`, the category bar is placed above the photos grid in a `VStack`:
   ```swift
   VStack(spacing: 0) {
       ScrollView(.horizontal, showsIndicators: false) { ... }
           .frame(height: 50)
           .background(Color(hex: "0F0F11"))
       ZStack {
           ScrollView { ... } // Photos grid
       }
   }
   ```
   Because the photo grid `ScrollView` starts below the category bar, photos never reach the top safe area or blur under the navigation bar.
4. **Imperative Haptic Feedback**: Category switching invokes imperative `UIImpactFeedbackGenerator(style: .light)` rather than declarative SwiftUI `.sensoryFeedback`.
5. **Inconsistent Toolbar Actions**: Toolbar action buttons (Search, Shuffle, Filter, Back chevrons) across `PhotosFeedTabView`, `CollectionsFeedView`, `SearchView`, `CollectionDetailView`, and `UserProfileView` lack unified tactile depth, glass backings, or specular edge treatment.
6. **Token Fragmentation & Missing Reduce Transparency Support**: Ad-hoc overlays and materials bypass `GlassTheme.swift`, failing to honor `@Environment(\.accessibilityReduceTransparency)` with high-contrast semantic fallbacks.

---

## Architectural Changes & Implementation Plan

### 1. Modernize TabBar & NavigationBar Translucency
*   **Root TabBar Translucency (`ContentView.swift`)**:
    *   Remove `UITabBarAppearance.configureWithOpaqueBackground()` from `ContentView.init()`.
    *   Configure `UITabBarAppearance.configureWithDefaultBackground()` or apply SwiftUI modifiers on the root `TabView`:
        ```swift
        #if os(iOS)
        .toolbarBackground(.ultraThinMaterial, for: .tabBar)
        .toolbarBackground(.visible, for: .tabBar)
        #endif
        ```
    *   Ensure edge appearances maintain subtle material depth without abruptly flashing to complete transparency when content reaches scroll limits.
*   **Translucent Navigation Bars (`PhotosFeedTabView`, `CollectionsFeedView`, `SearchView`, `CollectionDetailView`, `UserProfileView`)**:
    *   Replace hardcoded `Color(hex: "0F0F11")` toolbar backgrounds with:
        ```swift
        #if os(iOS)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        #endif
        ```
    *   Ensure content extends edge-to-edge behind the navigation bar on all views.

### 2. Full-Bleed Scroll Layout & Floating Frosted Glass Category Bar (`PhotosFeedTabView`)
*   **Restructure Layout to Full-Bleed Edge-to-Edge**:
    *   Eliminate the outer `VStack(spacing: 0)`. Promote the photo `ScrollView` to the full view canvas.
    *   Use `.contentMargins(.top, 64, for: .scrollContent)` (or `.safeAreaInset(edge: .top)`) so the initial row of photo cards starts comfortably below the floating bar, while scrolled items pass smoothly beneath the category bar and up into the navigation bar.
*   **Floating Frosted Glass Category Capsule**:
    *   Elevate the category selector into a floating bar positioned below the navigation bar with subtle vertical floating padding (e.g. 6pt).
    *   Container Backing: Apply `GlassTheme` styling via `.glassBackground(style: .ultraThin, shape: Capsule(), showBorder: true, hasShadow: true)` or `.glassCapsule(style: .ultraThin, showBorder: true, hasShadow: true)`.
    *   Edge Masking / Fade: Apply a subtle horizontal gradient mask (`LinearGradient` with clear edges) or gentle horizontal insets so categories gracefully fade at the capsule edges during horizontal scrolling instead of clipping sharply.
    *   Unselected items: Transparent or subtle `.white.opacity(0.08)` pill backing with secondary glass vibrancy (`.glassVibrancy(.secondary)`).
    *   Selected item: High-contrast white capsule with primary dark text (`.foregroundColor(.black)`), animated smoothly via `matchedGeometryEffect(id: "activeCategoryCapsule", in: categoryNamespace)`.
*   **Declarative Sensory Feedback**:
    *   Replace `UIImpactFeedbackGenerator` with declarative `.sensoryFeedback(.selection, trigger: viewModel.selectedTopicSlug)`.

### 3. Glass Action Toolbar Controls
*   **Unified Circular Glass Toolbar Buttons**:
    *   Create or standardize a reusable `GlassToolbarButton` component or view extension built upon `GlassTheme.swift`:
        ```swift
        struct GlassToolbarButton: View {
            let systemName: String
            let action: () -> Void
            
            var body: some View {
                Button(action: action) {
                    Image(systemName: systemName)
                        .font(.system(size: 14, weight: .semibold))
                        .glassVibrancy(.primary)
                        .frame(width: 34, height: 34)
                        .glassBackground(style: .ultraThin, shape: Circle(), showBorder: true, hasShadow: true)
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
        ```
    *   Update toolbar buttons across the application:
        *   `PhotosFeedTabView`: Search and Shuffle buttons.
        *   `CollectionsFeedView`: Search button.
        *   `SearchView`: Filter button.
        *   `CollectionDetailView`: Custom Back button (circular glass button with `chevron.left`).
        *   `UserProfileView`: Custom Back button and Safari action button.
    *   Ensure multiplatform toolbar placement compatibility (`#if os(iOS)` with `.navigationBarLeading` / `.navigationBarTrailing`, falling back to macOS `.navigation` / `.primaryAction`).

### 4. Accessibility & Multiplatform Support
*   **Accessibility Reduce Transparency**:
    *   All glass elements leverage `GlassBackgroundModifier` from `GlassTheme.swift`.
    *   When `@Environment(\.accessibilityReduceTransparency)` is active, translucent surfaces automatically degrade to high-contrast opaque `GlassTheme.fallbackBackgroundColor` (`Color(uiColor: .secondarySystemBackground)`) with enhanced contrast stroke borders (`GlassTheme.fallbackBorderColor`).
*   **macOS Target Compatibility (`#if os(iOS)`)**:
    *   Guard all iOS-specific modifiers (`.toolbarBackground(..., for: .tabBar)`, `.navigationBarTitleDisplayMode`, `UITabBarAppearance`) with `#if os(iOS)` to ensure `macosApp` builds cleanly.

---

## Implementation Checklist

- [x] **Tab Bar Modernization (`ContentView.swift`)**:
  - [x] Remove `UITabBarAppearance.configureWithOpaqueBackground()`.
  - [x] Configure `.toolbarBackground(.ultraThinMaterial, for: .tabBar)` with `.toolbarBackground(.visible, for: .tabBar)`.
- [x] **Navigation Bar Modernization Across Views**:
  - [x] Apply `.toolbarBackground(.ultraThinMaterial, for: .navigationBar)` and `.toolbarBackground(.visible, for: .navigationBar)` on `PhotosFeedTabView`, `CollectionsFeedView`, and `SearchView`.
  - [x] Ensure `CollectionDetailView` and `UserProfileView` apply translucent navigation chrome and match the dark color scheme.
- [x] **Edge-to-Edge Layout & Floating Category Bar (`PhotosFeedTabView.swift`)**:
  - [x] Restructure `PhotosFeedTabView` layout to full-bleed `ScrollView` with top content margins.
  - [x] Wrap horizontal category picker in a floating glass capsule using `GlassTheme.swift`.
  - [x] Apply smooth horizontal scroll fading/edge masking to the category scroll container.
  - [x] Replace `UIImpactFeedbackGenerator` with declarative `.sensoryFeedback(.selection, trigger: viewModel.selectedTopicSlug)`.
- [x] **Unified Glass Toolbar Controls**:
  - [x] Implement reusable circular glass button styling conforming to `GlassTheme`.
  - [x] Refactor toolbar buttons in `PhotosFeedTabView`, `CollectionsFeedView`, and `SearchView`.
  - [x] Refactor custom back buttons in `CollectionDetailView` and `UserProfileView` to circular glass chevron controls.
  - [x] Verify macOS `#if os(iOS)` placement compatibility across all modified toolbars.
- [x] **Verification & Testing**:
  - [x] Verify iOS simulator build: `cd iosApp && xcodegen && xcodebuild -project iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`.
  - [x] Verify macOS build: `cd iosApp && xcodebuild -project iosApp.xcodeproj -scheme macosApp -destination 'platform=macOS' build CODE_SIGNING_ALLOWED=NO`.
  - [x] Run Swift static analysis: `swiftlint lint iosApp/iosApp`.
  - [x] Verify translucency, fluid scrolling, and contrast in both Light/Dark modes and with Accessibility "Reduce Transparency" toggled on.
