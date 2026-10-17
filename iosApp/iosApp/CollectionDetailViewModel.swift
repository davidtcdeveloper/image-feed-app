import shared
import SwiftUI

@Observable
class CollectionDetailViewModel {
    var collection: PhotoCollection?
    var photos: [Photo] = []
    var related: [PhotoCollection] = []
    var isLoadingPhotos = false
    var isHeaderLoading = false
    var error: String?
    var hasReachedEnd = false

    private let viewModel: shared.CollectionDetailViewModel
    private var stateCloseable: Closeable?
    private var effectsCloseable: Closeable?

    init(collectionId: String) {
        let viewModel = MetroHelper.shared.getCollectionDetailViewModel(collectionId: collectionId)
        self.viewModel = viewModel

        self.stateCloseable = viewModel.iosState.watch { [weak self] state in
            guard let self, let state else { return }
            self.collection = state.collection
            self.photos = state.photos
            self.related = state.related
            self.isLoadingPhotos = state.isLoadingPhotos
            self.isHeaderLoading = state.isHeaderLoading
            self.error = state.error?.localizedMessage
            self.hasReachedEnd = state.hasReachedEnd
        }

        self.effectsCloseable = viewModel.iosEffects.watch { [weak self] effect in
            guard let self, let effect else { return }
            handleEffect(effect)
        }
    }

    private func handleEffect(_ effect: CollectionDetailEffect) {
        switch effect {
        case let showError as CollectionDetailEffectShowError:
            error = showError.error.localizedMessage
        default:
            break
        }
    }

    func loadNextPhotosPage() {
        viewModel.dispatch(intent: CollectionDetailIntentLoadNextPage.shared)
    }

    func refresh() {
        viewModel.dispatch(intent: CollectionDetailIntentRefresh.shared)
    }

    func trackDownload(photo: Photo) {
        viewModel.dispatch(intent: CollectionDetailIntentTrackDownload(photo: photo))
    }

    deinit {
        stateCloseable?.close()
        effectsCloseable?.close()
        viewModel.clear()
    }
}
