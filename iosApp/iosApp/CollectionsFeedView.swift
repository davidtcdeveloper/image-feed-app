import shared
import SwiftUI

struct CollectionsFeedView: View {
    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var sizeClass
    #endif

    @State private var viewModel = CollectionsFeedViewModel()
    let onCollectionSelect: (String) -> Void
    let onSearchClick: () -> Void

    private var columnCount: Int {
        #if os(iOS)
        return AdaptiveLayoutHelper.getCollectionColumnCount(sizeClass: sizeClass)
        #else
        return AdaptiveLayoutHelper.getCollectionColumnCount()
        #endif
    }

    var body: some View {
        GeometryReader { geometry in
            let containerWidth = geometry.size.width
            let totalSpacing = CGFloat((columnCount - 1) * 16 + 24)
            let cardWidth = max(200, (containerWidth - totalSpacing) / CGFloat(max(1, columnCount)))

            ZStack {
                Color(hex: "0F0F11")
                    .ignoresSafeArea()

                if viewModel.collections.isEmpty, viewModel.isLoading {
                    ScrollView {
                        if columnCount > 1 {
                            HStack(alignment: .top, spacing: 16) {
                                ForEach(0..<columnCount, id: \.self) { _ in
                                    LazyVStack(spacing: 16) {
                                        ForEach(0..<2, id: \.self) { _ in
                                            CollectionMosaicCardSkeleton()
                                        }
                                    }
                                }
                            }
                            .padding(.horizontal, 12)
                            .padding(.top, 10)
                        } else {
                            VStack(spacing: 16) {
                                ForEach(0..<4, id: \.self) { _ in
                                    CollectionMosaicCardSkeleton()
                                }
                            }
                            .padding(.horizontal, 12)
                            .padding(.top, 10)
                        }
                    }
                } else if viewModel.collections.isEmpty, viewModel.error != nil {
                    ErrorView(error: viewModel.error ?? "Failed to load collections", onRetry: {
                        viewModel.refresh()
                    })
                } else {
                    ScrollView {
                        if columnCount > 1 {
                            HStack(alignment: .top, spacing: 16) {
                                ForEach(0..<columnCount, id: \.self) { colIndex in
                                    let columnCollections = AdaptiveLayoutHelper.itemsForColumn(
                                        index: colIndex,
                                        totalColumns: columnCount,
                                        from: viewModel.collections
                                    )
                                    LazyVStack(spacing: 16) {
                                        ForEach(columnCollections, id: \.id) { collection in
                                            Button(action: {
                                                onCollectionSelect(collection.id)
                                            }) {
                                                CollectionMosaicCard(
                                                    collection: collection,
                                                    viewModel: viewModel,
                                                    cardWidth: cardWidth,
                                                    columnCount: columnCount,
                                                    onTap: {}
                                                )
                                            }
                                            .buttonStyle(SpringCardButtonStyle())
                                        }
                                    }
                                }
                            }
                            .padding(.horizontal, 12)
                            .padding(.top, 10)

                            if viewModel.isLoading {
                                ProgressView()
                                    .tint(.white)
                                    .padding(.vertical, 16)
                            }
                        } else {
                            LazyVStack(spacing: 16) {
                                ForEach(Array(viewModel.collections.enumerated()), id: \.element.id) { index, collection in
                                    Button(action: {
                                        onCollectionSelect(collection.id)
                                    }) {
                                        CollectionMosaicCard(
                                            collection: collection,
                                            viewModel: viewModel,
                                            cardWidth: cardWidth,
                                            columnCount: 1,
                                            onTap: {}
                                        )
                                        .staggeredReveal(index: index)
                                    }
                                    .buttonStyle(SpringCardButtonStyle())
                                }

                                if viewModel.isLoading {
                                    ProgressView()
                                        .tint(.white)
                                        .padding(.vertical, 16)
                                }
                            }
                            .padding(.horizontal, 12)
                            .padding(.top, 10)
                        }
                    }
                    .refreshable {
                        viewModel.refresh()
                    }
                }
            }
        }
        .navigationTitle("COLLECTIONS")
        #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
        #endif
            .toolbar {
                #if os(iOS)
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onSearchClick) {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.white)
                    }
                }
                #else
                ToolbarItem(placement: .navigation) {
                    Button(action: onSearchClick) {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.white)
                    }
                }
                #endif
            }
    }
}

struct CollectionMosaicCard: View {
    let collection: PhotoCollection
    let viewModel: CollectionsFeedViewModel
    var cardWidth: CGFloat? = nil
    var columnCount: Int = 1
    let onTap: () -> Void

    var body: some View {
        let width = Int(cardWidth ?? max(200, (AdaptiveLayoutHelper.getScreenWidth() - 24)))
        let sideWidth = width / 3

        VStack(alignment: .leading, spacing: 0) {
            // Mosaic previews: Left 2/3 and Right two 1/3s stacked
            HStack(spacing: 4) {
                // Large Cover Photo (Left)
                let coverUrl = (collection.coverPhoto?.urls.raw ?? "") + "&w=\(width * 2 / 3)&q=80&auto=format"
                KFImage(URL(string: coverUrl))
                    .placeholder {
                        RoundedRectangle(cornerRadius: 0)
                            .fill(Color(hex: collection.coverPhoto?.color ?? "1E1E24"))
                    }
                    .fade(duration: 0.25)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(height: 180)
                    .clipped()

                // Two smaller thumbnails (Right stack)
                VStack(spacing: 4) {
                    // Top preview
                    if let previewPhotos = collection.previewPhotos, previewPhotos.count > 1 {
                        let topPreview = previewPhotos[1]
                        KFImage(URL(string: topPreview.urls.small + "&w=\(sideWidth)&q=80&auto=format"))
                            .fade(duration: 0.25)
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(height: 88)
                            .clipped()
                    } else if let previewPhotos = collection.previewPhotos, previewPhotos.count > 0 {
                        let topPreview = previewPhotos[0]
                        KFImage(URL(string: topPreview.urls.small + "&w=\(sideWidth)&q=80&auto=format"))
                            .fade(duration: 0.25)
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(height: 88)
                            .clipped()
                    } else {
                        Color(hex: "1E1E24")
                            .frame(height: 88)
                    }

                    // Bottom preview
                    if let previewPhotos = collection.previewPhotos, previewPhotos.count > 2 {
                        let bottomPreview = previewPhotos[2]
                        KFImage(URL(string: bottomPreview.urls.small + "&w=\(sideWidth)&q=80&auto=format"))
                            .fade(duration: 0.25)
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(height: 88)
                            .clipped()
                    } else if let previewPhotos = collection.previewPhotos, previewPhotos.count > 1 {
                        let bottomPreview = previewPhotos[1]
                        KFImage(URL(string: bottomPreview.urls.small + "&w=\(sideWidth)&q=80&auto=format"))
                            .fade(duration: 0.25)
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(height: 88)
                            .clipped()
                    } else {
                        Color(hex: "1E1E24")
                            .frame(height: 88)
                    }
                }
                .frame(width: CGFloat(sideWidth))
            }
            .frame(height: 180)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .onTapGesture {
                onTap()
            }

            // Collection Details
            VStack(alignment: .leading, spacing: 6) {
                Text(collection.title)
                    .font(.headline)
                    .foregroundColor(.white)
                    .lineLimit(1)

                if let desc = collection.description_, !desc.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(desc)
                        .font(.subheadline)
                        .foregroundColor(.gray)
                        .lineLimit(2)
                }

                HStack {
                    // Curator info
                    HStack(spacing: 6) {
                        KFImage(URL(string: collection.user.profileImage.small))
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: 24, height: 24)
                            .clipShape(Circle())

                        VStack(alignment: .leading, spacing: 1) {
                            Text("Curated by")
                                .font(.system(size: 8))
                                .foregroundColor(.gray)
                            Text(collection.user.name)
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(.white)
                        }
                    }
                    .onTapGesture {
                        let utmProfile = "\(collection.user.links.html)?utm_source=ImageFeedApp&utm_medium=referral"
                        if let url = URL(string: utmProfile) {
                            URLHelper.open(url)
                        }
                    }

                    Spacer()

                    // Photos count label
                    Text("\(collection.totalPhotos) Photos")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Color.white.opacity(0.12))
                        .clipShape(Capsule())
                }
                .padding(.top, 6)
            }
            .padding(.vertical, 12)
        }
        .background(Color(hex: "0F0F11"))
        .onAppear {
            if let lastIndex = viewModel.collections.lastIndex(where: { $0.id == collection.id }),
               lastIndex >= viewModel.collections.count - max(4, columnCount * 2) {
                viewModel.loadNextPage()
            }
        }
    }
}
