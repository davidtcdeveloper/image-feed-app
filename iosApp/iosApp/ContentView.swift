import shared
import SwiftUI

struct ContentView: View {
    @State private var selectedTab: AppRootTab = .photos
    @State private var photosPath = NavigationPath()
    @State private var collectionsPath = NavigationPath()
    @State private var favoritesPath = NavigationPath()
    @State private var searchPath = NavigationPath()
    @State private var searchFocusTrigger = 0
    @State private var isPhotosDetailPresented = false
    @State private var isFavoritesDetailPresented = false
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
        initialTab: AppRootTab = .photos)
    {
        self.initialFeedViewModel = initialFeedViewModel
        self.initialSearchViewModel = initialSearchViewModel
        self._selectedTab = State(initialValue: initialTab)
    }
    #endif

    private var isCapsuleVisible: Bool {
        switch selectedTab {
        case .photos:
            photosPath.isEmpty && !isPhotosDetailPresented
        case .collections:
            collectionsPath.isEmpty
        case .favorites:
            favoritesPath.isEmpty && !isFavoritesDetailPresented
        case .search:
            searchPath.isEmpty
        }
    }

    var body: some View {
        #if os(iOS)
        PlatformNavigationShell(
            selectedTab: $selectedTab,
            isCapsuleVisible: isCapsuleVisible)
        {
            LegacyCoordinatorReader { coordinator in
                PhotosFeedTabView(
                    path: $photosPath,
                    isDetailPresented: $isPhotosDetailPresented,
                    coordinator: coordinator,
                    initialViewModel: initialFeedViewModel)
            }
        } collectionsContent: {
            CollectionsFeedTabView(path: $collectionsPath)
        } favoritesContent: {
            LegacyCoordinatorReader { coordinator in
                FavoritesTabView(
                    path: $favoritesPath,
                    isDetailPresented: $isFavoritesDetailPresented,
                    coordinator: coordinator)
            }
        } searchContent: {
            SearchTabView(path: $searchPath, initialViewModel: initialSearchViewModel)
        }
        .environment(\.openRootSearch, openRootSearch)
        .environment(\.searchFocusTrigger, searchFocusTrigger)
        .background {
            Button("") {
                openRootSearch()
            }
            .keyboardShortcut("f", modifiers: .command)
            .frame(width: 0, height: 0)
            .opacity(0)
            .accessibilityHidden(true)
        }
        #else
        MacOSContentView(initialFeedViewModel: initialFeedViewModel)
        #endif
    }

    private func openRootSearch() {
        selectedTab = .search
        searchPath = NavigationPath()
        searchFocusTrigger += 1
    }
}

#if os(iOS)
private struct LegacyCoordinatorReader<Content: View>: View {
    @Environment(\.legacyCapsuleCoordinator) private var coordinator
    @ViewBuilder let content: (CapsuleScrollCoordinator?) -> Content

    var body: some View {
        content(coordinator)
    }
}
#endif

struct CollectionsFeedTabView: View {
    @Environment(\.openRootSearch) private var openRootSearch
    private var externalPath: Binding<NavigationPath>?
    @State private var internalPath = NavigationPath()
    private var activePath: Binding<NavigationPath> {
        externalPath ?? $internalPath
    }

    @Namespace private var heroNamespace

    init(path: Binding<NavigationPath>? = nil) {
        self.externalPath = path
    }

    var body: some View {
        NavigationStack(path: activePath) {
            CollectionsFeedView(
                onCollectionSelect: { collectionId in
                    AppNavigationDefaults.push(.collection(id: collectionId), onto: activePath)
                },
                onSearchClick: {
                    if let openRootSearch {
                        openRootSearch()
                    } else {
                        AppNavigationDefaults.push(.search(query: ""), onto: activePath)
                    }
                })
                .appNavigationDestinations(path: activePath, heroNamespace: heroNamespace)
        }
    }
}

struct SearchTabView: View {
    private var externalPath: Binding<NavigationPath>?
    @State private var internalPath = NavigationPath()
    private var activePath: Binding<NavigationPath> {
        externalPath ?? $internalPath
    }

    private let initialQuery: String
    private let initialViewModel: SearchViewModel?
    @Namespace private var heroNamespace

    init(
        path: Binding<NavigationPath>? = nil,
        initialQuery: String = "",
        initialViewModel: SearchViewModel? = nil)
    {
        self.externalPath = path
        self.initialQuery = initialQuery
        self.initialViewModel = initialViewModel
    }

    var body: some View {
        NavigationStack(path: activePath) {
            SearchView(
                initialQuery: initialQuery,
                initialViewModel: initialViewModel,
                onPhotoSelect: { photoId in
                    AppNavigationDefaults.push(.photo(id: photoId), onto: activePath)
                },
                onUserSelect: { username in
                    AppNavigationDefaults.push(.user(username: username), onto: activePath)
                },
                onCollectionSelect: { colId in
                    AppNavigationDefaults.push(.collection(id: colId), onto: activePath)
                })
                .appNavigationDestinations(path: activePath, heroNamespace: heroNamespace)
        }
    }
}
