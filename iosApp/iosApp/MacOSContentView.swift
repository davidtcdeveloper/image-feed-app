import shared
import SwiftUI

struct MacOSContentView: View {
    enum SidebarSelection: Hashable {
        case photos
        case collections
        case favorites
        case search
    }

    @State private var selection: SidebarSelection? = .photos
    @State private var searchFocusTrigger = 0
    private let initialFeedViewModel: FeedViewModel?
    private let initialSearchViewModel: SearchViewModel?

    init() {
        self.initialFeedViewModel = nil
        self.initialSearchViewModel = nil
    }

    #if DEBUG
    init(
        initialFeedViewModel: FeedViewModel?,
        initialSearchViewModel: SearchViewModel? = nil,
        initialSelection: SidebarSelection? = .photos)
    {
        self.initialFeedViewModel = initialFeedViewModel
        self.initialSearchViewModel = initialSearchViewModel
        self._selection = State(initialValue: initialSelection)
    }
    #endif

    var body: some View {
        NavigationSplitView {
            List(selection: $selection) {
                NavigationLink(value: SidebarSelection.photos) {
                    Label("Photos", systemImage: "photo.stack")
                }

                NavigationLink(value: SidebarSelection.collections) {
                    Label("Collections", systemImage: "square.grid.2x2")
                }

                NavigationLink(value: SidebarSelection.favorites) {
                    Label("Favorites", systemImage: "bookmark.fill")
                }

                NavigationLink(value: SidebarSelection.search) {
                    Label("Search", systemImage: "magnifyingglass")
                }
            }
            .navigationTitle("Image Feed")
            .listStyle(SidebarListStyle())
            .frame(minWidth: 200)
        } detail: {
            if let selection {
                switch selection {
                case .photos:
                    PhotosFeedTabView(initialViewModel: initialFeedViewModel)
                        .frame(minWidth: 500)
                case .collections:
                    CollectionsFeedTabView()
                        .frame(minWidth: 500)
                case .favorites:
                    FavoritesTabView()
                        .frame(minWidth: 500)
                case .search:
                    SearchTabView(initialViewModel: initialSearchViewModel)
                        .frame(minWidth: 500)
                }
            } else {
                Text("Select a category from the sidebar")
                    .foregroundStyle(.secondary)
            }
        }
        .frame(minWidth: 800, minHeight: 600)
        .environment(\.openRootSearch) {
            selection = .search
            searchFocusTrigger += 1
        }
        .environment(\.searchFocusTrigger, searchFocusTrigger)
        #if os(macOS)
        .background {
            Button("") {
                selection = .search
                searchFocusTrigger += 1
            }
            .keyboardShortcut("f", modifiers: .command)
            .frame(width: 0, height: 0)
            .opacity(0)
            .accessibilityHidden(true)
        }
        #endif
    }
}
