# Specification: iOS Translucent Navigation Chrome & Floating Glass Category Bar

**Status:** New / Not Implemented

## Overview
This specification modernizes the application's top navigation bars, bottom tab bar, and category filtering strip using native Apple translucency and glass surfaces. Currently, `ContentView.swift` forces a rigid, opaque dark tab bar appearance via UIKit (`UITabBarAppearance.configureWithOpaqueBackground()`), navigation bars are forced to solid opaque `#0F0F11`, and the horizontal category selector is set to a solid black strip that clips scrolling content abruptly.

This spec transitions the shell chrome to modern SwiftUI translucent materials (`.toolbarBackground(.ultraThinMaterial)`), allowing images to fluidly blur under navigation and tab bars, and turns the category selector into a floating frosted glass pill bar.

---

## Current State & Deficiencies
1. **Opaque TabBar Override**: `ContentView.swift` executes UIKit appearance styling in `init()`:
   ```swift
   let appearance = UITabBarAppearance()
   appearance.configureWithOpaqueBackground()
   appearance.backgroundColor = UIColor(red: 15 / 255, green: 15 / 255, blue: 17 / 255, alpha: 1.0)
   UITabBar.appearance().standardAppearance = appearance
   ```
   This prevents content from blurring naturally beneath the tab bar and clashes with iPadOS 18 dynamic tab bars.
2. **Opaque Navigation Bars**: `PhotosFeedTabView` applies:
   ```swift
   .toolbarBackground(Color(hex: "0F0F11"), for: .navigationBar)
   ```
   This cuts off feed scrolling abruptly at the top safe area boundary.
3. **Solid Horizontal Topic Bar**: The category selector strip is backed by a solid `Color(hex: "0F0F11")` rectangle, creating a stark visual seam between the top bar and the image feed.

---

## Architectural Changes & Implementation Plan

### 1. Modernize TabBar & NavigationBar Translucency
*   Remove the manual `UITabBarAppearance.configureWithOpaqueBackground()` block in `ContentView.swift`.
*   Replace opaque toolbar backgrounds in `PhotosFeedTabView`, `CollectionsFeedTabView`, and `SearchView` with:
    ```swift
    .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
    .toolbarBackground(.ultraThinMaterial, for: .tabBar)
    ```
*   Enable edge-to-edge content scrolling so feed photos gently blur under navigation and tab chrome according to Apple HIG.

### 2. Floating Frosted Glass Category Bar
*   Convert the horizontal topic selector in `PhotosFeedTabView` from a full-width solid bar into an elevated, floating capsule bar:
    *   Backing: `.ultraThinMaterial` in a rounded capsule or padded floating strip with specular highlight overlay.
    *   Unselected tabs: Transparent or subtle `.white.opacity(0.08)`.
    *   Selected tab: High-contrast white capsule with matched geometry indicator (`matchedGeometryEffect(id: "activeCategoryCapsule", in: categoryNamespace)`).
    *   Floating positioning: Positioned with top padding directly under the navigation bar, allowing photos to scroll smoothly beneath it.

### 3. Glass Action Toolbar Controls
*   Update toolbar buttons (Search and Shuffle in `PhotosFeedTabView`, Filter in `SearchView`, Back chevrons):
    *   Style with circular ultra-thin glass backings:
        ```swift
        Button(action: ...) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(.primary)
                .padding(8)
                .background(.ultraThinMaterial, in: Circle())
                .overlay(Circle().strokeBorder(Color.white.opacity(0.18), lineWidth: 0.5))
        }
        ```

### 4. Implementation Checklist
- [ ] Remove `UITabBarAppearance.configureWithOpaqueBackground()` in `ContentView.swift`.
- [ ] Update `PhotosFeedTabView` and `CollectionsFeedTabView` to use `.toolbarBackground(.ultraThinMaterial, for: .navigationBar, .tabBar)`.
- [ ] Transform `CategoryTabButton` and the horizontal topics scroll view into a floating frosted glass strip.
- [ ] Refactor toolbar icon buttons into circular glass controls with specular edge borders.
- [ ] Test on iPhone and iPad simulators to verify dynamic navigation and tab bar translucency under varying scroll offsets.
