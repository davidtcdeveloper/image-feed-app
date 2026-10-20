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

    init() {
        let viewModel = MetroHelper.shared.getFeedViewModel()
        self.viewModel = viewModel

        self.stateCloseable = viewModel.iosState.watch { [weak self] state in
            guard let self, let state else { return }
            if self.photos != state.photos { self.photos = state.photos }
            if self.topics != state.topics { self.topics = state.topics }
            if self.selectedTopicSlug != state.selectedTopicSlug { self.selectedTopicSlug = state.selectedTopicSlug }
            if self.isLoading != state.isLoading { self.isLoading = state.isLoading }
            if self.isLoadingTopics != state.isLoadingTopics { self.isLoadingTopics = state.isLoadingTopics }
            if self.isRefreshing != state.isRefreshing { self.isRefreshing = state.isRefreshing }
            let errorMessage = state.error?.localizedMessage
            if self.error != errorMessage { self.error = errorMessage }
            if self.hasReachedEnd != state.hasReachedEnd { self.hasReachedEnd = state.hasReachedEnd }
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
        self.randomPhotoViewModel = randomVm

        randomPhotoCloseable = randomVm.iosState.watch { [weak self] state in
            guard let self = self else { return }
            if let photo = state?.photo {
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

    deinit {
        stateCloseable?.close()
        effectsCloseable?.close()
        viewModel.clear()
        randomPhotoCloseable?.close()
        randomPhotoViewModel?.clear()
    }
}
