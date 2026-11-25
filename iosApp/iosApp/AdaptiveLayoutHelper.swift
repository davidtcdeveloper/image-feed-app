import shared
import SwiftUI

#if os(macOS)
import AppKit
#elseif os(iOS)
import UIKit
#endif

// MARK: - Container-Width Environment & Quantized Measurement

private struct ContainerWidthKey: EnvironmentKey {
    static let defaultValue: CGFloat = 0
}

extension EnvironmentValues {
    var containerWidth: CGFloat {
        get { self[ContainerWidthKey.self] }
        set { self[ContainerWidthKey.self] = newValue }
    }
}

private struct ContainerWidthPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        let next = nextValue()
        if next > 0 {
            value = next
        }
    }
}

private struct ContainerWidthReaderModifier: ViewModifier {
    @Binding var containerWidth: CGFloat

    func body(content: Content) -> some View {
        if #available(iOS 18.0, macOS 15.0, *) {
            content
                .onGeometryChange(for: CGFloat.self) { proxy in
                    AdaptiveLayoutHelper.quantizeWidth(proxy.size.width)
                } action: { _, newWidth in
                    if newWidth > 0, abs(newWidth - containerWidth) >= 4 {
                        containerWidth = newWidth
                    }
                }
                .environment(\.containerWidth, containerWidth)
        } else {
            content
                .background {
                    GeometryReader { proxy in
                        Color.clear.preference(
                            key: ContainerWidthPreferenceKey.self,
                            value: AdaptiveLayoutHelper.quantizeWidth(proxy.size.width))
                    }
                }
                .onPreferenceChange(ContainerWidthPreferenceKey.self) { newWidth in
                    if newWidth > 0, abs(newWidth - containerWidth) >= 4 {
                        containerWidth = newWidth
                    }
                }
                .environment(\.containerWidth, containerWidth)
        }
    }
}

extension View {
    /// Measures and propagates quantized container width to both the owning view's state and `\.containerWidth`.
    func measureContainerWidth(_ containerWidth: Binding<CGFloat>) -> some View {
        modifier(ContainerWidthReaderModifier(containerWidth: containerWidth))
    }
}

// MARK: - AdaptiveLayoutHelper

enum AdaptiveLayoutHelper {
    private static let quantizeStepPt: CGFloat = 8

    static func quantizeWidth(_ rawWidth: CGFloat) -> CGFloat {
        guard rawWidth > 0 else { return 0 }
        let bucket = Int(rawWidth / quantizeStepPt) * Int(quantizeStepPt)
        return CGFloat(max(Int(quantizeStepPt), bucket))
    }

    static func displayScale() -> CGFloat {
        #if os(iOS)
        UIScreen.main.scale
        #elseif os(macOS)
        NSScreen.main?.backingScaleFactor ?? 2.0
        #else
        2.0
        #endif
    }

    static func effectiveWidth(containerWidth: CGFloat, fallback: CGFloat = 390) -> CGFloat {
        containerWidth > 0 ? containerWidth : fallback
    }

    #if os(iOS)
    static func getColumnCount(containerWidth: CGFloat, sizeClass: UserInterfaceSizeClass?) -> Int {
        if containerWidth > 0 {
            return photoColumnCount(for: containerWidth)
        }
        return sizeClass == .regular ? 4 : 2
    }

    static func getCollectionColumnCount(containerWidth: CGFloat, sizeClass: UserInterfaceSizeClass?) -> Int {
        if containerWidth > 0 {
            return collectionColumnCount(for: containerWidth)
        }
        return sizeClass == .regular ? 2 : 1
    }
    #else
    static func getColumnCount(containerWidth: CGFloat) -> Int {
        if containerWidth > 0 {
            return photoColumnCount(for: containerWidth)
        }
        return 3
    }

    static func getCollectionColumnCount(containerWidth: CGFloat) -> Int {
        if containerWidth > 0 {
            return collectionColumnCount(for: containerWidth)
        }
        return 2
    }
    #endif

    /// Computes 2–5 photo grid columns from actual measured container width.
    static func photoColumnCount(for containerWidth: CGFloat) -> Int {
        switch containerWidth {
        case 1200...:
            5
        case 840..<1200:
            4
        case 600..<840:
            3
        default:
            2
        }
    }

    /// Computes 1–3 collection columns from actual measured container width.
    static func collectionColumnCount(for containerWidth: CGFloat) -> Int {
        switch containerWidth {
        case 960...:
            3
        case 600..<960:
            2
        default:
            1
        }
    }

    static func calculateItemWidthPx(containerWidth: CGFloat, columnCount: Int) -> Int {
        let resolvedWidth = effectiveWidth(containerWidth: containerWidth)
        let spacing: CGFloat = 8 * CGFloat(columnCount + 1)
        let availableWidth = max(120, resolvedWidth - spacing)
        let widthPt = availableWidth / CGFloat(max(1, columnCount))
        return Int(widthPt * displayScale())
    }

    static func calculateCollectionCardWidthPt(containerWidth: CGFloat, columnCount: Int) -> CGFloat {
        let resolvedWidth = effectiveWidth(containerWidth: containerWidth, fallback: 390)
        let horizontalPadding: CGFloat = 24
        let columnSpacing = CGFloat(max(0, columnCount - 1) * 16)
        return max(200, (resolvedWidth - horizontalPadding - columnSpacing) / CGFloat(max(1, columnCount)))
    }

    struct IndexedItem<T>: Identifiable {
        let index: Int
        let item: T
        let id: String

        var element: T {
            item
        }
    }

    static func partitionIntoColumns<T>(from items: [T], totalColumns: Int, idOf: (T) -> String) -> [[IndexedItem<T>]] {
        guard totalColumns > 0 else { return [] }
        var buckets = Array(repeating: [IndexedItem<T>](), count: totalColumns)
        for (offset, element) in items.enumerated() {
            buckets[offset % totalColumns].append(IndexedItem(index: offset, item: element, id: idOf(element)))
        }
        return buckets
    }

    static func partitionPhotosIntoColumns(from photos: [Photo], totalColumns: Int) -> [[IndexedItem<Photo>]] {
        partitionIntoColumns(from: photos, totalColumns: totalColumns, idOf: \.id)
    }

    static func partitionCollectionsIntoColumns(from collections: [PhotoCollection], totalColumns: Int) -> [[IndexedItem<PhotoCollection>]] {
        partitionIntoColumns(from: collections, totalColumns: totalColumns, idOf: \.id)
    }

    static func indexedPhotosForColumn(index: Int, totalColumns: Int, from photos: [Photo]) -> [IndexedItem<Photo>] {
        guard totalColumns > 0, index < totalColumns else { return [] }
        return photos.enumerated()
            .filter { $0.offset % totalColumns == index }
            .map { IndexedItem(index: $0.offset, item: $0.element, id: $0.element.id) }
    }
}

enum URLHelper {
    static func open(_ url: URL) {
        #if os(iOS)
        UIApplication.shared.open(url)
        #else
        NSWorkspace.shared.open(url)
        #endif
    }

    static func canOpen(_ url: URL) -> Bool {
        #if os(iOS)
        UIApplication.shared.canOpenURL(url)
        #else
        true
        #endif
    }
}
