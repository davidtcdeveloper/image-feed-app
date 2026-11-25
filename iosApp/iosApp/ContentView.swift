import shared
import SwiftUI

@MainActor
@Observable
final class CapsuleScrollCoordinator {
    var isMinimized: Bool = false
    private var lastScrollOffset: CGFloat = 0
    private var scrollResetTask: Task<Void, Never>?

    func handleScroll(offset: CGFloat) {
        let delta = offset - lastScrollOffset
        guard abs(delta) >= 2 || offset >= -20 else { return }
        lastScrollOffset = offset

        let targetMinimized: Bool = if offset >= -20 {
            false
        } else if delta < -15 {
            true
        } else if delta > 15 {
            false
        } else {
            isMinimized
        }

        if isMinimized != targetMinimized {
            withAnimation(.spring(response: 0.35, dampingFraction: 0.75)) {
                isMinimized = targetMinimized
            }
        }

        if isMinimized {
            scheduleResetTaskIfNeeded()
        } else if scrollResetTask != nil {
            scrollResetTask?.cancel()
            scrollResetTask = nil
        }
    }

    private func scheduleResetTaskIfNeeded() {
        scrollResetTask?.cancel()
        scrollResetTask = Task {
            try? await Task.sleep(nanoseconds: 1200000000)
            guard !Task.isCancelled else { return }
            if isMinimized {
                withAnimation(.spring(response: 0.35, dampingFraction: 0.75)) {
                    isMinimized = false
                }
            }
        }
    }

    func expand() {
        scrollResetTask?.cancel()
        scrollResetTask = nil
        if isMinimized {
            withAnimation(.spring(response: 0.35, dampingFraction: 0.75)) {
                isMinimized = false
            }
        }
    }
}

private struct FloatingCapsuleHost: View {
    let coordinator: CapsuleScrollCoordinator
    @Binding var selectedTab: Int

    var body: some View {
        FloatingTabCapsule(
            selectedTab: $selectedTab,
            isMinimized: coordinator.isMinimized)
    }
}

struct ContentView: View {
    @State private var selectedTab = 0
    @State private var photosPath = NavigationPath()
    @State private var collectionsPath = NavigationPath()
    @State private var favoritesPath = NavigationPath()
    @State private var isPhotosDetailPresented = false
    @State private var isFavoritesDetailPresented = false
    @State private var capsuleCoordinator = CapsuleScrollCoordinator()

    init() {
        #if os(iOS)
        let appearance = UITabBarAppearance()
        appearance.configureWithDefaultBackground()
        appearance.backgroundColor = .clear
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
        UITabBar.appearance().isHidden = true
        #endif
    }

    private var isCapsuleVisible: Bool {
        if selectedTab == 0 {
            photosPath.isEmpty && !isPhotosDetailPresented
        } else if selectedTab == 1 {
            collectionsPath.isEmpty
        } else {
            favoritesPath.isEmpty && !isFavoritesDetailPresented
        }
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            TabView(selection: $selectedTab) {
                PhotosFeedTabView(
                    path: $photosPath,
                    isDetailPresented: $isPhotosDetailPresented,
                    coordinator: capsuleCoordinator)
                    .tabItem {
                        Label("Photos", systemImage: "photo.stack")
                    }
                    .tag(0)

                CollectionsFeedTabView(path: $collectionsPath)
                    .tabItem {
                        Label("Collections", systemImage: "square.grid.2x2")
                    }
                    .tag(1)

                FavoritesTabView(
                    path: $favoritesPath,
                    isDetailPresented: $isFavoritesDetailPresented,
                    coordinator: capsuleCoordinator)
                    .tabItem {
                        Label("Favorites", systemImage: "bookmark.fill")
                    }
                    .tag(2)
            }
            .tint(Color.primary)
            #if os(iOS)
            .toolbar(.hidden, for: .tabBar)
            #endif

            #if os(iOS)
            if isCapsuleVisible {
                FloatingCapsuleHost(
                    coordinator: capsuleCoordinator,
                    selectedTab: $selectedTab)
                    .padding(.bottom, 16)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .zIndex(10)
            }
            #endif
        }
        .onChange(of: selectedTab) { _, _ in
            capsuleCoordinator.expand()
        }
        .onChange(of: isPhotosDetailPresented) { _, isPresented in
            if !isPresented {
                capsuleCoordinator.expand()
            }
        }
        .onChange(of: isFavoritesDetailPresented) { _, isPresented in
            if !isPresented {
                capsuleCoordinator.expand()
            }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.8), value: isCapsuleVisible)
    }
}

struct CollectionsFeedTabView: View {
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
                    activePath.wrappedValue.append(CollectionPathItem(id: "", type: .search))
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
