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

    private var cardWidth: CGFloat {
        let containerWidth = AdaptiveLayoutHelper.getScreenWidth()
        let totalSpacing = CGFloat((columnCount - 1) * 16 + 24)
        return max(200, (containerWidth - totalSpacing) / CGFloat(max(1, columnCount)))
    }

    var body: some View {
        ZStack {
            GlassTheme.canvasBackgroundColor
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
                ErrorView(error: viewModel.error ?? String(localized: "Failed to load collections"), onRetry: {
                    viewModel.refresh()
                })
            } else {
                ScrollView {
                    if columnCount > 1 {
                        HStack(alignment: .top, spacing: 16) {
                            ForEach(0..<columnCount, id: \.self) { colIndex in
                                LazyVStack(spacing: 16) {
                                    ForEach(viewModel.columnItems(for: colIndex, totalColumns: columnCount)) { indexed in
                                        Button {
                                            onCollectionSelect(indexed.item.id)
                                        } label: {
                                            CollectionMosaicCard(
                                                collection: indexed.item,
                                                viewModel: viewModel,
                                                cardWidth: cardWidth,
                                                columnCount: columnCount,
                                                index: indexed.index)
                                                .feedScrollTransition()
                                        }
                                        .buttonStyle(SpringCardButtonStyle())
                                    }
                                }
                            }
                        }
                        .padding(.horizontal, 12)
                        .padding(.top, 10)
                        .padding(.bottom, 80)

                        if viewModel.isLoading {
                            ProgressView()
                                .tint(.white)
                                .padding(.vertical, 16)
                        }
                    } else {
                        LazyVStack(spacing: 16) {
                            ForEach(viewModel.columnItems(for: 0, totalColumns: 1)) { indexed in
                                Button {
                                    onCollectionSelect(indexed.item.id)
                                } label: {
                                    CollectionMosaicCard(
                                        collection: indexed.item,
                                        viewModel: viewModel,
                                        cardWidth: cardWidth,
                                        columnCount: 1,
                                        index: indexed.index)
                                        .staggeredReveal(index: indexed.index)
                                        .feedScrollTransition()
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
                        .padding(.bottom, 80)
                    }
                }
                .refreshable {
                    viewModel.refresh()
                }
            }
        }
        .navigationTitle("COLLECTIONS")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
        #endif
        .toolbar {
            #if os(iOS)
            ToolbarItem(placement: .navigationBarLeading) {
                GlassToolbarButton(systemName: "magnifyingglass", accessibilityLabel: "Search") {
                    onSearchClick()
                }
            }
            #else
            ToolbarItem(placement: .navigation) {
                GlassToolbarButton(systemName: "magnifyingglass", accessibilityLabel: "Search") {
                    onSearchClick()
                }
            }
            #endif
        }
    }
}

struct CollectionMosaicCard: View {
    let collection: PhotoCollection
    let viewModel: CollectionsFeedViewModel
    var cardWidth: CGFloat?
    var columnCount: Int = 1
    var index: Int = 0

    var body: some View {
        #if os(iOS)
        let scale = UIScreen.main.scale
        #elseif os(macOS)
        let scale = NSScreen.main?.backingScaleFactor ?? 2.0
        #endif
        let widthPt = cardWidth ?? max(200, AdaptiveLayoutHelper.getScreenWidth() - 24)
        let widthPx = Int32(widthPt * scale)
        let sideWidthPx = widthPx / 3
        let sideWidthPt = widthPt / 3

        VStack(alignment: .leading, spacing: 0) {
            // Mosaic previews: Left 2/3 and Right two 1/3s stacked
            HStack(spacing: 4) {
                // Large Cover Photo (Left)
                let coverUrl = collection.coverPhoto?.urls.getOptimalUrl(targetWidthPx: widthPx * 2 / 3)
                    ?? collection.previewPhotos?.first?.urls.getOptimalUrl(targetWidthPx: widthPx * 2 / 3)
                    ?? ""
                KFImage(URL(string: coverUrl))
                    .placeholder {
                        ZStack {
                            RoundedRectangle(cornerRadius: 0)
                                .fill(Color(hex: collection.coverPhoto?.color ?? "1E1E24"))
                            ProgressView()
                                .tint(.primary.opacity(0.6))
                        }
                    }
                    .fade(duration: 0.25)
                    .resizable()
                    .scaledToFill()
                    .frame(height: 180)
                    .clipped()

                // Two smaller thumbnails (Right stack)
                VStack(spacing: 4) {
                    // Top preview
                    if let previewPhotos = collection.previewPhotos, previewPhotos.count > 1 {
                        let topPreview = previewPhotos[1]
                        KFImage(URL(string: topPreview.urls.getOptimalUrl(targetWidthPx: sideWidthPx)))
                            .fade(duration: 0.25)
                            .resizable()
                            .scaledToFill()
                            .frame(height: 88)
                            .clipped()
                    } else if let previewPhotos = collection.previewPhotos, previewPhotos.count > 0 {
                        let topPreview = previewPhotos[0]
                        KFImage(URL(string: topPreview.urls.getOptimalUrl(targetWidthPx: sideWidthPx)))
                            .fade(duration: 0.25)
                            .resizable()
                            .scaledToFill()
                            .frame(height: 88)
                            .clipped()
                    } else {
                        Rectangle()
                            .fill(GlassTheme.surfaceBackgroundColor)
                            .frame(height: 88)
                    }

                    // Bottom preview
                    if let previewPhotos = collection.previewPhotos, previewPhotos.count > 2 {
                        let bottomPreview = previewPhotos[2]
                        KFImage(URL(string: bottomPreview.urls.getOptimalUrl(targetWidthPx: sideWidthPx)))
                            .fade(duration: 0.25)
                            .resizable()
                            .scaledToFill()
                            .frame(height: 88)
                            .clipped()
                    } else if let previewPhotos = collection.previewPhotos, previewPhotos.count > 1 {
                        let bottomPreview = previewPhotos[1]
                        KFImage(URL(string: bottomPreview.urls.getOptimalUrl(targetWidthPx: sideWidthPx)))
                            .fade(duration: 0.25)
                            .resizable()
                            .scaledToFill()
                            .frame(height: 88)
                            .clipped()
                    } else {
                        Rectangle()
                            .fill(GlassTheme.surfaceBackgroundColor)
                            .frame(height: 88)
                    }
                }
                .frame(width: CGFloat(sideWidthPt))
            }
            .frame(height: 180)
            .clipShape(RoundedRectangle(cornerRadius: 12))

            // Collection Details
            VStack(alignment: .leading, spacing: 6) {
                Text(collection.title)
                    .font(.headline)
                    .glassVibrancy(.primary)
                    .lineLimit(1)

                if let desc = collection.description_, !desc.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(desc)
                        .font(.subheadline)
                        .glassVibrancy(.secondary)
                        .lineLimit(2)
                }

                HStack {
                    // Curator info
                    HStack(spacing: 6) {
                        KFImage(URL(string: collection.user.profileImage.small))
                            .resizable()
                            .scaledToFill()
                            .frame(width: 24, height: 24)
                            .clipShape(Circle())

                        VStack(alignment: .leading, spacing: 1) {
                            Text("Curated by")
                                .font(.caption2)
                                .glassVibrancy(.secondary)
                            Text(collection.user.name)
                                .font(.caption.weight(.semibold))
                                .glassVibrancy(.primary)
                        }
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        let utmProfile = "\(collection.user.links.html)?utm_source=ImageFeedApp&utm_medium=referral"
                        if let url = URL(string: utmProfile) {
                            URLHelper.open(url)
                        }
                    }

                    Spacer()

                    // Photos count label
                    Text("\(collection.totalPhotos) Photos")
                        .font(.caption2.weight(.bold))
                        .glassVibrancy(.primary)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .glassCapsule(style: .lightweight, showBorder: true, hasShadow: false)
                }
                .padding(.top, 6)
            }
            .padding(.vertical, 12)
        }
        .onAppear {
            if index >= viewModel.collections.count - max(4, columnCount * 2) {
                viewModel.loadNextPage()
            }
        }
    }
}
