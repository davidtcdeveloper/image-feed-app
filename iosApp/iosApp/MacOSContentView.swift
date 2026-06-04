import SwiftUI
import shared

struct MacOSContentView: View {
    enum SidebarSelection: Hashable {
        case photos
        case collections
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
            }
            .navigationTitle("Image Feed")
            .listStyle(SidebarListStyle())
            .frame(minWidth: 200)
        } detail: {
            if let selection = selection {
                switch selection {
                case .photos:
                    PhotosFeedTabView()
                        .frame(minWidth: 500)
                case .collections:
                    CollectionsFeedTabView()
                        .frame(minWidth: 500)
                }
            } else {
                Text("Select a category from the sidebar")
                    .foregroundColor(.gray)
            }
        }
        .frame(minWidth: 800, minHeight: 600)
    }
}
