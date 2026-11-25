import XCTest

final class UIWindowServerSnapshotUITests: XCTestCase {
    private static var outputDirectory: URL? {
        guard let rawPath = ProcessInfo.processInfo.environment["SNAPSHOT_OUTPUT_DIR"], !rawPath.isEmpty else {
            return nil
        }
        return URL(fileURLWithPath: rawPath, isDirectory: true)
    }

    @MainActor
    func testCaptureWindowServerGlassScreens() throws {
        let screens = [
            "photoDetails",
        ]

        for screen in screens {
            let app = XCUIApplication()
            app.launchArguments = ["--ui-fixture", screen]
            app.launch()

            let readyElement = app.descendants(matching: .any)["ui_fixture_ready"]
            XCTAssertTrue(readyElement.waitForExistence(timeout: 10), "Timed out waiting for fixture screen: \(screen)")
            Thread.sleep(forTimeInterval: 0.4)

            let screenshot = XCUIScreen.main.screenshot()
            let pngData = screenshot.pngRepresentation
            XCTAssertGreaterThan(pngData.count, 1024, "Expected non-empty window-server PNG for \(screen)")

            let attachment = XCTAttachment(screenshot: screenshot)
            attachment.name = "window_server_\(screen)"
            attachment.lifetime = .keepAlways
            add(attachment)

            if let outputDir = Self.outputDirectory {
                try FileManager.default.createDirectory(at: outputDir, withIntermediateDirectories: true)
                let fileURL = outputDir.appendingPathComponent("ios_window_\(screen).png")
                try pngData.write(to: fileURL, options: .atomic)
            }

            app.terminate()
        }
    }
}
