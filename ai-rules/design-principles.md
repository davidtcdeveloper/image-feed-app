# Cross-Platform Design & UX Principles

## Purpose
Establish the shared UI, UX, and aesthetic principles across both Android (`androidApp`) and iOS/macOS (`iosApp`). While each platform employs its own native design language (Material 3 on Android, Human Interface Guidelines on Apple), both platforms MUST follow these unified standards.

---

## 1. Zero Hardcoded Design Tokens Policy

*   **PROHIBITED**:
    *   Hardcoded color literals (e.g., `Color(0xFF...)`, `Color(hex: "...")`, `Color.White`, `Color.Black`, `Color.Gray`) directly in view or composable code.
    *   Ad-hoc typography values (e.g., hardcoded `14.sp`, `16.sp`, `.font(.system(size: 20))` without binding to semantic scales).
    *   Ad-hoc corner radii (e.g., random `18.dp` or `.cornerRadius(15)`).
*   **MANDATORY**:
    *   Reference semantic tokens from the platform theme (`MaterialTheme.colorScheme.*` on Android; native semantic materials and styles on iOS).
    *   Use platform typography scales to respect user-level accessibility font scaling (Dynamic Type).
    *   Use categorized shape tokens (`extraSmall` through `extraLarge`) for consistent geometry.
*   **Exception**: Domain-specific content colors (such as Unsplash average photo background colors or camera sensor metadata tags).

---

## 2. Layered Surfaces & Elevation Hierarchy

Arbitrary drop shadows and flat, opaque solid backgrounds are prohibited:
*   **Android (Material 3)**: Use tonal surface elevation (`surface`, `surfaceContainerLowest` through `surfaceContainerHighest`).
*   **Apple (HIG)**: Use native semantic materials (`.ultraThinMaterial` through `.thickMaterial`) complemented by subtle specular edge lighting and ambient drop shadows.
*   **Backgrounds**: Root screens must use the platform base surface, allowing layered cards, dialogs, and sheets to convey depth naturally.

---

## 3. Accessibility & Theme Parity

*   **Light & Dark Theme Parity**: Never force dark mode unconditionally. Support system appearance switching (`isSystemInDarkTheme()` on Android, `@Environment(\.colorScheme)` on Apple) and ensure contrast ratios meet WCAG AA standards in both modes.
*   **Accessibility Degradation**:
    *   iOS: Always observe `@Environment(\.accessibilityReduceTransparency)` and fall back to high-contrast opaque surfaces.
    *   Android: Ensure legible contrast under dynamic color wallpaper theming (Android 12+) and accessibility high-contrast settings.
*   **Dynamic Font Scaling**: Ensure layouts never truncate critical text when users increase their system font size.

---

## 4. Edge-to-Edge & Immersive Navigation Chrome

*   **Edge-to-Edge**: Screens must draw behind status bars, home indicators, and navigation bars (`enableEdgeToEdge()` on Android, default safe area handling with native material bars on Apple).
*   **Translucent Chrome**: Avoid opaque, solid tab and navigation bars. Content should scroll naturally underneath chrome with dynamic blur or elevation shifts.
*   **Safe Drawing**: Always respect window insets (`WindowInsets.safeDrawing`, `Scaffold` padding, or SwiftUI `.safeAreaPadding`).

---

## 5. Media Presentation & Non-Destructive Attribution

*   **Media-First Canvas**: In photo detail/inspection views, high-resolution photography must take center stage. Avoid rigid layouts where the image scrolls away. Use side-by-side dual panes on wide displays or interactive bottom sheets on phones.
*   **Non-Destructive Inset Attribution**:
    *   **PROHIBITED**: Heavy, opaque linear gradient scrims stretching over photos that obscure photography.
    *   **MANDATORY**: Use compact, inset floating capsules/chips (with `.ultraThinMaterial` or surface container backing) displaying photographer avatar, name, and Unsplash link.
*   **BlurHash Placeholders**: Always transition smoothly from decoded BlurHash placeholders to the loaded image.
*   **Dynamic Resolution Tuning**: Always append CDN resizing query parameters (`&w=calculatedWidth&q=80&auto=format`) matching the actual container width to optimize network and memory footprint.

---

## 6. Purposeful Tactile & Sensory Feedback

Haptic feedback must be subtle, intentional, and tied to user interactions:
*   **Selection Feedback**: Light impact when toggling categories, filter chips, or switching tabs.
*   **Refresh Feedback**: Haptic pulse when a pull-to-refresh gesture crosses the trigger threshold.
*   **Action Completion**: Success haptic when a photo download completes or a bookmark is saved.
*   **Restraint**: Never trigger haptics during passive scrolling or automated background transitions.

---

## 7. Adaptive Form Factors

*   Layouts must adapt fluidly between Compact (phones), Medium (foldables, small tablets), and Expanded (large tablets, desktop/macOS).
*   Use adaptive multi-column grids (dynamic column calculations based on container width) rather than hardcoded column counts.
*   Utilize side-by-side split panes for detail views on large screens.
