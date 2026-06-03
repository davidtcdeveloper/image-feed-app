import SwiftUI
import shared

@Observable
class UserProfileViewModel {
    var user: User? = nil
    var activeTab: ProfileTab = .portfolio
    var portfolioPhotos: [Photo] = []
    var likedPhotos: [Photo] = []
    var collections: [PhotoCollection] = []
    var stats: UserStats? = nil
    var isLoadingContent = false
    var isHeaderLoading = false
    var isLoadingStats = false
    var error: String? = nil

    private let presenter: UserProfilePresenter
    private var closeable: Closeable?

    init(username: String) {
        let presenter = KoinHelper.shared.getUserProfilePresenter(username: username)
        self.presenter = presenter

        self.closeable = presenter.iosState.watch { [weak self] state in
            guard let self = self else { return }
            self.user = state.user
            self.activeTab = state.activeTab
            self.portfolioPhotos = state.portfolioPhotos
            self.likedPhotos = state.likedPhotos
            self.collections = state.collections
            self.stats = state.stats
            self.isLoadingContent = state.isLoadingContent
            self.isHeaderLoading = state.isHeaderLoading
            self.isLoadingStats = state.isLoadingStats
            self.error = state.error
        }
    }

    func selectTab(tab: ProfileTab) {
        presenter.selectTab(tab: tab)
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

    deinit {
        closeable?.close()
    }
}
