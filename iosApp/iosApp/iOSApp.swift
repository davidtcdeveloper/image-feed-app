import SwiftUI
import shared

@main
struct iOSApp: App {
    init() {
        // Initialize Koin DI on iOS startup
        KoinKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
