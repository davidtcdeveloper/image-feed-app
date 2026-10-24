# Specification 74: Search Tab Bar Glass Capsule Redesign & Visual Parity

**Status:** Implemented

## 1. Overview & Problem Statement

A visual inconsistency exists between the Home screen (`PhotosFeedTabView`) and the Search screen (`SearchView`) in the Apple applications (`macosApp` and `iosApp`):

1. **Home Screen (`PhotosFeedTabView` / `FeedCategoryBar`)**:
   - In [`ContentView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L599-L765), [`FeedCategoryBar`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L599-L697) renders selection tabs inside an elevated, full-width floating frosted glass capsule (`.frame(height: 44)`, `.glassBackground(style: .ultraThin, shape: Capsule(), showBorder: true, hasShadow: true)`) docked via `.safeAreaInset(edge: .top, spacing: 0)`.
   - Each tab pill uses [`CategoryTabButton`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L739-L765), displaying `.font(.system(size: 13, weight: .semibold))` with a high-contrast white selection pill (`Capsule().fill(Color.white)` + `matchedGeometryEffect`, black foreground text) and subtle unselected pills (`Capsule().fill(Color.white.opacity(0.08))`, secondary foreground text).

2. **Search Screen (`SearchView`)**:
   - In [`SearchView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/SearchView.swift#L47-L60), the `Photos`, `Collections`, and `Users` tabs are rendered using a native SwiftUI `Picker("Tab", selection: ...)` with `.pickerStyle(.segmented)` and `.background(.ultraThinMaterial)`.
   - **macOS Visual Defect**: On macOS (`NSSegmentedControl`), SwiftUI's `Picker("Tab", ...)` displays the literal `"Tab"` label to the left of the segmented control, renders a boxy system-blue active segment (`Photos | Collections | Users`), and stretches an unstyled `.ultraThinMaterial` rectangle behind the control rather than matching the app's `GlassTheme` capsule aesthetic.
   - **Scroll Hierarchy Defect**: Because the `Picker` sits at the top of a rigid `VStack(spacing: 0)` ([`SearchView.swift#L46-L60`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/SearchView.swift#L46-L60)) rather than inside `.safeAreaInset(edge: .top, spacing: 0)` on the content scroll views, search results and suggestions clip abruptly at the bottom of the picker instead of scrolling fluidly underneath a floating glass capsule bar.

---

## 2. Root Cause Analysis

| Aspect | Home Screen ([`FeedCategoryBar`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L599-L697)) | Search Screen ([`SearchView`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/SearchView.swift#L47-L60)) |
|---|---|---|
| **Control Type** | Custom `HStack` of [`CategoryTabButton`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L739-L765) pills | `Picker("Tab", ...)` with `.pickerStyle(.segmented)` |
| **Container Chrome** | Floating `Capsule()` with `.glassBackground(style: .ultraThin, shape: Capsule(), showBorder: true, hasShadow: true)`, height `44pt`, horizontal padding `12pt` | Unclipped `.background(.ultraThinMaterial)` box around `NSSegmentedControl` / `UISegmentedControl` |
| **Active Tab Visual** | White `Capsule()` with `.matchedGeometryEffect` and `Color.black` text (`.system(size: 13, weight: .semibold)`) | macOS system accent blue rectangle + visible `"Tab"` form label |
| **Unselected Tab Visual** | `Capsule().fill(Color.white.opacity(0.08))` with `Color.secondary` text | System segmented gray divider segments |
| **Scroll Integration** | Mounted via `.safeAreaInset(edge: .top, spacing: 0)` so masonry content scrolls and blurs underneath | Stacked in `VStack(spacing: 0)`, blocking edge-to-edge scroll under the bar |
| **Sensory Feedback** | `.sensoryFeedback(.selection, trigger: viewModel.selectedTopicSlug)` | None |

---

## 3. Proposed Solution

### 3.1 Introduce `SearchTabBar` Matching `FeedCategoryBar` & `CategoryTabButton`
Create a dedicated `SearchTabBar` component in [`SearchView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/SearchView.swift) (reusing [`CategoryTabButton`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L739-L765) from [`ContentView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift)) so that the Search tab selector shares the exact visual format of the Home screen's category bar:

* **Tabs**:
  * `SearchTab.photos` → `"Photos"`
  * `SearchTab.collections` → `"Collections"`
  * `SearchTab.users` → `"Users"`
* **Pill Styling & Animation**:
  * Render each tab using [`CategoryTabButton`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L739-L765) (or shared parameters) with a dedicated `@Namespace private var searchTabNamespace`.
  * Selected tab displays the high-contrast white `Capsule()` with `Color.black` text and smooth spring `matchedGeometryEffect` (`response: 0.35, dampingFraction: 0.8`).
  * Unselected tabs display `Capsule().fill(Color.white.opacity(0.08))` with `Color.secondary` text.
  * Attach `.accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)` to each tab button in [`CategoryTabButton`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L739-L765) for VoiceOver parity.
* **Outer Floating Glass Capsule Container**:
  * Match [`FeedCategoryBar`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L673-L677) dimensions and tokens:
    * Inner `HStack(spacing: 8)` with `.padding(.horizontal, 16)` and `.padding(.vertical, 5)`.
    * `.frame(maxWidth: .infinity, alignment: .leading)` inside the capsule (or scrollable horizontal container matching `FeedCategoryBar`) so the capsule spans the content width identically to the Home screen bar, or centers/aligns consistently across window sizes.
    * `.frame(height: 44)`
    * `.glassBackground(style: .ultraThin, shape: Capsule(), showBorder: true, hasShadow: true)`
    * `.padding(.horizontal, 12)`
    * `.padding(.top, 4)`
    * `.padding(.bottom, 6)`

### 3.2 Full-Bleed Scroll Integration via `.safeAreaInset(edge: .top, spacing: 0)`
In [`SearchView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/SearchView.swift):
* Replace the segmented `Picker` in the outer `VStack(spacing: 0)` with the floating `searchTabBar` mounted via `.safeAreaInset(edge: .top, spacing: 0)` on the scrollable states (`SearchSuggestionsView`, loading skeleton `ScrollView`, and search results `ScrollView`), and placed at the top of the non-scrollable error `VStack` (matching [`PhotosFeedTabView` in `ContentView.swift#L330-L408`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L330-L408)).
* Add `.sensoryFeedback(.selection, trigger: viewModel.activeTab)` when switching search tabs.

---

## 4. Architectural Invariants & Compliance

1. **Apple Glass Design & HIG ([`ios-glass-design`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/.agents/skills/ios-glass-design/SKILL.md))**:
   * Eliminates the raw `.background(.ultraThinMaterial)` and native `PickerStyle.segmented` mismatch in favor of `GlassTheme` tokens (`.glassBackground(style: .ultraThin, shape: Capsule(), showBorder: true, hasShadow: true)`), which automatically honor `@Environment(\.accessibilityReduceTransparency)` and `@Environment(\.colorSchemeContrast)`.
   * Uses declarative `.sensoryFeedback(.selection, trigger:)` for tab selection changes.
2. **Localization ([`localization`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/.agents/skills/localization/SKILL.md))**:
   * Reuses the existing localized string keys `"Photos"`, `"Collections"`, and `"Users"` already present in [`Localizable.xcstrings`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/Localizable.xcstrings).
3. **Platform Build Policy ([`verification-and-linting`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/.agents/skills/verification-and-linting/SKILL.md))**:
   * Because `iosApp/iosApp/SearchView.swift` and `iosApp/iosApp/ContentView.swift` are shared Apple sources, verify both `iosApp` and `macosApp` Xcode schemes alongside `:androidApp:assembleDebug`, `swiftformat`, and `swiftlint`.

---

## 5. Implementation Steps

1. **Enhance `CategoryTabButton` Accessibility in [`ContentView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L739-L765)**:
   * Add `.accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)` to `CategoryTabButton` so both Home category pills and Search tab pills expose selection state to VoiceOver.
2. **Implement `SearchTabBar` and Update Layout in [`SearchView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/SearchView.swift)**:
   * Remove the segmented `Picker("Tab", ...)` ([`SearchView.swift#L47-L60`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/SearchView.swift#L47-L60)).
   * Add `@Namespace private var searchTabNamespace` to `SearchView`.
   * Create `SearchTabBar` using `CategoryTabButton` for `.photos`, `.collections`, and `.users` inside the `44pt`-high floating `Capsule` with `.glassBackground(style: .ultraThin, shape: Capsule(), showBorder: true, hasShadow: true)` and matching padding (`.padding(.horizontal, 12)`, `.padding(.top, 4)`, `.padding(.bottom, 6)`).
   * Attach `searchTabBar` via `.safeAreaInset(edge: .top, spacing: 0)` to the scrollable views in `SearchView` (and top of the error state `VStack`), and attach `.sensoryFeedback(.selection, trigger: viewModel.activeTab)`.
3. **Update [`specs/steps.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/steps.md)**:
   * Add Step 74 entry referencing `specs/74_macos_search_tab_bar_visual_consistency.md`.
4. **Run Verification & Linting**:
   * Run `swiftformat --lint iosApp/` and `swiftlint`.
   * Generate Xcode project (`xcodegen generate --spec iosApp/project.yml`) and build both `macosApp` and `iosApp` with zero warnings.

---

## 6. Acceptance Criteria

- [x] `SearchView` replaces the segmented `Picker("Tab", ...)` with a floating glass capsule tab bar (`SearchTabBar`) visually matching `FeedCategoryBar` on the Home screen.
- [x] The literal `"Tab"` label and system-blue segmented control border on macOS are eliminated.
- [x] `Photos`, `Collections`, and `Users` tabs in `SearchView` use `CategoryTabButton` styling (`.system(size: 13, weight: .semibold)`, white active capsule with black text and `matchedGeometryEffect`, translucent `white.opacity(0.08)` inactive capsules with secondary text).
- [x] `SearchTabBar` uses `.glassBackground(style: .ultraThin, shape: Capsule(), showBorder: true, hasShadow: true)` with `height: 44` and horizontal/vertical padding matching `FeedCategoryBar`, and integrates with scroll content via `.safeAreaInset(edge: .top, spacing: 0)`.
- [x] Switching search tabs animates smoothly with `.spring(response: 0.35, dampingFraction: 0.8)` and triggers `.sensoryFeedback(.selection, trigger:)`.
- [x] Both `macosApp` and `iosApp` build with zero compiler warnings, and `swiftformat` / `swiftlint` pass with zero violations.
