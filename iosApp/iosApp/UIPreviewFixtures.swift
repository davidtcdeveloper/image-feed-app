#if DEBUG
import shared
import SwiftUI

enum UIFixtureScreen: String, CaseIterable {
    case photoDetails
}

enum UIPreviewFixtures {
    static let sampleUrls = PhotoUrls(
        raw: "https://fixture.imagefeed.local/photo_1",
        full: "https://fixture.imagefeed.local/photo_1",
        regular: "https://fixture.imagefeed.local/photo_1",
        small: "https://fixture.imagefeed.local/photo_1",
        thumb: "https://fixture.imagefeed.local/photo_1")

    static let userOne = User(
        id: "user_1",
        username: "clara_mendes",
        name: "Clara Mendes",
        portfolioUrl: "https://unsplash.com/@clara_mendes",
        profileImage: ProfileImage(
            small: "https://fixture.imagefeed.local/avatar_1",
            medium: "https://fixture.imagefeed.local/avatar_1",
            large: "https://fixture.imagefeed.local/avatar_1"),
        links: UserLinks(
            self: "https://api.unsplash.com/users/clara_mendes",
            html: "https://unsplash.com/@clara_mendes",
            photos: "https://api.unsplash.com/users/clara_mendes/photos"),
        bio: "Architectural & alpine landscape photographer documenting Nordic light.",
        location: "Lisbon, Portugal",
        totalLikes: KotlinInt(int: 4820),
        totalPhotos: KotlinInt(int: 312),
        totalCollections: KotlinInt(int: 18),
        social: Social(
            instagramUsername: "claramendes",
            twitterUsername: "claramendes",
            portfolioUrl: "https://claramendes.example.com"))

    static let samplePhoto = Photo(
        id: "photo_1",
        createdAt: "2026-10-12T08:30:00Z",
        width: 3000,
        height: 4000,
        color: "#1F4E78",
        blurHash: "L6PZfSi_.AyE_3t7t7R**0o#DgR4",
        description: "Morning fog drifting across granite alpine ridges.",
        altDescription: "Alpine ridge in soft morning fog",
        urls: sampleUrls,
        user: userOne,
        links: PhotoLinks(
            self: "https://api.unsplash.com/photos/photo_1",
            html: "https://unsplash.com/photos/photo_1",
            download: "https://unsplash.com/photos/photo_1/download",
            downloadLocation: "https://api.unsplash.com/photos/photo_1/download?ixid=fixture"),
        exif: Exif(
            make: "Fujifilm",
            model: "X-T5",
            exposureTime: "1/500",
            aperture: "2.8",
            focalLength: "35.0",
            iso: KotlinInt(int: 160)),
        location: Location(
            name: "Serra da Estrela, Portugal",
            city: "Covilha",
            country: "Portugal",
            position: nil),
        tags: [Tag(title: "alpine"), Tag(title: "architecture"), Tag(title: "minimal")])

    static let sampleHistoricalValues: [StatsValue] = [
        StatsValue(date: "2026-10-01", value: 320),
        StatsValue(date: "2026-10-05", value: 510),
        StatsValue(date: "2026-10-10", value: 480),
        StatsValue(date: "2026-10-15", value: 860),
        StatsValue(date: "2026-10-20", value: 1120),
        StatsValue(date: "2026-10-25", value: 940),
        StatsValue(date: "2026-10-30", value: 1380),
    ]

    static let samplePhotoStats = PhotoStats(
        id: "photo_1",
        downloads: StatMetric(
            total: 14820,
            historical: HistoricalStats(change: KotlinInt(int: 1840), average: nil, resolution: nil, quantity: nil, values: sampleHistoricalValues)),
        views: StatMetric(
            total: 482900,
            historical: HistoricalStats(change: KotlinInt(int: 54200), average: nil, resolution: nil, quantity: nil, values: sampleHistoricalValues)),
        likes: StatMetric(total: 6420, historical: nil))

    @MainActor
    static func makeView(for screen: UIFixtureScreen) -> AnyView {
        switch screen {
        case .photoDetails:
            let detailsVm = PhotoDetailsViewModel(
                previewPhoto: samplePhoto,
                previewStats: samplePhotoStats,
                isFavorite: true)
            return AnyView(
                PhotoDetailsPreviewContainer(viewModel: detailsVm)
                    .environment(\.isUISnapshotMode, true))
        }
    }
}

private struct PhotoDetailsPreviewContainer: View {
    let viewModel: PhotoDetailsViewModel
    @Namespace private var heroNamespace

    var body: some View {
        PhotoDetailsView(
            photoId: "photo_1",
            initialPhoto: viewModel.photo,
            initialViewModel: viewModel,
            heroNamespace: heroNamespace,
            onUserSelect: { _ in },
            onTagSelect: { _ in })
    }
}
#endif
