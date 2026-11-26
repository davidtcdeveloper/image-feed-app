import Charts
import shared
import SwiftUI

struct UserProfileView: View {
    let username: String
    @State private var containerWidth: CGFloat = 0
    @State private var viewModel: UserProfileViewModel?
    @Environment(\.dismiss) private var dismiss

    let onPhotoSelect: (String) -> Void
    let onCollectionSelect: (String) -> Void

    init(
        username: String,
        initialViewModel: UserProfileViewModel? = nil,
        onPhotoSelect: @escaping (String) -> Void,
        onCollectionSelect: @escaping (String) -> Void)
    {
        self.username = username
        self.onPhotoSelect = onPhotoSelect
        self.onCollectionSelect = onCollectionSelect
        self._viewModel = State(initialValue: initialViewModel)
    }

    var body: some View {
        ZStack {
            GlassTheme.canvasBackgroundColor
                .ignoresSafeArea()

            if let viewModel {
                if viewModel.isHeaderLoading, viewModel.user == nil {
                    UserProfileHeaderSkeleton()
                        .padding(.top, 40)
                } else if let error = viewModel.error, viewModel.user == nil {
                    VStack(spacing: 16) {
                        Text("Failed to load profile")
                            .font(.headline)
                            .glassVibrancy(.primary)
                        Text(error)
                            .font(.subheadline)
                            .glassVibrancy(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 24)
                        Button(action: { viewModel.refresh() }) {
                            Text("Retry")
                                .font(.subheadline.weight(.semibold))
                                .glassVibrancy(.primary)
                                .padding(.horizontal, 24)
                                .padding(.vertical, 10)
                                .glassBackground(style: .thin, shape: Capsule(), showBorder: true)
                        }
                    }
                } else if let user = viewModel.user {
                    ScrollView {
                        VStack(spacing: 16) {
                            // 1. Photographer Profile Info Header
                            ProfileHeaderView(user: user)

                            // 2. Multi-Tab Segment Bar
                            ProfileTabPicker(activeTab: viewModel.activeTab, user: user) { selectedTab in
                                viewModel.selectTab(tab: selectedTab)
                            }

                            // 3. Tab Specific Content View
                            VStack {
                                switch viewModel.activeTab {
                                case .portfolio:
                                    GridPhotosList(
                                        photos: viewModel.portfolioPhotos,
                                        columnItems: { col, total in viewModel.portfolioColumnItems(for: col, totalColumns: total) },
                                        isLoading: viewModel.isLoadingContent,
                                        onLoadMore: { viewModel.loadNextPage() },
                                        onSelect: onPhotoSelect)
                                case .likes:
                                    GridPhotosList(
                                        photos: viewModel.likedPhotos,
                                        columnItems: { col, total in viewModel.likedColumnItems(for: col, totalColumns: total) },
                                        isLoading: viewModel.isLoadingContent,
                                        onLoadMore: { viewModel.loadNextPage() },
                                        onSelect: onPhotoSelect)
                                case .collections:
                                    GridCollectionsList(
                                        collections: viewModel.collections,
                                        isLoading: viewModel.isLoadingContent,
                                        onLoadMore: { viewModel.loadNextPage() },
                                        onSelect: onCollectionSelect)
                                case .insights:
                                    InsightsView(
                                        stats: viewModel.stats,
                                        isLoading: viewModel.isLoadingStats,
                                        error: viewModel.error)
                                default:
                                    EmptyView()
                                }
                            }
                        }
                        .padding(.bottom, 32)
                    }
                    .satinScrollEdge()
                    .refreshable {
                        viewModel.refresh()
                    }
                }
            } else {
                UserProfileHeaderSkeleton()
                    .padding(.top, 40)
            }
        }
        .measureContainerWidth($containerWidth)
        .task(id: username) {
            if viewModel == nil {
                viewModel = UserProfileViewModel(username: username)
            }
        }
        .navigationTitle(viewModel?.user?.name ?? String(localized: "Profile"))
        #if os(iOS)
        .satinNavigationBar()
        .toolbar(.hidden, for: .tabBar)
        #endif
        .toolbar {
            #if os(iOS)
            ToolbarItem(placement: .navigationBarLeading) {
                PlatformToolbarButton(systemName: "chevron.backward", accessibilityLabel: "Back") {
                    dismiss()
                }
            }
            #else
            ToolbarItem(placement: .navigation) {
                PlatformToolbarButton(systemName: "chevron.backward", accessibilityLabel: "Back") {
                    dismiss()
                }
            }
            #endif

            if let user = viewModel?.user {
                #if os(iOS)
                ToolbarItem(placement: .navigationBarTrailing) {
                    PlatformToolbarButton(systemName: "safari", accessibilityLabel: "Open Profile in Browser") {
                        let utmProfile = "\(user.links.html)?utm_source=ImageFeedApp&utm_medium=referral"
                        if let url = URL(string: utmProfile) {
                            URLHelper.open(url)
                        }
                    }
                }
                #else
                ToolbarItem(placement: .primaryAction) {
                    PlatformToolbarButton(systemName: "safari", accessibilityLabel: "Open Profile in Browser") {
                        let utmProfile = "\(user.links.html)?utm_source=ImageFeedApp&utm_medium=referral"
                        if let url = URL(string: utmProfile) {
                            URLHelper.open(url)
                        }
                    }
                }
                #endif
            }
        }
        .navigationBarBackButtonHidden(true)
    }
}

struct ProfileHeaderView: View {
    let user: User

    var body: some View {
        VStack(spacing: 12) {
            KFImage(URL(string: user.profileImage.large))
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: 88, height: 88)
                .clipShape(Circle())
                .overlay(Circle().stroke(GlassTheme.matteBorderColor, lineWidth: 2))
                .shadow(radius: 4)

            VStack(spacing: 4) {
                Text(user.name)
                    .font(.title2.weight(.bold))
                    .glassVibrancy(.primary)

                Text("@\(user.username)")
                    .font(.subheadline)
                    .glassVibrancy(.secondary)
            }

            if let location = user.location, !location.isEmpty {
                HStack(spacing: 4) {
                    Image(systemName: "mappin.and.ellipse")
                        .font(.caption)
                        .glassVibrancy(.secondary)
                    Text(location)
                        .font(.caption)
                        .glassVibrancy(.secondary)
                }
            }

            if let bio = user.bio, !bio.isEmpty {
                Text(bio)
                    .font(.subheadline)
                    .glassVibrancy(.primary)
                    .lineSpacing(3)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
            }

            // Social scheme chips
            let hasSocials = (user.social?.instagramUsername?.isEmpty == false) ||
                (user.social?.twitterUsername?.isEmpty == false) ||
                (user.social?.portfolioUrl?.isEmpty == false)

            if hasSocials {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        if let instagram = user.social?.instagramUsername?.trimmingCharacters(in: .whitespacesAndNewlines), !instagram.isEmpty {
                            let handle = instagram.trimmingCharacters(in: CharacterSet(charactersIn: "@"))
                            SocialChip(systemImage: "camera", label: "@\(handle)", accessibilityLabel: "Instagram") {
                                if let schemeUrl = URL(string: "instagram://user?username=\(handle)"), URLHelper.canOpen(schemeUrl) {
                                    URLHelper.open(schemeUrl)
                                } else if let webUrl = URL(string: "https://instagram.com/\(handle)") {
                                    URLHelper.open(webUrl)
                                }
                            }
                        }
                        if let twitter = user.social?.twitterUsername?.trimmingCharacters(in: .whitespacesAndNewlines), !twitter.isEmpty {
                            let handle = twitter.trimmingCharacters(in: CharacterSet(charactersIn: "@"))
                            SocialChip(systemImage: "bubble.left", label: "@\(handle)", accessibilityLabel: "Twitter") {
                                if let schemeUrl = URL(string: "twitter://user?screen_name=\(handle)"), URLHelper.canOpen(schemeUrl) {
                                    URLHelper.open(schemeUrl)
                                } else if let webUrl = URL(string: "https://twitter.com/\(handle)") {
                                    URLHelper.open(webUrl)
                                }
                            }
                        }
                        if let web = user.social?.portfolioUrl?.trimmingCharacters(in: .whitespacesAndNewlines), !web.isEmpty {
                            let normalizedWeb = web.lowercased().hasPrefix("http://") || web.lowercased().hasPrefix("https://") ? web : "https://\(web)"
                            if let webUrl = URL(string: normalizedWeb) {
                                SocialChip(systemImage: "globe", label: "Website", accessibilityLabel: "Website") {
                                    URLHelper.open(webUrl)
                                }
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                }
                .padding(.top, 4)
            }
        }
        .padding(.vertical, 16)
    }
}

struct SocialChip: View {
    let systemImage: String
    let label: String
    let accessibilityLabel: LocalizedStringKey?
    let action: () -> Void

    init(systemImage: String, label: String, accessibilityLabel: LocalizedStringKey? = nil, action: @escaping () -> Void) {
        self.systemImage = systemImage
        self.label = label
        self.accessibilityLabel = accessibilityLabel
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 5) {
                Image(systemName: systemImage)
                    .font(.caption2.weight(.semibold))
                    .glassVibrancy(.primary)
                Text(LocalizedStringKey(label))
                    .font(.caption2.weight(.medium))
                    .glassVibrancy(.primary)
                    .lineLimit(1)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .glassCapsule(style: .ultraThin, showBorder: true, hasShadow: false)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(Text(accessibilityLabel ?? LocalizedStringKey(label)))
        .accessibilityAddTraits(.isButton)
    }
}

struct ProfileTabPicker: View {
    let activeTab: ProfileTab
    let user: User
    let onSelect: (ProfileTab) -> Void
    @Namespace private var tabIndicatorNamespace
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach([ProfileTab.portfolio, ProfileTab.likes, ProfileTab.collections, ProfileTab.insights], id: \.self) { tab in
                        let isSelected = activeTab == tab
                        Button {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                                onSelect(tab)
                            }
                        } label: {
                            HStack(spacing: 6) {
                                Text(tabTitle(for: tab))
                                    .font(.caption.weight(isSelected ? .bold : .medium))
                                    .foregroundStyle(isSelected ? GlassTheme.activePillForegroundColor : Color.secondary)

                                if let count = tabCount(for: tab) {
                                    Text("\(count)")
                                        .font(.caption2.weight(.bold))
                                        .padding(.horizontal, 6)
                                        .padding(.vertical, 2)
                                        .background(
                                            Capsule()
                                                .fill(isSelected ? GlassTheme.activePillForegroundColor.opacity(0.18) : GlassTheme.subtleFillColor))
                                        .foregroundStyle(isSelected ? GlassTheme.activePillForegroundColor : Color.secondary)
                                }
                            }
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background {
                                if isSelected {
                                    Capsule()
                                        .fill(GlassTheme.activePillFillColor)
                                        .matchedGeometryEffect(id: "ActiveProfileTab", in: tabIndicatorNamespace)
                                } else {
                                    Capsule()
                                        .fill(GlassTheme.subtleFillColor)
                                        .overlay {
                                            if colorSchemeContrast == .increased {
                                                Capsule()
                                                    .strokeBorder(GlassTheme.fallbackBorderColor, lineWidth: GlassTheme.fallbackBorderWidth)
                                            }
                                        }
                                }
                            }
                        }
                        .buttonStyle(.plain)
                        .id(tab)
                        .accessibilityElement(children: .combine)
                        .accessibilityLabel(tabAccessibilityLabel(for: tab))
                        .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .macOSHorizontalScroll()
            }
            .onAppear {
                proxy.scrollTo(activeTab, anchor: .center)
            }
            .onChange(of: activeTab) { _, newTab in
                withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                    proxy.scrollTo(newTab, anchor: .center)
                }
            }
        }
        .glassBackground(material: .ultraThinMaterial, shape: Rectangle(), showBorder: false)
    }

    private func tabTitle(for tab: ProfileTab) -> String {
        switch tab {
        case .portfolio: String(localized: "Photos")
        case .likes: String(localized: "Likes")
        case .collections: String(localized: "Collections")
        case .insights: String(localized: "Insights")
        default: ""
        }
    }

    private func tabCount(for tab: ProfileTab) -> Int? {
        switch tab {
        case .portfolio:
            user.totalPhotos.map { Int(truncating: $0) }
        case .likes:
            user.totalLikes.map { Int(truncating: $0) }
        case .collections:
            user.totalCollections.map { Int(truncating: $0) }
        case .insights:
            nil
        default:
            nil
        }
    }

    private func tabAccessibilityLabel(for tab: ProfileTab) -> Text {
        let title = tabTitle(for: tab)
        if let count = tabCount(for: tab) {
            return Text("\(title), \(count.formatted()) items")
        }
        return Text(title)
    }
}

struct GridPhotosList: View {
    #if os(iOS)
    @Environment(\.horizontalSizeClass) var sizeClass
    #endif
    @Environment(\.containerWidth) private var containerWidth
    let photos: [Photo]
    var columnItems: ((Int, Int) -> [AdaptiveLayoutHelper.IndexedItem<Photo>])?
    let isLoading: Bool
    let onLoadMore: () -> Void
    let onSelect: (String) -> Void

    private var columnCount: Int {
        #if os(iOS)
        AdaptiveLayoutHelper.getColumnCount(containerWidth: containerWidth, sizeClass: sizeClass)
        #else
        AdaptiveLayoutHelper.getColumnCount(containerWidth: containerWidth)
        #endif
    }

    var body: some View {
        VStack(spacing: 12) {
            if photos.isEmpty, isLoading {
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
                .padding(.top, 20)
            } else if photos.isEmpty {
                Text("No photos found.")
                    .font(.subheadline)
                    .glassVibrancy(.secondary)
                    .padding(.top, 40)
            } else {
                HStack(alignment: .top, spacing: 8) {
                    ForEach(0..<columnCount, id: \.self) { colIndex in
                        LazyVStack(spacing: 8) {
                            let items = columnItems?(colIndex, columnCount)
                                ?? AdaptiveLayoutHelper.indexedPhotosForColumn(index: colIndex, totalColumns: columnCount, from: photos)
                            ForEach(items) { indexed in
                                let photo = indexed.item
                                let flatIndex = indexed.index
                                PhotoGridCard(
                                    photo: photo,
                                    columnCount: columnCount,
                                    showAttribution: false,
                                    fadeDuration: 0,
                                    onSelect: { onSelect(photo.id) },
                                    onAppear: {
                                        if photo.id == photos.last?.id {
                                            onLoadMore()
                                        }
                                    })
                                    .staggeredReveal(index: flatIndex)
                            }
                        }
                    }
                }
                .padding(.horizontal, 8)

                if isLoading {
                    ProgressView()
                        .tint(.primary)
                        .padding(.vertical, 16)
                }
            }
        }
    }
}

struct GridCollectionsList: View {
    @Environment(\.containerWidth) private var containerWidth
    let collections: [PhotoCollection]
    let isLoading: Bool
    let onLoadMore: () -> Void
    let onSelect: (String) -> Void

    var body: some View {
        VStack(spacing: 12) {
            if collections.isEmpty, isLoading {
                VStack(spacing: 16) {
                    ForEach(0..<3, id: \.self) { _ in
                        CollectionMosaicCardSkeleton()
                    }
                }
                .padding(.horizontal, 12)
                .padding(.top, 20)
            } else if collections.isEmpty {
                Text("No collections found.")
                    .font(.subheadline)
                    .glassVibrancy(.secondary)
                    .padding(.top, 40)
            } else {
                LazyVStack(spacing: 16) {
                    ForEach(Array(collections.enumerated()), id: \.element.id) { index, col in
                        Button(action: {
                            NavigationPreviewSeeder.seedCollection(col)
                            onSelect(col.id)
                        }) {
                            ZStack(alignment: .bottomLeading) {
                                if let coverPhoto = col.coverPhoto {
                                    let widthPt = AdaptiveLayoutHelper.effectiveWidth(containerWidth: containerWidth)
                                    let itemWidthPx = Int32(widthPt * AdaptiveLayoutHelper.displayScale())
                                    KFImage(URL(string: coverPhoto.urls.getOptimalUrl(targetWidthPx: itemWidthPx)))
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                        .frame(height: 180)
                                        .clipped()
                                } else {
                                    GlassTheme.surfaceBackgroundColor
                                        .frame(height: 180)
                                }

                                // Dark scrim
                                Color.black.opacity(0.4)

                                VStack(alignment: .leading, spacing: 4) {
                                    Text(col.title.uppercased())
                                        .font(.headline.weight(.bold))
                                        .glassVibrancy(.primary)
                                        .tracking(1)

                                    HStack(spacing: 4) {
                                        Text("\(col.totalPhotos) Photos")
                                        Text("·")
                                        Text("Curated by")
                                        Text(col.user.name)
                                    }
                                    .font(.caption)
                                    .glassVibrancy(.secondary)
                                }
                                .environment(\.colorScheme, .dark)
                                .padding(16)
                            }
                            .frame(height: 180)
                            .cornerRadius(12)
                            .clipped()
                            .staggeredReveal(index: index)
                            .feedScrollTransition()
                        }
                        .buttonStyle(SpringCardButtonStyle())
                        .onAppear {
                            if col.id == collections.last?.id {
                                onLoadMore()
                            }
                        }
                    }
                }
                .padding(.horizontal, 12)

                if isLoading {
                    ProgressView()
                        .tint(.primary)
                        .padding(.vertical, 16)
                }
            }
        }
    }
}

struct InsightsView: View {
    let stats: UserStats?
    let isLoading: Bool
    let error: String?

    var body: some View {
        VStack(spacing: 20) {
            if isLoading, stats == nil {
                ProgressView()
                    .tint(.primary)
                    .padding(.top, 40)
            } else if error != nil, stats == nil {
                Text("Failed to load insights.")
                    .font(.subheadline)
                    .glassVibrancy(.secondary)
                    .padding(.top, 40)
            } else if let stats {
                VStack(alignment: .leading, spacing: 24) {
                    // Consolidated metrics
                    HStack(spacing: 12) {
                        MetricCard(label: "Total Views", value: formatMetric(stats.views.total), systemImage: "eye.fill")
                        MetricCard(label: "Total Downloads", value: formatMetric(stats.downloads.total), systemImage: "arrow.down.circle.fill")
                    }

                    // Interactive Views Chart
                    if let viewsHist = stats.views.historical, !viewsHist.values.isEmpty {
                        InteractiveTimelineChart(title: "VIEWS TRENDS (LAST 30 DAYS)", values: viewsHist.values)
                            .padding(16)
                            .matteSurface(cornerRadius: 12)
                    }

                    // Interactive Downloads Chart
                    if let downHist = stats.downloads.historical, !downHist.values.isEmpty {
                        InteractiveTimelineChart(title: "DOWNLOADS TRENDS (LAST 30 DAYS)", values: downHist.values)
                            .padding(16)
                            .matteSurface(cornerRadius: 12)
                    }
                }
                .padding(.horizontal, 16)
            }
        }
    }

    private func formatMetric(_ value: Int32?) -> String {
        guard let value else { return "--" }
        return Int(value).formatted(.number.notation(.compactName))
    }
}

struct InteractiveTimelineChart: View {
    let title: LocalizedStringKey
    let values: [StatsValue]
    @State private var rawSelectedDate: String? = nil

    private func formatTimelineDate(_ isoDate: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withFullDate]
        if let d = formatter.date(from: isoDate) {
            return d.formatted(date: .abbreviated, time: .omitted)
        }
        return isoDate
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(title)
                    .font(.caption.weight(.bold))
                    .glassVibrancy(.secondary)
                    .tracking(1)
                Spacer()
                if let selected = rawSelectedDate, let matched = values.first(where: { $0.date == selected }) {
                    Text("\(formatTimelineDate(matched.date)): \(matched.value)")
                        .font(.caption.weight(.bold))
                        .glassVibrancy(.primary)
                } else {
                    Text("Drag curve to inspect daily stats")
                        .font(.caption2)
                        .glassVibrancy(.secondary)
                }
            }

            Chart {
                ForEach(values, id: \.date) { item in
                    LineMark(
                        x: .value("Date", item.date),
                        y: .value("Count", item.value))
                        .foregroundStyle(Color.primary)
                        .interpolationMethod(.catmullRom)

                    AreaMark(
                        x: .value("Date", item.date),
                        y: .value("Count", item.value))
                        .foregroundStyle(
                            LinearGradient(
                                colors: [Color.primary.opacity(0.18), .clear],
                                startPoint: .top,
                                endPoint: .bottom))
                        .interpolationMethod(.catmullRom)

                    if let selected = rawSelectedDate, item.date == selected {
                        RuleMark(x: .value("Date", selected))
                            .foregroundStyle(Color.primary.opacity(0.4))
                            .lineStyle(StrokeStyle(lineWidth: 1, dash: [4, 4]))

                        PointMark(
                            x: .value("Date", selected),
                            y: .value("Count", item.value))
                            .foregroundStyle(Color.primary)
                            .symbolSize(100)
                    }
                }
            }
            .chartXAxis(.hidden)
            .chartYAxis(.hidden)
            .frame(height: 120)
            .chartXSelection(value: $rawSelectedDate)
        }
    }
}
