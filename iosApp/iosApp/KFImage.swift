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
            queue: nil)
        { [weak self] _ in
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

public struct KFImage<Placeholder: View>: View {
    private let url: URL?
    private let targetWidthPx: Int?
    private var placeholderBuilder: (() -> Placeholder)?
    private var fadeDuration: Double = 0.25
    private var isResizable = false

    @State private var loadedUrl: URL?
    @State private var loadedImage: PlatformImage?
    @State private var failedUrl: URL?

    public init(_ url: URL?, targetWidthPx: Int? = nil) where Placeholder == EmptyView {
        self.url = url
        self.targetWidthPx = targetWidthPx
        self.placeholderBuilder = nil
    }

    private init(
        url: URL?,
        targetWidthPx: Int?,
        placeholderBuilder: (() -> Placeholder)?,
        fadeDuration: Double,
        isResizable: Bool)
    {
        self.url = url
        self.targetWidthPx = targetWidthPx
        self.placeholderBuilder = placeholderBuilder
        self.fadeDuration = fadeDuration
        self.isResizable = isResizable
    }

    /// Custom placeholder builder (lazy closure, only evaluated on cache miss or failure)
    public func placeholder<P: View>(@ViewBuilder _ content: @escaping () -> P) -> KFImage<P> {
        KFImage<P>(
            url: url,
            targetWidthPx: targetWidthPx,
            placeholderBuilder: content,
            fadeDuration: fadeDuration,
            isResizable: isResizable)
    }

    /// Custom fade modifier
    public func fade(duration: Double) -> KFImage<Placeholder> {
        var copy = self
        copy.fadeDuration = duration
        return copy
    }

    /// Custom resizable modifier
    public func resizable() -> KFImage<Placeholder> {
        var copy = self
        copy.isResizable = true
        return copy
    }

    private var currentImage: PlatformImage? {
        guard let url else { return nil }
        if let loadedImage, loadedUrl == url {
            return loadedImage
        }
        return ImageMemoryCache.shared.image(for: url)
    }

    private var isFailed: Bool {
        guard let url else { return true }
        return failedUrl == url
    }

    public var body: some View {
        Group {
            if let image = currentImage {
                renderImage(image)
            } else if isFailed || url == nil {
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
            failedUrl = nil
            loadedImage = nil
            loadedUrl = nil
            return
        }

        if let cached = ImageMemoryCache.shared.image(for: url) {
            loadedUrl = url
            loadedImage = cached
            failedUrl = nil
            return
        }

        failedUrl = nil

        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            try Task.checkCancellation()

            let maxPixelSize = inferMaxPixelSize(from: url, explicit: targetWidthPx)
            let decoded = await Task.detached(priority: .userInitiated) {
                Self.downsample(data: data, maxPixelSize: maxPixelSize)
            }.value

            try Task.checkCancellation()

            if let decoded {
                ImageMemoryCache.shared.insert(decoded, for: url)
                if fadeDuration > 0 {
                    withAnimation(.easeOut(duration: fadeDuration)) {
                        loadedUrl = url
                        loadedImage = decoded
                    }
                } else {
                    loadedUrl = url
                    loadedImage = decoded
                }
            } else {
                failedUrl = url
            }
        } catch {
            if !Task.isCancelled {
                failedUrl = url
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
           wInt > 0
        {
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
              let cgImage = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary)
        else {
            return nil
        }
        #if os(iOS)
        return UIImage(cgImage: cgImage)
        #elseif os(macOS)
        return NSImage(cgImage: cgImage, size: NSSize(width: cgImage.width, height: cgImage.height))
        #endif
    }
}
