import shared
import SwiftUI

@MainActor
@Observable
final class FavoritesViewModel {
    var photos: [Photo] = []
    var isLoading = true
    var toastBanner: ToastBanner?

    private let viewModel: shared.FavoritesViewModel?
    @ObservationIgnored private nonisolated(unsafe) var stateCloseable: Closeable?
    @ObservationIgnored private nonisolated(unsafe) var effectsCloseable: Closeable?
    @ObservationIgnored private nonisolated(unsafe) var toastDismissTask: Task<Void, Never>?
    @ObservationIgnored private var cachedBuckets: [Int: [[AdaptiveLayoutHelper.IndexedItem<Photo>]]] = [:]

    #if DEBUG
    init(
        previewPhotos: [Photo],
        isLoading: Bool = false,
        toastBanner: ToastBanner? = nil)
    {
        self.viewModel = nil
        self.photos = previewPhotos
        self.isLoading = isLoading
        self.toastBanner = toastBanner
    }
    #endif

    init() {
        let viewModel = MetroHelper.shared.getFavoritesViewModel()
        self.viewModel = viewModel

        self.stateCloseable = viewModel.iosState.watch { [weak self] state in
            MainActor.assumeIsolated {
                guard let self, let state else { return }
                if self.photos != state.photos {
                    self.photos = state.photos
                    self.cachedBuckets.removeAll()
                }
                if self.isLoading != state.isLoading {
                    self.isLoading = state.isLoading
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

    private func handleEffect(_ effect: FavoritesEffect) {
        switch effect {
        case is FavoritesEffectShowFavoriteRemovedSnackbar:
            showToast(ToastBanner(
                message: String(localized: "Removed from bookmarks"),
                systemImage: "bookmark.slash"))
        case let snackbar as FavoritesEffectShowErrorSnackbar:
            showToast(ToastBanner(
                message: snackbar.error.localizedMessage,
                systemImage: "exclamationmark.triangle.fill"))
        default:
            break
        }
    }

    private func showToast(_ banner: ToastBanner) {
        toastDismissTask?.cancel()
        withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
            toastBanner = banner
        }
        toastDismissTask = Task { [weak self] in
            try? await Task.sleep(for: .seconds(2))
            guard !Task.isCancelled else { return }
            withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                self?.toastBanner = nil
            }
        }
    }

    func removeFavorite(photoId: String) {
        viewModel?.dispatch(intent: FavoritesIntentRemoveFavorite(photoId: photoId))
    }

    func columnItems(for columnIndex: Int, totalColumns: Int) -> [AdaptiveLayoutHelper.IndexedItem<Photo>] {
        guard totalColumns > 0 else { return [] }
        if let buckets = cachedBuckets[totalColumns], columnIndex < buckets.count {
            return buckets[columnIndex]
        }
        let buckets = AdaptiveLayoutHelper.partitionPhotosIntoColumns(from: photos, totalColumns: totalColumns)
        cachedBuckets[totalColumns] = buckets
        return columnIndex < buckets.count ? buckets[columnIndex] : []
    }

    deinit {
        toastDismissTask?.cancel()
        stateCloseable?.close()
        effectsCloseable?.close()
        viewModel?.clear()
    }
}
