import shared
import SwiftUI

@Observable
class SearchViewModel {
    var query = ""
    var activeTab = SearchTab.photos
    var filters = SearchFilters()
    var photos: [Photo] = []
    var collections: [CollectionSummary] = []
    var users: [User] = []
    var isLoading = false
    var isLoadingMore = false
    var error: String?
    var searchHistory: [String] = []
    var hasReachedEnd = false

    private let presenter: UnifiedSearchPresenter
    private var closeable: Closeable?

    init(initialQuery: String = "") {
        let presenter = KoinHelper.shared.getUnifiedSearchPresenter()
        self.presenter = presenter

        if !initialQuery.isEmpty {
            presenter.updateQuery(newQuery: initialQuery)
        }

        self.closeable = presenter.iosState.watch { [weak self] state in
            guard let self, let state else { return }
            self.query = state.query
            self.activeTab = state.activeTab
            self.filters = state.filters
            self.photos = state.photos
            self.collections = state.collections
            self.users = state.users
            self.isLoading = state.isLoading
            self.isLoadingMore = state.isLoadingMore
            self.error = state.error
            self.searchHistory = state.searchHistory
            self.hasReachedEnd = state.hasReachedEnd
        }
    }

    func updateQuery(newQuery: String) {
        presenter.updateQuery(newQuery: newQuery)
    }

    func setTab(tab: SearchTab) {
        presenter.setTab(tab: tab)
    }

    func applyFilters(orderBy: String, color: String?, orientation: String?) {
        let newFilters = SearchFilters(
            orderBy: orderBy,
            color: color,
            orientation: orientation,
            contentFilter: "low")
        presenter.applyFilters(filters: newFilters)
    }

    func loadNextPage() {
        presenter.loadNextPage()
    }

    func removeHistoryEntry(entry: String) {
        presenter.removeHistoryEntry(entry: entry)
    }

    func clearHistory() {
        presenter.clearHistory()
    }

    deinit {
        closeable?.close()
    }
}
