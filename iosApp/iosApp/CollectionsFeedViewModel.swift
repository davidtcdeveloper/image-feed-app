import shared
import SwiftUI

@Observable
class CollectionsFeedViewModel {
    var collections: [PhotoCollection] = []
    var isLoading = false
    var isRefreshing = false
    var error: String?
    var hasReachedEnd = false

    private let viewModel: shared.CollectionsFeedViewModel
    private var stateCloseable: Closeable?
    private var effectsCloseable: Closeable?
    @ObservationIgnored private var cachedBuckets: [Int: [[AdaptiveLayoutHelper.IndexedItem<PhotoCollection>]]] = [:]

    init() {
        let viewModel = MetroHelper.shared.getCollectionsFeedViewModel()
        self.viewModel = viewModel

        self.stateCloseable = viewModel.iosState.watch { [weak self] state in
            guard let self, let state else { return }
            if collections != state.collections {
                self.collections = state.collections
                cachedBuckets.removeAll()
            }
            if isLoading != state.isLoading {
                self.isLoading = state.isLoading
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

    private func handleEffect(_ effect: CollectionsFeedEffect) {
        switch effect {
        case let showError as CollectionsFeedEffectShowError:
            error = showError.error.localizedMessage
        default:
            break
        }
    }

    func loadNextPage() {
        viewModel.dispatch(intent: CollectionsFeedIntentLoadNextPage.shared)
    }

    func refresh() {
        viewModel.dispatch(intent: CollectionsFeedIntentRefresh.shared)
    }

    func columnItems(for columnIndex: Int, totalColumns: Int) -> [AdaptiveLayoutHelper.IndexedItem<PhotoCollection>] {
        guard totalColumns > 0 else { return [] }
        if let buckets = cachedBuckets[totalColumns], columnIndex < buckets.count {
            return buckets[columnIndex]
        }
        let buckets = AdaptiveLayoutHelper.partitionCollectionsIntoColumns(from: collections, totalColumns: totalColumns)
        cachedBuckets[totalColumns] = buckets
        return columnIndex < buckets.count ? buckets[columnIndex] : []
    }

    deinit {
        stateCloseable?.close()
        effectsCloseable?.close()
        viewModel.clear()
    }
}
