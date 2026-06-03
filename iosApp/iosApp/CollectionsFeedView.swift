import SwiftUI
import shared

struct CollectionsFeedView: View {
    @State private var viewModel = CollectionsFeedViewModel()
    let onCollectionSelect: (String) -> Void
    let onSearchClick: () -> Void

    var body: some View {
        ZStack {
            Color(hex: "0F0F11")
                .ignoresSafeArea()

            if viewModel.collections.isEmpty && viewModel.isLoading {
                ProgressView()
                    .tint(.white)
            } else if viewModel.collections.isEmpty && viewModel.error != nil {
                ErrorView(error: viewModel.error ?? "Failed to load collections", onRetry: {
                    viewModel.refresh()
                })
            } else {
                ScrollView {
                    LazyVStack(spacing: 16) {
                        ForEach(viewModel.collections, id: \.id) { collection in
                            CollectionMosaicCard(collection: collection, viewModel: viewModel) {
                                onCollectionSelect(collection.id)
                            }
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
                .refreshable {
                    viewModel.refresh()
                }
            }
        }
        .navigationTitle("COLLECTIONS")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onSearchClick) {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.white)
                }
            }
        }
    }
}

struct CollectionMosaicCard: View {
    let collection: PhotoCollection
    let viewModel: CollectionsFeedViewModel
    let onTap: () -> Void

    var body: some View {
        let screenWidth = UIScreen.main.bounds.width
        let cardWidth = Int(screenWidth - 24)
        let sideWidth = cardWidth / 3

        VStack(alignment: .leading, spacing: 0) {
            // Mosaic previews: Left 2/3 and Right two 1/3s stacked
            HStack(spacing: 4) {
                // Large Cover Photo (Left)
                let coverUrl = (collection.coverPhoto?.urls.raw ?? "") + "&w=\(cardWidth * 2 / 3)&q=80&auto=format"
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
                            UIApplication.shared.open(url)
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
            if collection.id == viewModel.collections.last?.id {
                viewModel.loadNextPage()
            }
        }
    }
}
