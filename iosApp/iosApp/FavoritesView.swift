import shared
import SwiftUI

struct FavoritesTabView: View {
    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var sizeClass
    #endif
    @State private var containerWidth: CGFloat = 0
    @State private var viewModel: FavoritesViewModel?

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

    @Namespace private var heroNamespace
    @State private var selectedPhotoForHero: Photo?
    @ScaledMetric(relativeTo: .largeTitle) private var emptyIconSize: CGFloat = 40

    init(
        path: Binding<NavigationPath>? = nil,
        isDetailPresented: Binding<Bool>? = nil,
        coordinator: CapsuleScrollCoordinator? = nil,
        initialViewModel: FavoritesViewModel? = nil)
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
                    if viewModel.isLoading, viewModel.photos.isEmpty {
                        ProgressView()
                            .tint(.primary)
                    } else if viewModel.photos.isEmpty {
                        VStack(spacing: 14) {
                            Image(systemName: "bookmark")
                                .font(.system(size: emptyIconSize, weight: .medium))
                                .glassVibrancy(.secondary)
                            Text("No saved photos yet")
                                .font(.headline)
                                .glassVibrancy(.primary)
                            Text("Tap the bookmark button on any photo to save it locally on this device.")
                                .font(.subheadline)
                                .glassVibrancy(.secondary)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 32)
                        }
                        .padding(24)
                    } else {
                        ScrollView {
                            LazyVStack(spacing: 8) {
                                #if os(iOS)
                                if #unavailable(iOS 18.0) {
                                    GeometryReader { proxy in
                                        let rawY = proxy.frame(in: .named("favoritesFeedScroll")).minY
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
                                                    contextMenu: {
                                                        Button(role: .destructive) {
                                                            viewModel.removeFavorite(photoId: photo.id)
                                                        } label: {
                                                            Label("Remove Bookmark", systemImage: "bookmark.slash")
                                                        }
                                                    })
                                            }
                                        }
                                    }
                                }
                                .padding(.horizontal, 8)
                                .padding(.top, 8)
                            }
                            .padding(.bottom, 80)
                        }
                        .satinScrollEdge()
                        #if os(iOS)
                        .modifier(FeedScrollTrackingModifier(coordinateSpaceName: "favoritesFeedScroll") { offset in
                            coordinator?.handleScroll(offset: offset)
                        })
                        #endif
                    }

                    if !isLegacyHeroOverlayActive, let banner = viewModel.toastBanner {
                        VStack {
                            GlassToastPill(banner: banner)
                                .padding(.top, 12)
                            Spacer()
                        }
                        .transition(.move(edge: .top).combined(with: .opacity))
                        .zIndex(0.5)
                    }
                } else {
                    ProgressView()
                        .tint(.primary)
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
            .navigationTitle("FAVORITES")
            .measureContainerWidth($containerWidth)
            .task {
                if viewModel == nil {
                    viewModel = FavoritesViewModel()
                }
            }
            #if os(iOS)
            .satinNavigationBar()
            .toolbar(isLegacyHeroOverlayActive ? .hidden : .visible, for: .navigationBar)
            .adaptiveTabBarVisibility(hideOnModernOS: isLegacyHeroOverlayActive)
            #endif
            .modifier(FavoritesLegacyHeroSyncModifier(
                selectedPhoto: $selectedPhotoForHero,
                activeDetailPresented: activeDetailPresented,
                pathCount: activePath.wrappedValue.count))
            .appNavigationDestinations(path: activePath, heroNamespace: heroNamespace)
            .onDisappear {
                #if os(iOS)
                coordinator?.expand()
                #endif
            }
        }
    }
}

private struct FavoritesLegacyHeroSyncModifier: ViewModifier {
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
