import shared
import SwiftUI

@Observable
class PhotoDetailsViewModel {
    var photo: Photo?
    var stats: PhotoStats?
    var isLoading = false
    var error: String?

    private let presenter: PhotoDetailsPresenter
    private var closeable: Closeable?

    init(photoId: String) {
        let presenter = MetroHelper.shared.getPhotoDetailsPresenter(photoId: photoId)
        self.presenter = presenter

        self.closeable = presenter.iosState.watch { [weak self] state in
            guard let self, let state else { return }
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
