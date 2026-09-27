import shared
import SwiftUI

struct ContentView: View {
    @State private var selectedTab = 0

    init() {
        #if os(iOS)
        let appearance = UITabBarAppearance()
        appearance.configureWithDefaultBackground()
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
        #endif
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            PhotosFeedTabView()
                .tabItem {
                    Label("Photos", systemImage: "photo.stack")
                }
                .tag(0)

            CollectionsFeedTabView()
                .tabItem {
                    Label("Collections", systemImage: "square.grid.2x2")
                }
                .tag(1)
        }
        .tint(.white)
        #if os(iOS)
        .toolbarBackground(.ultraThinMaterial, for: .tabBar)
        .toolbarBackground(.visible, for: .tabBar)
        .toolbarColorScheme(.dark, for: .tabBar)
        #endif
    }
}

struct CollectionsFeedTabView: View {
    @State private var path = NavigationPath()
    @Namespace private var dummyNamespace

    var body: some View {
        NavigationStack(path: $path) {
            CollectionsFeedView(
                onCollectionSelect: { collectionId in
                    path.append(CollectionPathItem(id: collectionId, type: .collection))
                },
                onSearchClick: {
                    path.append(CollectionPathItem(id: "", type: .search))
                })
                .navigationDestination(for: CollectionPathItem.self) { item in
                    switch item.type {
                    case .collection:
                        CollectionDetailView(
                            collectionId: item.id,
                            onPhotoSelect: { photoId in
                                path.append(CollectionPathItem(id: photoId, type: .photo))
                            },
                            onCollectionSelect: { relId in
                                path.append(CollectionPathItem(id: relId, type: .collection))
                            })
                    case .photo:
                        PhotoDetailsView(
                            photoId: item.id,
                            heroNamespace: dummyNamespace,
                            onUserSelect: { username in
                                path.append(CollectionPathItem(id: username, type: .user))
                            },
                            onTagSelect: { tag in
                                path.append(CollectionPathItem(id: tag, type: .search))
                            })
                    case .user:
                        UserProfileView(
                            username: item.id,
                            onPhotoSelect: { photoId in
                                path.append(CollectionPathItem(id: photoId, type: .photo))
                            },
                            onCollectionSelect: { colId in
                                path.append(CollectionPathItem(id: colId, type: .collection))
                            })
                    case .search:
                        SearchView(
                            initialQuery: item.id,
                            onPhotoSelect: { photoId in
                                path.append(CollectionPathItem(id: photoId, type: .photo))
                            },
                            onUserSelect: { username in
                                path.append(CollectionPathItem(id: username, type: .user))
                            })
                    }
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

struct PhotosFeedTabView: View {
    #if os(iOS)
    @Environment(\.horizontalSizeClass) var sizeClass
    #endif
    @State private var viewModel = FeedViewModel()
    @State private var path = NavigationPath()
    @State private var isShaking = false
    @Namespace private var categoryNamespace
    @Namespace private var heroNamespace
    @State private var selectedPhotoForHero: Photo? = nil

    private var columnCount: Int {
        #if os(iOS)
        return AdaptiveLayoutHelper.getColumnCount(sizeClass: sizeClass)
        #else
        return AdaptiveLayoutHelper.getColumnCount()
        #endif
    }

    var body: some View {
        NavigationStack(path: $path) {
            ZStack {
                Color(hex: "0F0F11")
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
                        ErrorView(error: viewModel.error ?? "Unknown error", onRetry: {
                            viewModel.refresh()
                        })
                        Spacer()
                    }
                } else {
                    ScrollView {
                        LazyVStack(spacing: 8) {
                            HStack(alignment: .top, spacing: 8) {
                                ForEach(0..<columnCount, id: \.self) { colIndex in
                                    LazyVStack(spacing: 8) {
                                        ForEach(
                                            AdaptiveLayoutHelper.photosForColumn(index: colIndex, totalColumns: columnCount, from: viewModel.photos),
                                            id: \.id)
                                        { photo in
                                            PhotoCard(
                                                photo: photo,
                                                viewModel: viewModel,
                                                heroNamespace: heroNamespace,
                                                onSelect: { photoId in
                                                    withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) {
                                                        selectedPhotoForHero = photo
                                                    }
                                                },
                                                onUserSelect: { username in
                                                    path.append(FeedPathItem(id: username, type: .user))
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
                    }
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
                                Text("🎲 Shaking up a random photo...")
                                    .foregroundColor(.white)
                                    .font(.headline)
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
                            path.append(FeedPathItem(id: username, type: .user))
                        },
                        onTagSelect: { tag in
                            withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) {
                                selectedPhotoForHero = nil
                            }
                            path.append(FeedPathItem(id: tag, type: .search))
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
                .toolbarColorScheme(.dark, for: .navigationBar)
                .toolbar(selectedPhotoForHero != nil ? .hidden : .visible, for: .navigationBar)
                .toolbar(selectedPhotoForHero != nil ? .hidden : .automatic, for: .tabBar)
            #endif
                .toolbar {
                    #if os(iOS)
                    ToolbarItem(placement: .navigationBarLeading) {
                        GlassToolbarButton(systemName: "magnifyingglass", accessibilityLabel: "Search") {
                            path.append(FeedPathItem(id: "", type: .search))
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
                            path.append(FeedPathItem(id: "", type: .search))
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
                .navigationDestination(for: FeedPathItem.self) { item in
                    switch item.type {
                    case .photo:
                        PhotoDetailsView(
                            photoId: item.id,
                            heroNamespace: heroNamespace,
                            onUserSelect: { username in
                                path.append(FeedPathItem(id: username, type: .user))
                            },
                            onTagSelect: { tag in
                                path.append(FeedPathItem(id: tag, type: .search))
                            })
                    case .user:
                        UserProfileView(
                            username: item.id,
                            onPhotoSelect: { photoId in
                                path.append(FeedPathItem(id: photoId, type: .photo))
                            },
                            onCollectionSelect: { colId in
                                path.append(FeedPathItem(id: colId, type: .collection))
                            })
                    case .collection:
                        CollectionDetailView(
                            collectionId: item.id,
                            onPhotoSelect: { photoId in
                                path.append(FeedPathItem(id: photoId, type: .photo))
                            },
                            onCollectionSelect: { colId in
                                path.append(FeedPathItem(id: colId, type: .collection))
                            })
                    case .search:
                        SearchView(
                            initialQuery: item.id,
                            onPhotoSelect: { photoId in
                                path.append(FeedPathItem(id: photoId, type: .photo))
                            },
                            onUserSelect: { username in
                                path.append(FeedPathItem(id: username, type: .user))
                            })
                    }
                }
                .onShake {
                    handleShake()
                }
        }
    }

    private var categoryBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(spacing: 8) {
                CategoryTabButton(
                    title: "Editorial",
                    isSelected: viewModel.selectedTopicSlug == "editorial",
                    namespace: categoryNamespace
                ) {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                        viewModel.selectTopic(slug: "editorial")
                    }
                }

                if viewModel.topics.isEmpty, viewModel.isLoadingTopics {
                    ForEach(0..<6, id: \.self) { _ in
                        Capsule()
                            .fill(Color.white.opacity(0.08))
                            .frame(width: 80, height: 32)
                            .opacity(0.5)
                    }
                } else {
                    ForEach(viewModel.topics, id: \.slug) { topic in
                        CategoryTabButton(
                            title: topic.title,
                            isSelected: viewModel.selectedTopicSlug == topic.slug,
                            namespace: categoryNamespace
                        ) {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                                viewModel.selectTopic(slug: topic.slug)
                            }
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
                    .init(color: .clear, location: 1.0)
                ],
                startPoint: .leading,
                endPoint: .trailing
            )
        }
        .glassBackground(style: .ultraThin, shape: Capsule(), showBorder: true, hasShadow: true)
        .padding(.horizontal, 12)
        .padding(.top, 4)
        .padding(.bottom, 6)
    }

    private func handleShake() {
        guard !isShaking else { return }
        isShaking = true
        viewModel.fetchRandomPhotoId { photoId in
            isShaking = false
            path.append(FeedPathItem(id: photoId, type: .photo))
        }
    }
}

struct CategoryTabButton: View {
    let title: String
    let isSelected: Bool
    let namespace: Namespace.ID
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(isSelected ? .black : nil)
                .glassVibrancy(isSelected ? .primary : .secondary)
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
    let photo: Photo
    let viewModel: FeedViewModel
    let heroNamespace: Namespace.ID
    let onSelect: (String) -> Void
    let onUserSelect: (String) -> Void

    var body: some View {
        let screenWidth = AdaptiveLayoutHelper.getScreenWidth()
        let itemWidth = Int(screenWidth / 2)

        // Dynamically resize image requesting only the resolution required by container
        let imageUrl = photo.urls.raw + "&w=\(itemWidth)&q=80&auto=format"
        let aspectRatio = CGFloat(photo.width) / CGFloat(photo.height)

        ZStack(alignment: .bottomLeading) {
            KFImage(URL(string: imageUrl))
                .placeholder {
                    // Display source-provided average hex color with frosted glass depth
                    ZStack {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color(hex: photo.color ?? "1E1E24"))
                        RoundedRectangle(cornerRadius: 12)
                            .fill(.ultraThinMaterial)
                        ProgressView()
                            .tint(.primary.opacity(0.6))
                    }
                    .aspectRatio(aspectRatio, contentMode: .fit)
                }
                .fade(duration: 0.25)
                .resizable()
                .aspectRatio(contentRatio(photoWidth: Int(photo.width), photoHeight: Int(photo.height)), contentMode: .fit)
                .matchedGeometryEffect(id: "photo-img-\(photo.id)", in: heroNamespace)
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
                .glassCapsule(style: .ultraThin, showBorder: true, hasShadow: true)
            }
            .buttonStyle(.plain)
            .contentShape(Capsule())
            .padding(8)
        }
        .onAppear {
            // Infinite pagination load trigger
            if photo.id == viewModel.photos.last?.id {
                viewModel.loadNextPage()
            }
        }
    }

    private func contentRatio(photoWidth: Int, photoHeight: Int) -> CGFloat {
        if photoWidth <= 0 || photoHeight <= 0 { return 1.0 }
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
                .foregroundColor(.white)

            Text(error)
                .font(.subheadline)
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            Button(action: onRetry) {
                Text("Retry")
                    .fontWeight(.medium)
                    .foregroundColor(.black)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 8)
                    .background(Color.white)
                    .cornerRadius(20)
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
