import Foundation
import ImageIO
import SwiftUI

#if os(iOS)
import UIKit
public typealias PlatformImage = UIImage
#elseif os(macOS)
import AppKit
public typealias PlatformImage = NSImage
#endif

public final class ImageMemoryCache: @unchecked Sendable {
    public static let shared = ImageMemoryCache()

    private let cache = NSCache<NSURL, PlatformImage>()

    private init() {
        cache.totalCostLimit = 50 * 1024 * 1024 // 50 MB
        cache.countLimit = 150

        #if os(iOS)
        NotificationCenter.default.addObserver(
            forName: UIApplication.didReceiveMemoryWarningNotification,
            object: nil,
            queue: nil
        ) { [weak self] _ in
            self?.removeAll()
        }
        #endif
    }

    public func image(for url: URL) -> PlatformImage? {
        cache.object(forKey: url as NSURL)
    }

    public func insert(_ image: PlatformImage, for url: URL) {
        let entryCost = cost(for: image)
        cache.setObject(image, forKey: url as NSURL, cost: entryCost)
    }

    public func removeAll() {
        cache.removeAllObjects()
    }

    private func cost(for image: PlatformImage) -> Int {
        #if os(iOS)
        if let cgImage = image.cgImage {
            return cgImage.bytesPerRow * cgImage.height
        }
        return Int(image.size.width * image.size.height * 4)
        #elseif os(macOS)
        if let cgImage = image.cgImage(forProposedRect: nil, context: nil, hints: nil) {
            return cgImage.bytesPerRow * cgImage.height
        }
        return Int(image.size.width * image.size.height * 4)
        #endif
    }
}

public struct KFImage: View {
    private let url: URL?
    private let targetWidthPx: Int?
    private var placeholderBuilder: (() -> AnyView)?
    private var fadeDuration: Double = 0.25
    private var isResizable = false

    @State private var loadedImage: PlatformImage?
    @State private var hasFailed = false

    public init(_ url: URL?, targetWidthPx: Int? = nil) {
        self.url = url
        self.targetWidthPx = targetWidthPx
    }

    /// Custom placeholder builder (lazy closure, only evaluated on cache miss or failure)
    public func placeholder(@ViewBuilder _ content: @escaping () -> some View) -> KFImage {
        var copy = self
        copy.placeholderBuilder = { AnyView(content()) }
        return copy
    }

    /// Custom fade modifier
    public func fade(duration: Double) -> KFImage {
        var copy = self
        copy.fadeDuration = duration
        return copy
    }

    /// Custom resizable modifier
    public func resizable() -> KFImage {
        var copy = self
        copy.isResizable = true
        return copy
    }

    private var currentImage: PlatformImage? {
        if let loadedImage {
            return loadedImage
        }
        if let url {
            return ImageMemoryCache.shared.image(for: url)
        }
        return nil
    }

    public var body: some View {
        Group {
            if let image = currentImage {
                renderImage(image)
            } else if hasFailed || url == nil {
                if let placeholderBuilder {
                    placeholderBuilder()
                } else {
                    Color(white: 0.1)
                }
            } else {
                if let placeholderBuilder {
                    placeholderBuilder()
                } else {
                    ProgressView()
                        .tint(.white.opacity(0.5))
                }
            }
        }
        .task(id: url) {
            await loadImage()
        }
    }

    @ViewBuilder
    private func renderImage(_ platformImage: PlatformImage) -> some View {
        #if os(iOS)
        let image = Image(uiImage: platformImage)
        #elseif os(macOS)
        let image = Image(nsImage: platformImage)
        #endif

        if isResizable {
            image.resizable()
        } else {
            image
        }
    }

    private func loadImage() async {
        guard let url else {
            hasFailed = true
            return
        }

        if let cached = ImageMemoryCache.shared.image(for: url) {
            self.loadedImage = cached
            return
        }

        hasFailed = false

        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            try Task.checkCancellation()

            let maxPixelSize = inferMaxPixelSize(from: url, explicit: targetWidthPx)
            let decoded = await Task.detached(priority: .userInitiated) {
                KFImage.downsample(data: data, maxPixelSize: maxPixelSize)
            }.value

            try Task.checkCancellation()

            if let decoded {
                ImageMemoryCache.shared.insert(decoded, for: url)
                if fadeDuration > 0 {
                    withAnimation(.easeOut(duration: fadeDuration)) {
                        self.loadedImage = decoded
                    }
                } else {
                    self.loadedImage = decoded
                }
            } else {
                self.hasFailed = true
            }
        } catch {
            if !Task.isCancelled {
                self.hasFailed = true
            }
        }
    }

    private func inferMaxPixelSize(from url: URL?, explicit: Int?) -> Int {
        if let explicit, explicit > 0 {
            return explicit
        }
        if let url,
           let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
           let wItem = components.queryItems?.first(where: { $0.name == "w" }),
           let wVal = wItem.value,
           let wInt = Int(wVal),
           wInt > 0 {
            return wInt
        }
        return 1080
    }

    private static func downsample(data: Data, maxPixelSize: Int) -> PlatformImage? {
        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceShouldCacheImmediately: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: maxPixelSize
        ]
        guard let source = CGImageSourceCreateWithData(data as CFData, nil),
              let cgImage = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary) else {
            return nil
        }
        #if os(iOS)
        return UIImage(cgImage: cgImage)
        #elseif os(macOS)
        return NSImage(cgImage: cgImage, size: NSSize(width: cgImage.width, height: cgImage.height))
        #endif
    }
}
