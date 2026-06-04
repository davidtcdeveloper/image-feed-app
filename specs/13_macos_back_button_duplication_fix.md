# macOS Back Button Duplication Fix Spec

## Problem
The macOS detail screens currently render two back arrows when a screen uses the custom navigation toolbar button:
- one native system back arrow from the default navigation chrome
- one custom arrow button added in the toolbar

This duplication appears on screens such as photo details, collection details, and user profiles because the back-button hiding logic was only applied under `#if os(iOS)`. On macOS, the system back affordance remained visible and overlapped with the custom button.

## Root Cause
- The custom toolbar button is implemented in each shared SwiftUI detail view.
- `navigationBarBackButtonHidden(true)` was gated behind `#if os(iOS)`, so the native macOS back control was not hidden.
- The result is a stacked/duplicated return arrow on macOS detail screens.

## Proposed Solution
1. Hide the system back button on all platforms for the affected detail views.
2. Keep the custom toolbar back button as the single return affordance.
3. Apply the fix in the shared iOS/macOS detail screens that currently add a manual back button:
   - `iosApp/iosApp/PhotoDetailsView.swift`
   - `iosApp/iosApp/CollectionDetailView.swift`
   - `iosApp/iosApp/UserProfileView.swift`

## Implementation Notes
- The fix should be limited to the SwiftUI navigation configuration in the shared iOS/macOS UI code.
- No changes are needed in the Kotlin shared presenter layer.
- The fix should ensure the native navigation back control is hidden consistently, regardless of whether the view is running on iOS or macOS.

## Acceptance Criteria
- The macOS detail screens show exactly one return arrow.
- The custom toolbar back button remains functional.
- The fix does not introduce regressions on iOS.

## Verification Plan
- Build the macOS target with Xcode to confirm the view compiles with the updated navigation configuration.
- Open the affected detail screens on macOS and confirm that only the custom back button is visible.
- Confirm the iOS path still behaves normally after the same navigation change.
