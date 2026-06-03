import SwiftUI
import shared

@Observable
class CollectionDetailViewModel {
    var collection: PhotoCollection? = nil
    var photos: [Photo] = []
    var related: [PhotoCollection] = []
    var isLoadingPhotos = false
    var isHeaderLoading = false
    var error: String? = nil
    var hasReachedEnd = false

    private let presenter: CollectionDetailPresenter
    private var closeable: Closeable?

    init(collectionId: String) {
        let presenter = KoinHelper.shared.getCollectionDetailPresenter(collectionId: collectionId)
        self.presenter = presenter

        self.closeable = presenter.iosState.watch { [weak self] state in
            guard let self = self, let state = state else { return }
            self.collection = state.collection
            self.photos = state.photos
            self.related = state.related
            self.isLoadingPhotos = state.isLoadingPhotos
            self.isHeaderLoading = state.isHeaderLoading
            self.error = state.error
            self.hasReachedEnd = state.hasReachedEnd
        }
    }

    func loadNextPhotosPage() {
        presenter.loadNextPhotosPage()
    }

    func trackDownload(photo: Photo) {
        presenter.trackDownload(photo: photo)
    }

    deinit {
        closeable?.close()
    }
}
