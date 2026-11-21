import shared
import SwiftUI

struct ToastBanner: Equatable {
    let message: String
    let systemImage: String
}

@MainActor
@Observable
final class PhotoDetailsViewModel {
    var photo: Photo?
    var stats: PhotoStats?
    var isFavorite = false
    var isLoading = false
    var error: String?
    var shouldDismiss = false
    var favoriteFeedbackTrigger = 0
    var favoriteToastBanner: ToastBanner?

    private let viewModel: shared.PhotoDetailsViewModel
    @ObservationIgnored private nonisolated(unsafe) var stateCloseable: Closeable?
    @ObservationIgnored private nonisolated(unsafe) var effectsCloseable: Closeable?
    @ObservationIgnored private nonisolated(unsafe) var toastDismissTask: Task<Void, Never>?

    init(photoId: String) {
        let viewModel = MetroHelper.shared.getPhotoDetailsViewModel(photoId: photoId)
        self.viewModel = viewModel

        self.stateCloseable = viewModel.iosState.watch { [weak self] state in
            MainActor.assumeIsolated {
                guard let self, let state else { return }
                if self.photo != state.photo {
                    self.photo = state.photo
                }
                if self.stats != state.stats {
                    self.stats = state.stats
                }
                if self.isFavorite != state.isFavorite {
                    self.isFavorite = state.isFavorite
                }
                if self.isLoading != state.isLoading {
                    self.isLoading = state.isLoading
                }
                let errorMessage = state.error?.localizedMessage
                if self.error != errorMessage {
                    self.error = errorMessage
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

    private func handleEffect(_ effect: PhotoDetailsEffect) {
        switch effect {
        case is PhotoDetailsEffectDismissScreen:
            shouldDismiss = true
        case let toggled as PhotoDetailsEffectShowFavoriteToggledSnackbar:
            favoriteFeedbackTrigger += 1
            let banner = ToastBanner(
                message: toggled.isFavorite
                    ? String(localized: "Saved to bookmarks")
                    : String(localized: "Removed from bookmarks"),
                systemImage: toggled.isFavorite ? "bookmark.fill" : "bookmark.slash")
            showToast(banner)
        case let snackbar as PhotoDetailsEffectShowErrorSnackbar:
            let localized = snackbar.error.localizedMessage
            error = localized
            if photo != nil {
                showToast(ToastBanner(message: localized, systemImage: "exclamationmark.triangle.fill"))
            }
        default:
            break
        }
    }

    private func showToast(_ banner: ToastBanner) {
        toastDismissTask?.cancel()
        withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
            favoriteToastBanner = banner
        }
        toastDismissTask = Task { [weak self] in
            try? await Task.sleep(for: .seconds(2))
            guard !Task.isCancelled else { return }
            withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                self?.favoriteToastBanner = nil
            }
        }
    }

    func send(_ intent: PhotoDetailsIntent) {
        viewModel.dispatch(intent: intent)
    }

    func loadDetails() {
        viewModel.dispatch(intent: PhotoDetailsIntentLoadDetails.shared)
    }

    func retry() {
        viewModel.dispatch(intent: PhotoDetailsIntentRetry.shared)
    }

    func trackDownload() {
        if let photo {
            viewModel.dispatch(intent: PhotoDetailsIntentTrackDownload(photo: photo))
        }
    }

    func toggleFavorite(fallbackPhoto: Photo? = nil) {
        viewModel.dispatch(intent: PhotoDetailsIntentToggleFavorite(fallbackPhoto: fallbackPhoto))
    }

    deinit {
        toastDismissTask?.cancel()
        stateCloseable?.close()
        effectsCloseable?.close()
        viewModel.clear()
    }
}
