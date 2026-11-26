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

    @Namespace private var dummyNamespace

    init(path: Binding<NavigationPath>? = nil) {
        self.externalPath = path
    }

    var body: some View {
        NavigationStack(path: activePath) {
            CollectionsFeedView(
                onCollectionSelect: { collectionId in
                    activePath.wrappedValue.append(CollectionPathItem(id: collectionId, type: .collection))
                },
                onSearchClick: {
                    if let openRootSearch {
                        openRootSearch()
                    } else {
                        activePath.wrappedValue.append(CollectionPathItem(id: "", type: .search))
                    }
                })
                .navigationDestination(for: CollectionPathItem.self) { item in
                    Group {
                        switch item.type {
                        case .collection:
                            CollectionDetailView(
                                collectionId: item.id,
                                onPhotoSelect: { photoId in
                                    activePath.wrappedValue.append(CollectionPathItem(id: photoId, type: .photo))
                                },
                                onCollectionSelect: { relId in
                                    activePath.wrappedValue.append(CollectionPathItem(id: relId, type: .collection))
                                },
                                onUserSelect: { username in
                                    activePath.wrappedValue.append(CollectionPathItem(id: username, type: .user))
                                })
                        case .photo:
                            PhotoDetailsView(
                                photoId: item.id,
                                heroNamespace: dummyNamespace,
                                onUserSelect: { username in
                                    activePath.wrappedValue.append(CollectionPathItem(id: username, type: .user))
                                },
                                onTagSelect: { tag in
                                    activePath.wrappedValue.append(CollectionPathItem(id: tag, type: .search))
                                })
                        case .user:
                            UserProfileView(
                                username: item.id,
                                onPhotoSelect: { photoId in
                                    activePath.wrappedValue.append(CollectionPathItem(id: photoId, type: .photo))
                                },
                                onCollectionSelect: { colId in
                                    activePath.wrappedValue.append(CollectionPathItem(id: colId, type: .collection))
                                })
                        case .search:
                            SearchView(
                                initialQuery: item.id,
                                onPhotoSelect: { photoId in
                                    activePath.wrappedValue.append(CollectionPathItem(id: photoId, type: .photo))
                                },
                                onUserSelect: { username in
                                    activePath.wrappedValue.append(CollectionPathItem(id: username, type: .user))
                                },
                                onCollectionSelect: { colId in
                                    activePath.wrappedValue.append(CollectionPathItem(id: colId, type: .collection))
                                })
                        }
                    }
                    #if os(iOS)
                    .toolbar(.hidden, for: .tabBar)
                    #endif
                }
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
    @Namespace private var dummyNamespace

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
                    activePath.wrappedValue.append(SearchPathItem(id: photoId, type: .photo))
                },
                onUserSelect: { username in
                    activePath.wrappedValue.append(SearchPathItem(id: username, type: .user))
                },
                onCollectionSelect: { colId in
                    activePath.wrappedValue.append(SearchPathItem(id: colId, type: .collection))
                })
                .navigationDestination(for: SearchPathItem.self) { item in
                    Group {
                        switch item.type {
                        case .photo:
                            PhotoDetailsView(
                                photoId: item.id,
                                heroNamespace: dummyNamespace,
                                onUserSelect: { username in
                                    activePath.wrappedValue.append(SearchPathItem(id: username, type: .user))
                                },
                                onTagSelect: { tag in
                                    activePath.wrappedValue.append(SearchPathItem(id: tag, type: .search))
                                })
                        case .user:
                            UserProfileView(
                                username: item.id,
                                onPhotoSelect: { photoId in
                                    activePath.wrappedValue.append(SearchPathItem(id: photoId, type: .photo))
                                },
                                onCollectionSelect: { colId in
                                    activePath.wrappedValue.append(SearchPathItem(id: colId, type: .collection))
                                })
                        case .collection:
                            CollectionDetailView(
                                collectionId: item.id,
                                onPhotoSelect: { photoId in
                                    activePath.wrappedValue.append(SearchPathItem(id: photoId, type: .photo))
                                },
                                onCollectionSelect: { relId in
                                    activePath.wrappedValue.append(SearchPathItem(id: relId, type: .collection))
                                },
                                onUserSelect: { username in
                                    activePath.wrappedValue.append(SearchPathItem(id: username, type: .user))
                                })
                        case .search:
                            SearchView(
                                initialQuery: item.id,
                                onPhotoSelect: { photoId in
                                    activePath.wrappedValue.append(SearchPathItem(id: photoId, type: .photo))
                                },
                                onUserSelect: { username in
                                    activePath.wrappedValue.append(SearchPathItem(id: username, type: .user))
                                },
                                onCollectionSelect: { colId in
                                    activePath.wrappedValue.append(SearchPathItem(id: colId, type: .collection))
                                })
                        }
                    }
                    #if os(iOS)
                    .toolbar(.hidden, for: .tabBar)
                    #endif
                }
        }
    }
}

struct SearchPathItem: Hashable {
    let id: String
    let type: ItemType

    enum ItemType {
        case photo
        case user
        case collection
        case search
    }
}

struct CollectionPathItem: Hashable {
    let id: String
    let type: ItemType

    enum ItemType {
        case collection
        case photo
        case search
        case user
    }
}

struct FeedPathItem: Hashable {
    let id: String
    let type: ItemType

    enum ItemType {
        case photo
        case user
        case collection
        case search
    }
}
