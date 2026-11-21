import shared
import SwiftUI

struct MacOSContentView: View {
    enum SidebarSelection: Hashable {
        case photos
        case collections
        case favorites
    }

    @State private var selection: SidebarSelection? = .photos

    var body: some View {
        NavigationSplitView {
            List(selection: $selection) {
                NavigationLink(value: SidebarSelection.photos) {
                    Label("Photos", systemImage: "photo.stack")
                }

                NavigationLink(value: SidebarSelection.collections) {
                    Label("Collections", systemImage: "square.grid.2x2")
                }

                NavigationLink(value: SidebarSelection.favorites) {
                    Label("Favorites", systemImage: "bookmark.fill")
                }
            }
            .navigationTitle("Image Feed")
            .listStyle(SidebarListStyle())
            .frame(minWidth: 200)
        } detail: {
            if let selection {
                switch selection {
                case .photos:
                    PhotosFeedTabView()
                        .frame(minWidth: 500)
                case .collections:
                    CollectionsFeedTabView()
                        .frame(minWidth: 500)
                case .favorites:
                    FavoritesTabView()
                        .frame(minWidth: 500)
                }
            } else {
                Text("Select a category from the sidebar")
                    .foregroundStyle(.secondary)
            }
        }
        .frame(minWidth: 800, minHeight: 600)
    }
}
