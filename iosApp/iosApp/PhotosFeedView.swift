import shared
import SwiftUI

struct FeedScrollOffsetKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

#if os(iOS)
struct FeedScrollTrackingModifier: ViewModifier {
    var coordinateSpaceName: String = "photoFeedScroll"
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
                .coordinateSpace(name: coordinateSpaceName)
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
    @State private var containerWidth: CGFloat = 0
    @State private var viewModel: FeedViewModel?

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
        coordinator: CapsuleScrollCoordinator? = nil,
        initialViewModel: FeedViewModel? = nil)
    {
        self.externalPath = path
        self.externalDetailPresented = isDetailPresented
        self.coordinator = coordinator
        self._viewModel = State(initialValue: initialViewModel)
    }

    private var columnCount: Int {
        #if os(iOS)
        AdaptiveLayoutHelper.getColumnCount(containerWidth: containerWidth, sizeClass: sizeClass)
        #else
        AdaptiveLayoutHelper.getColumnCount(containerWidth: containerWidth)
        #endif
    }

    private var isLegacyHeroOverlayActive: Bool {
        #if os(iOS)
        if #available(iOS 18.0, *) {
            false
        } else {
            selectedPhotoForHero != nil
        }
        #else
        false
        #endif
    }

    var body: some View {
        NavigationStack(path: activePath) {
            ZStack {
                GlassTheme.canvasBackgroundColor
                    .ignoresSafeArea()

                if let viewModel {
                    if viewModel.photos.isEmpty, viewModel.isLoading {
                        VStack(spacing: 0) {
                            categoryBar(viewModel: viewModel)
                            Spacer()
                            ProgressView()
                                .tint(.primary)
                            Spacer()
                        }
                    } else if viewModel.photos.isEmpty, viewModel.error != nil {
                        VStack(spacing: 0) {
                            categoryBar(viewModel: viewModel)
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
                                                PhotoGridCard(
                                                    photo: photo,
                                                    columnCount: columnCount,
                                                    heroNamespace: heroNamespace,
                                                    isHeroSource: selectedPhotoForHero?.id != photo.id,
                                                    onSelect: {
                                                        #if os(iOS)
                                                        if #available(iOS 18.0, *) {
                                                            AppNavigationDefaults.push(.photo(id: photo.id), onto: activePath)
                                                        } else {
                                                            withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) {
                                                                selectedPhotoForHero = photo
                                                            }
                                                        }
                                                        #else
                                                        AppNavigationDefaults.push(.photo(id: photo.id), onto: activePath)
                                                        #endif
                                                    },
                                                    onUserSelect: { username in
                                                        AppNavigationDefaults.push(.user(username: username), onto: activePath)
                                                    },
                                                    onAppear: {
                                                        if photo.id == viewModel.photos.last?.id {
                                                            viewModel.loadNextPage()
                                                        }
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
                                        .tint(.primary)
                                        .padding(.vertical, 16)
                                }
                            }
                            .padding(.bottom, 80)
                        }
                        .satinScrollEdge()
                        #if os(iOS)
                        .modifier(FeedScrollTrackingModifier { offset in
                            coordinator?.handleScroll(offset: offset)
                        })
                        #endif
                        .refreshable {
                            viewModel.refresh()
                        }
                        .safeAreaInset(edge: .top, spacing: 0) {
                            categoryBar(viewModel: viewModel)
                        }
                    }
                } else {
                    ProgressView()
                        .tint(.primary)
                }

                if isShaking {
                    Color.black.opacity(0.45)
                        .ignoresSafeArea()
                        .overlay {
                            VStack(spacing: 16) {
                                ProgressView()
                                    .tint(.primary)
                                    .controlSize(.large)
                                Text("🎲 Shaking up a random photo…")
                                    .font(.headline)
                                    .glassVibrancy(.primary)
                            }
                            .environment(\.colorScheme, .dark)
                        }
                }

                #if os(iOS)
                if #unavailable(iOS 18.0), let photo = selectedPhotoForHero {
                    PhotoDetailsView(
                        photoId: photo.id,
                        initialPhoto: photo,
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
                            AppNavigationDefaults.push(.user(username: username), onto: activePath)
                        },
                        onTagSelect: { tag in
                            withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) {
                                selectedPhotoForHero = nil
                            }
                            AppNavigationDefaults.push(.search(query: tag), onto: activePath)
                        })
                        .transition(.asymmetric(insertion: .identity, removal: .identity))
                        .zIndex(1)
                }
                #endif
            }
            .measureContainerWidth($containerWidth)
            .navigationTitle("FEED")
            #if os(iOS)
            .satinNavigationBar()
            .toolbar(isLegacyHeroOverlayActive ? .hidden : .visible, for: .navigationBar)
            .adaptiveTabBarVisibility(hideOnModernOS: isLegacyHeroOverlayActive)
            #endif
            .toolbar {
                #if os(iOS)
                ToolbarItem(placement: .navigationBarTrailing) {
                    PlatformToolbarButton(systemName: "shuffle", accessibilityLabel: "Shuffle") {
                        handleShake()
                    }
                    .symbolEffect(.bounce, value: shakeFeedbackTrigger)
                    .keyboardShortcut("s", modifiers: .command)
                }
                #else
                ToolbarItem(placement: .primaryAction) {
                    PlatformToolbarButton(systemName: "shuffle", accessibilityLabel: "Shuffle") {
                        handleShake()
                    }
                    .symbolEffect(.bounce, value: shakeFeedbackTrigger)
                    .keyboardShortcut("s", modifiers: .command)
                }
                #endif
            }
            .task {
                if viewModel == nil {
                    viewModel = FeedViewModel()
                }
            }
            .sensoryFeedback(.selection, trigger: viewModel?.selectedTopicSlug ?? "")
            .sensoryFeedback(.impact(weight: .medium), trigger: shakeFeedbackTrigger)
            .modifier(LegacyHeroOverlaySyncModifier(
                selectedPhoto: $selectedPhotoForHero,
                activeDetailPresented: activeDetailPresented,
                pathCount: activePath.wrappedValue.count))
            .appNavigationDestinations(path: activePath, heroNamespace: heroNamespace)
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

    private func categoryBar(viewModel: FeedViewModel) -> some View {
        FeedCategoryBar(
            topics: viewModel.topics,
            selectedTopicSlug: viewModel.selectedTopicSlug,
            isLoadingTopics: viewModel.isLoadingTopics,
            namespace: categoryNamespace,
            isScrollEnabled: !isLegacyHeroOverlayActive && !isShaking)
        { slug in
            withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                viewModel.selectTopic(slug: slug)
            }
        }
        .opacity(isLegacyHeroOverlayActive ? 0 : 1)
        .animation(.easeInOut(duration: 0.2), value: isLegacyHeroOverlayActive)
    }

    private func handleShake() {
        guard !isShaking, let viewModel else { return }
        shakeFeedbackTrigger += 1
        isShaking = true
        #if os(iOS)
        if #unavailable(iOS 18.0), selectedPhotoForHero != nil {
            withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) {
                selectedPhotoForHero = nil
            }
        }
        #endif
        viewModel.fetchRandomPhotoId { photoId in
            isShaking = false
            if let photoId {
                AppNavigationDefaults.push(.photo(id: photoId), onto: activePath)
            }
        }
    }
}

private struct LegacyHeroOverlaySyncModifier: ViewModifier {
    @Binding var selectedPhoto: Photo?
    let activeDetailPresented: Binding<Bool>
    let pathCount: Int

    func body(content: Content) -> some View {
        #if os(iOS)
        if #available(iOS 18.0, *) {
            content
        } else {
            content
                .onChange(of: selectedPhoto) { _, newValue in
                    activeDetailPresented.wrappedValue = (newValue != nil)
                }
                .onChange(of: pathCount) { _, count in
                    if count > 0, selectedPhoto != nil {
                        withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) {
                            selectedPhoto = nil
                        }
                    }
                }
        }
        #else
        content
        #endif
    }
}

struct FeedCategoryBar: View {
    let topics: [Topic]
    let selectedTopicSlug: String
    let isLoadingTopics: Bool
    let namespace: Namespace.ID
    var isScrollEnabled: Bool = true
    let onSelect: (String) -> Void

    @State private var scrollCoordinator = MacOSHorizontalScrollCoordinator()

    private var orderedSlugs: [String] {
        ["editorial"] + topics.map(\.slug)
    }

    var body: some View {
        ScrollViewReader { proxy in
            HStack(spacing: 0) {
                #if os(macOS)
                if !topics.isEmpty {
                    categoryStepButton(systemName: "chevron.backward", accessibilityLabel: "Previous categories") {
                        stepCategory(by: -3, proxy: proxy)
                    }
                    .padding(.leading, 8)
                }
                #endif

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        CategoryTabButton(title: "Editorial", isSelected: selectedTopicSlug == "editorial", namespace: namespace) {
                            onSelect("editorial")
                        }
                        .id("editorial")

                        if topics.isEmpty, isLoadingTopics {
                            ForEach(0..<6, id: \.self) { _ in
                                Capsule()
                                    .fill(GlassTheme.subtleFillColor)
                                    .frame(width: 80, height: 32)
                                    .opacity(0.6)
                            }
                        } else {
                            ForEach(topics, id: \.slug) { topic in
                                CategoryTabButton(title: topic.title, isSelected: selectedTopicSlug == topic.slug, namespace: namespace) {
                                    onSelect(topic.slug)
                                }
                                .id(topic.slug)
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 5)
                    .macOSHorizontalScroll(isEnabled: isScrollEnabled, coordinator: scrollCoordinator)
                }
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

                #if os(macOS)
                if !topics.isEmpty {
                    categoryStepButton(systemName: "chevron.forward", accessibilityLabel: "Next categories") {
                        stepCategory(by: 3, proxy: proxy)
                    }
                    .padding(.trailing, 8)
                }
                #endif
            }
            .frame(height: 44)
            .floatGlass(shape: Capsule())
            .padding(.horizontal, 12)
            .padding(.top, 4)
            .padding(.bottom, 6)
            .onChange(of: selectedTopicSlug) { _, newSlug in
                let slugs = orderedSlugs
                let maxIndex = max(0, slugs.count - 1)
                if let idx = slugs.firstIndex(of: newSlug) {
                    scrollCoordinator.stepIndex = idx
                    scrollCoordinator.scrollFraction = maxIndex > 0 ? CGFloat(idx) / CGFloat(maxIndex) : 0
                }
                let anchor: UnitPoint = if newSlug == "editorial" {
                    .leading
                } else if newSlug == slugs.last {
                    .trailing
                } else {
                    .center
                }
                withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                    proxy.scrollTo(newSlug, anchor: anchor)
                }
            }
        }
    }

    #if os(macOS)
    private func categoryStepButton(systemName: String, accessibilityLabel: LocalizedStringKey, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 12, weight: .bold))
                .glassVibrancy(.secondary)
                .frame(width: 28, height: 28)
                .background(Circle().fill(GlassTheme.subtleFillColor))
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(accessibilityLabel))
    }

    private func stepCategory(by delta: Int, proxy: ScrollViewProxy) {
        let slugs = orderedSlugs
        let maxIndex = max(0, slugs.count - 1)
        guard maxIndex > 0 else { return }

        let currentIndex = scrollCoordinator.stepIndex
            ?? Int(round(scrollCoordinator.currentFraction() * CGFloat(maxIndex)))
        let targetIndex = min(max(0, currentIndex + delta), maxIndex)
        scrollCoordinator.stepIndex = targetIndex
        scrollCoordinator.scrollFraction = CGFloat(targetIndex) / CGFloat(maxIndex)

        let targetSlug = slugs[targetIndex]
        let anchor: UnitPoint = if targetIndex == 0 {
            .leading
        } else if targetIndex == maxIndex {
            .trailing
        } else {
            .center
        }
        withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
            proxy.scrollTo(targetSlug, anchor: anchor)
        }
    }
    #endif
}

struct CategoryTabButton: View {
    let title: String
    let isSelected: Bool
    let namespace: Namespace.ID
    let action: () -> Void

    @Environment(\.colorSchemeContrast) private var colorSchemeContrast

    var body: some View {
        Button(action: action) {
            Text(LocalizedStringKey(title))
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(isSelected ? GlassTheme.activePillForegroundColor : Color.secondary)
                .padding(.horizontal, 14)
                .padding(.vertical, 6)
                .background {
                    if isSelected {
                        Capsule()
                            .fill(GlassTheme.activePillFillColor)
                            .matchedGeometryEffect(id: "activeCategoryCapsule", in: namespace)
                    } else {
                        Capsule()
                            .fill(GlassTheme.subtleFillColor)
                            .overlay {
                                if colorSchemeContrast == .increased {
                                    Capsule()
                                        .strokeBorder(GlassTheme.fallbackBorderColor, lineWidth: GlassTheme.fallbackBorderWidth)
                                }
                            }
                    }
                }
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)
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
