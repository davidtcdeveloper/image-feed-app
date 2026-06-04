# Photo Detail Description Fix Spec

## Problem
The iOS and macOS photo detail screens currently render the raw Swift/Kotlin object description instead of the actual Unsplash photo caption.

Root cause:
- The shared Kotlin model exposes `description` in the Unsplash payload.
- The generated Swift/Objective-C bridge for KMP exposes that field as `description_` to avoid colliding with Swift's built-in `description` / NSObject debug text.
- The current iOS/macOS detail view uses `photo.description`, which resolves to the object dump path instead of the API caption.

This is why the detail screen shows a JSON-like object dump instead of the expected human-readable photo description.

## Proposed Solution
1. Update the iOS/macOS photo detail view to use the generated binding property `photo.description_`.
2. Keep the current fallback to `photo.altDescription` for cases where the main caption is blank.
3. Add a small, explicit comment near the caption rendering to document the KMP binding behavior so future Swift changes do not regress.

## Implementation Notes
- The fix should be applied in `iosApp/iosApp/PhotoDetailsView.swift` because that view is shared by both iOS and macOS targets.
- The Android implementation already uses the Kotlin property names directly (`photo.description` / `photo.altDescription`), which is why it behaves correctly.
- The same naming collision pattern already exists in the collection views, where Swift code uses `collection.description_`.

## Acceptance Criteria
- The photo detail screen shows the Unsplash caption text, not the Swift object dump.
- The fallback behavior still uses `altDescription` when `description_` is blank.
- The fix is limited to the iOS/macOS binding path and does not change Android behavior.

## Verification Plan
- Build the Xcode project to confirm the updated Swift binding compiles.
- Confirm the caption renders through `description_ ?? altDescription` in the shared iOS/macOS detail view.
- Note that direct screenshot capture via Xcode MCP is not available in this setup because the current project has no `#Preview`/`PreviewProvider` entry points to render a preview snapshot.
