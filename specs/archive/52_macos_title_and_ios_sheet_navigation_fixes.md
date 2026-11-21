# macOS Window Title & iOS Navigation Bar Sheet Overlap Fix Spec

**Status:** Implemented

## Overview
This specification details the root causes and architectural fixes for two navigation and presentation defects identified in the macOS and iOS applications:
1. **macOS Collection Detail Window Title Bug**: When navigating to a collection detail screen, the window title displays the application bundle process name (`macosapp`) instead of the collection's title.
2. **iOS Photo Details Sheet Navigation Bar Focus/Overlap Bug**: When opening a photo from a pushed view (e.g., `UserProfileView`, `CollectionDetailView`, or `SearchView`) and dragging down the interactive inspector sheet, the underlying navigation bar from the presenting view hierarchy moves over the sheet and steals touch focus.

---

## Issue 1: macOS Collection Detail Window Title Bug

### Problem Description
On macOS, when a user selects a collection from `CollectionsFeedView`, the detail pane opens `CollectionDetailView`. However, the window title bar changes to `macosapp` rather than displaying the collection title.

### Root Cause Analysis
In [CollectionDetailView.swift](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/CollectionDetailView.swift):
* Unlike [`CollectionsFeedView`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/CollectionsFeedView.swift#L126) (`.navigationTitle("COLLECTIONS")`), [`UserProfileView`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/UserProfileView.swift#L98) (`.navigationTitle(viewModel.user?.name ?? "Profile")`), and [`SearchView`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/SearchView.swift#L224) (`.navigationTitle("SEARCH")`), `CollectionDetailView` completely omits a `.navigationTitle(...)` modifier.
* Inside macOS `NavigationSplitView` / `NavigationStack`, when a view pushed to the detail column does not declare a navigation title, SwiftUI / AppKit falls back to the executable/bundle target name (`macosapp`).

### Proposed Solution
1. In [CollectionDetailView.swift](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/CollectionDetailView.swift), attach `.navigationTitle(viewModel.collection?.title ?? "Collection")`.
2. Configure `.navigationBarTitleDisplayMode(.inline)` under `#if os(iOS)` to preserve design consistency with other detail screens.

---

## Issue 2: iOS Photo Details Sheet Navigation Bar Overlap & Focus Bug

### Problem Description
On iPhone, when viewing a photo after navigating into a profile (`UserProfileView`), collection (`CollectionDetailView`), or search result (`SearchView`), dragging the interactive inspector sheet all the way down causes the parent navigation bar to appear over the bottom sheet and steal user interaction. This does not happen when selecting a photo from the main feed (`PhotosFeedTabView`).

### Root Cause Analysis
1. **Presentation Context Discrepancy**:
   * **Main Feed**: Photos are presented in-place as a full-screen overlay in `PhotosFeedTabView` via `selectedPhotoForHero` within a root `ZStack`. The view explicitly hides both system bars:
     ```swift
     .toolbar(selectedPhotoForHero != nil ? .hidden : .visible, for: .navigationBar)
     .toolbar(selectedPhotoForHero != nil ? .hidden : .automatic, for: .tabBar)
     ```
   * **Pushed Screens (User Profile, Collection Detail, Search)**: Selecting a photo pushes `PhotoDetailsView` into the active `NavigationStack` path (`FeedPathItem(id: photoId, type: .photo)` or `CollectionPathItem(id: photoId, type: .photo)`).
2. **Sheet Background Interaction Leak**:
   * In [PhotoDetailsView.swift](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/PhotoDetailsView.swift), the sheet configuration includes:
     ```swift
     .presentationBackgroundInteraction(.enabled(upThrough: .fraction(0.70)))
     ```
   * When pushed onto a `NavigationStack`, `PhotoDetailsView` hides the back button (`.navigationBarBackButtonHidden(true)`) and supplies custom toolbar items, but **does not hide the navigation bar chrome** (`.toolbar(.hidden, for: .navigationBar)`).
   * Because `presentationBackgroundInteraction` enables touch and layout pass-through to the underlying view hierarchy up through the `0.70` detent fraction, dragging the sheet towards the lower detent (`0.35`) or downward allows the active navigation bar container of the navigation stack to intercept gestures and render on top of the sheet window hierarchy.
3. **Trapped Navigation Regression Risk**:
   * In [PhotoDetailsView.swift](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/PhotoDetailsView.swift#L106-L128), floating top overlay controls (back button and inspector toggle) are guarded by `if onDismiss != nil`.
   * When `PhotoDetailsView` is pushed onto a `NavigationStack` from `UserProfileView`, `CollectionDetailView`, or `SearchView` ([`ContentView.swift` line 68](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/ContentView.swift#L68)), `onDismiss` is `nil`.
   * The **only** navigation controls rendered on pushed views are in `.toolbar { ToolbarItem(placement: .navigationBarLeading) ... }`.
   * Therefore, simply applying `.toolbar(.hidden, for: .navigationBar)` without updating the overlay controls would hide the toolbar items and trap the user on the screen with zero navigation controls!

### Proposed Solution
1. **Hide the Navigation Bar in `PhotoDetailsView` on iOS & Render Floating Controls Unconditionally**:
   * In `PhotoDetailsView.swift`, hide the navigation bar chrome on iOS:
     ```swift
     #if os(iOS)
     .toolbar(.hidden, for: .navigationBar)
     #endif
     ```
   * **Promote floating overlay controls**: Update the top overlays (`overlay(alignment: .topLeading)` and `overlay(alignment: .topTrailing)`) to render unconditionally on iOS (or whenever navigation bar is hidden), rather than gating behind `if onDismiss != nil`. This guarantees the floating back button (invoking `handleDismiss()`) and inspector toggle button remain fully visible and interactive on pushed `NavigationStack` destinations.
2. **Constrain or Conditionally Enable Background Interaction**:
   * Ensure that background interaction is active only when appropriate and does not leak gestures into parent navigation bars.
3. **Explicit Empty Navigation Title**:
   * Add `.navigationTitle("")` on `PhotoDetailsView` to prevent parent screen titles from bleeding through during navigation push/pop transitions.

---

## Architectural Invariants & Code Hygiene

1. **Dual-Platform Build Policy**:
   * Changes affect shared Swift views (`CollectionDetailView.swift` and `PhotoDetailsView.swift`) used on both iOS and macOS.
   * Verify both platforms build cleanly:
     * Android: `./gradlew :androidApp:assembleDebug`
     * iOS: `xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`
     * macOS: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme macosApp -destination 'platform=macOS' build CODE_SIGNING_ALLOWED=NO`
2. **Zero Dead Code & Zero Compiler Warning Policy**:
   * Ensure no unused imports or deprecated modifiers are introduced.
3. **Living Code Policy**:
   * Reference living files via Markdown links (`[PhotoDetailsView.swift](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/PhotoDetailsView.swift)`).

---

## Implementation Steps

### Step 1: Fix macOS Collection Detail Title
* Edit [CollectionDetailView.swift](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/CollectionDetailView.swift):
  * Add `.navigationTitle(viewModel.collection?.title ?? "Collection")`.
  * Add `#if os(iOS) .navigationBarTitleDisplayMode(.inline) #endif`.

### Step 2: Fix iOS Photo Details Navigation Bar Overlap & Promote Floating Controls
* Edit [PhotoDetailsView.swift](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/PhotoDetailsView.swift):
  * Add `.navigationTitle("")` and hide the navigation bar:
    ```swift
    #if os(iOS)
    .toolbar(.hidden, for: .navigationBar)
    #endif
    ```
  * Update `overlay(alignment: .topLeading)` and `overlay(alignment: .topTrailing)` to render floating back and inspector buttons unconditionally on iOS (or whenever navigation bar is hidden), ensuring destinations pushed onto `NavigationStack` (where `onDismiss == nil`) have interactive navigation affordances.
  * Verify that floating top controls (back button and info button) render seamlessly without navigation bar interference.

---

## Acceptance Criteria

- [x] When clicking on any collection on macOS, the window title displays the collection's title (or `"Collection"` while loading) instead of `macosapp`.
- [x] On iPhone, navigating to a user profile (or collection/search) and clicking an image displays the photo details without showing an unwanted navigation bar.
- [x] Sliding the inspector sheet down to the minimum detent or dragging it downwards does not cause the background navigation bar to overlay the sheet or capture touch focus.
- [x] Pushed `PhotoDetailsView` destinations on `NavigationStack` display floating back and info buttons that allow dismissing back to the previous screen (`UserProfileView`, `CollectionDetailView`, or `SearchView`).
- [x] Both `iosApp` and `macosApp` schemes compile cleanly with zero warnings.

---

## Verification Plan

1. **Compilation Check**:
   * Build `macosApp` using `xcodebuild -project iosApp/iosApp.xcodeproj -scheme macosApp -destination 'platform=macOS' build CODE_SIGNING_ALLOWED=NO`.
   * Build `iosApp` using `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`.
2. **Functional Verification (macOS)**:
   * Run macOS app.
   * Select "Collections" from the sidebar.
   * Click on a collection card.
   * Verify the window title changes to the collection name instead of `macosapp`.
3. **Functional Verification (iOS)**:
   * Run iOS app in simulator.
   * Tap on a photographer capsule on any photo card to open `UserProfileView`.
   * Tap any photo in the photographer's portfolio grid.
   * Drag the details sheet down to the lowest detent (`0.35`) and gesture downward.
   * Verify the parent navigation bar does NOT appear over the sheet or steal touch focus.
