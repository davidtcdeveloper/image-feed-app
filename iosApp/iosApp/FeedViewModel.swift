import shared
import SwiftUI

@MainActor
@Observable
final class FeedViewModel {
    var photos: [Photo] = []
    var topics: [Topic] = []
    var selectedTopicSlug: String = "editorial"
    var isLoading = false
    var isLoadingTopics = false
    var isRefreshing = false
    var error: String?
    var hasReachedEnd = false

    private let viewModel: shared.FeedViewModel?
    @ObservationIgnored private nonisolated(unsafe) var stateCloseable: Closeable?
    @ObservationIgnored private nonisolated(unsafe) var effectsCloseable: Closeable?
    @ObservationIgnored private nonisolated(unsafe) var randomPhotoCloseable: Closeable?
    @ObservationIgnored private nonisolated(unsafe) var randomPhotoViewModel: shared.RandomPhotoViewModel?
    @ObservationIgnored private var cachedBuckets: [Int: [[AdaptiveLayoutHelper.IndexedItem<Photo>]]] = [:]

    #if DEBUG
    init(
        previewPhotos: [Photo],
        previewTopics: [Topic] = [],
        selectedTopicSlug: String = "editorial",
        isLoading: Bool = false,
        isLoadingTopics: Bool = false,
        error: String? = nil)
    {
        self.viewModel = nil
        self.photos = previewPhotos
        self.topics = previewTopics
        self.selectedTopicSlug = selectedTopicSlug
        self.isLoading = isLoading
        self.isLoadingTopics = isLoadingTopics
        self.error = error
    }
    #endif

    init() {
        let viewModel = MetroHelper.shared.getFeedViewModel()
        self.viewModel = viewModel

        self.stateCloseable = viewModel.iosState.watch { [weak self] state in
            MainActor.assumeIsolated {
                guard let self, let state else { return }
                if self.photos != state.photos {
                    self.photos = state.photos
                    self.cachedBuckets.removeAll()
                }
                if self.topics != state.topics {
                    self.topics = state.topics
                }
                if self.selectedTopicSlug != state.selectedTopicSlug {
                    self.selectedTopicSlug = state.selectedTopicSlug
                }
                if self.isLoading != state.isLoading {
                    self.isLoading = state.isLoading
                }
                if self.isLoadingTopics != state.isLoadingTopics {
                    self.isLoadingTopics = state.isLoadingTopics
                }
                if self.isRefreshing != state.isRefreshing {
                    self.isRefreshing = state.isRefreshing
                }
                let errorMessage = state.error?.localizedMessage
                if self.error != errorMessage {
                    self.error = errorMessage
                }
                if self.hasReachedEnd != state.hasReachedEnd {
                    self.hasReachedEnd = state.hasReachedEnd
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

    private func handleEffect(_ effect: FeedEffect) {
        switch effect {
        case let toast as FeedEffectShowToast:
            error = toast.error.localizedMessage
        default:
            break
        }
    }

    func selectTopic(slug: String) {
        viewModel?.dispatch(intent: FeedIntentSelectTopic(slug: slug))
    }

    func loadNextPage() {
        viewModel?.dispatch(intent: FeedIntentLoadNextPage.shared)
    }

    func refresh() {
        viewModel?.dispatch(intent: FeedIntentRefresh.shared)
    }

    func trackDownload(photo: Photo) {
        viewModel?.dispatch(intent: FeedIntentTrackDownload(photo: photo))
    }

    func fetchRandomPhotoId(completion: @escaping (String?) -> Void) {
        randomPhotoCloseable?.close()
        randomPhotoCloseable = nil
        randomPhotoViewModel?.clear()
        randomPhotoViewModel = nil

        let randomVm = MetroHelper.shared.getRandomPhotoViewModel()
        randomPhotoViewModel = randomVm

        randomPhotoCloseable = randomVm.iosState.watch { [weak self] state in
            MainActor.assumeIsolated {
                guard let self else { return }
                if let photo = state?.photo {
                    MetroHelper.shared.seedPhotoPreview(photo: photo)
                    MetroHelper.shared.seedUserPreview(user: photo.user)
                    completion(photo.id)
                    self.randomPhotoCloseable?.close()
                    self.randomPhotoCloseable = nil
                    self.randomPhotoViewModel?.clear()
                    self.randomPhotoViewModel = nil
                } else if let error = state?.error {
                    print("Failed to load random photo on shake: \(error)")
                    completion(nil)
                    self.randomPhotoCloseable?.close()
                    self.randomPhotoCloseable = nil
                    self.randomPhotoViewModel?.clear()
                    self.randomPhotoViewModel = nil
                }
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
        viewModel?.clear()
        randomPhotoCloseable?.close()
        randomPhotoViewModel?.clear()
    }
}
