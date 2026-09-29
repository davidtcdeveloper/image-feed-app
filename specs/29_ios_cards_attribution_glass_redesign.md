# Specification: iOS Floating Glass Attribution Chips & Card Modernization

**Status:** Implemented

## Overview
This specification modernizes image cards, attribution overlays, collection mosaics, and photographer profile elements across the iOS and macOS targets (`iosApp` and `macosApp`). Currently, `PhotoCard` and `CollectionPhotoGridCard` overlay photographer credentials using a heavy linear gradient (`LinearGradient(colors: [.clear, .black.opacity(0.75)])`) that stretches across the bottom portion of the image, darkening photo composition. Furthermore, metadata cards and metric tiles in `PhotoDetailsView` and `UserProfileView` rely on flat solid dark backgrounds (`#1E1E24`), while collection cards and profile headers use hardcoded opaque backgrounds (`#0F0F11`) and ad-hoc opacity fills.

This spec replaces heavy gradient scrims with elegant inset floating glass attribution capsules leveraging `GlassTheme.swift`, refactors card containers with glassmorphic depth, specular edge lighting, and Dynamic Type vibrancy, and refines image loading placeholders with frosted material aesthetics.

---

## Current State & Deficiencies

1. **Heavy Dark Gradient on Photos**:
   * `PhotoCard` in `ContentView.swift` and `CollectionPhotoGridCard` in `CollectionDetailView.swift` apply:
     ```swift
     LinearGradient(
         colors: [.clear, .black.opacity(0.75)],
         startPoint: .top,
         endPoint: .bottom
     )
     ```
     This covers up the bottom 25–30% of the image with a murky black tint, obscuring details in landscape, architectural, or low-key photography.
2. **Duplicated Grid Card Implementations**:
   * `ContentView.swift` (`PhotoCard`) and `CollectionDetailView.swift` (`CollectionPhotoGridCard`) have near-identical card implementations with duplicated gradient overlays, image decoding, and tap gesture structures.
3. **Flat Solid Metadata Containers**:
   * `MetricCard` in `PhotoDetailsView.swift` and `UserProfileView.swift` uses solid `Color(hex: "1E1E24")` and hardcoded static text colors (`.white`, `.gray`).
   * `CollectionMosaicCard` in `CollectionsFeedView.swift` applies a solid `Color(hex: "0F0F11")` container background and uses an ad-hoc `.background(Color.white.opacity(0.12))` photo count capsule.
   * `ProfileHeaderView` in `UserProfileView.swift` uses ad-hoc `Color.white.opacity(0.1)` backgrounds for `SocialButton` and lacks cohesive glass styling.
4. **Bypassing the Centralized `GlassTheme` Design System**:
   * Views across screens rely on ad-hoc strokes, solid hex values, or direct material calls rather than leveraging the unified `GlassTheme.swift` system (`.glassCapsule(...)`, `.glassCard(...)`, `GlassVibrancy`), thereby missing built-in accessibility accommodations for `@Environment(\.accessibilityReduceTransparency)` and `@Environment(\.colorSchemeContrast)`.
5. **Flat Color Loading Placeholders**:
   * `PhotoCard` and `CollectionPhotoGridCard` render a flat `RoundedRectangle.fill(Color(hex: photo.color))` placeholder without texture or depth during network latency.

---

## Architectural Changes & Implementation Plan

### 1. Inset Floating Glass Attribution Capsule
Replace the full-width linear gradient bottom scrim in both `PhotoCard` (`ContentView.swift`) and `CollectionPhotoGridCard` (`CollectionDetailView.swift`) with an inset floating frosted glass capsule.

* **Design System Integration**: Use `.glassCapsule(style: .ultraThin, showBorder: true, hasShadow: true)` from `GlassTheme.swift` to automatically support specular gradient borders and high-contrast accessibility fallbacks.
* **Typography & Dynamic Type**: Bind text to semantic typography scales (`.font(.caption2.weight(.semibold))`) and apply `.glassVibrancy(.primary)` conforming to Apple HIG.
* **Touch Targets & Gesture Isolation**: Isolate capsule interaction using `.contentShape(Capsule())` and a dedicated `Button` so tapping photographer attribution consistently routes to their in-app profile (`onUserSelect(photo.user.username)`, per Spec 33) across all card grids rather than launching an external browser.
* **Layout Blueprint**:
  ```swift
  ZStack(alignment: .bottomLeading) {
      KFImage(URL(string: imageUrl))
          .placeholder {
              cardPlaceholder(aspectRatio: aspectRatio, colorHex: photo.color)
          }
          .fade(duration: 0.25)
          .resizable()
          .aspectRatio(aspectRatio, contentMode: .fit)
          .matchedGeometryEffect(id: "photo-img-\(photo.id)", in: heroNamespace)
          .clipShape(RoundedRectangle(cornerRadius: 12))
          .contentShape(Rectangle())
          .onTapGesture {
              onSelect(photo.id)
          }

      // Floating Glass Attribution Capsule
      Button(action: {
          onUserSelect(photo.user.username)
      }) {
          HStack(spacing: 6) {
              KFImage(URL(string: photo.user.profileImage.small))
                  .resizable()
                  .aspectRatio(contentMode: .fill)
                  .frame(width: 20, height: 20)
                  .clipShape(Circle())

              Text(photo.user.name)
                  .font(.caption2.weight(.semibold))
                  .glassVibrancy(.primary)
                  .lineLimit(1)
                  .truncationMode(.tail)
          }
          .padding(.horizontal, 8)
          .padding(.vertical, 5)
          .glassCapsule(style: .ultraThin, showBorder: true, hasShadow: true)
      }
      .buttonStyle(.plain)
      .contentShape(Capsule())
      .padding(8)
  }
  ```

### 2. Glassmorphic Metric & Info Cards
Refactor `MetricCard` in `PhotoDetailsView.swift` (and its counterpart usage in `UserProfileView.swift`):
* Replace solid `Color(hex: "1E1E24")` with `.glassCard(cornerRadius: 12, style: .ultraThin)`.
* Replace hardcoded colors (`.white`, `.gray`) with semantic Dynamic Type scales and Apple HIG vibrancy hierarchy (`.glassVibrancy(.primary)`, `.glassVibrancy(.secondary)`).
* **Implementation Blueprint**:
  ```swift
  struct MetricCard: View {
      let label: String
      let value: String
      let systemImage: String

      var body: some View {
          VStack(spacing: 8) {
              Image(systemName: systemImage)
                  .font(.system(size: 18))
                  .glassVibrancy(.secondary)
              Text(value)
                  .font(.subheadline.weight(.bold))
                  .glassVibrancy(.primary)
              Text(label)
                  .font(.caption2.weight(.semibold))
                  .glassVibrancy(.secondary)
          }
          .frame(maxWidth: .infinity)
          .padding(.vertical, 12)
          .glassCard(cornerRadius: 12, style: .ultraThin)
      }
  }
  ```

### 3. Collection Mosaic Card Modernization
In `CollectionsFeedView.swift` (`CollectionMosaicCard`):
* **Photo Count Pill**: Modernize `Text("\(collection.totalPhotos) Photos")` by replacing `.background(Color.white.opacity(0.12))` with `.glassCapsule(style: .ultraThin)` and `.glassVibrancy(.primary)`.
* **Curator Info**: Replace raw text colors with `.glassVibrancy(.secondary)` for the "Curated by" label and `.glassVibrancy(.primary)` for curator name.
* **Card Container**: Remove the hardcoded container fill `.background(Color(hex: "0F0F11"))`, allowing natural platform surface layering and edge blur underneath navigation chrome.

### 4. User Profile Header & Social Chips
In `UserProfileView.swift` (`ProfileHeaderView`):
* **Social Chips (`SocialButton`)**: Refactor into floating glass capsules using `.glassCapsule(style: .ultraThin)` with semantic font sizing (`.font(.caption2.weight(.bold))`) and `.glassVibrancy(.primary)`.
* **Profile Metadata**: Update location and bio labels to utilize `.glassVibrancy(.secondary)` and `.glassVibrancy(.primary)`.
* **Tab Picker**: Eliminate hardcoded `#0F0F11` in `ProfileTabPicker` in favor of semantic background materials.

### 5. Frosted Loading Placeholders
Modernize the image download transition state in `PhotoCard` and `CollectionPhotoGridCard`:
* Layer an `.ultraThinMaterial` backing and subtle gradient shimmer over the source average color (`photo.color ?? "1E1E24"`).
* Center a soft `ProgressView()` with semantic `.tint(.primary.opacity(0.6))` to give visual depth during network latency.

---

## Implementation Checklist

- [x] Refactor `PhotoCard` in `ContentView.swift`:
  - [x] Remove full-width `LinearGradient` bottom scrim.
  - [x] Add inset floating attribution capsule using `.glassCapsule(style: .ultraThin, showBorder: true, hasShadow: true)`.
  - [x] Ensure tap gesture isolation between photo selection (`onSelect`) and user profile navigation (`onUserSelect`).
  - [x] Use semantic Dynamic Type font scales and `.glassVibrancy(.primary)`.
- [x] Refactor `CollectionPhotoGridCard` in `CollectionDetailView.swift` to align with the same glass attribution capsule pattern.
- [x] Update `MetricCard` in `PhotoDetailsView.swift`:
  - [x] Replace `Color(hex: "1E1E24")` with `.glassCard(cornerRadius: 12, style: .ultraThin)`.
  - [x] Apply semantic Dynamic Type styles and `GlassVibrancy`.
- [x] Modernize `CollectionMosaicCard` in `CollectionsFeedView.swift`:
  - [x] Convert photo count badge to `.glassCapsule(style: .ultraThin)`.
  - [x] Apply `GlassVibrancy` to curator info.
  - [x] Remove hardcoded `Color(hex: "0F0F11")`.
- [x] Modernize `UserProfileView.swift`:
  - [x] Update `SocialButton` to use `.glassCapsule(style: .ultraThin)`.
  - [x] Apply `GlassVibrancy` to profile header information.
  - [x] Remove hardcoded `Color(hex: "0F0F11")` in tab picker.
- [x] Refine image loading placeholders with frosted material depth.
- [x] Regenerate Xcode project and verify dual-platform compilation:
  - [x] `cd iosApp && xcodegen`
  - [x] `xcodebuild -project iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`
  - [x] `xcodebuild -project iosApp.xcodeproj -scheme macosApp -destination 'generic/platform=macOS' build CODE_SIGNING_ALLOWED=NO`
- [x] Run static analysis:
  - [x] `swiftlint lint iosApp/iosApp`
- [x] Accessibility Verification:
  - [x] Verify high-contrast fallback rendering with `@Environment(\.accessibilityReduceTransparency)` enabled.
  - [x] Verify dynamic font scaling behavior under iOS Dynamic Type.
