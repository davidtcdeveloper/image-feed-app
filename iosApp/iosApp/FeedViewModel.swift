import SwiftUI
import shared

@Observable
class FeedViewModel {
    var photos: [Photo] = []
    var topics: [Topic] = []
    var selectedTopicSlug: String = "editorial"
    var isLoading = false
    var isLoadingTopics = false
    var isRefreshing = false
    var error: String? = nil
    var hasReachedEnd = false

    private let presenter: FeedPresenter
    private var closeable: Closeable?

    init() {
        // Resolve FeedPresenter using the KoinHelper from the shared KMP module
        let presenter = KoinHelper.shared.getFeedPresenter()
        self.presenter = presenter
        
        // Start watching the StateFlow and update state on main thread
        self.closeable = presenter.iosState.watch { [weak self] state in
            guard let self = self else { return }
            self.photos = state.photos
            self.topics = state.topics
            self.selectedTopicSlug = state.selectedTopicSlug
            self.isLoading = state.isLoading
            self.isLoadingTopics = state.isLoadingTopics
            self.isRefreshing = state.isRefreshing
            self.error = state.error
            self.hasReachedEnd = state.hasReachedEnd
        }
    }

    func selectTopic(slug: String) {
        presenter.selectTopic(slug: slug)
    }

    func loadNextPage() {
        presenter.loadNextPage()
    }

    func refresh() {
        presenter.refresh()
    }

    func trackDownload(photo: Photo) {
        presenter.trackDownload(photo: photo)
    }

    func fetchRandomPhotoId(completion: @escaping (String) -> Void) {
        let presenter = KoinHelper.shared.getRandomPhotoPresenter()
        var closeable: Closeable? = nil
        closeable = presenter.iosState.watch { state in
            if let photo = state.photo {
                completion(photo.id)
                closeable?.close()
            } else if let error = state.error {
                print("Failed to load random photo on shake: \(error)")
                closeable?.close()
            }
        }
    }

    deinit {
        closeable?.close()
    }
}
