import shared
import SwiftUI

struct SearchView: View {
    #if os(iOS)
    @Environment(\.horizontalSizeClass) var sizeClass
    #endif
    @Environment(\.searchFocusTrigger) private var searchFocusTrigger
    @State private var containerWidth: CGFloat = 0
    let initialQuery: String
    @State private var viewModel: SearchViewModel?
    @State private var showFilters = false
    @State private var searchText: String
    @State private var isSearchPresented = false
    @Namespace private var searchTabNamespace

    let onPhotoSelect: (String) -> Void
    let onUserSelect: (String) -> Void
    let onCollectionSelect: (String) -> Void

    init(
        initialQuery: String = "",
        initialViewModel: SearchViewModel? = nil,
        onPhotoSelect: @escaping (String) -> Void,
        onUserSelect: @escaping (String) -> Void,
        onCollectionSelect: @escaping (String) -> Void)
    {
        self.initialQuery = initialQuery
        self.onPhotoSelect = onPhotoSelect
        self.onUserSelect = onUserSelect
        self.onCollectionSelect = onCollectionSelect
        self._searchText = State(initialValue: initialQuery)
        self._viewModel = State(initialValue: initialViewModel)
    }

    private var columnCount: Int {
        #if os(iOS)
        AdaptiveLayoutHelper.getColumnCount(containerWidth: containerWidth, sizeClass: sizeClass)
        #else
        AdaptiveLayoutHelper.getColumnCount(containerWidth: containerWidth)
        #endif
    }

    var body: some View {
        ZStack {
            GlassTheme.canvasBackgroundColor
                .ignoresSafeArea()

            if let viewModel {
                Group {
                    if searchText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        // Suggestions & History View
                        SearchSuggestionsView(
                            history: viewModel.searchHistory,
                            onSelect: { text in
                                searchText = text
                            },
                            onDelete: { text in
                                viewModel.removeHistoryEntry(entry: text)
                            },
                            onClearAll: {
                                viewModel.clearHistory()
                            })
                            .safeAreaInset(edge: .top, spacing: 0) {
                                searchTabBar(viewModel: viewModel)
                            }
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
                                                    .fill(GlassTheme.subtleFillColor)
                                                    .frame(width: 50, height: 50)
                                                VStack(alignment: .leading, spacing: 4) {
                                                    RoundedRectangle(cornerRadius: 4)
                                                        .fill(GlassTheme.subtleFillColor)
                                                        .frame(width: 120, height: 14)
                                                    RoundedRectangle(cornerRadius: 4)
                                                        .fill(GlassTheme.subtleFillColor)
                                                        .frame(width: 80, height: 10)
                                                }
                                                Spacer()
                                            }
                                            .padding(12)
                                            .matteSurface(cornerRadius: 12)
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
                        .satinScrollEdge()
                        .safeAreaInset(edge: .top, spacing: 0) {
                            searchTabBar(viewModel: viewModel)
                        }
                    } else if let error = viewModel.error {
                        VStack(spacing: 0) {
                            searchTabBar(viewModel: viewModel)
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
                        }
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
                                                    ForEach(viewModel.columnItems(for: colIndex, totalColumns: columnCount)) { indexed in
                                                        let photo = indexed.item
                                                        let flatIndex = indexed.index
                                                        PhotoGridCard(
                                                            photo: photo,
                                                            columnCount: columnCount,
                                                            fadeDuration: 0,
                                                            onSelect: { onPhotoSelect(photo.id) },
                                                            onUserSelect: onUserSelect)
                                                            .staggeredReveal(index: flatIndex)
                                                    }
                                                }
                                            }
                                        }
                                        .padding(.horizontal, 8)
                                        .padding(.top, 4)
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
                                        .padding(.top, 4)
                                    }

                                case .users:
                                    if viewModel.users.isEmpty {
                                        NoSearchResultsView()
                                    } else {
                                        ForEach(Array(viewModel.users.enumerated()), id: \.element.id) { index, user in
                                            Button(action: {
                                                NavigationPreviewSeeder.seedUser(user)
                                                onUserSelect(user.username)
                                            }) {
                                                UserCardView(user: user)
                                                    .staggeredReveal(index: index)
                                                    .feedScrollTransition()
                                            }
                                            .buttonStyle(SpringCardButtonStyle())
                                        }
                                        .padding(.horizontal, 16)
                                        .padding(.top, 4)
                                    }

                                default:
                                    EmptyView()
                                }

                                if viewModel.isLoadingMore {
                                    ProgressView()
                                        .tint(.primary)
                                        .padding(.vertical, 16)
                                } else if !viewModel.hasReachedEnd, !searchText.isEmpty {
                                    Color.clear
                                        .frame(height: 1)
                                        .onAppear {
                                            viewModel.loadNextPage()
                                        }
                                }
                            }
                            .padding(.bottom, 32)
                        }
                        .satinScrollEdge()
                        .refreshable {
                            viewModel.refresh()
                        }
                        .safeAreaInset(edge: .top, spacing: 0) {
                            searchTabBar(viewModel: viewModel)
                        }
                    }
                }
                .sensoryFeedback(.selection, trigger: viewModel.activeTab)
            } else {
                ProgressView()
                    .tint(.primary)
            }
        }
        .measureContainerWidth($containerWidth)
        .task(id: initialQuery) {
            if viewModel == nil {
                viewModel = SearchViewModel(initialQuery: initialQuery)
            }
        }
        .navigationTitle("SEARCH")
        #if os(iOS)
        .satinNavigationBar()
        .adaptiveTabBarVisibility()
        #endif
        .toolbar {
            #if os(iOS)
            ToolbarItem(placement: .navigationBarTrailing) {
                PlatformToolbarButton(systemName: "line.3.horizontal.decrease", accessibilityLabel: "Filters") {
                    showFilters = true
                }
            }
            #else
            ToolbarItem(placement: .primaryAction) {
                PlatformToolbarButton(systemName: "line.3.horizontal.decrease", accessibilityLabel: "Filters") {
                    showFilters = true
                }
            }
            #endif
        }
        .searchable(text: $searchText, isPresented: $isSearchPresented, prompt: Text("Search Photos, Collections, Users…"))
        .onAppear {
            if searchFocusTrigger > 0 {
                isSearchPresented = true
            }
        }
        .onChange(of: searchFocusTrigger) { _, newValue in
            if newValue > 0 {
                isSearchPresented = true
            }
        }
        .onSubmit(of: .search) {
            viewModel?.submitSearch(query: searchText)
        }
        .onChange(of: searchText) { _, newValue in
            guard newValue != viewModel?.query else { return }
            viewModel?.updateQuery(newQuery: newValue)
        }
        .sheet(isPresented: $showFilters) {
            if let viewModel {
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

    private func searchTabBar(viewModel: SearchViewModel) -> some View {
        SearchTabBar(
            activeTab: viewModel.activeTab,
            namespace: searchTabNamespace,
            onSelect: { selectedTab in
                withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                    viewModel.setTab(tab: selectedTab)
                }
            })
    }
}

struct SearchTabBar: View {
    let activeTab: SearchTab
    let namespace: Namespace.ID
    let onSelect: (SearchTab) -> Void

    private let tabs: [(tab: SearchTab, title: String)] = [
        (.photos, "Photos"),
        (.collections, "Collections"),
        (.users, "Users"),
    ]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(tabs, id: \.tab) { item in
                    CategoryTabButton(
                        title: item.title,
                        isSelected: activeTab == item.tab,
                        namespace: namespace,
                        action: {
                            onSelect(item.tab)
                        })
                        .id(item.tab)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 5)
            .macOSHorizontalScroll()
        }
        .frame(height: 44)
        .floatGlass(shape: Capsule())
        .padding(.horizontal, 12)
        .padding(.top, 4)
        .padding(.bottom, 6)
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
                                .accessibilityLabel("Clear")
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

                    LazyVGrid(
                        columns: [
                            GridItem(.flexible(), spacing: 10),
                            GridItem(.flexible(), spacing: 10),
                        ],
                        spacing: 10)
                    {
                        ForEach(popularTopics, id: \.self) { topic in
                            SearchTopicCard(topic: topic) {
                                onSelect(topic)
                            }
                        }
                    }
                }
            }
            .padding(16)
        }
        .satinScrollEdge()
    }
}

struct CollectionCardView: View {
    @Environment(\.containerWidth) private var containerWidth
    let collection: CollectionSummary

    var body: some View {
        let widthPt = AdaptiveLayoutHelper.effectiveWidth(containerWidth: containerWidth)
        let itemWidthPx = Int32(widthPt * AdaptiveLayoutHelper.displayScale())
        let coverUrl = collection.coverPhoto?.urls.getOptimalUrl(targetWidthPx: itemWidthPx)

        ZStack(alignment: .bottomLeading) {
            if let coverUrl {
                KFImage(URL(string: coverUrl))
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

                HStack(spacing: 4) {
                    Text("\(collection.totalPhotos) Photos")
                    Text("·")
                    Text("Curated by")
                    Text(collection.user.name)
                }
                .font(.caption)
                .glassVibrancy(.secondary)
            }
            .environment(\.colorScheme, .dark)
            .padding(16)
        }
        .cornerRadius(12)
        .clipped()
    }
}

struct UserCardView: View {
    @Environment(\.heroNamespace) private var heroNamespace
    let user: User

    var body: some View {
        HStack(spacing: 16) {
            userAvatar

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
                .accessibilityLabel("View Profile")
        }
        .padding(12)
        .matteSurface(cornerRadius: 12)
    }

    @ViewBuilder
    private var userAvatar: some View {
        let avatar = KFImage(URL(string: user.profileImage.medium))
            .resizable()
            .aspectRatio(contentMode: .fill)
            .frame(width: 50, height: 50)
            .clipShape(Circle())

        #if os(iOS)
        if #available(iOS 18.0, *), let heroNamespace {
            avatar.matchedTransitionSource(
                id: AppNavigationDefaults.userSourceID(user.username),
                in: heroNamespace)
            { source in
                source.clipShape(RoundedRectangle(cornerRadius: 25, style: .continuous))
            }
        } else {
            avatar
        }
        #else
        avatar
        #endif
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

    private let colors: [(LocalizedStringKey, String?)] = [
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
                            ForEach(0..<colors.count, id: \.self) { idx in
                                let name = colors[idx].0
                                let val = colors[idx].1

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
                                                    .stroke(Color.primary, lineWidth: color == val ? 2 : 0.5))
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
    let title: LocalizedStringKey
    let isSelected: Bool
    let action: () -> Void

    @Environment(\.colorSchemeContrast) private var colorSchemeContrast

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.caption.weight(.bold))
                .foregroundStyle(isSelected ? GlassTheme.activePillForegroundColor : Color.primary)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(isSelected ? GlassTheme.activePillFillColor : GlassTheme.canvasBackgroundColor)
                .clipShape(Capsule())
                .overlay(
                    Capsule()
                        .stroke(
                            isSelected ? Color.clear : (colorSchemeContrast == .increased ? GlassTheme.fallbackBorderColor : GlassTheme.matteBorderColor),
                            lineWidth: colorSchemeContrast == .increased ? GlassTheme.fallbackBorderWidth : GlassTheme.matteBorderWidth))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)
    }
}
