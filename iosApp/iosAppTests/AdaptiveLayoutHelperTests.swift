import XCTest
#if os(iOS)
@testable import iosApp
#elseif os(macOS)
@testable import macosApp
#endif

final class AdaptiveLayoutHelperTests: XCTestCase {
    func testPhotoColumnCountAdaptsToContainerWidth() {
        XCTAssertEqual(AdaptiveLayoutHelper.photoColumnCount(for: 375), 2)
        XCTAssertEqual(AdaptiveLayoutHelper.photoColumnCount(for: 599), 2)
        XCTAssertEqual(AdaptiveLayoutHelper.photoColumnCount(for: 600), 3)
        XCTAssertEqual(AdaptiveLayoutHelper.photoColumnCount(for: 839), 3)
        XCTAssertEqual(AdaptiveLayoutHelper.photoColumnCount(for: 840), 4)
        XCTAssertEqual(AdaptiveLayoutHelper.photoColumnCount(for: 1199), 4)
        XCTAssertEqual(AdaptiveLayoutHelper.photoColumnCount(for: 1200), 5)
        XCTAssertEqual(AdaptiveLayoutHelper.photoColumnCount(for: 1440), 5)
    }

    func testCollectionColumnCountAdaptsToContainerWidth() {
        XCTAssertEqual(AdaptiveLayoutHelper.collectionColumnCount(for: 393), 1)
        XCTAssertEqual(AdaptiveLayoutHelper.collectionColumnCount(for: 599), 1)
        XCTAssertEqual(AdaptiveLayoutHelper.collectionColumnCount(for: 600), 2)
        XCTAssertEqual(AdaptiveLayoutHelper.collectionColumnCount(for: 959), 2)
        XCTAssertEqual(AdaptiveLayoutHelper.collectionColumnCount(for: 960), 3)
        XCTAssertEqual(AdaptiveLayoutHelper.collectionColumnCount(for: 1280), 3)
    }

    func testCalculateItemWidthPxUsesContainerWidthAndDisplayScale() {
        let scale = AdaptiveLayoutHelper.displayScale()
        let narrowPx = AdaptiveLayoutHelper.calculateItemWidthPx(containerWidth: 393, columnCount: 2)
        let widePx = AdaptiveLayoutHelper.calculateItemWidthPx(containerWidth: 1024, columnCount: 4)
        XCTAssertEqual(narrowPx, Int(((393.0 - 24.0) / 2.0) * scale))
        XCTAssertEqual(widePx, Int(((1024.0 - 40.0) / 4.0) * scale))
        XCTAssertGreaterThan(widePx, narrowPx)
    }
}
