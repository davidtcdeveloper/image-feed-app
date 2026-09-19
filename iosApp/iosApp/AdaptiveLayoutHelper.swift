import shared
import SwiftUI

#if os(macOS)
import AppKit
#endif

enum AdaptiveLayoutHelper {
    #if os(iOS)
    static func getColumnCount(sizeClass: UserInterfaceSizeClass?) -> Int {
        if UIDevice.current.userInterfaceIdiom == .pad {
            return sizeClass == .regular ? 4 : 3
        }
        return 2
    }

    static func getCollectionColumnCount(sizeClass: UserInterfaceSizeClass?) -> Int {
        if UIDevice.current.userInterfaceIdiom == .pad {
            return sizeClass == .regular ? 3 : 2
        }
        return 1
    }
    #else
    static func getColumnCount() -> Int {
        3 // Standard desktop column count
    }

    static func getCollectionColumnCount() -> Int {
        2 // Standard desktop collection count
    }
    #endif

    static func getScreenWidth() -> CGFloat {
        #if os(iOS)
        return UIScreen.main.bounds.width
        #else
        return NSScreen.main?.frame.width ?? 1200
        #endif
    }

    static func itemsForColumn<T>(index: Int, totalColumns: Int, from items: [T]) -> [T] {
        guard totalColumns > 0 else { return [] }
        return items.enumerated()
            .filter { $0.offset % totalColumns == index }
            .map(\.element)
    }

    static func photosForColumn(index: Int, totalColumns: Int, from photos: [Photo]) -> [Photo] {
        itemsForColumn(index: index, totalColumns: totalColumns, from: photos)
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
        return UIApplication.shared.canOpenURL(url)
        #else
        return true
        #endif
    }
}
