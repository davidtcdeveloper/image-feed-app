import SwiftUI
import shared

struct CollectionDetailView: View {
    let collectionId: String
    @State private var viewModel: CollectionDetailViewModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.horizontalSizeClass) var sizeClass

    let onPhotoSelect: (String) -> Void
    let onCollectionSelect: (String) -> Void

    init(collectionId: String, onPhotoSelect: @escaping (String) -> Void, onCollectionSelect: @escaping (String) -> Void) {
        self.collectionId = collectionId
        self._viewModel = State(initialValue: CollectionDetailViewModel(collectionId: collectionId))
        self.onPhotoSelect = onPhotoSelect
        self.onCollectionSelect = onCollectionSelect
    }

    private var columnCount: Int {
        AdaptiveLayoutHelper.getColumnCount(sizeClass: sizeClass)
    }

    var body: some View {
        ZStack(alignment: .top) {
            Color(hex: "0F0F11")
                .ignoresSafeArea()

            if viewModel.photos.isEmpty && viewModel.isLoadingPhotos && viewModel.isHeaderLoading {
                VStack {
                    Spacer()
                    ProgressView()
                        .tint(.white)
                    Spacer()
                }
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
                                        .aspectRatio(contentMode: .fill)
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
                                    endPoint: .bottom
                                )
                                .frame(width: geo.size.width, height: stretchedHeight)
                                .offset(y: scrollOffset > 0 ? -scrollOffset : 0)

                                // Information details
                                if let collection = viewModel.collection {
                                    VStack(alignment: .leading, spacing: 6) {
                                        // Curator info
                                        HStack(spacing: 8) {
                                            KFImage(URL(string: collection.user.profileImage.medium))
                                                .resizable()
                                                .aspectRatio(contentMode: .fill)
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
                                                UIApplication.shared.open(url)
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
                                            ForEach(AdaptiveLayoutHelper.photosForColumn(index: colIndex, totalColumns: columnCount, from: viewModel.photos), id: \.id) { photo in
                                                CollectionPhotoGridCard(photo: photo, viewModel: viewModel) {
                                                    onPhotoSelect(photo.id)
                                                }
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
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: { dismiss() }) {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                        Text("Back")
                    }
                    .foregroundColor(.white)
                }
            }
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
                        .aspectRatio(contentMode: .fill)
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
                    endPoint: .bottom
                )
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
    }
}

struct CollectionPhotoGridCard: View {
    let photo: Photo
    let viewModel: CollectionDetailViewModel
    let onSelect: () -> Void

    var body: some View {
        let screenWidth = UIScreen.main.bounds.width
        let itemWidth = Int(screenWidth / 2)
        let imageUrl = photo.urls.raw + "&w=\(itemWidth)&q=80&auto=format"
        let aspectRatio = CGFloat(photo.width) / CGFloat(photo.height)

        ZStack(alignment: .bottom) {
            KFImage(URL(string: imageUrl))
                .placeholder {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color(hex: photo.color ?? "1E1E24"))
                        .aspectRatio(aspectRatio, contentMode: .fit)
                        .overlay {
                            ProgressView()
                                .tint(.white.opacity(0.5))
                        }
                }
                .fade(duration: 0.25)
                .resizable()
                .aspectRatio(aspectRatio, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .contentShape(Rectangle())
                .onTapGesture {
                    onSelect()
                }

            HStack(spacing: 6) {
                KFImage(URL(string: photo.user.profileImage.small))
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 20, height: 20)
                    .clipShape(Circle())

                Text(photo.user.name)
                    .font(.system(size: 10, weight: .medium))
                    .foregroundColor(.white)
                    .lineLimit(1)
                    .truncationMode(.tail)

                Spacer()
            }
            .padding(6)
            .background(
                LinearGradient(
                    colors: [.clear, .black.opacity(0.75)],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .clipShape(RoundedCorner(radius: 12, corners: [.bottomLeft, .bottomRight]))
            .contentShape(Rectangle())
            .onTapGesture {
                let utmProfile = "\(photo.user.links.html)?utm_source=ImageFeedApp&utm_medium=referral"
                if let url = URL(string: utmProfile) {
                    UIApplication.shared.open(url)
                }
            }
        }
        .onAppear {
            if photo.id == viewModel.photos.last?.id {
                viewModel.loadNextPhotosPage()
            }
        }
    }
}
