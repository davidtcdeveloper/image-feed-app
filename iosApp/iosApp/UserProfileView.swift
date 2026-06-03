import SwiftUI
import Charts
import shared
import Kingfisher

struct UserProfileView: View {
    let username: String
    @State private var viewModel: UserProfileViewModel
    @Environment(\.dismiss) private var dismiss

    let onPhotoSelect: (String) -> Void
    let onCollectionSelect: (String) -> Void

    init(username: String, onPhotoSelect: @escaping (String) -> Void, onCollectionSelect: @escaping (String) -> Void) {
        self.username = username
        self.onPhotoSelect = onPhotoSelect
        self.onCollectionSelect = onCollectionSelect
        self._viewModel = State(initialValue: UserProfileViewModel(username: username))
    }

    var body: some View {
        ZStack {
            Color(hex: "0F0F11")
                .ignoresSafeArea()

            if viewModel.isHeaderLoading && viewModel.user == nil {
                ProgressView()
                    .tint(.white)
            } else if let error = viewModel.error, viewModel.user == nil {
                VStack(spacing: 16) {
                    Text("Failed to load profile")
                        .font(.headline)
                        .foregroundColor(.white)
                    Text(error)
                        .font(.subheadline)
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                    Button(action: { viewModel.refresh() }) {
                        Text("Retry")
                            .fontWeight(.medium)
                            .foregroundColor(.black)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 8)
                            .background(Color.white)
                            .cornerRadius(20)
                    }
                }
            } else if let user = viewModel.user {
                ScrollView {
                    VStack(spacing: 16) {
                        // 1. Photographer Profile Info Header
                        ProfileHeaderView(user: user)

                        // 2. Multi-Tab Segment Bar
                        ProfileTabPicker(activeTab: viewModel.activeTab, user: user) { selectedTab in
                            withAnimation(.easeInOut(duration: 0.2)) {
                                viewModel.selectTab(tab: selectedTab)
                            }
                        }

                        // 3. Tab Specific Content View
                        VStack {
                            switch viewModel.activeTab {
                            case .portfolio:
                                GridPhotosList(
                                    photos: viewModel.portfolioPhotos,
                                    isLoading: viewModel.isLoadingContent,
                                    onLoadMore: { viewModel.loadNextPage() },
                                    onSelect: onPhotoSelect
                                )
                            case .likes:
                                GridPhotosList(
                                    photos: viewModel.likedPhotos,
                                    isLoading: viewModel.isLoadingContent,
                                    onLoadMore: { viewModel.loadNextPage() },
                                    onSelect: onPhotoSelect
                                )
                            case .collections:
                                GridCollectionsList(
                                    collections: viewModel.collections,
                                    isLoading: viewModel.isLoadingContent,
                                    onLoadMore: { viewModel.loadNextPage() },
                                    onSelect: onCollectionSelect
                                )
                            case .insights:
                                InsightsView(
                                    stats: viewModel.stats,
                                    isLoading: viewModel.isLoadingStats,
                                    error: viewModel.error
                                )
                            default:
                                EmptyView()
                            }
                        }
                    }
                    .padding(.bottom, 32)
                }
                .refreshable {
                    viewModel.refresh()
                }
            }
        }
        .navigationTitle(viewModel.user?.name ?? "Profile")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(Color(hex: "0F0F11"), for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.left")
                        .foregroundColor(.white)
                        .fontWeight(.semibold)
                }
            }
            if let user = viewModel.user {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        let utmProfile = "\(user.links.html)?utm_source=ImageFeedApp&utm_medium=referral"
                        if let url = URL(string: utmProfile) {
                            UIApplication.shared.open(url)
                        }
                    }) {
                        Image(systemName: "safari")
                            .foregroundColor(.white)
                    }
                }
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
                .overlay(Circle().stroke(.white.opacity(0.4), lineWidth: 2))
                .shadow(radius: 4)

            VStack(spacing: 4) {
                Text(user.name)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.white)

                Text("@\(user.username)")
                    .font(.system(size: 13))
                    .foregroundColor(.gray)
            }

            if let location = user.location, !location.isEmpty {
                HStack(spacing: 4) {
                    Image(systemName: "mappin.and.ellipse")
                        .font(.system(size: 12))
                        .foregroundColor(.lightGray)
                    Text(location)
                        .font(.system(size: 12))
                        .foregroundColor(.lightGray)
                }
            }

            if let bio = user.bio, !bio.isEmpty {
                Text(bio)
                    .font(.system(size: 13))
                    .foregroundColor(.white.opacity(0.85))
                    .lineSpacing(3)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
            }

            // Social scheme buttons
            HStack(spacing: 12) {
                if let instagram = user.social?.instagramUsername, !instagram.isEmpty {
                    SocialButton(title: "Instagram", handle: instagram) {
                        let schemeUrl = URL(string: "instagram://user?username=\(instagram)")!
                        if UIApplication.shared.canOpenURL(schemeUrl) {
                            UIApplication.shared.open(schemeUrl)
                        } else {
                            UIApplication.shared.open(URL(string: "https://instagram.com/\(instagram)")!)
                        }
                    }
                }
                if let twitter = user.social?.twitterUsername, !twitter.isEmpty {
                    SocialButton(title: "Twitter", handle: twitter) {
                        let schemeUrl = URL(string: "twitter://user?screen_name=\(twitter)")!
                        if UIApplication.shared.canOpenURL(schemeUrl) {
                            UIApplication.shared.open(schemeUrl)
                        } else {
                            UIApplication.shared.open(URL(string: "https://twitter.com/\(twitter)")!)
                        }
                    }
                }
                if let web = user.social?.portfolioUrl, !web.isEmpty, let webUrl = URL(string: web) {
                    SocialButton(title: "Website", handle: "Link") {
                        UIApplication.shared.open(webUrl)
                    }
                }
            }
            .padding(.top, 4)
        }
        .padding(.vertical, 16)
    }
}

struct SocialButton: View {
    let title: String
    let handle: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text("\(title): @\(handle)".uppercased())
                .font(.system(size: 9, weight: .bold))
                .foregroundColor(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.white.opacity(0.1))
                .cornerRadius(12)
        }
    }
}

struct ProfileTabPicker: View {
    let activeTab: ProfileTab
    let user: User
    let onSelect: (ProfileTab) -> Void

    var body: some View {
        HStack(spacing: 0) {
            ForEach(ProfileTab.values(), id: \.self) { tab in
                let isSelected = activeTab == tab
                Button(action: { onSelect(tab) }) {
                    VStack(spacing: 6) {
                        Text(tabLabel(for: tab))
                            .font(.system(size: 10, weight: isSelected ? .bold : .medium))
                            .foregroundColor(isSelected ? .white : .gray)
                            .frame(maxWidth: .infinity)

                        Rectangle()
                            .fill(isSelected ? Color.white : Color.clear)
                            .frame(height: 2)
                    }
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
        .background(Color(hex: "0F0F11"))
    }

    private func tabLabel(for tab: ProfileTab) -> String {
        switch tab {
        case .portfolio:
            return "PHOTOS (\(user.totalPhotos ?? 0))"
        case .likes:
            return "LIKES (\(user.totalLikes ?? 0))"
        case .collections:
            return "COLLECTIONS (\(user.totalCollections ?? 0))"
        case .insights:
            return "INSIGHTS"
        default:
            return ""
        }
    }
}

struct GridPhotosList: View {
    let photos: [Photo]
    let isLoading: Bool
    let onLoadMore: () -> Void
    let onSelect: (String) -> Void

    var leftColumn: [Photo] {
        photos.enumerated().filter { $0.offset % 2 == 0 }.map { $0.element }
    }

    var rightColumn: [Photo] {
        photos.enumerated().filter { $0.offset % 2 != 0 }.map { $0.element }
    }

    var body: some View {
        VStack(spacing: 12) {
            if photos.isEmpty && isLoading {
                ProgressView()
                    .tint(.white)
                    .padding(.top, 40)
            } else if photos.isEmpty {
                Text("No photos to display.")
                    .font(.subheadline)
                    .foregroundColor(.gray)
                    .padding(.top, 40)
            } else {
                HStack(alignment: .top, spacing: 8) {
                    LazyVStack(spacing: 8) {
                        ForEach(leftColumn, id: \.id) { photo in
                            KFImage(URL(string: photo.urls.small))
                                .placeholder {
                                    RoundedRectangle(cornerRadius: 12)
                                        .fill(Color(hex: photo.color ?? "1E1E24"))
                                        .aspectRatio(CGFloat(photo.width)/CGFloat(photo.height), contentMode: .fit)
                                }
                                .resizable()
                                .aspectRatio(CGFloat(photo.width)/CGFloat(photo.height), contentMode: .fit)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .onTapGesture {
                                    onSelect(photo.id)
                                }
                                .onAppear {
                                    if photo.id == photos.last?.id {
                                        onLoadMore()
                                    }
                                }
                        }
                    }

                    LazyVStack(spacing: 8) {
                        ForEach(rightColumn, id: \.id) { photo in
                            KFImage(URL(string: photo.urls.small))
                                .placeholder {
                                    RoundedRectangle(cornerRadius: 12)
                                        .fill(Color(hex: photo.color ?? "1E1E24"))
                                        .aspectRatio(CGFloat(photo.width)/CGFloat(photo.height), contentMode: .fit)
                                }
                                .resizable()
                                .aspectRatio(CGFloat(photo.width)/CGFloat(photo.height), contentMode: .fit)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .onTapGesture {
                                    onSelect(photo.id)
                                }
                                .onAppear {
                                    if photo.id == photos.last?.id {
                                        onLoadMore()
                                    }
                                }
                        }
                    }
                }
                .padding(.horizontal, 8)

                if isLoading {
                    ProgressView()
                        .tint(.white)
                        .padding(.vertical, 16)
                }
            }
        }
    }
}

struct GridCollectionsList: View {
    let collections: [PhotoCollection]
    let isLoading: Bool
    let onLoadMore: () -> Void
    let onSelect: (String) -> Void

    var body: some View {
        VStack(spacing: 12) {
            if collections.isEmpty && isLoading {
                ProgressView()
                    .tint(.white)
                    .padding(.top, 40)
            } else if collections.isEmpty {
                Text("No collections to display.")
                    .font(.subheadline)
                    .foregroundColor(.gray)
                    .padding(.top, 40)
            } else {
                LazyVStack(spacing: 16) {
                    ForEach(collections, id: \.id) { col in
                        ZStack(alignment: .bottomStart) {
                            if let coverPhoto = col.coverPhoto {
                                KFImage(URL(string: coverPhoto.urls.regular))
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .frame(height: 180)
                                    .clipped()
                            } else {
                                Color(hex: "1E1E24")
                                    .frame(height: 180)
                            }

                            // Dark scrim
                            Color.black.opacity(0.4)

                            VStack(alignment: .leading, spacing: 4) {
                                Text(col.title.uppercased())
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.white)
                                    .tracking(1)

                                Text("\(col.totalPhotos) Photos · Curated by \(col.user.name)")
                                    .font(.system(size: 11))
                                    .foregroundColor(.lightGray)
                            }
                            .padding(16)
                        }
                        .frame(height: 180)
                        .cornerRadius(12)
                        .clipped()
                        .onTapGesture {
                            onSelect(col.id)
                        }
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
                        .tint(.white)
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
            if isLoading && stats == nil {
                ProgressView()
                    .tint(.white)
                    .padding(.top, 40)
            } else if error != nil && stats == nil {
                Text("Failed to load insights.")
                    .font(.subheadline)
                    .foregroundColor(.gray)
                    .padding(.top, 40)
            } else if let stats = stats {
                VStack(alignment: .leading, spacing: 24) {
                    // Consolidated metrics
                    HStack(spacing: 12) {
                        MetricCard(label: "Total Views", value: formatMetric(stats.views.total), systemImage: "eye.fill")
                        MetricCard(label: "Total Downloads", value: formatMetric(stats.downloads.total), systemImage: "arrow.down.circle.fill")
                    }

                    // Interactive Views Chart
                    if let viewsHist = stats.views.historical, !viewsHist.values.isEmpty {
                        InteractiveTimelineChart(title: "VIEWS TRENDS", values: viewsHist.values)
                            .padding(16)
                            .background(.ultraThinMaterial)
                            .cornerRadius(12)
                    }

                    // Interactive Downloads Chart
                    if let downHist = stats.downloads.historical, !downHist.values.isEmpty {
                        InteractiveTimelineChart(title: "DOWNLOADS TRENDS", values: downHist.values)
                            .padding(16)
                            .background(.ultraThinMaterial)
                            .cornerRadius(12)
                    }
                }
                .padding(.horizontal, 16)
            }
        }
    }

    private func formatMetric(_ value: Int32?) -> String {
        guard let value = value else { return "--" }
        let num = Int(value)
        if num >= 1_000_000 {
            return String(format: "%.1fM", Double(num) / 1_000_000.0)
        } else if num >= 1_000 {
            return String(format: "%.1fK", Double(num) / 1_000.0)
        } else {
            return "\(num)"
        }
    }
}

struct InteractiveTimelineChart: View {
    let title: String
    let values: [StatsValue]
    @State private var rawSelectedDate: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(title)
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(.gray)
                    .tracking(1)
                Spacer()
                if let selected = rawSelectedDate, let matched = values.first(where: { $0.date == selected }) {
                    Text("\(matched.date): \(matched.value)")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.white)
                } else {
                    Text("Drag curve to inspect daily stats")
                        .font(.system(size: 10))
                        .foregroundColor(.gray)
                }
            }

            Chart {
                ForEach(values, id: \.date) { item in
                    LineMark(
                        x: .value("Date", item.date),
                        y: .value("Count", item.value)
                    )
                    .foregroundStyle(.white)
                    .interpolationMethod(.catmullRom)

                    AreaMark(
                        x: .value("Date", item.date),
                        y: .value("Count", item.value)
                    )
                    .foregroundStyle(
                        LinearGradient(
                            colors: [.white.opacity(0.2), .clear],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .interpolationMethod(.catmullRom)

                    if let selected = rawSelectedDate, item.date == selected {
                        RuleMark(x: .value("Date", selected))
                            .foregroundStyle(.white.opacity(0.4))
                            .lineStyle(StrokeStyle(lineWidth: 1, dash: [4, 4]))

                        PointMark(
                            x: .value("Date", selected),
                            y: .value("Count", item.value)
                        )
                        .foregroundStyle(.white)
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
