import shared
import SwiftUI

@MainActor
@Observable
final class CollectionDetailViewModel {
    var collection: PhotoCollection?
    var photos: [Photo] = []
    var related: [PhotoCollection] = []
    var isLoadingPhotos = false
    var isHeaderLoading = false
    var error: String?
    var hasReachedEnd = false

    private let viewModel: shared.CollectionDetailViewModel
    @ObservationIgnored private nonisolated(unsafe) var stateCloseable: Closeable?
    @ObservationIgnored private nonisolated(unsafe) var effectsCloseable: Closeable?
    @ObservationIgnored private var cachedBuckets: [Int: [[AdaptiveLayoutHelper.IndexedItem<Photo>]]] = [:]

    init(collectionId: String) {
        let viewModel = MetroHelper.shared.getCollectionDetailViewModel(collectionId: collectionId)
        self.viewModel = viewModel

        self.stateCloseable = viewModel.iosState.watch { [weak self] state in
            MainActor.assumeIsolated {
                guard let self, let state else { return }
                if self.collection != state.collection {
                    self.collection = state.collection
                }
                if self.photos != state.photos {
                    self.photos = state.photos
                    self.cachedBuckets.removeAll()
                }
                if self.related != state.related {
                    self.related = state.related
                }
                if self.isLoadingPhotos != state.isLoadingPhotos {
                    self.isLoadingPhotos = state.isLoadingPhotos
                }
                if self.isHeaderLoading != state.isHeaderLoading {
                    self.isHeaderLoading = state.isHeaderLoading
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
    }
}
