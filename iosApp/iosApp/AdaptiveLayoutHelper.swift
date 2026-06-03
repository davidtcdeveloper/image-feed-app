import SwiftUI
import shared

struct AdaptiveLayoutHelper {
    static func getColumnCount(sizeClass: UserInterfaceSizeClass?) -> Int {
        #if os(iOS)
        if UIDevice.current.userInterfaceIdiom == .pad {
            return sizeClass == .regular ? 4 : 3
        }
        #endif
        return 2
    }
    
    static func photosForColumn(index: Int, totalColumns: Int, from photos: [Photo]) -> [Photo] {
        guard totalColumns > 0 else { return [] }
        return photos.enumerated()
            .filter { $0.offset % totalColumns == index }
            .map { $0.element }
    }
}
