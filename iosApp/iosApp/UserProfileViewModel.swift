import shared
import SwiftUI

@MainActor
@Observable
final class UserProfileViewModel {
    var user: User?
    var activeTab: ProfileTab = .portfolio
    var portfolioPhotos: [Photo] = []
    var likedPhotos: [Photo] = []
    var collections: [PhotoCollection] = []
    var stats: UserStats?
    var isLoadingContent = false
    var isHeaderLoading = false
    var isLoadingStats = false
    var error: String?

    private let viewModel: shared.UserProfileViewModel?
    @ObservationIgnored private nonisolated(unsafe) var stateCloseable: Closeable?
    @ObservationIgnored private nonisolated(unsafe) var effectsCloseable: Closeable?
    @ObservationIgnored private var cachedPortfolioBuckets: [Int: [[AdaptiveLayoutHelper.IndexedItem<Photo>]]] = [:]
    @ObservationIgnored private var cachedLikedBuckets: [Int: [[AdaptiveLayoutHelper.IndexedItem<Photo>]]] = [:]

    #if DEBUG
    init(
        previewUser: User?,
        previewTab: ProfileTab = .insights,
        previewPortfolioPhotos: [Photo] = [],
        previewLikedPhotos: [Photo] = [],
        previewCollections: [PhotoCollection] = [],
        previewStats: UserStats? = nil,
        isHeaderLoading: Bool = false,
        isLoadingContent: Bool = false,
        isLoadingStats: Bool = false,
        error: String? = nil)
    {
        self.viewModel = nil
        self.user = previewUser
        self.activeTab = previewTab
        self.portfolioPhotos = previewPortfolioPhotos
        self.likedPhotos = previewLikedPhotos
        self.collections = previewCollections
        self.stats = previewStats
        self.isHeaderLoading = isHeaderLoading
        self.isLoadingContent = isLoadingContent
        self.isLoadingStats = isLoadingStats
        self.error = error
    }
    #endif

    init(username: String) {
        let viewModel = MetroHelper.shared.getUserProfileViewModel(username: username)
        self.viewModel = viewModel

        self.stateCloseable = viewModel.iosState.watch { [weak self] state in
            MainActor.assumeIsolated {
                guard let self, let state else { return }
                if self.user != state.user {
                    self.user = state.user
                }
                if self.activeTab != state.activeTab {
                    self.activeTab = state.activeTab
                }
                if self.portfolioPhotos != state.portfolioPhotos {
                    self.portfolioPhotos = state.portfolioPhotos
                    self.cachedPortfolioBuckets.removeAll()
                }
                if self.likedPhotos != state.likedPhotos {
                    self.likedPhotos = state.likedPhotos
                    self.cachedLikedBuckets.removeAll()
                }
                if self.collections != state.collections {
                    self.collections = state.collections
                }
                if self.stats != state.stats {
                    self.stats = state.stats
                }
                if self.isLoadingContent != state.isLoadingContent {
                    self.isLoadingContent = state.isLoadingContent
                }
                if self.isHeaderLoading != state.isHeaderLoading {
                    self.isHeaderLoading = state.isHeaderLoading
                }
                if self.isLoadingStats != state.isLoadingStats {
                    self.isLoadingStats = state.isLoadingStats
                }
                let errorMessage = state.error?.localizedMessage
                if self.error != errorMessage {
                    self.error = errorMessage
                }
            }
        }

        self.effectsCloseable = viewModel.iosEffects.watch { [weak self] effect in
            MainActor.assumeIsolated {
                guard let self, let effect else { return }
                self.handleEffect(effect)
            }
        }
    }

    private func handleEffect(_ effect: UserProfileEffect) {
        switch effect {
        case let showError as UserProfileEffectShowError:
            error = showError.error.localizedMessage
        default:
            break
        }
    }

    func selectTab(tab: ProfileTab) {
        viewModel?.dispatch(intent: UserProfileIntentSelectTab(tab: tab))
    }

    func loadNextPage() {
        viewModel?.dispatch(intent: UserProfileIntentLoadNextPage.shared)
    }

    func refresh() {
        viewModel?.dispatch(intent: UserProfileIntentRefresh.shared)
    }

    func trackDownload(photo: Photo) {
        viewModel?.dispatch(intent: UserProfileIntentTrackDownload(photo: photo))
    }

    func portfolioColumnItems(for columnIndex: Int, totalColumns: Int) -> [AdaptiveLayoutHelper.IndexedItem<Photo>] {
        guard totalColumns > 0 else { return [] }
        if let buckets = cachedPortfolioBuckets[totalColumns], columnIndex < buckets.count {
            return buckets[columnIndex]
        }
        let buckets = AdaptiveLayoutHelper.partitionPhotosIntoColumns(from: portfolioPhotos, totalColumns: totalColumns)
        cachedPortfolioBuckets[totalColumns] = buckets
        return columnIndex < buckets.count ? buckets[columnIndex] : []
    }

    func likedColumnItems(for columnIndex: Int, totalColumns: Int) -> [AdaptiveLayoutHelper.IndexedItem<Photo>] {
        guard totalColumns > 0 else { return [] }
        if let buckets = cachedLikedBuckets[totalColumns], columnIndex < buckets.count {
            return buckets[columnIndex]
        }
        let buckets = AdaptiveLayoutHelper.partitionPhotosIntoColumns(from: likedPhotos, totalColumns: totalColumns)
        cachedLikedBuckets[totalColumns] = buckets
        return columnIndex < buckets.count ? buckets[columnIndex] : []
    }

    deinit {
        stateCloseable?.close()
        effectsCloseable?.close()
        viewModel?.clear()
    }
}
