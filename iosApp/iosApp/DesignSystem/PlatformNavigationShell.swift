import SwiftUI
#if os(iOS)
import UIKit
#endif

// MARK: - Root Destination Model (4-Tab Cross-Platform Parity)

public enum AppRootTab: Int, CaseIterable, Hashable, Identifiable {
    case photos = 0
    case collections = 1
    case favorites = 2
    case search = 3

    public var id: Int {
        rawValue
    }

    public var titleKey: LocalizedStringKey {
        switch self {
        case .photos: "Photos"
        case .collections: "Collections"
        case .favorites: "Favorites"
        case .search: "Search"
        }
    }

    public var systemImage: String {
        switch self {
        case .photos: "photo.stack"
        case .collections: "square.grid.2x2"
        case .favorites: "bookmark.fill"
        case .search: "magnifyingglass"
        }
    }
}

enum PlatformNavigationShellDefaults {
    static let compactBottomPadding: CGFloat = 16
}

// MARK: - Legacy Scroll Coordinator (iOS 17-25 Only)

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

// MARK: - OS-Gated iOS Navigation Shell

#if os(iOS)
struct PlatformNavigationShell<
    PhotosContent: View,
    CollectionsContent: View,
    FavoritesContent: View,
    SearchContent: View
>: View {
    @Binding var selectedTab: AppRootTab
    let isCapsuleVisible: Bool
    @ViewBuilder let photosContent: () -> PhotosContent
    @ViewBuilder let collectionsContent: () -> CollectionsContent
    @ViewBuilder let favoritesContent: () -> FavoritesContent
    @ViewBuilder let searchContent: () -> SearchContent

    var body: some View {
        if #available(iOS 26.0, *) {
            NativeLiquidGlassTabShell(
                selectedTab: $selectedTab,
                isTabBarVisible: isCapsuleVisible,
                photosContent: photosContent,
                collectionsContent: collectionsContent,
                favoritesContent: favoritesContent,
                searchContent: searchContent)
        } else {
            LegacyFloatingTabShell(
                selectedTab: $selectedTab,
                isCapsuleVisible: isCapsuleVisible,
                photosContent: photosContent,
                collectionsContent: collectionsContent,
                favoritesContent: favoritesContent,
                searchContent: searchContent)
        }
    }
}

@available(iOS 26.0, *)
private struct NativeLiquidGlassTabShell<
    PhotosContent: View,
    CollectionsContent: View,
    FavoritesContent: View,
    SearchContent: View
>: View {
    @Binding var selectedTab: AppRootTab
    let isTabBarVisible: Bool
    @ViewBuilder let photosContent: () -> PhotosContent
    @ViewBuilder let collectionsContent: () -> CollectionsContent
    @ViewBuilder let favoritesContent: () -> FavoritesContent
    @ViewBuilder let searchContent: () -> SearchContent

    var body: some View {
        let visibility: Visibility = isTabBarVisible ? .visible : .hidden
        TabView(selection: $selectedTab) {
            Tab(AppRootTab.photos.titleKey, systemImage: AppRootTab.photos.systemImage, value: AppRootTab.photos) {
                photosContent()
                    .toolbarVisibility(visibility, for: .tabBar)
            }

            Tab(AppRootTab.collections.titleKey, systemImage: AppRootTab.collections.systemImage, value: AppRootTab.collections) {
                collectionsContent()
                    .toolbarVisibility(visibility, for: .tabBar)
            }

            Tab(AppRootTab.favorites.titleKey, systemImage: AppRootTab.favorites.systemImage, value: AppRootTab.favorites) {
                favoritesContent()
                    .toolbarVisibility(visibility, for: .tabBar)
            }

            Tab(AppRootTab.search.titleKey, systemImage: AppRootTab.search.systemImage, value: AppRootTab.search, role: .search) {
                searchContent()
                    .toolbarVisibility(visibility, for: .tabBar)
            }
        }
        .tint(Color.primary)
        .tabBarMinimizeBehavior(.onScrollDown)
        .sensoryFeedback(.impact(weight: .light), trigger: selectedTab)
    }
}

@available(iOS, introduced: 17.0, deprecated: 26.0, message: "Legacy floating capsule fallback for iOS 17-25")
private struct LegacyFloatingTabShell<
    PhotosContent: View,
    CollectionsContent: View,
    FavoritesContent: View,
    SearchContent: View
>: View {
    @Binding var selectedTab: AppRootTab
    let isCapsuleVisible: Bool
    @State private var capsuleCoordinator = CapsuleScrollCoordinator()
    @ViewBuilder let photosContent: () -> PhotosContent
    @ViewBuilder let collectionsContent: () -> CollectionsContent
    @ViewBuilder let favoritesContent: () -> FavoritesContent
    @ViewBuilder let searchContent: () -> SearchContent

    init(
        selectedTab: Binding<AppRootTab>,
        isCapsuleVisible: Bool,
        @ViewBuilder photosContent: @escaping () -> PhotosContent,
        @ViewBuilder collectionsContent: @escaping () -> CollectionsContent,
        @ViewBuilder favoritesContent: @escaping () -> FavoritesContent,
        @ViewBuilder searchContent: @escaping () -> SearchContent)
    {
        self._selectedTab = selectedTab
        self.isCapsuleVisible = isCapsuleVisible
        self.photosContent = photosContent
        self.collectionsContent = collectionsContent
        self.favoritesContent = favoritesContent
        self.searchContent = searchContent

        let appearance = UITabBarAppearance()
        appearance.configureWithDefaultBackground()
        appearance.backgroundColor = .clear
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
        UITabBar.appearance().isHidden = true
    }

    private var selectedIndexBinding: Binding<Int> {
        Binding(
            get: { selectedTab.rawValue },
            set: { newValue in
                if let tab = AppRootTab(rawValue: newValue) {
                    selectedTab = tab
                }
            })
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            TabView(selection: $selectedTab) {
                photosContent()
                    .environment(\.legacyCapsuleCoordinator, capsuleCoordinator)
                    .tabItem {
                        Label(AppRootTab.photos.titleKey, systemImage: AppRootTab.photos.systemImage)
                    }
                    .tag(AppRootTab.photos)

                collectionsContent()
                    .tabItem {
                        Label(AppRootTab.collections.titleKey, systemImage: AppRootTab.collections.systemImage)
                    }
                    .tag(AppRootTab.collections)

                favoritesContent()
                    .environment(\.legacyCapsuleCoordinator, capsuleCoordinator)
                    .tabItem {
                        Label(AppRootTab.favorites.titleKey, systemImage: AppRootTab.favorites.systemImage)
                    }
                    .tag(AppRootTab.favorites)

                searchContent()
                    .tabItem {
                        Label(AppRootTab.search.titleKey, systemImage: AppRootTab.search.systemImage)
                    }
                    .tag(AppRootTab.search)
            }
            .tint(Color.primary)
            .toolbar(.hidden, for: .tabBar)

            if isCapsuleVisible {
                FloatingTabCapsule(
                    selectedTab: selectedIndexBinding,
                    isMinimized: capsuleCoordinator.isMinimized)
                    .padding(.bottom, PlatformNavigationShellDefaults.compactBottomPadding)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .zIndex(10)
            }
        }
        .onChange(of: selectedTab) { _, _ in
            capsuleCoordinator.expand()
        }
        .onChange(of: isCapsuleVisible) { _, visible in
            if visible {
                capsuleCoordinator.expand()
            }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.8), value: isCapsuleVisible)
    }
}

private struct LegacyCapsuleCoordinatorKey: EnvironmentKey {
    static let defaultValue: CapsuleScrollCoordinator? = nil
}

extension EnvironmentValues {
    var legacyCapsuleCoordinator: CapsuleScrollCoordinator? {
        get { self[LegacyCapsuleCoordinatorKey.self] }
        set { self[LegacyCapsuleCoordinatorKey.self] = newValue }
    }
}

extension View {
    /// Hides the system tab bar on iOS 17-25 (where `FloatingTabCapsule` replaces it),
    /// and preserves the native iOS 26+ Liquid Glass tab bar unless `hideOnModernOS` is true.
    @ViewBuilder
    func adaptiveTabBarVisibility(hideOnModernOS: Bool = false) -> some View {
        if #available(iOS 26.0, *) {
            if hideOnModernOS {
                toolbar(.hidden, for: .tabBar)
            } else {
                self
            }
        } else {
            toolbar(.hidden, for: .tabBar)
        }
    }
}
#endif

// MARK: - Root Search Routing & Focus Environment

private struct OpenRootSearchKey: EnvironmentKey {
    static let defaultValue: (() -> Void)? = nil
}

private struct SearchFocusTriggerKey: EnvironmentKey {
    static let defaultValue: Int = 0
}

extension EnvironmentValues {
    var openRootSearch: (() -> Void)? {
        get { self[OpenRootSearchKey.self] }
        set { self[OpenRootSearchKey.self] = newValue }
    }

    var searchFocusTrigger: Int {
        get { self[SearchFocusTriggerKey.self] }
        set { self[SearchFocusTriggerKey.self] = newValue }
    }
}
