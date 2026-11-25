import shared
import SwiftUI

struct CollectionDetailView: View {
    let collectionId: String
    @State private var viewModel: CollectionDetailViewModel?
    @Environment(\.dismiss) private var dismiss
    #if os(iOS)
    @Environment(\.horizontalSizeClass) var sizeClass
    #endif
    @State private var containerWidth: CGFloat = 0

    let onPhotoSelect: (String) -> Void
    let onCollectionSelect: (String) -> Void
    let onUserSelect: (String) -> Void

    init(
        collectionId: String,
        onPhotoSelect: @escaping (String) -> Void,
        onCollectionSelect: @escaping (String) -> Void,
        onUserSelect: @escaping (String) -> Void)
    {
        self.collectionId = collectionId
        self._viewModel = State(initialValue: nil)
        self.onPhotoSelect = onPhotoSelect
        self.onCollectionSelect = onCollectionSelect
        self.onUserSelect = onUserSelect
    }

    private var columnCount: Int {
        #if os(iOS)
        AdaptiveLayoutHelper.getColumnCount(containerWidth: containerWidth, sizeClass: sizeClass)
        #else
        AdaptiveLayoutHelper.getColumnCount(containerWidth: containerWidth)
        #endif
    }

    var body: some View {
        ZStack(alignment: .top) {
            GlassTheme.canvasBackgroundColor
                .ignoresSafeArea()

            if let viewModel, viewModel.collection != nil {
                ScrollView {
                    VStack(spacing: 0) {
                        // Parallax Scaling Header
                        GeometryReader { geo in
                            let scrollOffset = geo.frame(in: .global).minY
                            let headerHeight: CGFloat = 300
                            let stretchedHeight = headerHeight + (scrollOffset > 0 ? scrollOffset : 0)

                            ZStack(alignment: .bottom) {
                                // Background cover photo with stretch
                                if let coverPhoto = viewModel.collection?.coverPhoto {
                                    KFImage(URL(string: coverPhoto.urls.small))
                                        .resizable()
                                        .scaledToFill()
                                        .frame(width: geo.size.width, height: stretchedHeight)
                                        .blur(radius: 12)
                                        .clipped()
                                        .offset(y: scrollOffset > 0 ? -scrollOffset : 0)
                                } else {
                                    GlassTheme.surfaceBackgroundColor
                                        .frame(width: geo.size.width, height: stretchedHeight)
                                }

                                // Dark overlay gradient for readability
                                LinearGradient(
                                    colors: [.clear, .black.opacity(0.4), .black.opacity(0.85), GlassTheme.canvasBackgroundColor],
                                    startPoint: .top,
                                    endPoint: .bottom)
                                    .frame(width: geo.size.width, height: stretchedHeight)
                                    .offset(y: scrollOffset > 0 ? -scrollOffset : 0)

                                // Information details
                                if let collection = viewModel.collection {
                                    VStack(alignment: .leading, spacing: 6) {
                                        // Curator info
                                        HStack(spacing: 8) {
                                            KFImage(URL(string: collection.user.profileImage.medium))
                                                .resizable()
                                                .scaledToFill()
                                                .frame(width: 32, height: 32)
                                                .clipShape(Circle())

                                            VStack(alignment: .leading, spacing: 1) {
                                                Text("Curated by")
                                                    .font(.caption2)
                                                    .glassVibrancy(.secondary)
                                                Text(collection.user.name)
                                                    .font(.subheadline.weight(.bold))
                                                    .glassVibrancy(.primary)
                                            }
                                        }
                                        .padding(.bottom, 6)
                                        .contentShape(Rectangle())
                                        .onTapGesture {
                                            onUserSelect(collection.user.username)
                                        }

                                        Text(collection.title)
                                            .font(.title2.weight(.bold))
                                            .glassVibrancy(.primary)

                                        Text("\(collection.totalPhotos) Photos")
                                            .font(.subheadline)
                                            .glassVibrancy(.secondary)

                                        if let desc = collection.description_, !desc.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                                            Text(desc)
                                                .font(.footnote)
                                                .glassVibrancy(.primary)
                                                .opacity(0.85)
                                                .lineLimit(3)
                                                .lineSpacing(3)
                                                .padding(.top, 4)
                                        }
                                    }
                                    .padding(.horizontal, 16)
                                    .padding(.bottom, 16)
                                    .offset(y: scrollOffset > 0 ? -scrollOffset : 0)
                                }
                            }
                        }
                        .frame(height: 300)

                        // Related Collections Carousel
                        if !viewModel.related.isEmpty {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("Related Collections")
                                    .font(.headline)
                                    .glassVibrancy(.primary)
                                    .padding(.horizontal, 16)
                                    .padding(.top, 16)

                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 12) {
                                        ForEach(viewModel.related, id: \.id) { rel in
                                            RelatedCollectionCard(collection: rel) {
                                                onCollectionSelect(rel.id)
                                            }
                                        }
                                    }
                                    .padding(.horizontal, 16)
                                }
                            }
                        }

                        // Collection Photos Grid
                        if !viewModel.photos.isEmpty {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Photos")
                                    .font(.headline)
                                    .glassVibrancy(.primary)
                                    .padding(.horizontal, 16)
                                    .padding(.top, 24)

                                HStack(alignment: .top, spacing: 8) {
                                    ForEach(0..<columnCount, id: \.self) { colIndex in
                                        LazyVStack(spacing: 8) {
                                            ForEach(viewModel.columnItems(for: colIndex, totalColumns: columnCount)) { indexed in
                                                let photo = indexed.item
                                                let flatIndex = indexed.index
                                                PhotoGridCard(
                                                    photo: photo,
                                                    columnCount: columnCount,
                                                    onSelect: { onPhotoSelect(photo.id) },
                                                    onUserSelect: onUserSelect,
                                                    onAppear: {
                                                        if photo.id == viewModel.photos.last?.id {
                                                            viewModel.loadNextPhotosPage()
                                                        }
                                                    })
                                                    .staggeredReveal(index: flatIndex)
                                            }
                                        }
                                    }
                                }
                                .padding(.horizontal, 8)
                            }
                        }

                        if viewModel.isLoadingPhotos {
                            ProgressView()
                                .tint(.primary)
                                .padding(.vertical, 24)
                        }
                    }
                }
                .satinScrollEdge()
                .ignoresSafeArea(edges: .top)
            } else {
                skeletonView
            }
        }
        .measureContainerWidth($containerWidth)
        .task(id: collectionId) {
            if viewModel == nil {
                viewModel = CollectionDetailViewModel(collectionId: collectionId)
            }
        }
        .navigationTitle(viewModel?.collection?.title ?? String(localized: "Collection"))
        .navigationBarBackButtonHidden(true)
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
        #endif
        .toolbar {
            #if os(iOS)
            ToolbarItem(placement: .navigationBarLeading) {
                PlatformToolbarButton(systemName: "chevron.backward", accessibilityLabel: "Back") {
                    dismiss()
                }
            }
            #else
            ToolbarItem(placement: .navigation) {
                PlatformToolbarButton(systemName: "chevron.backward", accessibilityLabel: "Back") {
                    dismiss()
                }
            }
            #endif
        }
    }

    private var skeletonView: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Header skeleton
                RoundedRectangle(cornerRadius: 12)
                    .fill(GlassTheme.surfaceBackgroundColor)
                    .frame(height: 300)
                    .shimmer()

                // Related collections label skeleton
                RoundedRectangle(cornerRadius: 4)
                    .fill(GlassTheme.surfaceBackgroundColor)
                    .frame(width: 150, height: 20)
                    .padding(.horizontal, 16)
                    .shimmer()

                // Related collections placeholder
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(0..<3, id: \.self) { _ in
                            RoundedRectangle(cornerRadius: 12)
                                .fill(GlassTheme.surfaceBackgroundColor)
                                .frame(width: 160, height: 110)
                                .shimmer()
                        }
                    }
                    .padding(.horizontal, 16)
                }

                // Grid photos label skeleton
                RoundedRectangle(cornerRadius: 4)
                    .fill(GlassTheme.surfaceBackgroundColor)
                    .frame(width: 100, height: 20)
                    .padding(.horizontal, 16)
                    .shimmer()

                // Grid photos placeholder
                HStack(alignment: .top, spacing: 8) {
                    ForEach(0..<columnCount, id: \.self) { _ in
                        VStack(spacing: 8) {
                            ForEach(0..<3, id: \.self) { index in
                                PhotoCardSkeleton(height: index % 2 == 0 ? 180 : 240)
                            }
                        }
                    }
                }
                .padding(.horizontal, 8)
            }
        }
        .ignoresSafeArea(edges: .top)
    }
}

struct RelatedCollectionCard: View {
    let collection: PhotoCollection
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            ZStack(alignment: .bottomLeading) {
                if let coverPhoto = collection.coverPhoto {
                    KFImage(URL(string: coverPhoto.urls.small))
                        .resizable()
                        .scaledToFill()
                        .frame(width: 160, height: 110)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                } else {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(GlassTheme.surfaceBackgroundColor)
                        .frame(width: 160, height: 110)
                }

                LinearGradient(
                    colors: [.clear, .black.opacity(0.8)],
                    startPoint: .top,
                    endPoint: .bottom)
                    .clipShape(RoundedRectangle(cornerRadius: 12))

                VStack(alignment: .leading, spacing: 2) {
                    Text(collection.title)
                        .font(.caption.weight(.bold))
                        .glassVibrancy(.primary)
                        .lineLimit(1)
                    Text("\(collection.totalPhotos) Photos")
                        .font(.caption2)
                        .glassVibrancy(.secondary)
                }
                .environment(\.colorScheme, .dark)
                .padding(8)
            }
            .frame(width: 160, height: 110)
        }
        .buttonStyle(SpringCardButtonStyle())
    }
}
