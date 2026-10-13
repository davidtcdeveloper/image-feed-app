import shared
import SwiftUI

@Observable
class UserProfileViewModel {
    var user: User?
    var activeTab: ProfileTab = .portfolio
    var portfolioPhotos: [Photo] = []
    var likedPhotos: [Photo] = []
    var collections: [PhotoCollection] = []
    var stats: UserStats?
    var isLoadingContent = false
    var isHeaderLoading = false
    var isLoadingStats = false
    var error: String?

    private let viewModel: shared.UserProfileViewModel
    private var stateCloseable: Closeable?
    private var effectsCloseable: Closeable?

    init(username: String) {
        let viewModel = MetroHelper.shared.getUserProfileViewModel(username: username)
        self.viewModel = viewModel

        self.stateCloseable = viewModel.iosState.watch { [weak self] state in
            guard let self, let state else { return }
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

        self.effectsCloseable = viewModel.iosEffects.watch { [weak self] effect in
            guard let self, let effect else { return }
            handleEffect(effect)
        }
    }

    private func handleEffect(_ effect: UserProfileEffect) {
        switch effect {
        case let showError as UserProfileEffectShowError:
            error = showError.message
        default:
            break
        }
    }

    func selectTab(tab: ProfileTab) {
        viewModel.dispatch(intent: UserProfileIntentSelectTab(tab: tab))
    }

    func loadNextPage() {
        viewModel.dispatch(intent: UserProfileIntentLoadNextPage.shared)
    }

    func refresh() {
        viewModel.dispatch(intent: UserProfileIntentRefresh.shared)
    }

    func trackDownload(photo: Photo) {
        viewModel.dispatch(intent: UserProfileIntentTrackDownload(photo: photo))
    }

    deinit {
        stateCloseable?.close()
        effectsCloseable?.close()
        viewModel.clear()
    }
}
