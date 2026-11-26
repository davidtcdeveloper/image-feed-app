import shared
import SwiftUI

struct FavoritesPathItem: Hashable {
    let id: String
    let type: ItemType

    enum ItemType {
        case photo
        case user
        case collection
        case search
    }
}

struct FavoritesTabView: View {
    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var sizeClass
    #endif
    @State private var containerWidth: CGFloat = 0
    @State private var viewModel = FavoritesViewModel()

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
        coordinator: CapsuleScrollCoordinator? = nil)
    {
        self.externalPath = path
        self.externalDetailPresented = isDetailPresented
        self.coordinator = coordinator
    }

    private var columnCount: Int {
        #if os(iOS)
        AdaptiveLayoutHelper.getColumnCount(containerWidth: containerWidth, sizeClass: sizeClass)
        #else
        AdaptiveLayoutHelper.getColumnCount(containerWidth: containerWidth)
        #endif
    }

    var body: some View {
        NavigationStack(path: activePath) {
            ZStack {
                GlassTheme.canvasBackgroundColor
                    .ignoresSafeArea()

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
                                                    withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) {
                                                        selectedPhotoForHero = photo
                                                    }
                                                },
                                                onUserSelect: { username in
                                                    activePath.wrappedValue.append(FavoritesPathItem(id: username, type: .user))
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
                    #if os(iOS)
                    .modifier(FeedScrollTrackingModifier(coordinateSpaceName: "favoritesFeedScroll") { offset in
                        coordinator?.handleScroll(offset: offset)
                    })
                    #endif
                }

                if selectedPhotoForHero == nil, let banner = viewModel.toastBanner {
                    VStack {
                        GlassToastPill(banner: banner)
                            .padding(.top, 12)
                        Spacer()
                    }
                    .transition(.move(edge: .top).combined(with: .opacity))
                    .zIndex(0.5)
                }

                if let photo = selectedPhotoForHero {
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
                            activePath.wrappedValue.append(FavoritesPathItem(id: username, type: .user))
                        },
                        onTagSelect: { tag in
                            withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) {
                                selectedPhotoForHero = nil
                            }
                            activePath.wrappedValue.append(FavoritesPathItem(id: tag, type: .search))
                        })
                        .transition(.asymmetric(insertion: .identity, removal: .identity))
                        .zIndex(1)
                }
            }
            .navigationTitle("FAVORITES")
            .measureContainerWidth($containerWidth)
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbar(selectedPhotoForHero != nil ? .hidden : .visible, for: .navigationBar)
            .adaptiveTabBarVisibility(hideOnModernOS: selectedPhotoForHero != nil)
            #endif
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
            .navigationDestination(for: FavoritesPathItem.self) { item in
                Group {
                    switch item.type {
                    case .photo:
                        PhotoDetailsView(
                            photoId: item.id,
                            heroNamespace: heroNamespace,
                            onUserSelect: { username in
                                activePath.wrappedValue.append(FavoritesPathItem(id: username, type: .user))
                            },
                            onTagSelect: { tag in
                                activePath.wrappedValue.append(FavoritesPathItem(id: tag, type: .search))
                            })
                    case .user:
                        UserProfileView(
                            username: item.id,
                            onPhotoSelect: { photoId in
                                activePath.wrappedValue.append(FavoritesPathItem(id: photoId, type: .photo))
                            },
                            onCollectionSelect: { colId in
                                activePath.wrappedValue.append(FavoritesPathItem(id: colId, type: .collection))
                            })
                    case .collection:
                        CollectionDetailView(
                            collectionId: item.id,
                            onPhotoSelect: { photoId in
                                activePath.wrappedValue.append(FavoritesPathItem(id: photoId, type: .photo))
                            },
                            onCollectionSelect: { colId in
                                activePath.wrappedValue.append(FavoritesPathItem(id: colId, type: .collection))
                            },
                            onUserSelect: { username in
                                activePath.wrappedValue.append(FavoritesPathItem(id: username, type: .user))
                            })
                    case .search:
                        SearchView(
                            initialQuery: item.id,
                            onPhotoSelect: { photoId in
                                activePath.wrappedValue.append(FavoritesPathItem(id: photoId, type: .photo))
                            },
                            onUserSelect: { username in
                                activePath.wrappedValue.append(FavoritesPathItem(id: username, type: .user))
                            },
                            onCollectionSelect: { colId in
                                activePath.wrappedValue.append(FavoritesPathItem(id: colId, type: .collection))
                            })
                    }
                }
                #if os(iOS)
                .toolbar(.hidden, for: .tabBar)
                #endif
            }
            .onDisappear {
                #if os(iOS)
                coordinator?.expand()
                #endif
            }
        }
    }
}
