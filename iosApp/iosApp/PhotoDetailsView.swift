import Charts
import MapKit
import shared
import SwiftUI

struct PhotoDetailsView: View {
    let photoId: String
    let heroNamespace: Namespace.ID
    let onDismiss: (() -> Void)?
    let onUserSelect: (String) -> Void
    let onTagSelect: (String) -> Void
    @State private var viewModel: PhotoDetailsViewModel
    @Environment(\.dismiss) private var dismiss

    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    #endif
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    @State private var showInspector = true
    @State private var currentScale: CGFloat = 1.0
    @State private var finalScale: CGFloat = 1.0
    @State private var currentOffset: CGSize = .zero
    @State private var finalOffset: CGSize = .zero
    @State private var downloadFeedbackTrigger = 0

    private var isDualPane: Bool {
        #if os(iOS)
        return horizontalSizeClass == .regular
        #else
        return true
        #endif
    }

    private func handleDismiss() {
        withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
            currentScale = 1.0
            finalScale = 1.0
            currentOffset = .zero
            finalOffset = .zero
        }
        showInspector = false
        if let onDismiss {
            onDismiss()
        } else {
            dismiss()
        }
    }

    init(
        photoId: String,
        heroNamespace: Namespace.ID,
        onDismiss: (() -> Void)? = nil,
        onUserSelect: @escaping (String) -> Void,
        onTagSelect: @escaping (String) -> Void)
    {
        self.photoId = photoId
        self.heroNamespace = heroNamespace
        self.onDismiss = onDismiss
        self.onUserSelect = onUserSelect
        self.onTagSelect = onTagSelect
        self._viewModel = State(initialValue: PhotoDetailsViewModel(photoId: photoId))
    }

    var body: some View {
        ZStack {
            Color.black
                .ignoresSafeArea()

            if viewModel.isLoading, viewModel.photo == nil {
                ProgressView()
                    .tint(.white)
            } else if let error = viewModel.error, viewModel.photo == nil {
                VStack(spacing: 16) {
                    Text("Failed to load details")
                        .font(.headline)
                        .glassVibrancy(.primary)
                    Text(error)
                        .font(.subheadline)
                        .glassVibrancy(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                    Button(action: { viewModel.loadDetails() }) {
                        Text("Retry")
                            .font(.subheadline.weight(.semibold))
                            .glassVibrancy(.primary)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 10)
                            .glassBackground(style: .thin, shape: Capsule(), showBorder: true)
                    }
                }
                .environment(\.colorScheme, .dark)
            } else if let photo = viewModel.photo {
                if isDualPane {
                    dualPaneLayout(photo: photo)
                } else {
                    compactPhotoLayout(photo: photo)
                }
            }
        }
        .navigationTitle("")
        .navigationBarBackButtonHidden(true)
        #if os(iOS)
        .toolbar(.hidden, for: .navigationBar, .tabBar)
        #endif
        .onChange(of: viewModel.shouldDismiss) { _, shouldDismiss in
            if shouldDismiss {
                handleDismiss()
            }
        }
        .overlay(alignment: .topLeading) {
            #if os(iOS)
            GlassToolbarButton(systemName: "chevron.backward", accessibilityLabel: "Back") {
                handleDismiss()
            }
            .padding(.top, 60)
            .padding(.leading, 16)
            .environment(\.colorScheme, .dark)
            #else
            if onDismiss != nil {
                GlassToolbarButton(systemName: "chevron.backward", accessibilityLabel: "Back") {
                    handleDismiss()
                }
                .padding(.top, 60)
                .padding(.leading, 16)
                .environment(\.colorScheme, .dark)
            }
            #endif
        }
        .overlay(alignment: .topTrailing) {
            #if os(iOS)
            if !isDualPane {
                GlassToolbarButton(
                    systemName: showInspector ? "info.circle.fill" : "info.circle",
                    accessibilityLabel: "Photo Details")
                {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                        showInspector.toggle()
                    }
                }
                .padding(.top, 60)
                .padding(.trailing, 16)
                .environment(\.colorScheme, .dark)
            }
            #else
            if onDismiss != nil, !isDualPane {
                GlassToolbarButton(
                    systemName: showInspector ? "info.circle.fill" : "info.circle",
                    accessibilityLabel: "Photo Details")
                {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                        showInspector.toggle()
                    }
                }
                .padding(.top, 60)
                .padding(.trailing, 16)
                .environment(\.colorScheme, .dark)
            }
            #endif
        }
        .sensoryFeedback(.success, trigger: downloadFeedbackTrigger)
        #if os(iOS)
        .sheet(isPresented: Binding(
            get: { showInspector && !isDualPane && viewModel.photo != nil },
            set: { showInspector = $0 }))
        {
            if let photo = viewModel.photo {
                ScrollView {
                    PhotoInspectorView(
                        photo: photo,
                        viewModel: viewModel,
                        onUserSelect: { username in
                            showInspector = false
                            onUserSelect(username)
                        },
                        onTagSelect: { tag in
                            showInspector = false
                            onTagSelect(tag)
                        },
                        onDownload: {
                            downloadFeedbackTrigger += 1
                        })
                        .padding(20)
                        .padding(.bottom, 32)
                }
                .environment(\.colorScheme, .dark)
                .preferredColorScheme(.dark)
                .presentationDetents([.fraction(0.35), .fraction(0.70), .large])
                .presentationBackgroundInteraction(.enabled(upThrough: .fraction(0.70)))
                .presentationDragIndicator(.visible)
                .presentationCornerRadius(24)
                .presentationBackground {
                    if reduceTransparency {
                        GlassTheme.fallbackBackgroundColor
                    } else {
                        ZStack {
                            GlassTheme.highContrastScrimColor.opacity(0.82)
                            Rectangle().fill(.regularMaterial)
                        }
                    }
                }
            }
        }
        #endif
        .toolbar {
            #if !os(iOS)
            ToolbarItem(placement: .navigation) {
                GlassToolbarButton(systemName: "chevron.backward", accessibilityLabel: "Back") {
                    handleDismiss()
                }
            }
            #endif
        }
        .onDisappear {
            currentScale = 1.0
            finalScale = 1.0
            currentOffset = .zero
            finalOffset = .zero
            showInspector = false
        }
    }
}

// MARK: - PhotoDetailsView Layout Helpers

private extension PhotoDetailsView {
    func dualPaneLayout(photo: Photo) -> some View {
        GeometryReader { totalGeo in
            let inspectorWidth = max(320, min(420, totalGeo.size.width * 0.42))

            HStack(spacing: 0) {
                // Left: Photo Canvas
                ZStack(alignment: .bottomLeading) {
                    ambientBackdrop(photo: photo)

                    GeometryReader { geo in
                        let aspectRatio = max(0.1, CGFloat(photo.width) / CGFloat(photo.height))
                        let imageUrl = photo.urls.raw + "&w=\(Int(geo.size.width * 2))&q=85&auto=format"

                        KFImage(URL(string: imageUrl))
                            .placeholder {
                                Rectangle()
                                    .fill(Color(hex: photo.color ?? "1E1E24"))
                            }
                            .resizable()
                            .aspectRatio(aspectRatio, contentMode: .fit)
                            .matchedGeometryEffect(id: "photo-img-\(photoId)", in: heroNamespace)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    }

                    // Photographer floating attribution
                    HStack(spacing: 10) {
                        KFImage(URL(string: photo.user.profileImage.medium))
                            .resizable()
                            .scaledToFill()
                            .frame(width: 36, height: 36)
                            .clipShape(Circle())
                            .overlay(Circle().stroke(.white.opacity(0.4), lineWidth: 1.5))

                        VStack(alignment: .leading, spacing: 2) {
                            Text(photo.user.name)
                                .font(.subheadline.weight(.bold))
                                .glassVibrancy(.primary)
                            Text("@\(photo.user.username)")
                                .font(.caption2)
                                .glassVibrancy(.secondary)
                        }

                        Spacer()

                        Button(action: {
                            onUserSelect(photo.user.username)
                        }) {
                            Image(systemName: "arrow.up.right")
                                .font(.caption.weight(.bold))
                                .glassVibrancy(.primary)
                        }
                        .accessibilityLabel(Text("View Profile"))
                    }
                    .padding(12)
                    .background {
                        Capsule()
                            .fill(Color.black.opacity(0.42))
                    }
                    .glassCapsule(style: .ultraThin, showBorder: true, hasShadow: true)
                    .environment(\.colorScheme, .dark)
                    .padding(16)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                // Right: Inspector Pane
                ScrollView {
                    PhotoInspectorView(
                        photo: photo,
                        viewModel: viewModel,
                        onUserSelect: onUserSelect,
                        onTagSelect: onTagSelect,
                        onDownload: {
                            downloadFeedbackTrigger += 1
                        })
                        .padding(20)
                        .padding(.bottom, 32)
                }
                .frame(width: inspectorWidth)
                .background(GlassTheme.canvasBackgroundColor)
            }
        }
        .ignoresSafeArea(edges: .top)
    }

    func compactPhotoLayout(photo: Photo) -> some View {
        GeometryReader { geo in
            let containerWidth = geo.size.width
            let aspectRatio = max(0.1, CGFloat(photo.width) / CGFloat(photo.height))
            let calculatedHeight = containerWidth / aspectRatio
            let imageUrl = photo.urls.raw + "&w=\(Int(containerWidth * 2))&q=85&auto=format"

            ZStack(alignment: .bottom) {
                ambientBackdrop(photo: photo)

                ZStack {
                    KFImage(URL(string: imageUrl))
                        .placeholder {
                            Rectangle()
                                .fill(Color(hex: photo.color ?? "1E1E24"))
                        }
                        .resizable()
                        .aspectRatio(aspectRatio, contentMode: .fit)
                        .frame(width: containerWidth, height: calculatedHeight)
                        .matchedGeometryEffect(id: "photo-img-\(photoId)", in: heroNamespace)
                        .scaleEffect(currentScale)
                        .offset(currentOffset)
                        .contentShape(Rectangle())
                        .gesture(
                            MagnificationGesture()
                                .onChanged { value in
                                    let newScale = finalScale * value
                                    currentScale = max(1.0, min(4.0, newScale))
                                }
                                .onEnded { _ in
                                    finalScale = currentScale
                                    if finalScale <= 1.0 {
                                        withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                            currentScale = 1.0
                                            finalScale = 1.0
                                            currentOffset = .zero
                                            finalOffset = .zero
                                        }
                                    } else {
                                        let maxOffsetX = max(0, (containerWidth * (finalScale - 1)) / 2)
                                        let maxOffsetY = max(0, (calculatedHeight * finalScale - geo.size.height) / 2)
                                        let clampedX = max(-maxOffsetX, min(maxOffsetX, finalOffset.width))
                                        let clampedY = max(-maxOffsetY, min(maxOffsetY, finalOffset.height))
                                        if clampedX != finalOffset.width || clampedY != finalOffset.height {
                                            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                                currentOffset = CGSize(width: clampedX, height: clampedY)
                                                finalOffset = currentOffset
                                            }
                                        }
                                    }
                                })
                        .simultaneousGesture(
                            DragGesture()
                                .onChanged { value in
                                    guard currentScale > 1.05 else { return }
                                    let maxOffsetX = max(0, (containerWidth * (currentScale - 1)) / 2)
                                    let maxOffsetY = max(0, (calculatedHeight * currentScale - geo.size.height) / 2)
                                    let newX = finalOffset.width + value.translation.width
                                    let newY = finalOffset.height + value.translation.height
                                    currentOffset = CGSize(
                                        width: max(-maxOffsetX, min(maxOffsetX, newX)),
                                        height: max(-maxOffsetY, min(maxOffsetY, newY)))
                                }
                                .onEnded { _ in
                                    guard currentScale > 1.05 else {
                                        finalOffset = .zero
                                        currentOffset = .zero
                                        return
                                    }
                                    finalOffset = currentOffset
                                })
                        .onTapGesture(count: 2) {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                                if currentScale > 1.05 {
                                    currentScale = 1.0
                                    finalScale = 1.0
                                    currentOffset = .zero
                                    finalOffset = .zero
                                } else {
                                    currentScale = 2.5
                                    finalScale = 2.5
                                }
                            }
                        }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                // Floating action bar when inspector is dismissed
                if !showInspector {
                    compactFloatingBar(photo: photo)
                }
            }
        }
        .ignoresSafeArea()
    }

    private func compactFloatingBar(photo: Photo) -> some View {
        HStack(spacing: 12) {
            // Photographer attribution button
            Button(action: {
                onUserSelect(photo.user.username)
            }) {
                HStack(spacing: 8) {
                    KFImage(URL(string: photo.user.profileImage.medium))
                        .resizable()
                        .scaledToFill()
                        .frame(width: 32, height: 32)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(GlassTheme.specularBorderGradient, lineWidth: 1.5))

                    VStack(alignment: .leading, spacing: 2) {
                        Text(photo.user.name)
                            .font(.subheadline.weight(.bold))
                            .glassVibrancy(.primary)
                            .lineLimit(1)
                        Text("@\(photo.user.username)")
                            .font(.caption2)
                            .glassVibrancy(.secondary)
                            .lineLimit(1)
                    }
                }
            }
            .buttonStyle(.plain)

            Spacer()

            // Download Action
            GlassToolbarButton(
                systemName: "arrow.down.to.line",
                accessibilityLabel: "Download photo")
            {
                downloadFeedbackTrigger += 1
                viewModel.trackDownload()
                if let url = URL(string: photo.urls.full) {
                    URLHelper.open(url)
                }
            }

            // Toggle Inspector Button
            GlassToolbarButton(
                systemName: "info.circle",
                accessibilityLabel: "Inspect photo details")
            {
                withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                    showInspector = true
                }
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background {
            Capsule()
                .fill(Color.black.opacity(0.42))
        }
        .glassCapsule(style: .ultraThin, showBorder: true, hasShadow: true)
        .environment(\.colorScheme, .dark)
        .padding(.horizontal, 16)
        .padding(.bottom, 24)
        .transition(.move(edge: .bottom).combined(with: .opacity))
    }

    @ViewBuilder
    private func ambientBackdrop(photo: Photo) -> some View {
        if reduceTransparency {
            Color.black.ignoresSafeArea()
        } else {
            ZStack {
                Color.black
                KFImage(URL(string: photo.urls.small))
                    .placeholder {
                        Rectangle().fill(Color(hex: photo.color ?? "1E1E24"))
                    }
                    .resizable()
                    .scaledToFill()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .scaleEffect(1.3)
                    .blur(radius: 60)
                    .clipped()
                Color.black.opacity(0.35)
            }
            .ignoresSafeArea()
            .allowsHitTesting(false)
        }
    }
}

struct PhotoInspectorView: View {
    let photo: Photo
    let viewModel: PhotoDetailsViewModel
    let onUserSelect: (String) -> Void
    let onTagSelect: (String) -> Void
    var onDownload: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            // Photographer Info Row
            HStack(spacing: 12) {
                KFImage(URL(string: photo.user.profileImage.medium))
                    .resizable()
                    .scaledToFill()
                    .frame(width: 44, height: 44)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(.white.opacity(0.4), lineWidth: 1.5))

                VStack(alignment: .leading, spacing: 2) {
                    Text(photo.user.name)
                        .font(.subheadline.weight(.bold))
                        .glassVibrancy(.primary)
                    Text("@\(photo.user.username)")
                        .font(.caption)
                        .glassVibrancy(.secondary)
                }

                Spacer()

                Button(action: {
                    onUserSelect(photo.user.username)
                }) {
                    HStack(spacing: 4) {
                        Text("Profile")
                            .font(.caption.weight(.semibold))
                        Image(systemName: "arrow.up.right")
                            .font(.caption2.weight(.bold))
                    }
                    .glassVibrancy(.primary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .glassCapsule(style: .ultraThin)
                }
            }
            .padding(14)
            .glassCard(cornerRadius: 12, style: .ultraThin)

            VStack(alignment: .leading, spacing: 16) {
                // Title / Description
                if let description = photo.description_ ?? photo.altDescription {
                    Text(description)
                        .font(.body)
                        .lineSpacing(4)
                        .glassVibrancy(.primary)
                }

                // Metrics Grid
                HStack(spacing: 12) {
                    MetricCard(label: "Views", value: formatMetric(viewModel.stats?.views.total), systemImage: "eye.fill")
                    MetricCard(label: "Downloads", value: formatMetric(viewModel.stats?.downloads.total), systemImage: "arrow.down.circle.fill")
                    MetricCard(label: "Likes", value: formatMetric(viewModel.stats?.likes?.total), systemImage: "heart.fill")
                }

                // Interactive Chart Section
                if let viewsHist = viewModel.stats?.views.historical, !viewsHist.values.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("HISTORICAL VIEWS (LAST 30 DAYS)")
                            .font(.caption.weight(.bold))
                            .glassVibrancy(.secondary)
                            .tracking(1)

                        HistoricalStatsChart(values: viewsHist.values)
                            .padding(16)
                            .glassCard(cornerRadius: 12, style: .ultraThin)
                    }
                }

                // Action Download button
                Button(action: {
                    onDownload?()
                    viewModel.trackDownload()
                    if let url = URL(string: photo.urls.full) {
                        URLHelper.open(url)
                    }
                }) {
                    HStack {
                        Spacer()
                        Image(systemName: "arrow.down.doc.fill")
                        Text("Download High Resolution")
                            .font(.subheadline.weight(.bold))
                        Spacer()
                    }
                    .glassVibrancy(.primary)
                    .padding(.vertical, 14)
                    .glassBackground(style: .thin, shape: RoundedRectangle(cornerRadius: 12), showBorder: true)
                }

                // EXIF Glassmorphic Card
                if let exif = photo.exif, hasExif(exif) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("CAMERA & LENS SPECS")
                            .font(.caption.weight(.bold))
                            .glassVibrancy(.secondary)
                            .tracking(1)

                        VStack(spacing: 12) {
                            ExifRowView(label: "Camera", value: formatCamera(make: exif.make, model: exif.model))
                            ExifRowView(label: "Aperture", value: exif.aperture != nil ? "f/\(exif.aperture!)" : nil)
                            ExifRowView(label: "Exposure Time", value: exif.exposureTime != nil ? "\(exif.exposureTime!)s" : nil)
                            ExifRowView(label: "Focal Length", value: exif.focalLength != nil ? "\(exif.focalLength!)mm" : nil)
                            ExifRowView(label: "ISO", value: exif.iso != nil ? "\(exif.iso!)" : nil)
                        }
                        .padding(16)
                        .glassCard(cornerRadius: 12, style: .ultraThin)
                    }
                }

                // MapKit Coordinates Card
                if let location = photo.location, hasLocation(location) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("LOCATION")
                            .font(.caption.weight(.bold))
                            .glassVibrancy(.secondary)
                            .tracking(1)

                        VStack(alignment: .leading, spacing: 10) {
                            if let name = location.name {
                                Text(name)
                                    .font(.subheadline.weight(.bold))
                                    .glassVibrancy(.primary)
                            }

                            if let latValue = location.position?.latitude, let lonValue = location.position?.longitude {
                                let lat = Double(truncating: latValue)
                                let lon = Double(truncating: lonValue)
                                MapCardView(latitude: lat, longitude: lon, name: location.name)
                                    .onTapGesture {
                                        let encodedName = location.name?.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
                                        let urlString = "maps://?q=\(encodedName)&ll=\(lat),\(lon)"
                                        if let url = URL(string: urlString) {
                                            URLHelper.open(url)
                                        }
                                    }
                            }
                        }
                        .padding(16)
                        .glassCard(cornerRadius: 12, style: .ultraThin)
                    }
                }

                // Related Tags / Badges Flow
                if let tags = photo.tags, !tags.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("RELATED TAGS")
                            .font(.caption.weight(.bold))
                            .glassVibrancy(.secondary)
                            .tracking(1)

                        FlowLayout(spacing: 8) {
                            ForEach(tags, id: \.title) { tag in
                                Button(action: {
                                    onTagSelect(tag.title)
                                }) {
                                    Text(tag.title.uppercased())
                                        .font(.caption2.weight(.bold))
                                        .glassVibrancy(.primary)
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 6)
                                        .glassCapsule(style: .ultraThin)
                                }
                                .buttonStyle(PlainButtonStyle())
                            }
                        }
                    }
                }
            }
        }
    }

    private func formatMetric(_ value: Int32?) -> String {
        guard let value else { return "--" }
        return Int(value).formatted(.number.notation(.compactName))
    }

    private func hasExif(_ exif: Exif) -> Bool {
        exif.make != nil || exif.model != nil || exif.exposureTime != nil || exif.aperture != nil || exif.iso != nil
    }

    private func formatCamera(make: String?, model: String?) -> String? {
        guard make != nil || model != nil else { return nil }
        return "\(make ?? "") \(model ?? "")".trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func hasLocation(_ location: Location) -> Bool {
        if let latValue = location.position?.latitude, let lonValue = location.position?.longitude {
            let lat = Double(truncating: latValue)
            let lon = Double(truncating: lonValue)
            if lat == 0.0, lon == 0.0 {
                return false
            }
        }
        return location.name != nil || location.position != nil
    }
}

struct MetricCard: View {
    let label: LocalizedStringKey
    let value: String
    let systemImage: String

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: systemImage)
                .font(.title3)
                .glassVibrancy(.secondary)
            Text(value)
                .font(.subheadline.weight(.bold))
                .glassVibrancy(.primary)
            Text(label)
                .font(.caption2.weight(.semibold))
                .glassVibrancy(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .glassCard(cornerRadius: 12, style: .ultraThin)
    }
}

struct ExifRowView: View {
    let label: LocalizedStringKey
    let value: String?

    var body: some View {
        if let value, !value.isEmpty {
            HStack {
                Text(label)
                    .font(.footnote)
                    .glassVibrancy(.secondary)
                Spacer()
                Text(value)
                    .font(.footnote.weight(.semibold))
                    .glassVibrancy(.primary)
            }
        }
    }
}

struct HistoricalStatsChart: View {
    let values: [StatsValue]
    @State private var selectedIndex: Int?

    private static let isoFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withFullDate, .withDashSeparatorInDate]
        return formatter
    }()

    private var selectedItem: StatsValue? {
        guard let index = selectedIndex, values.indices.contains(index) else { return nil }
        return values[index]
    }

    var body: some View {
        if values.isEmpty {
            Text("No historical data available")
                .font(.caption)
                .glassVibrancy(.secondary)
                .frame(maxWidth: .infinity, minHeight: 120, alignment: .center)
        } else {
            VStack(alignment: .leading, spacing: 8) {
                // Interactive metric callout banner
                HStack(alignment: .firstTextBaseline) {
                    if let selectedItem {
                        HStack(spacing: 6) {
                            Text(formatDate(selectedItem.date)).font(.caption.weight(.medium)).glassVibrancy(.secondary)
                            Text("•").font(.caption2).glassVibrancy(.tertiary)
                            Text(formatViewsCount(selectedItem.value)).font(.caption.weight(.bold)).glassVibrancy(.primary)
                        }
                        .transition(.opacity)
                    } else {
                        HStack(spacing: 4) {
                            Image(systemName: "hand.point.left.and.right").font(.caption2)
                            Text("Drag curve to inspect daily views").font(.caption2)
                        }
                        .glassVibrancy(.secondary)
                        .transition(.opacity)
                    }
                    Spacer()
                }
                .frame(height: 20)

                Chart {
                    ForEach(values, id: \.date) { item in
                        LineMark(
                            x: .value("Date", item.date),
                            y: .value("Views", item.value))
                            .foregroundStyle(.white)
                            .interpolationMethod(.catmullRom)

                        AreaMark(
                            x: .value("Date", item.date),
                            y: .value("Views", item.value))
                            .foregroundStyle(
                                LinearGradient(
                                    colors: [.white.opacity(0.2), .clear],
                                    startPoint: .top,
                                    endPoint: .bottom))
                            .interpolationMethod(.catmullRom)
                    }

                    // Single point fallback visibility
                    if values.count == 1, let singleItem = values.first {
                        PointMark(
                            x: .value("Date", singleItem.date),
                            y: .value("Views", singleItem.value))
                            .foregroundStyle(.white)
                            .symbolSize(60)
                    }

                    // Glowing indicator and inspection rule for scrubbed data point
                    if let selectedItem {
                        RuleMark(x: .value("Date", selectedItem.date))
                            .foregroundStyle(Color.white.opacity(0.35))
                            .lineStyle(StrokeStyle(lineWidth: 1, dash: [4, 4]))

                        PointMark(
                            x: .value("Date", selectedItem.date),
                            y: .value("Views", selectedItem.value))
                            .symbol {
                                ZStack {
                                    Circle().fill(Color.white.opacity(0.25)).frame(width: 18, height: 18)
                                    Circle().fill(Color.white).frame(width: 8, height: 8).shadow(color: .white, radius: 4)
                                }
                            }
                    }
                }
                .chartXAxis(.hidden)
                .chartYAxis(.hidden)
                .frame(height: 120)
                .chartOverlay { proxy in
                    GeometryReader { geometry in
                        Rectangle()
                            .fill(Color.clear)
                            .contentShape(Rectangle())
                            .gesture(
                                DragGesture(minimumDistance: 0)
                                    .onChanged { value in
                                        updateSelection(at: value.location, proxy: proxy, geometry: geometry)
                                    }
                                    .onEnded { _ in
                                        withAnimation(.easeOut(duration: 0.2)) {
                                            selectedIndex = nil
                                        }
                                    })
                    }
                }
            }
            .sensoryFeedback(.impact(weight: .light), trigger: selectedIndex)
        }
    }

    private func updateSelection(at location: CGPoint, proxy: ChartProxy, geometry: GeometryProxy) {
        guard !values.isEmpty else {
            selectedIndex = nil
            return
        }
        guard values.count > 1 else {
            selectedIndex = 0
            return
        }

        let plotArea = proxy.plotFrame.map { geometry[$0] } ?? geometry.frame(in: .local)

        // Attempt exact category coordinate matching via ChartProxy
        var closestIndex = 0
        var minDistance = CGFloat.greatestFiniteMagnitude
        var resolvedFromProxy = false

        for (index, item) in values.enumerated() {
            if let posX = proxy.position(forX: item.date) {
                resolvedFromProxy = true
                let distance = abs(posX - location.x)
                if distance < minDistance {
                    minDistance = distance
                    closestIndex = index
                }
            }
        }

        if resolvedFromProxy {
            selectedIndex = closestIndex
            return
        }

        // Proportional interpolation fallback across plot area width
        let clampedX = min(max(location.x, plotArea.minX), plotArea.maxX)
        let fraction = (clampedX - plotArea.minX) / max(plotArea.width, 1.0)
        let computedIndex = Int(round(fraction * CGFloat(values.count - 1)))
        selectedIndex = min(max(computedIndex, 0), values.count - 1)
    }

    private func formatDate(_ dateString: String) -> String {
        if let date = Self.isoFormatter.date(from: dateString) {
            return date.formatted(date: .abbreviated, time: .omitted)
        }
        return dateString
    }

    private func formatViewsCount(_ count: Int32) -> String {
        String(localized: "\(count) views")
    }
}

struct MapCardView: View {
    let latitude: Double
    let longitude: Double
    let name: String?

    @State private var position: MapCameraPosition

    init(latitude: Double, longitude: Double, name: String?) {
        self.latitude = latitude
        self.longitude = longitude
        self.name = name
        let coordinate = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        let region = MKCoordinateRegion(
            center: coordinate,
            span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05))
        self._position = State(initialValue: .region(region))
    }

    var body: some View {
        Map(position: $position) {
            Marker(name ?? String(localized: "Photo Location"), coordinate: CLLocationCoordinate2D(latitude: latitude, longitude: longitude))
                .tint(.white)
        }
        .frame(height: 150)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

/// FlowLayout for dynamic SwiftUI wrapping badges
struct FlowLayout: Layout {
    var spacing: CGFloat

    init(spacing: CGFloat = 8) {
        self.spacing = spacing
    }

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let sizes = subviews.map { $0.sizeThatFits(.unspecified) }
        var width: CGFloat = 0
        var height: CGFloat = 0
        var currentX: CGFloat = 0
        var currentY: CGFloat = 0
        var maxRowHeight: CGFloat = 0
        let maxW = proposal.width ?? .infinity

        for size in sizes {
            if currentX + size.width > maxW {
                currentX = 0
                currentY += maxRowHeight + spacing
                maxRowHeight = 0
            }
            currentX += size.width + spacing
            width = max(width, currentX)
            maxRowHeight = max(maxRowHeight, size.height)
            height = max(height, currentY + size.height)
        }

        return CGSize(width: width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let sizes = subviews.map { $0.sizeThatFits(.unspecified) }
        var currentX: CGFloat = bounds.minX
        var currentY: CGFloat = bounds.minY
        var maxRowHeight: CGFloat = 0
        let maxW = bounds.width

        for index in subviews.indices {
            let size = sizes[index]
            if currentX + size.width > bounds.minX + maxW {
                currentX = bounds.minX
                currentY += maxRowHeight + spacing
                maxRowHeight = 0
            }
            subviews[index].place(at: CGPoint(x: currentX, y: currentY), proposal: .unspecified)
            currentX += size.width + spacing
            maxRowHeight = max(maxRowHeight, size.height)
        }
    }
}
