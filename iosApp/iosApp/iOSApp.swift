import shared
import SwiftUI

@main
struct IOSApp: App {
    init() {
        // Initialize Koin DI on iOS startup
        KoinKt.doInitKoin()
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
