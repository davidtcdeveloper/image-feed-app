import shared
import SwiftUI

struct SearchView: View {
    #if os(iOS)
    @Environment(\.horizontalSizeClass) var sizeClass
    #endif
    @State private var viewModel: SearchViewModel
    @State private var showFilters = false
    @State private var searchText: String
    @FocusState private var isSearchFocused: Bool

    let onPhotoSelect: (String) -> Void
    let onUserSelect: (String) -> Void
    let onCollectionSelect: (String) -> Void

    init(
        initialQuery: String = "",
        onPhotoSelect: @escaping (String) -> Void,
        onUserSelect: @escaping (String) -> Void,
        onCollectionSelect: @escaping (String) -> Void)
    {
        self.onPhotoSelect = onPhotoSelect
        self.onUserSelect = onUserSelect
        self.onCollectionSelect = onCollectionSelect
        self._searchText = State(initialValue: initialQuery)
        self._viewModel = State(initialValue: SearchViewModel(initialQuery: initialQuery))
    }

    private var columnCount: Int {
        #if os(iOS)
        return AdaptiveLayoutHelper.getColumnCount(sizeClass: sizeClass)
        #else
        return AdaptiveLayoutHelper.getColumnCount()
        #endif
    }

    var body: some View {
        ZStack {
            GlassTheme.canvasBackgroundColor
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // Search Tab Picker
                Picker("Tab", selection: Binding(
                    get: { viewModel.activeTab },
                    set: { viewModel.setTab(tab: $0) }))
                {
                    Text("Photos").tag(SearchTab.photos)
                    Text("Collections").tag(SearchTab.collections)
                    Text("Users").tag(SearchTab.users)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(.ultraThinMaterial)

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
                        })
                } else if viewModel.isLoading {
                    ScrollView {
                        VStack(spacing: 16) {
                            switch viewModel.activeTab {
                            case .photos:
                                HStack(alignment: .top, spacing: 8) {
                                    ForEach(0..<columnCount, id: \.self) { _ in
                                        VStack(spacing: 8) {
                                            ForEach(0..<3, id: \.self) { index in
                                                PhotoCardSkeleton(height: index % 2 == 0 ? 180 : 240)
                                            }
                                        }
                                    }
                                }
                                .padding(.horizontal, 8)
                            case .collections:
                                VStack(spacing: 16) {
                                    ForEach(0..<3, id: \.self) { _ in
                                        CollectionMosaicCardSkeleton()
                                    }
                                }
                                .padding(.horizontal, 16)
                            case .users:
                                VStack(spacing: 12) {
                                    ForEach(0..<4, id: \.self) { _ in
                                        HStack(spacing: 16) {
                                            Circle()
                                                .fill(Color.white.opacity(0.06))
                                                .frame(width: 50, height: 50)
                                            VStack(alignment: .leading, spacing: 4) {
                                                RoundedRectangle(cornerRadius: 4)
                                                    .fill(Color.white.opacity(0.06))
                                                    .frame(width: 120, height: 14)
                                                RoundedRectangle(cornerRadius: 4)
                                                    .fill(Color.white.opacity(0.06))
                                                    .frame(width: 80, height: 10)
                                            }
                                            Spacer()
                                        }
                                        .padding(12)
                                        .background(GlassTheme.surfaceBackgroundColor)
                                        .cornerRadius(12)
                                        .shimmer()
                                    }
                                }
                                .padding(.horizontal, 16)
                            default:
                                EmptyView()
                            }
                        }
                        .padding(.top, 10)
                    }
                } else if let error = viewModel.error {
                    Spacer()
                    VStack(spacing: 8) {
                        Text("Search Failed")
                            .font(.headline)
                            .glassVibrancy(.primary)
                        Text(error)
                            .font(.subheadline)
                            .glassVibrancy(.secondary)
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
                                        ForEach(0..<columnCount, id: \.self) { colIndex in
                                            LazyVStack(spacing: 8) {
                                                ForEach(
                                                    AdaptiveLayoutHelper.photosForColumn(index: colIndex, totalColumns: columnCount, from: viewModel.photos),
                                                    id: \.id)
                                                { photo in
                                                    let flatIndex = viewModel.photos.firstIndex(where: { $0.id == photo.id }) ?? 0
                                                    SearchPhotoGridCard(
                                                        photo: photo,
                                                        columnCount: columnCount,
                                                        onPhotoSelect: { onPhotoSelect(photo.id) },
                                                        onUserSelect: onUserSelect)
                                                        .staggeredReveal(index: flatIndex)
                                                        .feedScrollTransition()
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
                                    ForEach(Array(viewModel.collections.enumerated()), id: \.element.id) { index, collection in
                                        Button(action: {
                                            onCollectionSelect(collection.id)
                                        }) {
                                            CollectionCardView(collection: collection)
                                                .staggeredReveal(index: index)
                                                .feedScrollTransition()
                                        }
                                        .buttonStyle(SpringCardButtonStyle())
                                    }
                                    .padding(.horizontal, 16)
                                }

                            case .users:
                                if viewModel.users.isEmpty {
                                    NoSearchResultsView()
                                } else {
                                    ForEach(Array(viewModel.users.enumerated()), id: \.element.id) { index, user in
                                        Button(action: {
                                            onUserSelect(user.username)
                                        }) {
                                            UserCardView(user: user)
                                                .staggeredReveal(index: index)
                                                .feedScrollTransition()
                                        }
                                        .buttonStyle(SpringCardButtonStyle())
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
                            } else if !viewModel.hasReachedEnd, !searchText.isEmpty {
                                Color.clear
                                    .frame(height: 1)
                                    .onAppear {
                                        viewModel.loadNextPage()
                                    }
                            }
                        }
                    }
                    .refreshable {
                        viewModel.refresh()
                    }
                }
            }
        }
        .navigationTitle("SEARCH")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        #endif
        .toolbar {
            #if os(iOS)
            ToolbarItem(placement: .navigationBarTrailing) {
                GlassToolbarButton(systemName: "line.3.horizontal.decrease", accessibilityLabel: "Filter") {
                    showFilters = true
                }
            }
            #else
            ToolbarItem(placement: .primaryAction) {
                GlassToolbarButton(systemName: "line.3.horizontal.decrease", accessibilityLabel: "Filter") {
                    showFilters = true
                }
            }
            #endif
        }
        .searchable(text: $searchText, prompt: "Photos, collections, or users")
        .onSubmit(of: .search) {
            viewModel.submitSearch(query: searchText)
        }
        .onChange(of: searchText) { _, newValue in
            viewModel.updateQuery(newQuery: newValue)
        }
        .sheet(isPresented: $showFilters) {
            SearchFiltersSheetView(filters: viewModel.filters) { newFilters in
                viewModel.applyFilters(
                    orderBy: newFilters.orderBy,
                    color: newFilters.color,
                    orientation: newFilters.orientation)
            }
            .presentationDetents([.medium])
            .presentationDragIndicator(.visible)
        }
    }
}

struct SearchPhotoGridCard: View {
    let photo: Photo
    let columnCount: Int
    let onPhotoSelect: () -> Void
    let onUserSelect: (String) -> Void

    var body: some View {
        let screenWidth = AdaptiveLayoutHelper.getScreenWidth()
        let itemWidth = AdaptiveLayoutHelper.calculateItemWidthPx(screenWidth: screenWidth, columnCount: columnCount)
        let imageUrl = photo.urls.raw + "&w=\(itemWidth)&q=80&auto=format"
        let aspectRatio = photo.height > 0 ? CGFloat(photo.width) / CGFloat(photo.height) : 1.0

        ZStack(alignment: .bottomLeading) {
            KFImage(URL(string: imageUrl))
                .placeholder {
                    ZStack {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color(hex: photo.color ?? "1E1E24"))
                        RoundedRectangle(cornerRadius: 12)
                            .fill(.ultraThinMaterial)
                        ProgressView()
                            .tint(.primary.opacity(0.6))
                    }
                    .aspectRatio(aspectRatio, contentMode: .fit)
                }
                .resizable()
                .aspectRatio(aspectRatio, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .contentShape(Rectangle())
                .onTapGesture {
                    onPhotoSelect()
                }

            // Inset Floating Glass Attribution Capsule
            Button {
                onUserSelect(photo.user.username)
            } label: {
                HStack(spacing: 6) {
                    KFImage(URL(string: photo.user.profileImage.small))
                        .resizable()
                        .scaledToFill()
                        .frame(width: 20, height: 20)
                        .clipShape(Circle())

                    Text(photo.user.name)
                        .font(.caption2.weight(.semibold))
                        .glassVibrancy(.primary)
                        .lineLimit(1)
                        .truncationMode(.tail)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 5)
                .glassCapsule(style: .ultraThin, showBorder: true, hasShadow: true)
            }
            .buttonStyle(.plain)
            .contentShape(Capsule())
            .padding(8)
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
                                .font(.caption.weight(.bold))
                                .glassVibrancy(.secondary)
                                .tracking(1)
                            Spacer()
                            Button("CLEAR ALL", action: onClearAll)
                                .font(.caption.weight(.bold))
                                .glassVibrancy(.primary)
                        }

                        ForEach(history, id: \.self) { item in
                            HStack {
                                Image(systemName: "clock.arrow.circlepath")
                                    .glassVibrancy(.secondary)
                                    .font(.subheadline)
                                Button(item) {
                                    onSelect(item)
                                }
                                .glassVibrancy(.primary)
                                .font(.subheadline)
                                Spacer()
                                Button(action: { onDelete(item) }) {
                                    Image(systemName: "xmark")
                                        .glassVibrancy(.secondary)
                                        .font(.caption)
                                }
                            }
                            .padding(.vertical, 8)
                        }
                    }
                }

                VStack(alignment: .leading, spacing: 12) {
                    Text("POPULAR TOPICS")
                        .font(.caption.weight(.bold))
                        .glassVibrancy(.secondary)
                        .tracking(1)

                    FlowLayout(spacing: 8) {
                        ForEach(popularTopics, id: \.self) { topic in
                            Button(action: { onSelect(topic) }) {
                                Text(topic)
                                    .font(.caption.weight(.medium))
                                    .glassVibrancy(.primary)
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                    .glassCapsule(style: .ultraThin, showBorder: true)
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
                GlassTheme.surfaceBackgroundColor
                    .frame(height: 180)
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(collection.title.uppercased())
                    .font(.headline.weight(.bold))
                    .glassVibrancy(.primary)
                    .tracking(1)

                Text("\(collection.totalPhotos) Photos  ·  Curated by \(collection.user.name)")
                    .font(.caption)
                    .glassVibrancy(.secondary)
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
                    .font(.subheadline.weight(.bold))
                    .glassVibrancy(.primary)
                Text("@\(user.username)")
                    .font(.caption)
                    .glassVibrancy(.secondary)
            }

            Spacer()

            Image(systemName: "arrow.up.right")
                .glassVibrancy(.secondary)
                .font(.footnote.weight(.semibold))
        }
        .padding(12)
        .glassCard(cornerRadius: 12, style: .ultraThin)
    }
}

struct NoSearchResultsView: View {
    var body: some View {
        VStack {
            Spacer().frame(height: 100)
            Text("No results found.")
                .font(.subheadline)
                .glassVibrancy(.secondary)
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
        ("Blue", "blue"),
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
        "blue": .blue,
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
            GlassTheme.surfaceBackgroundColor
                .ignoresSafeArea()

            VStack(alignment: .leading, spacing: 20) {
                HStack {
                    Text("FILTERS")
                        .font(.headline.weight(.bold))
                        .glassVibrancy(.primary)
                    Spacer()
                    Button("RESET") {
                        orderBy = "relevant"
                        orientation = nil
                        color = nil
                    }
                    .font(.subheadline.weight(.bold))
                    .glassVibrancy(.secondary)
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("SORT BY")
                        .font(.caption.weight(.bold))
                        .glassVibrancy(.secondary)
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
                        .font(.caption.weight(.bold))
                        .glassVibrancy(.secondary)
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
                        .font(.caption.weight(.bold))
                        .glassVibrancy(.secondary)
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
                                            .fill(val == "black_and_white" ? .gray : (colorMap[val ?? ""] ?? GlassTheme.surfaceBackgroundColor))
                                            .frame(width: 32, height: 32)
                                            .overlay(
                                                Circle()
                                                    .stroke(.white, lineWidth: color == val ? 2 : 0))
                                            .scaleEffect(color == val ? 1.15 : 1.0)
                                        Text(name)
                                            .font(.caption2)
                                            .glassVibrancy(.secondary)
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
                        contentFilter: "low")
                    onApply(result)
                    dismiss()
                }) {
                    Text("APPLY FILTERS")
                        .font(.subheadline.weight(.bold))
                        .glassVibrancy(.primary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .glassBackground(style: .thin, shape: RoundedRectangle(cornerRadius: 12), showBorder: true)
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
                .font(.caption.weight(.bold))
                .foregroundColor(isSelected ? .black : .white)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(isSelected ? Color.white : GlassTheme.surfaceBackgroundColor)
                .clipShape(Capsule())
                .overlay(
                    Capsule()
                        .stroke(isSelected ? Color.clear : GlassTheme.fallbackBorderColor, lineWidth: 0.5))
        }
    }
}
