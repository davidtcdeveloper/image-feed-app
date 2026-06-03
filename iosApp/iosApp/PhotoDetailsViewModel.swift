import SwiftUI
import shared

@Observable
class PhotoDetailsViewModel {
    var photo: Photo? = nil
    var stats: PhotoStats? = nil
    var isLoading = false
    var error: String? = nil

    private let presenter: PhotoDetailsPresenter
    private var closeable: Closeable?

    init(photoId: String) {
        let presenter = KoinHelper.shared.getPhotoDetailsPresenter(photoId: photoId)
        self.presenter = presenter

        self.closeable = presenter.iosState.watch { [weak self] state in
            guard let self = self, let state = state else { return }
            self.photo = state.photo
            self.stats = state.stats
            self.isLoading = state.isLoading
            self.error = state.error
        }
    }

    func loadDetails() {
        presenter.loadDetails()
    }

    func trackDownload() {
        presenter.trackDownload()
    }

    deinit {
        closeable?.close()
    }
}
