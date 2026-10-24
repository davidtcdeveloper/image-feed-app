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
    @State private var isPhotosDetailPresented = false
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
        } else {
            collectionsPath.isEmpty
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

private struct FeedScrollOffsetKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

#if os(iOS)
private struct FeedScrollTrackingModifier: ViewModifier {
    let onOffsetChange: (CGFloat) -> Void

    func body(content: Content) -> some View {
        if #available(iOS 18.0, macOS 15.0, *) {
            content
                .onScrollGeometryChange(for: Int.self) { geo in
                    let offset = -(geo.contentOffset.y + geo.contentInsets.top)
                    return Int(offset / 8)
                } action: { _, newBucket in
                    onOffsetChange(CGFloat(newBucket * 8))
                }
        } else {
            content
                .coordinateSpace(name: "photoFeedScroll")
                .onPreferenceChange(FeedScrollOffsetKey.self) { offset in
                    onOffsetChange(offset)
                }
        }
    }
}
#endif

struct PhotosFeedTabView: View {
    #if os(iOS)
    @Environment(\.horizontalSizeClass) var sizeClass
    #endif
    @State private var viewModel = FeedViewModel()

    private var externalPath: Binding<NavigationPath>?
    @State private var internalPath = NavigationPath()
    private var activePath: Binding<NavigationPath> {
        externalPath ?? $internalPath
    }

    private var externalDetailPresented: Binding<Bool>?
    @State private var internalDetailPresented = false
    private var activeDetailPresented: Binding<Bool> {
        externalDetailPresented ?? $internalDetailPresented
    }

    private var coordinator: CapsuleScrollCoordinator?

    @State private var isShaking = false
    @State private var shakeFeedbackTrigger = 0
    @Namespace private var categoryNamespace
    @Namespace private var heroNamespace
    @State private var selectedPhotoForHero: Photo?

    init(
        path: Binding<NavigationPath>? = nil,
        isDetailPresented: Binding<Bool>? = nil,
        coordinator: CapsuleScrollCoordinator? = nil)
    {
        self.externalPath = path
        self.externalDetailPresented = isDetailPresented
        self.coordinator = coordinator
    }

    private var columnCount: Int {
        #if os(iOS)
        return AdaptiveLayoutHelper.getColumnCount(sizeClass: sizeClass)
        #else
        return AdaptiveLayoutHelper.getColumnCount()
        #endif
    }

    var body: some View {
        NavigationStack(path: activePath) {
            ZStack {
                GlassTheme.canvasBackgroundColor
                    .ignoresSafeArea()

                if viewModel.photos.isEmpty, viewModel.isLoading {
                    VStack(spacing: 0) {
                        categoryBar
                        Spacer()
                        ProgressView()
                            .tint(.white)
                        Spacer()
                    }
                } else if viewModel.photos.isEmpty, viewModel.error != nil {
                    VStack(spacing: 0) {
                        categoryBar
                        Spacer()
                        ErrorView(error: viewModel.error ?? String(localized: "Unknown error"), onRetry: {
                            viewModel.refresh()
                        })
                        Spacer()
                    }
                } else {
                    ScrollView {
                        LazyVStack(spacing: 8) {
                            #if os(iOS)
                            if #unavailable(iOS 18.0) {
                                GeometryReader { proxy in
                                    let rawY = proxy.frame(in: .named("photoFeedScroll")).minY
                                    let quantizedY = CGFloat(Int(rawY / 8) * 8)
                                    Color.clear.preference(
                                        key: FeedScrollOffsetKey.self,
                                        value: quantizedY)
                                }
                                .frame(height: 0)
                            }
                            #endif

                            HStack(alignment: .top, spacing: 8) {
                                ForEach(0..<columnCount, id: \.self) { colIndex in
                                    LazyVStack(spacing: 8) {
                                        ForEach(viewModel.columnItems(for: colIndex, totalColumns: columnCount)) { indexed in
                                            let photo = indexed.item
                                            PhotoCard(
                                                photo: photo,
                                                viewModel: viewModel,
                                                heroNamespace: heroNamespace,
                                                isHeroSource: selectedPhotoForHero?.id != photo.id,
                                                onSelect: { _ in
                                                    withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) {
                                                        selectedPhotoForHero = photo
                                                    }
                                                },
                                                onUserSelect: { username in
                                                    activePath.wrappedValue.append(FeedPathItem(id: username, type: .user))
                                                })
                                        }
                                    }
                                }
                            }
                            .padding(.horizontal, 8)
                            .padding(.top, 4)

                            // Bottom Loading Indicator
                            if viewModel.isLoading {
                                ProgressView()
                                    .tint(.white)
                                    .padding(.vertical, 16)
                            }
                        }
                        .padding(.bottom, 80)
                    }
                    #if os(iOS)
                    .modifier(FeedScrollTrackingModifier { offset in
                        coordinator?.handleScroll(offset: offset)
                    })
                    #endif
                    .refreshable {
                        viewModel.refresh()
                    }
                    .safeAreaInset(edge: .top, spacing: 0) {
                        categoryBar
                    }
                }

                if isShaking {
                    Color.black.opacity(0.4)
                        .ignoresSafeArea()
                        .overlay {
                            VStack(spacing: 16) {
                                ProgressView()
                                    .tint(.white)
                                    .controlSize(.large)
                                Text("🎲 Shaking up a random photo…")
                                    .font(.headline)
                                    .glassVibrancy(.primary)
                            }
                        }
                }

                if let photo = selectedPhotoForHero {
                    PhotoDetailsView(
                        photoId: photo.id,
                        heroNamespace: heroNamespace,
                        onDismiss: {
                            withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) {
                                selectedPhotoForHero = nil
                            }
                        },
                        onUserSelect: { username in
                            withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) {
                                selectedPhotoForHero = nil
                            }
                            activePath.wrappedValue.append(FeedPathItem(id: username, type: .user))
                        },
                        onTagSelect: { tag in
                            withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) {
                                selectedPhotoForHero = nil
                            }
                            activePath.wrappedValue.append(FeedPathItem(id: tag, type: .search))
                        })
                        .transition(.asymmetric(insertion: .identity, removal: .identity))
                        .zIndex(1)
                }
            }
            .navigationTitle("FEED")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbar(selectedPhotoForHero != nil ? .hidden : .visible, for: .navigationBar)
            .toolbar(.hidden, for: .tabBar)
            #endif
            .toolbar {
                #if os(iOS)
                ToolbarItem(placement: .navigationBarLeading) {
                    GlassToolbarButton(systemName: "magnifyingglass", accessibilityLabel: "Search") {
                        withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) {
                            selectedPhotoForHero = nil
                        }
                        activePath.wrappedValue.append(FeedPathItem(id: "", type: .search))
                    }
                    .keyboardShortcut("f", modifiers: .command)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    GlassToolbarButton(systemName: "shuffle", accessibilityLabel: "Shuffle") {
                        handleShake()
                    }
                    .keyboardShortcut("s", modifiers: .command)
                }
                #else
                ToolbarItem(placement: .navigation) {
                    GlassToolbarButton(systemName: "magnifyingglass", accessibilityLabel: "Search") {
                        withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) {
                            selectedPhotoForHero = nil
                        }
                        activePath.wrappedValue.append(FeedPathItem(id: "", type: .search))
                    }
                    .keyboardShortcut("f", modifiers: .command)
                }
                ToolbarItem(placement: .primaryAction) {
                    GlassToolbarButton(systemName: "shuffle", accessibilityLabel: "Shuffle") {
                        handleShake()
                    }
                    .keyboardShortcut("s", modifiers: .command)
                }
                #endif
            }
            .sensoryFeedback(.selection, trigger: viewModel.selectedTopicSlug)
            .sensoryFeedback(.impact(weight: .medium), trigger: shakeFeedbackTrigger)
            .onChange(of: selectedPhotoForHero) { _, newValue in
                activeDetailPresented.wrappedValue = (newValue != nil)
            }
            .onChange(of: activePath.wrappedValue.count) { _, count in
                if count > 0, selectedPhotoForHero != nil {
                    withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) {
                        selectedPhotoForHero = nil
                    }
                }
            }
            .navigationDestination(for: FeedPathItem.self) { item in
                Group {
                    switch item.type {
                    case .photo:
                        PhotoDetailsView(
                            photoId: item.id,
                            heroNamespace: heroNamespace,
                            onUserSelect: { username in
                                activePath.wrappedValue.append(FeedPathItem(id: username, type: .user))
                            },
                            onTagSelect: { tag in
                                activePath.wrappedValue.append(FeedPathItem(id: tag, type: .search))
                            })
                    case .user:
                        UserProfileView(
                            username: item.id,
                            onPhotoSelect: { photoId in
                                activePath.wrappedValue.append(FeedPathItem(id: photoId, type: .photo))
                            },
                            onCollectionSelect: { colId in
                                activePath.wrappedValue.append(FeedPathItem(id: colId, type: .collection))
                            })
                    case .collection:
                        CollectionDetailView(
                            collectionId: item.id,
                            onPhotoSelect: { photoId in
                                activePath.wrappedValue.append(FeedPathItem(id: photoId, type: .photo))
                            },
                            onCollectionSelect: { colId in
                                activePath.wrappedValue.append(FeedPathItem(id: colId, type: .collection))
                            },
                            onUserSelect: { username in
                                activePath.wrappedValue.append(FeedPathItem(id: username, type: .user))
                            })
                    case .search:
                        SearchView(
                            initialQuery: item.id,
                            onPhotoSelect: { photoId in
                                activePath.wrappedValue.append(FeedPathItem(id: photoId, type: .photo))
                            },
                            onUserSelect: { username in
                                activePath.wrappedValue.append(FeedPathItem(id: username, type: .user))
                            },
                            onCollectionSelect: { colId in
                                activePath.wrappedValue.append(FeedPathItem(id: colId, type: .collection))
                            })
                    }
                }
                #if os(iOS)
                .toolbar(.hidden, for: .tabBar)
                #endif
            }
            .onShake {
                handleShake()
            }
            .onDisappear {
                #if os(iOS)
                coordinator?.expand()
                #endif
            }
        }
    }

    private var categoryBar: some View {
        FeedCategoryBar(
            topics: viewModel.topics,
            selectedTopicSlug: viewModel.selectedTopicSlug,
            isLoadingTopics: viewModel.isLoadingTopics,
            namespace: categoryNamespace)
        { slug in
            withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                viewModel.selectTopic(slug: slug)
            }
        }
    }

    private func handleShake() {
        guard !isShaking else { return }
        shakeFeedbackTrigger += 1
        isShaking = true
        withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) {
            selectedPhotoForHero = nil
        }
        viewModel.fetchRandomPhotoId { photoId in
            isShaking = false
            if let photoId {
                activePath.wrappedValue.append(FeedPathItem(id: photoId, type: .photo))
            }
        }
    }
}

struct FeedCategoryBar: View {
    let topics: [Topic]
    let selectedTopicSlug: String
    let isLoadingTopics: Bool
    let namespace: Namespace.ID
    let onSelect: (String) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(spacing: 8) {
                CategoryTabButton(
                    title: "Editorial",
                    isSelected: selectedTopicSlug == "editorial",
                    namespace: namespace)
                {
                    onSelect("editorial")
                }

                if topics.isEmpty, isLoadingTopics {
                    ForEach(0..<6, id: \.self) { _ in
                        Capsule()
                            .fill(Color.white.opacity(0.08))
                            .frame(width: 80, height: 32)
                            .opacity(0.5)
                    }
                } else {
                    ForEach(topics, id: \.slug) { topic in
                        CategoryTabButton(
                            title: topic.title,
                            isSelected: selectedTopicSlug == topic.slug,
                            namespace: namespace)
                        {
                            onSelect(topic.slug)
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 5)
        }
        .frame(height: 44)
        .mask {
            LinearGradient(
                stops: [
                    .init(color: .clear, location: 0),
                    .init(color: .black, location: 0.04),
                    .init(color: .black, location: 0.96),
                    .init(color: .clear, location: 1.0),
                ],
                startPoint: .leading,
                endPoint: .trailing)
        }
        .glassBackground(style: .ultraThin, shape: Capsule(), showBorder: true, hasShadow: true)
        .padding(.horizontal, 12)
        .padding(.top, 4)
        .padding(.bottom, 6)
    }
}

struct CategoryTabButton: View {
    let title: String
    let isSelected: Bool
    let namespace: Namespace.ID
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(LocalizedStringKey(title))
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(isSelected ? Color.black : Color.secondary)
                .padding(.horizontal, 14)
                .padding(.vertical, 6)
                .background {
                    if isSelected {
                        Capsule()
                            .fill(Color.white)
                            .matchedGeometryEffect(id: "activeCategoryCapsule", in: namespace)
                    } else {
                        Capsule()
                            .fill(Color.white.opacity(0.08))
                    }
                }
        }
        .buttonStyle(.plain)
    }
}

struct PhotoCard: View {
    #if os(iOS)
    @Environment(\.horizontalSizeClass) var sizeClass
    private var columnCount: Int {
        AdaptiveLayoutHelper.getColumnCount(sizeClass: sizeClass)
    }
    #else
    private var columnCount: Int {
        AdaptiveLayoutHelper.getColumnCount()
    }
    #endif

    let photo: Photo
    let viewModel: FeedViewModel
    let heroNamespace: Namespace.ID
    var isHeroSource: Bool = true
    let onSelect: (String) -> Void
    let onUserSelect: (String) -> Void

    var body: some View {
        let screenWidth = AdaptiveLayoutHelper.getScreenWidth()
        let itemWidth = AdaptiveLayoutHelper.calculateItemWidthPx(screenWidth: screenWidth, columnCount: columnCount)

        // Dynamically resize image requesting only the resolution required by container
        let imageUrl = photo.urls.getOptimalUrl(targetWidthPx: Int32(itemWidth))
        let aspectRatio = CGFloat(photo.width) / CGFloat(photo.height)

        ZStack(alignment: .bottomLeading) {
            KFImage(URL(string: imageUrl))
                .placeholder {
                    // Display source-provided average hex color
                    ZStack {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color(hex: photo.color ?? "1E1E24"))
                        ProgressView()
                            .tint(.primary.opacity(0.6))
                    }
                    .aspectRatio(aspectRatio, contentMode: .fit)
                }
                .fade(duration: 0.25)
                .resizable()
                .aspectRatio(contentRatio(photoWidth: Int(photo.width), photoHeight: Int(photo.height)), contentMode: .fit)
                .matchedGeometryEffect(id: "photo-img-\(photo.id)", in: heroNamespace, isSource: isHeroSource)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .contentShape(Rectangle())
                .onTapGesture {
                    onSelect(photo.id)
                }

            // Inset Floating Glass Attribution Capsule
            Button {
                onUserSelect(photo.user.username)
            } label: {
                HStack(spacing: 6) {
                    KFImage(URL(string: photo.user.profileImage.small))
                        .resizable()
                        .scaledToFill()
                        .frame(width: 20, height: 20)
                        .clipShape(Circle())

                    Text(photo.user.name)
                        .font(.caption2.weight(.semibold))
                        .glassVibrancy(.primary)
                        .lineLimit(1)
                        .truncationMode(.tail)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 5)
                .glassCapsule(style: .lightweight, showBorder: true, hasShadow: false)
                .environment(\.colorScheme, .dark)
            }
            .buttonStyle(.plain)
            .contentShape(Capsule())
            .padding(8)
        }
        .feedScrollTransition()
        .onAppear {
            // Infinite pagination load trigger
            if photo.id == viewModel.photos.last?.id {
                viewModel.loadNextPage()
            }
        }
    }

    private func contentRatio(photoWidth: Int, photoHeight: Int) -> CGFloat {
        if photoWidth <= 0 || photoHeight <= 0 {
            return 1.0
        }
        return CGFloat(photoWidth) / CGFloat(photoHeight)
    }
}

struct ErrorView: View {
    let error: String
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Text("Error Loading Feed")
                .font(.headline)
                .glassVibrancy(.primary)

            Text(error)
                .font(.subheadline)
                .glassVibrancy(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            Button(action: onRetry) {
                Text("Retry")
                    .font(.subheadline.weight(.semibold))
                    .glassVibrancy(.primary)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 10)
                    .glassBackground(style: .thin, shape: Capsule(), showBorder: true)
            }
        }
    }
}

/// Color Hex parsing extensions
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 30, 30, 36)
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255)
    }
}

#if os(iOS)
/// Shake gesture detection support
extension Notification.Name {
    static let deviceDidShake = Notification.Name("MyDeviceDidShakeNotification")
}

extension UIWindow {
    override open func motionEnded(_ motion: UIEvent.EventSubtype, with event: UIEvent?) {
        if motion == .motionShake {
            NotificationCenter.default.post(name: .deviceDidShake, object: nil)
        }
    }
}

struct DeviceShakeViewModifier: ViewModifier {
    let action: () -> Void

    func body(content: Content) -> some View {
        content
            .onAppear()
            .onReceive(NotificationCenter.default.publisher(for: .deviceDidShake)) { _ in
                action()
            }
    }
}

extension View {
    func onShake(perform action: @escaping () -> Void) -> some View {
        modifier(DeviceShakeViewModifier(action: action))
    }
}
#else
extension View {
    func onShake(perform action: @escaping () -> Void) -> some View {
        self // No-op on macOS
    }
}
#endif
