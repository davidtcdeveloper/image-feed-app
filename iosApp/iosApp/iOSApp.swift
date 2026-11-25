import shared
import SwiftUI

@main
struct IOSApp: App {
    init() {}

    var body: some Scene {
        WindowGroup {
            #if DEBUG
            if let fixtureScreen = Self.requestedFixtureScreen {
                UIPreviewFixtures.makeView(for: fixtureScreen)
                    .accessibilityIdentifier("ui_fixture_ready")
            } else {
                rootView
            }
            #else
            rootView
            #endif
        }
    }

    private var rootView: some View {
        #if os(iOS)
        ContentView()
        #else
        MacOSContentView()
        #endif
    }

    #if DEBUG
    private static var requestedFixtureScreen: UIFixtureScreen? {
        let args = ProcessInfo.processInfo.arguments
        guard let flagIndex = args.firstIndex(of: "--ui-fixture"),
              flagIndex + 1 < args.count
        else {
            return nil
        }
        return UIFixtureScreen(rawValue: args[flagIndex + 1])
    }
    #endif
}
