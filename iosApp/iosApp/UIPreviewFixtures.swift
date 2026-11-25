#if DEBUG
import shared
import SwiftUI

enum UIFixtureScreen: String, CaseIterable {
    case photoDetails
    case photosFeed
    case collectionsFeed
    case searchResults
    case searchFilters
    case userProfileInsights
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

    static let userTwo = User(
        id: "user_2",
        username: "lucas_almeida",
        name: "Lucas Almeida",
        portfolioUrl: "https://unsplash.com/@lucas_almeida",
        profileImage: ProfileImage(
            small: "https://fixture.imagefeed.local/avatar_2",
            medium: "https://fixture.imagefeed.local/avatar_2",
            large: "https://fixture.imagefeed.local/avatar_2"),
        links: UserLinks(
            self: "https://api.unsplash.com/users/lucas_almeida",
            html: "https://unsplash.com/@lucas_almeida",
            photos: "https://api.unsplash.com/users/lucas_almeida/photos"),
        bio: "Minimalist street and coastal essays.",
        location: "Porto, Portugal",
        totalLikes: KotlinInt(int: 1290),
        totalPhotos: KotlinInt(int: 145),
        totalCollections: KotlinInt(int: 9),
        social: nil)

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

    static let samplePhotos: [Photo] = [
        samplePhoto,
        Photo(
            id: "photo_2",
            createdAt: "2026-10-11T14:00:00Z",
            width: 4000,
            height: 3000,
            color: "#D4A373",
            blurHash: "L5H2EC=PM+yV0g-mq.wG9c010J}I",
            description: "Sunlit limestone courtyard.",
            altDescription: "Limestone courtyard",
            urls: sampleUrls,
            user: userTwo,
            links: samplePhoto.links,
            exif: nil,
            location: nil,
            tags: nil),
        Photo(
            id: "photo_3",
            createdAt: "2026-10-10T18:00:00Z",
            width: 3200,
            height: 4200,
            color: "#2B5876",
            blurHash: nil,
            description: "Atlantic mist over basalt cliffs.",
            altDescription: "Basalt cliffs",
            urls: sampleUrls,
            user: userOne,
            links: samplePhoto.links,
            exif: nil,
            location: nil,
            tags: nil),
        Photo(
            id: "photo_4",
            createdAt: "2026-10-09T11:00:00Z",
            width: 3600,
            height: 2800,
            color: "#52796F",
            blurHash: nil,
            description: "Mossy cedar canopy in winter light.",
            altDescription: "Cedar canopy",
            urls: sampleUrls,
            user: userTwo,
            links: samplePhoto.links,
            exif: nil,
            location: nil,
            tags: nil),
        Photo(
            id: "photo_5",
            createdAt: "2026-10-08T09:00:00Z",
            width: 3000,
            height: 3600,
            color: "#6D597A",
            blurHash: nil,
            description: "Dusk reflection on slate fjord.",
            altDescription: "Slate fjord",
            urls: sampleUrls,
            user: userOne,
            links: samplePhoto.links,
            exif: nil,
            location: nil,
            tags: nil),
    ]

    static let sampleTopics: [Topic] = [
        Topic(
            id: "t1",
            slug: "nature",
            title: "Nature",
            description: nil,
            publishedAt: nil,
            updatedAt: nil,
            totalPhotos: 1200,
            status: "open",
            coverPhoto: nil,
            owners: nil),
        Topic(
            id: "t2",
            slug: "architecture",
            title: "Architecture",
            description: nil,
            publishedAt: nil,
            updatedAt: nil,
            totalPhotos: 980,
            status: "open",
            coverPhoto: nil,
            owners: nil),
        Topic(
            id: "t3",
            slug: "minimal",
            title: "Minimal",
            description: nil,
            publishedAt: nil,
            updatedAt: nil,
            totalPhotos: 740,
            status: "open",
            coverPhoto: nil,
            owners: nil),
        Topic(
            id: "t4",
            slug: "travel",
            title: "Travel",
            description: nil,
            publishedAt: nil,
            updatedAt: nil,
            totalPhotos: 1520,
            status: "open",
            coverPhoto: nil,
            owners: nil),
    ]

    static let sampleCollections: [PhotoCollection] = [
        PhotoCollection(
            id: "col_1",
            title: "Nordic Timber & Stone",
            description: "Quiet architectural studies across granite ridges and fjord cabins.",
            publishedAt: "2026-10-01T10:00:00Z",
            updatedAt: "2026-10-12T10:00:00Z",
            totalPhotos: 42,
            private: false,
            coverPhoto: samplePhotos[0],
            user: userOne,
            links: nil,
            previewPhotos: [
                PreviewPhoto(id: "p1", createdAt: nil, updatedAt: nil, blurHash: nil, urls: sampleUrls),
                PreviewPhoto(id: "p2", createdAt: nil, updatedAt: nil, blurHash: nil, urls: sampleUrls),
                PreviewPhoto(id: "p3", createdAt: nil, updatedAt: nil, blurHash: nil, urls: sampleUrls),
            ]),
        PhotoCollection(
            id: "col_2",
            title: "Warm Terracotta Light",
            description: "Late afternoon shadows across Mediterranean plazas and courtyards.",
            publishedAt: "2026-09-20T10:00:00Z",
            updatedAt: "2026-10-10T10:00:00Z",
            totalPhotos: 28,
            private: false,
            coverPhoto: samplePhotos[1],
            user: userTwo,
            links: nil,
            previewPhotos: [
                PreviewPhoto(id: "p4", createdAt: nil, updatedAt: nil, blurHash: nil, urls: sampleUrls),
                PreviewPhoto(id: "p5", createdAt: nil, updatedAt: nil, blurHash: nil, urls: sampleUrls),
                PreviewPhoto(id: "p6", createdAt: nil, updatedAt: nil, blurHash: nil, urls: sampleUrls),
            ]),
        PhotoCollection(
            id: "col_3",
            title: "Atlantic Mist & Cedar",
            description: "Coastal cliffs and evergreens captured in winter fog.",
            publishedAt: "2026-09-10T10:00:00Z",
            updatedAt: "2026-10-05T10:00:00Z",
            totalPhotos: 19,
            private: false,
            coverPhoto: samplePhotos[2],
            user: userOne,
            links: nil,
            previewPhotos: [
                PreviewPhoto(id: "p7", createdAt: nil, updatedAt: nil, blurHash: nil, urls: sampleUrls),
                PreviewPhoto(id: "p8", createdAt: nil, updatedAt: nil, blurHash: nil, urls: sampleUrls),
                PreviewPhoto(id: "p9", createdAt: nil, updatedAt: nil, blurHash: nil, urls: sampleUrls),
            ]),
    ]

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

    static let sampleUserStats = UserStats(
        username: "clara_mendes",
        downloads: StatMetric(
            total: 98400,
            historical: HistoricalStats(change: KotlinInt(int: 4210), average: nil, resolution: nil, quantity: nil, values: sampleHistoricalValues)),
        views: StatMetric(
            total: 3420000,
            historical: HistoricalStats(change: KotlinInt(int: 148000), average: nil, resolution: nil, quantity: nil, values: sampleHistoricalValues)))

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

        case .photosFeed:
            let feedVm = FeedViewModel(
                previewPhotos: samplePhotos,
                previewTopics: sampleTopics,
                selectedTopicSlug: "editorial",
                isLoading: false)
            return AnyView(
                PhotosFeedTabView(initialViewModel: feedVm)
                    .environment(\.isUISnapshotMode, true))

        case .collectionsFeed:
            let collectionsVm = CollectionsFeedViewModel(
                previewCollections: sampleCollections,
                isLoading: false)
            return AnyView(
                NavigationStack {
                    CollectionsFeedView(
                        initialViewModel: collectionsVm,
                        onCollectionSelect: { _ in },
                        onSearchClick: {})
                }
                .environment(\.isUISnapshotMode, true))

        case .searchResults:
            let searchVm = SearchViewModel(
                previewQuery: "alpine",
                previewTab: .photos,
                previewPhotos: samplePhotos,
                previewHistory: ["alpine", "architecture"],
                isLoading: false)
            return AnyView(
                NavigationStack {
                    SearchView(
                        initialQuery: "alpine",
                        initialViewModel: searchVm,
                        onPhotoSelect: { _ in },
                        onUserSelect: { _ in },
                        onCollectionSelect: { _ in })
                }
                .environment(\.isUISnapshotMode, true))

        case .searchFilters:
            return AnyView(
                SearchFiltersSheetView(
                    filters: SearchFilters(orderBy: "relevant", color: nil, orientation: nil, contentFilter: "low"),
                    onApply: { _ in })
                    .environment(\.isUISnapshotMode, true))

        case .userProfileInsights:
            let profileVm = UserProfileViewModel(
                previewUser: userOne,
                previewTab: .insights,
                previewStats: sampleUserStats)
            return AnyView(
                NavigationStack {
                    UserProfileView(
                        username: userOne.username,
                        initialViewModel: profileVm,
                        onPhotoSelect: { _ in },
                        onCollectionSelect: { _ in })
                }
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
