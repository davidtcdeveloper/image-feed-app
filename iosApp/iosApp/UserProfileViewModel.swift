import shared
import SwiftUI

@Observable
class UserProfileViewModel {
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

    private let viewModel: shared.UserProfileViewModel
    private var stateCloseable: Closeable?
    private var effectsCloseable: Closeable?
    @ObservationIgnored private var cachedPortfolioBuckets: [Int: [[AdaptiveLayoutHelper.IndexedItem<Photo>]]] = [:]
    @ObservationIgnored private var cachedLikedBuckets: [Int: [[AdaptiveLayoutHelper.IndexedItem<Photo>]]] = [:]

    init(username: String) {
        let viewModel = MetroHelper.shared.getUserProfileViewModel(username: username)
        self.viewModel = viewModel

        self.stateCloseable = viewModel.iosState.watch { [weak self] state in
            guard let self, let state else { return }
            if user != state.user {
                self.user = state.user
            }
            if activeTab != state.activeTab {
                self.activeTab = state.activeTab
            }
            if portfolioPhotos != state.portfolioPhotos {
                self.portfolioPhotos = state.portfolioPhotos
                cachedPortfolioBuckets.removeAll()
            }
            if likedPhotos != state.likedPhotos {
                self.likedPhotos = state.likedPhotos
                cachedLikedBuckets.removeAll()
            }
            if collections != state.collections {
                self.collections = state.collections
            }
            if stats != state.stats {
                self.stats = state.stats
            }
            if isLoadingContent != state.isLoadingContent {
                self.isLoadingContent = state.isLoadingContent
            }
            if isHeaderLoading != state.isHeaderLoading {
                self.isHeaderLoading = state.isHeaderLoading
            }
            if isLoadingStats != state.isLoadingStats {
                self.isLoadingStats = state.isLoadingStats
            }
            let errorMessage = state.error?.localizedMessage
            if error != errorMessage {
                self.error = errorMessage
            }
        }

        self.effectsCloseable = viewModel.iosEffects.watch { [weak self] effect in
            guard let self, let effect else { return }
            handleEffect(effect)
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
        viewModel.dispatch(intent: UserProfileIntentSelectTab(tab: tab))
    }

    func loadNextPage() {
        viewModel.dispatch(intent: UserProfileIntentLoadNextPage.shared)
    }

    func refresh() {
        viewModel.dispatch(intent: UserProfileIntentRefresh.shared)
    }

    func trackDownload(photo: Photo) {
        viewModel.dispatch(intent: UserProfileIntentTrackDownload(photo: photo))
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
        viewModel.clear()
    }
}
