import shared
import SwiftUI

// MARK: - Unified Navigation Routes & Hero Continuity

enum AppNavigationDefaults {
    static let zoomTransitionCornerRadius: CGFloat = 12

    static func photoSourceID(_ photoId: String) -> String {
        "photo-\(photoId)"
    }

    static func userSourceID(_ username: String) -> String {
        "user-\(username)"
    }

    @MainActor
    static func push(_ route: AppRoute, onto path: Binding<NavigationPath>) {
        #if os(macOS)
        withAnimation(.easeInOut(duration: 0.28)) {
            path.wrappedValue.append(route)
        }
        #else
        path.wrappedValue.append(route)
        #endif
    }
}

enum AppRoute: Hashable {
    case photo(id: String)
    case collection(id: String)
    case user(username: String)
    case search(query: String)
}

// MARK: - Hero Namespace Environment

private struct HeroNamespaceKey: EnvironmentKey {
    static let defaultValue: Namespace.ID? = nil
}

extension EnvironmentValues {
    var heroNamespace: Namespace.ID? {
        get { self[HeroNamespaceKey.self] }
        set { self[HeroNamespaceKey.self] = newValue }
    }
}

// MARK: - Frame-0 Preview Cache Seeding

@MainActor
enum NavigationPreviewSeeder {
    static func seedPhoto(_ photo: Photo) {
        MetroHelper.shared.seedPhotoPreview(photo: photo)
        MetroHelper.shared.seedUserPreview(user: photo.user)
    }

    static func seedCollection(_ collection: PhotoCollection) {
        MetroHelper.shared.seedCollectionPreview(collection: collection)
        MetroHelper.shared.seedUserPreview(user: collection.user)
    }

    static func seedUser(_ user: User) {
        MetroHelper.shared.seedUserPreview(user: user)
    }
}

// MARK: - Unified Navigation Destinations

struct AppNavigationDestinationsModifier: ViewModifier {
    let path: Binding<NavigationPath>
    let heroNamespace: Namespace.ID

    func body(content: Content) -> some View {
        content
            .environment(\.heroNamespace, heroNamespace)
            .navigationDestination(for: AppRoute.self) { route in
                destinationView(for: route)
                    #if os(iOS)
                    .toolbar(.hidden, for: .tabBar)
                    #endif
            }
    }

    @ViewBuilder
    private func destinationView(for route: AppRoute) -> some View {
        switch route {
        case .photo(let id):
            photoDestination(photoId: id)
        case .user(let username):
            userDestination(username: username)
        case .collection(let id):
            CollectionDetailView(
                collectionId: id,
                onPhotoSelect: { photoId in
                    push(.photo(id: photoId))
                },
                onCollectionSelect: { relId in
                    push(.collection(id: relId))
                },
                onUserSelect: { username in
                    push(.user(username: username))
                })
                .environment(\.heroNamespace, heroNamespace)
        case .search(let query):
            SearchView(
                initialQuery: query,
                onPhotoSelect: { photoId in
                    push(.photo(id: photoId))
                },
                onUserSelect: { username in
                    push(.user(username: username))
                },
                onCollectionSelect: { colId in
                    push(.collection(id: colId))
                })
                .environment(\.heroNamespace, heroNamespace)
        }
    }

    @ViewBuilder
    private func photoDestination(photoId: String) -> some View {
        let details = PhotoDetailsView(
            photoId: photoId,
            initialPhoto: MetroHelper.shared.getPhotoPreview(photoId: photoId),
            heroNamespace: heroNamespace,
            onUserSelect: { username in
                push(.user(username: username))
            },
            onTagSelect: { tag in
                push(.search(query: tag))
            })
            .environment(\.heroNamespace, heroNamespace)

        #if os(iOS)
        if #available(iOS 18.0, *) {
            details
                .navigationTransition(.zoom(sourceID: AppNavigationDefaults.photoSourceID(photoId), in: heroNamespace))
        } else {
            details
        }
        #else
        details
        #endif
    }

    @ViewBuilder
    private func userDestination(username: String) -> some View {
        let profile = UserProfileView(
            username: username,
            onPhotoSelect: { photoId in
                push(.photo(id: photoId))
            },
            onCollectionSelect: { colId in
                push(.collection(id: colId))
            })
            .environment(\.heroNamespace, heroNamespace)

        #if os(iOS)
        if #available(iOS 18.0, *) {
            profile
                .navigationTransition(.zoom(sourceID: AppNavigationDefaults.userSourceID(username), in: heroNamespace))
        } else {
            profile
        }
        #else
        profile
        #endif
    }

    private func push(_ route: AppRoute) {
        #if os(macOS)
        withAnimation(.easeInOut(duration: 0.28)) {
            path.wrappedValue.append(route)
        }
        #else
        path.wrappedValue.append(route)
        #endif
    }
}

extension View {
    func appNavigationDestinations(path: Binding<NavigationPath>, heroNamespace: Namespace.ID) -> some View {
        modifier(AppNavigationDestinationsModifier(path: path, heroNamespace: heroNamespace))
    }
}
