import shared
import SwiftUI

@Observable
class CollectionsFeedViewModel {
    var collections: [PhotoCollection] = []
    var isLoading = false
    var isRefreshing = false
    var error: String?
    var hasReachedEnd = false

    private let presenter: CollectionsFeedPresenter
    private var closeable: Closeable?

    init() {
        let presenter = KoinHelper.shared.getCollectionsFeedPresenter()
        self.presenter = presenter

        self.closeable = presenter.iosState.watch { [weak self] state in
            guard let self, let state else { return }
            self.collections = state.collections
            self.isLoading = state.isLoading
            self.isRefreshing = state.isRefreshing
            self.error = state.error
            self.hasReachedEnd = state.hasReachedEnd
        }
    }

    func loadNextPage() {
        presenter.loadNextPage()
    }

    func refresh() {
        presenter.refresh()
    }

    deinit {
        closeable?.close()
    }
}
