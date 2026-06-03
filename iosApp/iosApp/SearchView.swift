import SwiftUI
import shared
import Kingfisher

struct SearchView: View {
    @State private var viewModel = SearchViewModel()
    @State private var showFilters = false
    @State private var searchText = ""
    @FocusState private var isSearchFocused: Bool

    let onPhotoSelect: (String) -> Void
    let onUserSelect: (String) -> Void

    var leftColumnPhotos: [Photo] {
        viewModel.photos.enumerated().filter { $0.offset % 2 == 0 }.map { $0.element }
    }
    
    var rightColumnPhotos: [Photo] {
        viewModel.photos.enumerated().filter { $0.offset % 2 != 0 }.map { $0.element }
    }

    var body: some View {
        ZStack {
            Color(hex: "0F0F11")
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // Search Tab Picker
                Picker("Tab", selection: Binding(
                    get: { viewModel.activeTab },
                    set: { viewModel.setTab(tab: $0) }
                )) {
                    Text("Photos").tag(SearchTab.photos)
                    Text("Collections").tag(SearchTab.collections)
                    Text("Users").tag(SearchTab.users)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(Color(hex: "0F0F11"))

                if searchText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    // Suggestions & History View
                    SearchSuggestionsView(
                        history: viewModel.searchHistory,
                        onSelect: { text in
                            searchText = text
                            viewModel.updateQuery(newQuery: text)
                        },
                        onDelete: { text in
                            viewModel.removeHistoryEntry(entry: text)
                        },
                        onClearAll: {
                            viewModel.clearHistory()
                        }
                    )
                } else if viewModel.isLoading {
                    Spacer()
                    ProgressView()
                        .tint(.white)
                    Spacer()
                } else if let error = viewModel.error {
                    Spacer()
                    VStack(spacing: 8) {
                        Text("Search Failed")
                            .font(.headline)
                            .foregroundColor(.white)
                        Text(error)
                            .font(.subheadline)
                            .foregroundColor(.gray)
                    }
                    Spacer()
                } else {
                    // Search Results depending on Active Tab
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            switch viewModel.activeTab {
                            case .photos:
                                if viewModel.photos.isEmpty {
                                    NoSearchResultsView()
                                } else {
                                    HStack(alignment: .top, spacing: 8) {
                                        LazyVStack(spacing: 8) {
                                            ForEach(leftColumnPhotos, id: \.id) { photo in
                                                KFImage(URL(string: photo.urls.small))
                                                    .resizable()
                                                    .aspectRatio(CGFloat(photo.width) / CGFloat(photo.height), contentMode: .fit)
                                                    .cornerRadius(12)
                                                    .contentShape(Rectangle())
                                                    .onTapGesture {
                                                        onPhotoSelect(photo.id)
                                                    }
                                            }
                                        }
                                        LazyVStack(spacing: 8) {
                                            ForEach(rightColumnPhotos, id: \.id) { photo in
                                                KFImage(URL(string: photo.urls.small))
                                                    .resizable()
                                                    .aspectRatio(CGFloat(photo.width) / CGFloat(photo.height), contentMode: .fit)
                                                    .cornerRadius(12)
                                                    .contentShape(Rectangle())
                                                    .onTapGesture {
                                                        onPhotoSelect(photo.id)
                                                    }
                                            }
                                        }
                                    }
                                    .padding(.horizontal, 8)
                                    .animation(.spring(), value: viewModel.photos)
                                }

                            case .collections:
                                if viewModel.collections.isEmpty {
                                    NoSearchResultsView()
                                } else {
                                    ForEach(viewModel.collections, id: \.id) { collection in
                                        CollectionCardView(collection: collection)
                                            .onTapGesture {
                                                if let links = collection.links, let url = URL(string: "\(links.html)?utm_source=ImageFeedApp&utm_medium=referral") {
                                                    UIApplication.shared.open(url)
                                                }
                                            }
                                    }
                                    .padding(.horizontal, 16)
                                }

                            case .users:
                                if viewModel.users.isEmpty {
                                    NoSearchResultsView()
                                } else {
                                    ForEach(viewModel.users, id: \.id) { user in
                                        UserCardView(user: user)
                                            .onTapGesture {
                                                onUserSelect(user.username)
                                            }
                                    }
                                    .padding(.horizontal, 16)
                                }
                            default:
                                EmptyView()
                            }

                            if viewModel.isLoadingMore {
                                ProgressView()
                                    .tint(.white)
                                    .padding(.vertical, 16)
                            } else if !viewModel.hasReachedEnd && !searchText.isEmpty {
                                Color.clear
                                    .frame(height: 1)
                                    .onAppear {
                                        viewModel.loadNextPage()
                                    }
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("SEARCH")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showFilters = true }) {
                    Image(systemName: "line.3.horizontal.decrease.circle")
                        .foregroundColor(.white)
                }
            }
        }
        .searchable(text: $searchText, prompt: "Photos, collections, or users")
        .onChange(of: searchText) { _, newValue in
            viewModel.updateQuery(newQuery: newValue)
        }
        .sheet(isPresented: $showFilters) {
            SearchFiltersSheetView(filters: viewModel.filters) { newFilters in
                viewModel.applyFilters(
                    orderBy: newFilters.orderBy,
                    color: newFilters.color,
                    orientation: newFilters.orientation
                )
            }
            .presentationDetents([.medium])
            .presentationDragIndicator(.visible)
        }
    }
}

struct SearchSuggestionsView: View {
    let history: [String]
    let onSelect: (String) -> Void
    let onDelete: (String) -> Void
    let onClearAll: () -> Void

    let popularTopics = ["nature", "travel", "architecture", "wallpapers", "neon", "minimalist", "urban", "textures"]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                if !history.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text("RECENTS")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.gray)
                                .tracking(1)
                            Spacer()
                            Button("CLEAR ALL", action: onClearAll)
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.white.opacity(0.6))
                        }

                        ForEach(history, id: \.self) { item in
                            HStack {
                                Image(systemName: "clock.arrow.circlepath")
                                    .foregroundColor(.gray)
                                    .font(.system(size: 14))
                                Button(item) {
                                    onSelect(item)
                                }
                                .foregroundColor(.white)
                                .font(.system(size: 14))
                                Spacer()
                                Button(action: { onDelete(item) }) {
                                    Image(systemName: "xmark")
                                        .foregroundColor(.gray)
                                        .font(.system(size: 12))
                                }
                            }
                            .padding(.vertical, 8)
                        }
                    }
                }

                VStack(alignment: .leading, spacing: 12) {
                    Text("POPULAR TOPICS")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.gray)
                        .tracking(1)

                    FlowLayout(spacing: 8) {
                        ForEach(popularTopics, id: \.self) { topic in
                            Button(action: { onSelect(topic) }) {
                                Text(topic)
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                    .background(Color(hex: "1E1E24"))
                                    .cornerRadius(18)
                            }
                        }
                    }
                }
            }
            .padding(16)
        }
    }
}

struct CollectionCardView: View {
    let collection: CollectionSummary

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            if let cover = collection.coverPhoto {
                KFImage(URL(string: cover.urls.regular))
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(height: 180)
                    .clipped()
                    .overlay(Color.black.opacity(0.45))
            } else {
                Color(hex: "1E1E24")
                    .frame(height: 180)
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(collection.title.uppercased())
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)
                    .tracking(1)
                
                Text("\(collection.totalPhotos) Photos  ·  Curated by \(collection.user.name)")
                    .font(.system(size: 12))
                    .foregroundColor(.gray)
            }
            .padding(16)
        }
        .cornerRadius(12)
        .clipped()
    }
}

struct UserCardView: View {
    let user: User

    var body: some View {
        HStack(spacing: 16) {
            KFImage(URL(string: user.profileImage.medium))
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: 50, height: 50)
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 2) {
                Text(user.name)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.white)
                Text("@\(user.username)")
                    .font(.system(size: 12))
                    .foregroundColor(.gray)
            }

            Spacer()

            Image(systemName: "arrow.up.right")
                .foregroundColor(.gray)
                .font(.system(size: 14))
        }
        .padding(12)
        .background(Color(hex: "1E1E24"))
        .cornerRadius(12)
    }
}

struct NoSearchResultsView: View {
    var body: some View {
        VStack {
            Spacer().frame(height: 100)
            Text("No results found.")
                .font(.subheadline)
                .foregroundColor(.gray)
        }
    }
}

struct SearchFiltersSheetView: View {
    let originalFilters: SearchFilters
    let onApply: (SearchFilters) -> Void
    @Environment(\.dismiss) private var dismiss

    @State private var orderBy = "relevant"
    @State private var orientation: String? = nil
    @State private var color: String? = nil

    private let colors: [(String, String?)] = [
        ("Any", nil),
        ("B&W", "black_and_white"),
        ("Black", "black"),
        ("White", "white"),
        ("Yellow", "yellow"),
        ("Orange", "orange"),
        ("Red", "red"),
        ("Purple", "purple"),
        ("Magenta", "magenta"),
        ("Green", "green"),
        ("Teal", "teal"),
        ("Blue", "blue")
    ]

    private let colorMap: [String: Color] = [
        "black": .black,
        "white": .white,
        "yellow": .yellow,
        "orange": .orange,
        "red": .red,
        "purple": .purple,
        "magenta": .pink,
        "green": .green,
        "teal": .teal,
        "blue": .blue
    ]

    init(filters: SearchFilters, onApply: @escaping (SearchFilters) -> Void) {
        self.originalFilters = filters
        self.onApply = onApply
        self._orderBy = State(initialValue: filters.orderBy)
        self._orientation = State(initialValue: filters.orientation)
        self._color = State(initialValue: filters.color)
    }

    var body: some View {
        ZStack {
            Color(hex: "1E1E24")
                .ignoresSafeArea()

            VStack(alignment: .leading, spacing: 20) {
                HStack {
                    Text("FILTERS")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.white)
                    Spacer()
                    Button("RESET") {
                        orderBy = "relevant"
                        orientation = nil
                        color = nil
                    }
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(.gray)
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("SORT BY")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.gray)
                        .tracking(1)

                    HStack(spacing: 8) {
                        FilterButtonView(title: "RELEVANT", isSelected: orderBy == "relevant") {
                            orderBy = "relevant"
                        }
                        FilterButtonView(title: "LATEST", isSelected: orderBy == "latest") {
                            orderBy = "latest"
                        }
                    }
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("ORIENTATION")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.gray)
                        .tracking(1)

                    HStack(spacing: 8) {
                        FilterButtonView(title: "ALL", isSelected: orientation == nil) {
                            orientation = nil
                        }
                        FilterButtonView(title: "LANDSCAPE", isSelected: orientation == "landscape") {
                            orientation = "landscape"
                        }
                        FilterButtonView(title: "PORTRAIT", isSelected: orientation == "portrait") {
                            orientation = "portrait"
                        }
                    }
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("COLOR TONE")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.gray)
                        .tracking(1)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(colors, id: \.0) { item in
                                let name = item.0
                                let val = item.1

                                Button(action: {
                                    withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                                        color = val
                                    }
                                }) {
                                    VStack(spacing: 4) {
                                        Circle()
                                            .fill(val == "black_and_white" ? .gray : (colorMap[val ?? ""] ?? Color(hex: "2C2C35")))
                                            .frame(width: 32, height: 32)
                                            .overlay(
                                                Circle()
                                                    .stroke(.white, lineWidth: color == val ? 2 : 0)
                                            )
                                            .scaleEffect(color == val ? 1.15 : 1.0)
                                        Text(name)
                                            .font(.system(size: 10))
                                            .foregroundColor(.gray)
                                    }
                                }
                            }
                        }
                        .padding(.vertical, 4)
                        .padding(.horizontal, 2)
                    }
                }

                Spacer()

                Button(action: {
                    let result = SearchFilters(
                        orderBy: orderBy,
                        color: color,
                        orientation: orientation,
                        contentFilter: "low"
                    )
                    onApply(result)
                    dismiss()
                }) {
                    Text("APPLY FILTERS")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.white)
                        .cornerRadius(8)
                }
            }
            .padding(20)
        }
    }
}

struct FilterButtonView: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(isSelected ? .black : .white)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(isSelected ? .white : Color(hex: "2C2C35"))
                .cornerRadius(16)
        }
    }
}
