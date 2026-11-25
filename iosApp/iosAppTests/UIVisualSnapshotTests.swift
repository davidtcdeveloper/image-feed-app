import SwiftUI
import XCTest
#if os(iOS)
@testable import iosApp
import UIKit
#elseif os(macOS)
import AppKit
@testable import macosApp
#endif

#if os(iOS)
private struct IOSSnapshotCase {
    let name: String
    let screen: UIFixtureScreen
    let size: CGSize
    let style: UIUserInterfaceStyle
    let sizeClass: UserInterfaceSizeClass
}
#elseif os(macOS)
private struct MacOSSnapshotCase {
    let name: String
    let screen: UIFixtureScreen
    let size: CGSize
    let dark: Bool
}
#endif

@MainActor
final class UIVisualSnapshotTests: XCTestCase {
    private static var outputDirectory: URL? {
        guard let rawPath = ProcessInfo.processInfo.environment["SNAPSHOT_OUTPUT_DIR"], !rawPath.isEmpty else {
            return nil
        }
        return URL(fileURLWithPath: rawPath, isDirectory: true)
    }

    #if os(iOS)
    func testCaptureIOSSnapshots() throws {
        let screens = [
            IOSSnapshotCase(
                name: "photo_details_compact_light",
                screen: .photoDetails,
                size: CGSize(width: 393, height: 852),
                style: .light,
                sizeClass: .compact),
            IOSSnapshotCase(
                name: "photo_details_compact_dark",
                screen: .photoDetails,
                size: CGSize(width: 393, height: 852),
                style: .dark,
                sizeClass: .compact),
            IOSSnapshotCase(
                name: "photo_details_regular_dark",
                screen: .photoDetails,
                size: CGSize(width: 1024, height: 768),
                style: .dark,
                sizeClass: .regular),
        ]

        let targetDir = try resolveOutputDirectory()
        for entry in screens {
            let colorScheme: ColorScheme = entry.style == .dark ? .dark : .light
            let view = UIPreviewFixtures.makeView(for: entry.screen)
                .environment(\.colorScheme, colorScheme)
                .environment(\.horizontalSizeClass, entry.sizeClass)
            let data = try renderIOSView(view, size: entry.size, style: entry.style)
            XCTAssertGreaterThan(data.count, 1024, "Expected non-empty PNG for \(entry.name)")
            try writeSnapshot(data: data, name: "ios_\(entry.name).png", to: targetDir)
        }
    }

    private func renderIOSView(_ view: some View, size: CGSize, style: UIUserInterfaceStyle) throws -> Data {
        let controller = UIHostingController(rootView: view)
        controller.overrideUserInterfaceStyle = style

        let windowScene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first
        let window = if let windowScene {
            UIWindow(windowScene: windowScene)
        } else {
            UIWindow(frame: CGRect(origin: .zero, size: size))
        }
        window.frame = CGRect(origin: .zero, size: size)
        window.windowLevel = .statusBar + 1
        window.overrideUserInterfaceStyle = style
        window.rootViewController = controller
        window.makeKeyAndVisible()

        controller.view.frame = window.bounds
        controller.view.setNeedsLayout()
        controller.view.layoutIfNeeded()
        RunLoop.main.run(until: Date().addingTimeInterval(0.25))

        let renderer = UIGraphicsImageRenderer(bounds: window.bounds)
        let image = renderer.image { _ in
            window.drawHierarchy(in: window.bounds, afterScreenUpdates: true)
        }
        window.isHidden = true
        window.rootViewController = nil

        guard let pngData = image.pngData() else {
            throw NSError(domain: "UIVisualSnapshotTests", code: 1, userInfo: [NSLocalizedDescriptionKey: "Failed to encode PNG"])
        }
        return pngData
    }
    #endif

    #if os(macOS)
    func testCaptureMacOSSnapshots() throws {
        let screens = [
            MacOSSnapshotCase(name: "macos_photo_details_light", screen: .photoDetails, size: CGSize(width: 1280, height: 800), dark: false),
            MacOSSnapshotCase(name: "macos_photo_details_dark", screen: .photoDetails, size: CGSize(width: 1280, height: 800), dark: true),
        ]

        let targetDir = try resolveOutputDirectory()
        for entry in screens {
            let colorScheme: ColorScheme = entry.dark ? .dark : .light
            let view = UIPreviewFixtures.makeView(for: entry.screen)
                .environment(\.colorScheme, colorScheme)
            let data = try renderMacOSView(view, size: entry.size, dark: entry.dark)
            XCTAssertGreaterThan(data.count, 1024, "Expected non-empty PNG for \(entry.name)")
            try writeSnapshot(data: data, name: "\(entry.name).png", to: targetDir)
        }
    }

    private func renderMacOSView(_ view: some View, size: CGSize, dark: Bool) throws -> Data {
        let hostingView = NSHostingView(rootView: view)
        hostingView.frame = NSRect(origin: .zero, size: size)

        let window = NSWindow(
            contentRect: NSRect(origin: .zero, size: size),
            styleMask: [.titled, .closable, .resizable],
            backing: .buffered,
            defer: false)
        window.appearance = NSAppearance(named: dark ? .darkAqua : .aqua)
        window.contentView = hostingView
        window.orderFront(nil)

        hostingView.layoutSubtreeIfNeeded()
        RunLoop.main.run(until: Date().addingTimeInterval(0.2))

        guard let rep = hostingView.bitmapImageRepForCachingDisplay(in: hostingView.bounds) else {
            window.orderOut(nil)
            throw NSError(domain: "UIVisualSnapshotTests", code: 2, userInfo: [NSLocalizedDescriptionKey: "Failed to allocate bitmap rep"])
        }
        hostingView.cacheDisplay(in: hostingView.bounds, to: rep)
        window.orderOut(nil)

        guard let pngData = rep.representation(using: .png, properties: [:]) else {
            throw NSError(domain: "UIVisualSnapshotTests", code: 3, userInfo: [NSLocalizedDescriptionKey: "Failed to encode macOS PNG"])
        }
        return pngData
    }
    #endif

    private func resolveOutputDirectory() throws -> URL {
        if let configured = Self.outputDirectory {
            try FileManager.default.createDirectory(at: configured, withIntermediateDirectories: true)
            return configured
        }
        let tempDir = FileManager.default.temporaryDirectory
            .appendingPathComponent("imagefeed_ui_snapshots_\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: tempDir, withIntermediateDirectories: true)
        addTeardownBlock {
            try? FileManager.default.removeItem(at: tempDir)
        }
        return tempDir
    }

    private func writeSnapshot(data: Data, name: String, to directory: URL) throws {
        let fileURL = directory.appendingPathComponent(name)
        try data.write(to: fileURL, options: .atomic)
    }
}
