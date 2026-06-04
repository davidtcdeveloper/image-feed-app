import SwiftUI
import shared

#if os(macOS)
import AppKit
#endif

struct AdaptiveLayoutHelper {
    #if os(iOS)
    static func getColumnCount(sizeClass: UserInterfaceSizeClass?) -> Int {
        if UIDevice.current.userInterfaceIdiom == .pad {
            return sizeClass == .regular ? 4 : 3
        }
        return 2
    }
    #else
    static func getColumnCount() -> Int {
        return 3 // Standard desktop column count
    }
    #endif
    
    static func getScreenWidth() -> CGFloat {
        #if os(iOS)
        return UIScreen.main.bounds.width
        #else
        return NSScreen.main?.frame.width ?? 1200
        #endif
    }
    
    static func photosForColumn(index: Int, totalColumns: Int, from photos: [Photo]) -> [Photo] {
        guard totalColumns > 0 else { return [] }
        return photos.enumerated()
            .filter { $0.offset % totalColumns == index }
            .map { $0.element }
    }
}

struct URLHelper {
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
