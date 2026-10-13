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
    var isSearchActive = false
    var isRefreshing = false
    var error: String?
    var searchHistory: [String] = []
    var hasReachedEnd = false

    private let viewModel: shared.UnifiedSearchViewModel
    private var stateCloseable: Closeable?
    private var effectsCloseable: Closeable?

    init(initialQuery: String = "") {
        let viewModel = MetroHelper.shared.getUnifiedSearchViewModel()
        self.viewModel = viewModel

        if !initialQuery.isEmpty {
            viewModel.dispatch(intent: UnifiedSearchIntentUpdateQuery(query: initialQuery))
        }

        self.stateCloseable = viewModel.iosState.watch { [weak self] state in
            guard let self, let state else { return }
            self.query = state.query
            self.isSearchActive = state.isSearchActive
            self.isRefreshing = state.isRefreshing
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

        self.effectsCloseable = viewModel.iosEffects.watch { [weak self] effect in
            guard let self, let effect else { return }
            handleEffect(effect)
        }
    }

    private func handleEffect(_ effect: UnifiedSearchEffect) {
        switch effect {
        case let showError as UnifiedSearchEffectShowError:
            error = showError.message
        default:
            break
        }
    }

    func updateQuery(newQuery: String) {
        viewModel.dispatch(intent: UnifiedSearchIntentUpdateQuery(query: newQuery))
    }

    func setSearchActive(active: Bool) {
        viewModel.dispatch(intent: UnifiedSearchIntentSetSearchActive(active: active))
    }

    func submitSearch(query: String) {
        viewModel.dispatch(intent: UnifiedSearchIntentSubmitSearch(query: query))
    }

    func refresh() {
        viewModel.dispatch(intent: UnifiedSearchIntentRefresh.shared)
    }

    func setTab(tab: SearchTab) {
        viewModel.dispatch(intent: UnifiedSearchIntentSelectSearchType(type: tab))
    }

    func applyFilters(orderBy: String, color: String?, orientation: String?) {
        let newFilters = SearchFilters(
            orderBy: orderBy,
            color: color,
            orientation: orientation,
            contentFilter: "low")
        viewModel.dispatch(intent: UnifiedSearchIntentApplyFilters(filters: newFilters))
    }

    func loadNextPage() {
        viewModel.dispatch(intent: UnifiedSearchIntentLoadNextPage.shared)
    }

    func removeHistoryEntry(entry: String) {
        viewModel.dispatch(intent: UnifiedSearchIntentRemoveHistoryEntry(entry: entry))
    }

    func clearHistory() {
        viewModel.dispatch(intent: UnifiedSearchIntentClearHistory.shared)
    }

    deinit {
        stateCloseable?.close()
        effectsCloseable?.close()
        viewModel.clear()
    }
}
