import shared
import SwiftUI

struct CollectionDetailView: View {
    let collectionId: String
    @State private var viewModel: CollectionDetailViewModel
    @Environment(\.dismiss) private var dismiss
    #if os(iOS)
    @Environment(\.horizontalSizeClass) var sizeClass
    #endif

    let onPhotoSelect: (String) -> Void
    let onCollectionSelect: (String) -> Void

    init(collectionId: String, onPhotoSelect: @escaping (String) -> Void, onCollectionSelect: @escaping (String) -> Void) {
        self.collectionId = collectionId
        self._viewModel = State(initialValue: CollectionDetailViewModel(collectionId: collectionId))
        self.onPhotoSelect = onPhotoSelect
        self.onCollectionSelect = onCollectionSelect
    }

    private var columnCount: Int {
        #if os(iOS)
        return AdaptiveLayoutHelper.getColumnCount(sizeClass: sizeClass)
        #else
        return AdaptiveLayoutHelper.getColumnCount()
        #endif
    }

    var body: some View {
        ZStack(alignment: .top) {
            Color(hex: "0F0F11")
                .ignoresSafeArea()

            if viewModel.photos.isEmpty, viewModel.isLoadingPhotos, viewModel.isHeaderLoading {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        // Header skeleton
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color.white.opacity(0.06))
                            .frame(height: 300)
                            .shimmer()

                        // Related collections label skeleton
                        RoundedRectangle(cornerRadius: 4)
                            .fill(Color.white.opacity(0.06))
                            .frame(width: 150, height: 20)
                            .padding(.horizontal, 16)
                            .shimmer()

                        // Related collections placeholder
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 12) {
                                ForEach(0..<3, id: \.self) { _ in
                                    RoundedRectangle(cornerRadius: 12)
                                        .fill(Color.white.opacity(0.06))
                                        .frame(width: 160, height: 110)
                                        .shimmer()
                                }
                            }
                            .padding(.horizontal, 16)
                        }

                        // Grid photos label skeleton
                        RoundedRectangle(cornerRadius: 4)
                            .fill(Color.white.opacity(0.06))
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
            } else {
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
                                    KFImage(URL(string: coverPhoto.urls.regular))
                                        .resizable()
                                        .scaledToFill()
                                        .frame(width: geo.size.width, height: stretchedHeight)
                                        .blur(radius: 12)
                                        .clipped()
                                        .offset(y: scrollOffset > 0 ? -scrollOffset : 0)
                                } else {
                                    Color(hex: "1E1E24")
                                        .frame(width: geo.size.width, height: stretchedHeight)
                                }

                                // Dark overlay gradient for readability
                                LinearGradient(
                                    colors: [.clear, .black.opacity(0.4), .black.opacity(0.85), Color(hex: "0F0F11")],
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
                                                    .font(.system(size: 9))
                                                    .foregroundColor(.gray)
                                                Text(collection.user.name)
                                                    .font(.system(size: 13, weight: .bold))
                                                    .foregroundColor(.white)
                                            }
                                        }
                                        .padding(.bottom, 6)
                                        .onTapGesture {
                                            let utmProfile = "\(collection.user.links.html)?utm_source=ImageFeedApp&utm_medium=referral"
                                            if let url = URL(string: utmProfile) {
                                                URLHelper.open(url)
                                            }
                                        }

                                        Text(collection.title)
                                            .font(.system(size: 24, weight: .bold))
                                            .foregroundColor(.white)

                                        Text("\(collection.totalPhotos) Photos")
                                            .font(.subheadline)
                                            .foregroundColor(.gray)

                                        if let desc = collection.description_, !desc.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                                            Text(desc)
                                                .font(.system(size: 13))
                                                .foregroundColor(.white.opacity(0.8))
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
                                    .foregroundColor(.white)
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
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 16)
                                    .padding(.top, 24)

                                HStack(alignment: .top, spacing: 8) {
                                    ForEach(0..<columnCount, id: \.self) { colIndex in
                                        LazyVStack(spacing: 8) {
                                            ForEach(
                                                AdaptiveLayoutHelper.photosForColumn(index: colIndex, totalColumns: columnCount, from: viewModel.photos),
                                                id: \.id
                                            ) { photo in
                                                let flatIndex = viewModel.photos.firstIndex(where: { $0.id == photo.id }) ?? 0
                                                Button {
                                                    onPhotoSelect(photo.id)
                                                } label: {
                                                    CollectionPhotoGridCard(photo: photo, viewModel: viewModel)
                                                        .staggeredReveal(index: flatIndex)
                                                        .feedScrollTransition()
                                                }
                                                .buttonStyle(SpringCardButtonStyle())
                                            }
                                        }
                                    }
                                }
                                .padding(.horizontal, 8)
                            }
                        }

                        if viewModel.isLoadingPhotos {
                            ProgressView()
                                .tint(.white)
                                .padding(.vertical, 24)
                        }
                    }
                }
                .ignoresSafeArea(edges: .top)
            }
        }
        .navigationBarBackButtonHidden(true)
        #if os(iOS)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        #endif
        .toolbar {
            #if os(iOS)
            ToolbarItem(placement: .navigationBarLeading) {
                GlassToolbarButton(systemName: "chevron.left", accessibilityLabel: "Back") {
                    dismiss()
                }
            }
            #else
            ToolbarItem(placement: .navigation) {
                GlassToolbarButton(systemName: "chevron.left", accessibilityLabel: "Back") {
                    dismiss()
                }
            }
            #endif
        }
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
                        .fill(Color(hex: "1E1E24"))
                        .frame(width: 160, height: 110)
                }

                LinearGradient(
                    colors: [.clear, .black.opacity(0.8)],
                    startPoint: .top,
                    endPoint: .bottom)
                    .clipShape(RoundedRectangle(cornerRadius: 12))

                VStack(alignment: .leading, spacing: 2) {
                    Text(collection.title)
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.white)
                        .lineLimit(1)
                    Text("\(collection.totalPhotos) Photos")
                        .font(.system(size: 9))
                        .foregroundColor(.gray)
                }
                .padding(8)
            }
            .frame(width: 160, height: 110)
        }
        .buttonStyle(SpringCardButtonStyle())
    }
}

struct CollectionPhotoGridCard: View {
    let photo: Photo
    let viewModel: CollectionDetailViewModel

    var body: some View {
        let screenWidth = AdaptiveLayoutHelper.getScreenWidth()
        let itemWidth = Int(screenWidth / 2)
        let imageUrl = photo.urls.raw + "&w=\(itemWidth)&q=80&auto=format"
        let aspectRatio = photo.height > 0 ? CGFloat(photo.width) / CGFloat(photo.height) : 1.0

        ZStack(alignment: .bottomLeading) {
            KFImage(URL(string: imageUrl))
                .placeholder {
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
                .aspectRatio(aspectRatio, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: 12))

            Button {
                let utmProfile = "\(photo.user.links.html)?utm_source=ImageFeedApp&utm_medium=referral"
                if let url = URL(string: utmProfile) {
                    URLHelper.open(url)
                }
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
            if photo.id == viewModel.photos.last?.id {
                viewModel.loadNextPhotosPage()
            }
        }
    }
}
