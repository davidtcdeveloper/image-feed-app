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

    init() {
        let viewModel = MetroHelper.shared.getCollectionsFeedViewModel()
        self.viewModel = viewModel

        self.stateCloseable = viewModel.iosState.watch { [weak self] state in
            guard let self, let state else { return }
            if self.collections != state.collections { self.collections = state.collections }
            if self.isLoading != state.isLoading { self.isLoading = state.isLoading }
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

    deinit {
        stateCloseable?.close()
        effectsCloseable?.close()
        viewModel.clear()
    }
}
