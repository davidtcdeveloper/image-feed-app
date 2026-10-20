import shared
import SwiftUI

@Observable
class FeedViewModel {
    var photos: [Photo] = []
    var topics: [Topic] = []
    var selectedTopicSlug: String = "editorial"
    var isLoading = false
    var isLoadingTopics = false
    var isRefreshing = false
    var error: String?
    var hasReachedEnd = false

    private let viewModel: shared.FeedViewModel
    private var stateCloseable: Closeable?
    private var effectsCloseable: Closeable?
    private var randomPhotoCloseable: Closeable?
    private var randomPhotoViewModel: shared.RandomPhotoViewModel?
    @ObservationIgnored private var cachedBuckets: [Int: [[AdaptiveLayoutHelper.IndexedItem<Photo>]]] = [:]

    init() {
        let viewModel = MetroHelper.shared.getFeedViewModel()
        self.viewModel = viewModel

        self.stateCloseable = viewModel.iosState.watch { [weak self] state in
            guard let self, let state else { return }
            if photos != state.photos {
                self.photos = state.photos
                cachedBuckets.removeAll()
            }
            if topics != state.topics {
                self.topics = state.topics
            }
            if selectedTopicSlug != state.selectedTopicSlug {
                self.selectedTopicSlug = state.selectedTopicSlug
            }
            if isLoading != state.isLoading {
                self.isLoading = state.isLoading
            }
            if isLoadingTopics != state.isLoadingTopics {
                self.isLoadingTopics = state.isLoadingTopics
            }
            if isRefreshing != state.isRefreshing {
                self.isRefreshing = state.isRefreshing
            }
            let errorMessage = state.error?.localizedMessage
            if error != errorMessage {
                self.error = errorMessage
            }
            if hasReachedEnd != state.hasReachedEnd {
                self.hasReachedEnd = state.hasReachedEnd
            }
        }

        self.effectsCloseable = viewModel.iosEffects.watch { [weak self] effect in
            guard let self, let effect else { return }
            handleEffect(effect)
        }
    }

    private func handleEffect(_ effect: FeedEffect) {
        switch effect {
        case let toast as FeedEffectShowToast:
            error = toast.error.localizedMessage
        default:
            break
        }
    }

    func selectTopic(slug: String) {
        viewModel.dispatch(intent: FeedIntentSelectTopic(slug: slug))
    }

    func loadNextPage() {
        viewModel.dispatch(intent: FeedIntentLoadNextPage.shared)
    }

    func refresh() {
        viewModel.dispatch(intent: FeedIntentRefresh.shared)
    }

    func trackDownload(photo: Photo) {
        viewModel.dispatch(intent: FeedIntentTrackDownload(photo: photo))
    }

    func fetchRandomPhotoId(completion: @escaping (String?) -> Void) {
        randomPhotoCloseable?.close()
        randomPhotoCloseable = nil
        randomPhotoViewModel?.clear()
        randomPhotoViewModel = nil

        let randomVm = MetroHelper.shared.getRandomPhotoViewModel()
        randomPhotoViewModel = randomVm

        randomPhotoCloseable = randomVm.iosState.watch { [weak self] state in
            guard let self else { return }
            if let photo = state?.photo {
                completion(photo.id)
                randomPhotoCloseable?.close()
                randomPhotoCloseable = nil
                randomPhotoViewModel?.clear()
                randomPhotoViewModel = nil
            } else if let error = state?.error {
                print("Failed to load random photo on shake: \(error)")
                completion(nil)
                randomPhotoCloseable?.close()
                randomPhotoCloseable = nil
                randomPhotoViewModel?.clear()
                randomPhotoViewModel = nil
            }
        }
    }

    func columnItems(for columnIndex: Int, totalColumns: Int) -> [AdaptiveLayoutHelper.IndexedItem<Photo>] {
        guard totalColumns > 0 else { return [] }
        if let buckets = cachedBuckets[totalColumns], columnIndex < buckets.count {
            return buckets[columnIndex]
        }
        let buckets = AdaptiveLayoutHelper.partitionPhotosIntoColumns(from: photos, totalColumns: totalColumns)
        cachedBuckets[totalColumns] = buckets
        return columnIndex < buckets.count ? buckets[columnIndex] : []
    }

    deinit {
        stateCloseable?.close()
        effectsCloseable?.close()
        viewModel.clear()
        randomPhotoCloseable?.close()
        randomPhotoViewModel?.clear()
    }
}
