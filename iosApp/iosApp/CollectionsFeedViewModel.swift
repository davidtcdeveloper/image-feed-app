import shared
import SwiftUI

@MainActor
@Observable
final class CollectionsFeedViewModel {
    var collections: [PhotoCollection] = []
    var isLoading = false
    var isRefreshing = false
    var error: String?
    var hasReachedEnd = false

    private let viewModel: shared.CollectionsFeedViewModel?
    @ObservationIgnored private nonisolated(unsafe) var stateCloseable: Closeable?
    @ObservationIgnored private nonisolated(unsafe) var effectsCloseable: Closeable?
    @ObservationIgnored private var cachedBuckets: [Int: [[AdaptiveLayoutHelper.IndexedItem<PhotoCollection>]]] = [:]

    #if DEBUG
    init(
        previewCollections: [PhotoCollection],
        isLoading: Bool = false,
        error: String? = nil)
    {
        self.viewModel = nil
        self.collections = previewCollections
        self.isLoading = isLoading
        self.error = error
    }
    #endif

    init() {
        let viewModel = MetroHelper.shared.getCollectionsFeedViewModel()
        self.viewModel = viewModel

        self.stateCloseable = viewModel.iosState.watch { [weak self] state in
            MainActor.assumeIsolated {
                guard let self, let state else { return }
                if self.collections != state.collections {
                    self.collections = state.collections
                    self.cachedBuckets.removeAll()
                }
                if self.isLoading != state.isLoading {
                    self.isLoading = state.isLoading
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

    private func handleEffect(_ effect: CollectionsFeedEffect) {
        switch effect {
        case let showError as CollectionsFeedEffectShowError:
            error = showError.error.localizedMessage
        default:
            break
        }
    }

    func loadNextPage() {
        viewModel?.dispatch(intent: CollectionsFeedIntentLoadNextPage.shared)
    }

    func refresh() {
        viewModel?.dispatch(intent: CollectionsFeedIntentRefresh.shared)
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
        viewModel?.clear()
    }
}
