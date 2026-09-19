# Specification: iOS Floating Glass Attribution Chips & Card Modernization

**Status:** New / Not Implemented

## Overview
This specification modernizes image cards, attribution overlays, collection mosaics, and photographer profile elements across the iOS and macOS targets. Currently, `PhotoCard` overlays photographer credentials using a heavy linear gradient (`LinearGradient(colors: [.clear, .black.opacity(0.75)])`) that stretches across the entire bottom half of the image, darkening photo composition. Furthermore, metadata cards and metric tiles in `PhotoDetailsView` and `UserProfileView` use flat solid dark backgrounds (`#1E1E24`).

This spec replaces heavy gradient overlays with elegant inset floating glass attribution capsules (`.ultraThinMaterial`), refactors card containers with glassmorphic depth and specular edge lighting, and integrates seamless BlurHash material layering.

---

## Current State & Deficiencies
1. **Heavy Dark Gradient on Photos**: `PhotoCard` in `ContentView.swift` applies:
   ```swift
   LinearGradient(
       colors: [.clear, .black.opacity(0.75)],
       startPoint: .top,
       endPoint: .bottom
   )
   ```
   This covers up the bottom 25-30% of the image with a murky black tint, obscuring details in landscape, architectural, or low-key photography.
2. **Flat Solid Metadata Containers**:
   *   Metric cards (`MetricCard`) in `PhotoDetailsView` use solid `Color(hex: "1E1E24")`.
   *   EXIF and Location cards use basic materials without specular borders or responsive vibrancy.
   *   Photographer profile header (`ProfileHeaderView`) in `UserProfileView` uses flat dark containers.
3. **Flat BlurHash & Color Loading Placeholders**:
   *   `PhotoCard` loads a flat `RoundedRectangle.fill(Color(hex: photo.color))` placeholder without texture or depth during network latency.

---

## Architectural Changes & Implementation Plan

### 1. Inset Floating Glass Attribution Capsule
*   In `PhotoCard`:
    *   Remove the full-width `LinearGradient` bottom scrim.
    *   Place a compact, floating frosted capsule in the bottom-leading corner of the photo with safe insets (`padding(8)`):
        ```swift
        HStack(spacing: 6) {
            KFImage(URL(string: photo.user.profileImage.small))
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: 20, height: 20)
                .clipShape(Circle())

            Text(photo.user.name)
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(.primary)
                .lineLimit(1)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 5)
        .background(.ultraThinMaterial, in: Capsule())
        .overlay(
            Capsule().strokeBorder(
                LinearGradient(
                    colors: [Color.white.opacity(0.35), Color.white.opacity(0.10)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ),
                lineWidth: 0.5
            )
        )
        .shadow(color: .black.opacity(0.2), radius: 6, x: 0, y: 2)
        ```
    *   **Benefits:** Unsplash attribution guidelines remain 100% compliant while leaving the photo canvas unclouded and crisp.

### 2. Glassmorphic Metric & Info Cards
*   Refactor `MetricCard` in `PhotoDetailsView`:
    *   Replace `Color(hex: "1E1E24")` with `.ultraThinMaterial`.
    *   Add a subtle specular border and inner highlight.
*   Refactor `CollectionMosaicCard` in `CollectionsFeedView`:
    *   Apply glass pill badge for photo counts (`Text("\(collection.totalPhotos) Photos")` backed by `.ultraThinMaterial`).
    *   Add subtle glass surface backing to the curator info footer.

### 3. Glass Loading Placeholders
*   In `PhotoCard`:
    *   Layer an `.ultraThinMaterial` overlay on top of the average color / BlurHash rectangle.
    *   This provides a soft frosted glass aesthetic during download transitions that mirrors the final image texture.

### 4. Implementation Checklist
- [ ] Replace `PhotoCard` gradient bottom mask with an inset floating glass capsule attribution pill.
- [ ] Update `MetricCard` in `PhotoDetailsView` to use `.ultraThinMaterial` with specular border.
- [ ] Modernize `CollectionMosaicCard` photo count chips and curator headers with glass styling.
- [ ] Update `UserProfileView` stats and bio panels with subtle glass cards.
- [ ] Verify photo clarity, contrast, and layout performance across grid feeds.
