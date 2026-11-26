import SwiftUI
import XCTest
#if os(iOS)
@testable import iosApp
#elseif os(macOS)
@testable import macosApp
#endif

@MainActor
final class PlatformNavigationShellTests: XCTestCase {
    func testAppRootTabFourDestinationParity() {
        XCTAssertEqual(AppRootTab.allCases, [.photos, .collections, .favorites, .search])
        XCTAssertEqual(AppRootTab.photos.rawValue, 0)
        XCTAssertEqual(AppRootTab.collections.rawValue, 1)
        XCTAssertEqual(AppRootTab.favorites.rawValue, 2)
        XCTAssertEqual(AppRootTab.search.rawValue, 3)
        XCTAssertEqual(AppRootTab.search.systemImage, "magnifyingglass")
    }

    func testCapsuleScrollCoordinatorMinimizesAndExpands() {
        let coordinator = CapsuleScrollCoordinator()
        XCTAssertFalse(coordinator.isMinimized)

        // Scrolling down past the threshold minimizes the legacy floating capsule
        coordinator.handleScroll(offset: -10)
        coordinator.handleScroll(offset: -60)
        XCTAssertTrue(coordinator.isMinimized)

        // Scrolling back up expands it
        coordinator.handleScroll(offset: -30)
        XCTAssertFalse(coordinator.isMinimized)

        // Explicit expand resets minimized state
        coordinator.handleScroll(offset: -90)
        XCTAssertTrue(coordinator.isMinimized)
        coordinator.expand()
        XCTAssertFalse(coordinator.isMinimized)
    }

    #if os(macOS)
    func testMacOSSidebarSelectionIncludesSearch() {
        let selections: [MacOSContentView.SidebarSelection] = [.photos, .collections, .favorites, .search]
        XCTAssertEqual(Set(selections).count, 4)
    }
    #endif
}
