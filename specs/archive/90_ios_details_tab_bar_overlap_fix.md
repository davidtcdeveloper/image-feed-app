# Specification 90: iOS 26+ TabView Bar Overlap Fix on Photo Details Dismissal

**Status:** Implemented (Final)

* **Depends On:** [Spec 87](87_apple_float_navigation_shell_and_macos_search.md), [Spec 88](88_apple_satin_headers_zoom_transitions_and_unified_routes.md), [Spec 89](89_apple_photo_details_inspector_parity_and_glass_morphing.md)
* **Target Files:** [`PlatformNavigationShell.swift`](../../iosApp/iosApp/DesignSystem/PlatformNavigationShell.swift)

---

## 1. Problem Statement

On iOS 26+ devices running [`NativeLiquidGlassTabShell`](../../iosApp/iosApp/DesignSystem/PlatformNavigationShell.swift#L141-L181) (with `.tabBarMinimizeBehavior(.onScrollDown)`), navigating from a tab feed into [`PhotoDetailsView`](../../iosApp/iosApp/PhotoDetailsView.swift) leaves the system `TabView` bar visible behind the inspector sheet. When the user drags/scrolls the inspector sheet off-screen (`showInspector = false`), `PhotoDetailsView`'s `compactFloatingBar` appears in the bottom safe area directly overlapped by the minimized Liquid Glass `TabView` orbs (`photo.stack` and `magnifyingglass`).

### Root Cause
1. Destination-level `.toolbar(.hidden, for: .tabBar)` (and `.toolbarVisibility(.hidden, for: .tabBar)`) inside [`AppNavigation.swift`](../../iosApp/iosApp/DesignSystem/AppNavigation.swift#L77-L82) and [`PhotoDetailsView.swift`](../../iosApp/iosApp/PhotoDetailsView.swift#L71-L73) does not hide the iOS 26+ `Tab(...)` bar for pushed `navigationDestination` routes.
2. [`adaptiveTabBarVisibility(hideOnModernOS:)`](../../iosApp/iosApp/DesignSystem/PlatformNavigationShell.swift#L298-L310) explicitly applied `.toolbar(.visible, for: .tabBar)` on root tab views when `hideOnModernOS` was `false`.

---

## 2. Solution

1. Pass `isCapsuleVisible` from [`ContentView.swift`](../../iosApp/iosApp/ContentView.swift#L33-L50) into `NativeLiquidGlassTabShell` as `isTabBarVisible` (matching [`LegacyFloatingTabShell`](../../iosApp/iosApp/DesignSystem/PlatformNavigationShell.swift#L184-L282)).
2. Apply `.toolbarVisibility(isTabBarVisible ? .visible : .hidden, for: .tabBar)` directly on each `Tab`'s root content inside [`NativeLiquidGlassTabShell`](../../iosApp/iosApp/DesignSystem/PlatformNavigationShell.swift#L154-L176).
3. Update [`adaptiveTabBarVisibility(hideOnModernOS:)`](../../iosApp/iosApp/DesignSystem/PlatformNavigationShell.swift#L298-L310) on iOS 26+ to apply `.toolbar(.hidden, for: .tabBar)` only when `hideOnModernOS` is `true` (returning `self` otherwise), preventing root tab views from overriding `Tab`-level visibility.

---

## 3. Wiki & Skill Impact

* [`wiki/design-systems-and-navigation.md`](../../wiki/design-systems-and-navigation.md) — Updated to document `Tab`-root `.toolbarVisibility(..., for: .tabBar)` driven by `ContentView.isCapsuleVisible` in `NativeLiquidGlassTabShell` and the updated iOS 26+ behavior of `adaptiveTabBarVisibility(hideOnModernOS:)`.
* [`.agents/skills/ios-glass-design/SKILL.md`](../../.agents/skills/ios-glass-design/SKILL.md) — Updated §4 (`Modern Navigation Chrome & Translucent Bars`) to reflect `Tab`-root `.toolbarVisibility` and prohibit forcing `.toolbar(.visible, for: .tabBar)` on root tabs.

---

## 4. Acceptance Criteria

- [x] `NativeLiquidGlassTabShell` hides the iOS 26+ `TabView` bar whenever `isCapsuleVisible` is `false` (any pushed `AppRoute` or active legacy detail overlay).
- [x] Dismissing the `PhotoDetailsView` inspector sheet renders `compactFloatingBar` cleanly with zero `TabView` orb overlap (`app.tabBars.count == 0`).
- [x] Navigating back to the root feed restores the `TabView` bar (`app.tabBars.count == 1`).
- [x] `iosApp` and `macosApp` builds/tests and `swiftformat --lint .` pass with zero compiler or formatting errors.
- [x] Living wiki, `ios-glass-design` skill, and `specs/steps.md` are updated and the spec is archived.

---

## 5. Verification Record

### Verified
- **Live iOS Simulator Reproduction & Fix Check** (`iPhone 18 Pro`, iOS 27.0, `id=76AE7EB6-1124-4485-9D47-7FE3D70681D2`):
  - Re-ran UI interaction flow (`--ui-fixture navigationShell`, unscrolled & scrolled feed → `PhotoDetailsView` → swipe-dismiss inspector sheet → back to feed): `app.tabBars.count == 0` while on `PhotoDetailsView` (sheet open and dismissed) and `1` on feed return.
- `swiftformat --lint .` — Passed (`0/35` files require formatting).
- `swiftlint lint iosApp/iosApp/DesignSystem/PlatformNavigationShell.swift iosApp/iosAppUITests/UIWindowServerSnapshotUITests.swift` — `0` errors (`2` pre-existing `opening_brace` / `trailing_comma` warnings per `ISS-007`).
- `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'id=76AE7EB6-1124-4485-9D47-7FE3D70681D2' -only-testing:iosAppTests CODE_SIGNING_ALLOWED=NO` — Passed (`9/9` tests).
- `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme macosApp -destination 'platform=macOS,arch=arm64' -only-testing:macosAppTests CODE_SIGNING_ALLOWED=NO` — Passed (`10/10` tests).

### Unverified / Gaps
- **Android Build**: Not run (`iosApp/**` and docs-only change).
