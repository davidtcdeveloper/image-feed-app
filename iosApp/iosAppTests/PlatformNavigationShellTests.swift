import shared
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

    func testUnifiedAppRouteEqualityAndSourceIDs() {
        let photoRoute = AppRoute.photo(id: "p1")
        let collectionRoute = AppRoute.collection(id: "c1")
        let userRoute = AppRoute.user(username: "clara")
        let searchRoute = AppRoute.search(query: "alpine")

        let routes: Set<AppRoute> = [photoRoute, collectionRoute, userRoute, searchRoute, .photo(id: "p1")]
        XCTAssertEqual(routes.count, 4)
        XCTAssertEqual(AppNavigationDefaults.photoSourceID("p1"), "photo-p1")
        XCTAssertEqual(AppNavigationDefaults.userSourceID("clara"), "user-clara")
        XCTAssertEqual(AppNavigationDefaults.zoomTransitionCornerRadius, 12)
    }

    func testNavigationPreviewSeederPopulatesMetroCache() {
        let photo = UIPreviewFixtures.samplePhoto
        let collection = UIPreviewFixtures.sampleCollections[0]

        NavigationPreviewSeeder.seedPhoto(photo)
        NavigationPreviewSeeder.seedCollection(collection)

        XCTAssertEqual(MetroHelper.shared.getPhotoPreview(photoId: photo.id)?.id, photo.id)
        XCTAssertEqual(MetroHelper.shared.getUserPreview(username: photo.user.username)?.username, photo.user.username)
        XCTAssertEqual(MetroHelper.shared.getCollectionPreview(collectionId: collection.id)?.id, collection.id)
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
