import shared
import SwiftUI

@main
struct IOSApp: App {
    init() {
    }

    var body: some Scene {
        WindowGroup {
            #if os(iOS)
            ContentView()
            #else
            MacOSContentView()
            #endif
        }
    }
}
