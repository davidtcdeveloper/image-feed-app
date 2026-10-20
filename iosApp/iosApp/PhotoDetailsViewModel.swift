import shared
import SwiftUI

@Observable
class PhotoDetailsViewModel {
    var photo: Photo?
    var stats: PhotoStats?
    var isLoading = false
    var error: String?
    var shouldDismiss = false

    private let viewModel: shared.PhotoDetailsViewModel
    private var stateCloseable: Closeable?
    private var effectsCloseable: Closeable?

    init(photoId: String) {
        let viewModel = MetroHelper.shared.getPhotoDetailsViewModel(photoId: photoId)
        self.viewModel = viewModel

        self.stateCloseable = viewModel.iosState.watch { [weak self] state in
            guard let self, let state else { return }
            if self.photo != state.photo { self.photo = state.photo }
            if self.stats != state.stats { self.stats = state.stats }
            if self.isLoading != state.isLoading { self.isLoading = state.isLoading }
            let errorMessage = state.error?.localizedMessage
            if self.error != errorMessage { self.error = errorMessage }
        }

        self.effectsCloseable = viewModel.iosEffects.watch { [weak self] effect in
            guard let self, let effect else { return }
            handleEffect(effect)
        }
    }

    private func handleEffect(_ effect: PhotoDetailsEffect) {
        switch effect {
        case is PhotoDetailsEffectDismissScreen:
            shouldDismiss = true
        case let snackbar as PhotoDetailsEffectShowErrorSnackbar:
            error = snackbar.error.localizedMessage
        default:
            break
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

    deinit {
        stateCloseable?.close()
        effectsCloseable?.close()
        viewModel.clear()
    }
}
