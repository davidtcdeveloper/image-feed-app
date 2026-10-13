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

    init() {
        let viewModel = MetroHelper.shared.getFeedViewModel()
        self.viewModel = viewModel

        self.stateCloseable = viewModel.iosState.watch { [weak self] state in
            guard let self, let state else { return }
            self.photos = state.photos
            self.topics = state.topics
            self.selectedTopicSlug = state.selectedTopicSlug
            self.isLoading = state.isLoading
            self.isLoadingTopics = state.isLoadingTopics
            self.isRefreshing = state.isRefreshing
            self.error = state.error
            self.hasReachedEnd = state.hasReachedEnd
        }

        self.effectsCloseable = viewModel.iosEffects.watch { [weak self] effect in
            guard let self, let effect else { return }
            handleEffect(effect)
        }
    }

    private func handleEffect(_ effect: FeedEffect) {
        switch effect {
        case let toast as FeedEffectShowToast:
            error = toast.message
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

    func fetchRandomPhotoId(completion: @escaping (String) -> Void) {
        let randomVm = MetroHelper.shared.getRandomPhotoViewModel()
        var closeable: Closeable?
        closeable = randomVm.iosState.watch { state in
            if let photo = state?.photo {
                completion(photo.id)
                closeable?.close()
                randomVm.clear()
            } else if let error = state?.error {
                print("Failed to load random photo on shake: \(error)")
                closeable?.close()
                randomVm.clear()
            }
        }
    }

    deinit {
        stateCloseable?.close()
        effectsCloseable?.close()
        viewModel.clear()
    }
}
