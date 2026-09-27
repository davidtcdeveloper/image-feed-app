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
        if let onDismiss = onDismiss {
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
        onTagSelect: @escaping (String) -> Void
    ) {
        self.photoId = photoId
        self.heroNamespace = heroNamespace
        self.onDismiss = onDismiss
        self.onUserSelect = onUserSelect
        self.onTagSelect = onTagSelect
        self._viewModel = State(initialValue: PhotoDetailsViewModel(photoId: photoId))
    }

    var body: some View {
        ZStack {
            Color(hex: "0F0F11")
                .ignoresSafeArea()

            if viewModel.isLoading, viewModel.photo == nil {
                ProgressView()
                    .tint(.white)
            } else if let error = viewModel.error, viewModel.photo == nil {
                VStack(spacing: 16) {
                    Text("Failed to load details")
                        .font(.headline)
                        .foregroundColor(.white)
                    Text(error)
                        .font(.subheadline)
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                    Button(action: { viewModel.loadDetails() }) {
                        Text("Retry")
                            .fontWeight(.medium)
                            .foregroundColor(.black)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 8)
                            .background(Color.white)
                            .cornerRadius(20)
                    }
                }
            } else if let photo = viewModel.photo {
                if isDualPane {
                    dualPaneLayout(photo: photo)
                } else {
                    compactPhotoLayout(photo: photo)
                }
            }
        }
        .navigationBarBackButtonHidden(true)
        .overlay(alignment: .topLeading) {
            if onDismiss != nil {
                GlassToolbarButton(systemName: "chevron.left", accessibilityLabel: "Back") {
                    handleDismiss()
                }
                .padding(.top, 60)
                .padding(.leading, 16)
            }
        }
        .overlay(alignment: .topTrailing) {
            if onDismiss != nil && !isDualPane {
                GlassToolbarButton(
                    systemName: showInspector ? "info.circle.fill" : "info.circle",
                    accessibilityLabel: "Photo Details"
                ) {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                        showInspector.toggle()
                    }
                }
                .padding(.top, 60)
                .padding(.trailing, 16)
            }
        }
        .sensoryFeedback(.success, trigger: downloadFeedbackTrigger)
        #if os(iOS)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .sheet(isPresented: Binding(
            get: { showInspector && !isDualPane && viewModel.photo != nil },
            set: { showInspector = $0 }
        )) {
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
                        }
                    )
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
                        Color(hex: "0F0F11")
                    } else {
                        ZStack {
                            Color(hex: "0A0A0C").opacity(0.82)
                            Rectangle().fill(.regularMaterial)
                        }
                    }
                }
            }
        }
        #endif
        .toolbar {
            #if os(iOS)
            ToolbarItem(placement: .navigationBarLeading) {
                GlassToolbarButton(systemName: "chevron.left", accessibilityLabel: "Back") {
                    handleDismiss()
                }
            }
            if !isDualPane {
                ToolbarItem(placement: .navigationBarTrailing) {
                    GlassToolbarButton(
                        systemName: showInspector ? "info.circle.fill" : "info.circle",
                        accessibilityLabel: "Photo Details"
                    ) {
                        withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                            showInspector.toggle()
                        }
                    }
                }
            }
            #else
            ToolbarItem(placement: .navigation) {
                GlassToolbarButton(systemName: "chevron.left", accessibilityLabel: "Back") {
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
    @ViewBuilder
    func dualPaneLayout(photo: Photo) -> some View {
        GeometryReader { totalGeo in
            let inspectorWidth = max(320, min(420, totalGeo.size.width * 0.42))

            HStack(spacing: 0) {
                // Left: Photo Canvas
                ZStack(alignment: .bottomLeading) {
                    Color(hex: "070709")
                        .ignoresSafeArea()

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
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(.white)
                            Text("@\(photo.user.username)")
                                .font(.system(size: 11))
                                .foregroundColor(.gray)
                        }

                        Spacer()

                        Button(action: {
                            onUserSelect(photo.user.username)
                        }) {
                            Image(systemName: "arrow.up.right")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }
                    .padding(12)
                    .background {
                        Capsule()
                            .fill(Color.black.opacity(0.42))
                    }
                    .glassCapsule(style: .ultraThin, showBorder: true, hasShadow: true)
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
                        }
                    )
                    .padding(20)
                    .padding(.bottom, 32)
                }
                .frame(width: inspectorWidth)
                .background(Color(hex: "0F0F11"))
            }
        }
        .ignoresSafeArea(edges: .top)
    }

    @ViewBuilder
    func compactPhotoLayout(photo: Photo) -> some View {
        GeometryReader { geo in
            let containerWidth = geo.size.width
            let aspectRatio = max(0.1, CGFloat(photo.width) / CGFloat(photo.height))
            let calculatedHeight = containerWidth / aspectRatio
            let imageUrl = photo.urls.raw + "&w=\(Int(containerWidth * 2))&q=85&auto=format"

            ZStack(alignment: .bottom) {
                Color(hex: "070709")
                    .ignoresSafeArea()

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
                                }
                        )
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
                                        height: max(-maxOffsetY, min(maxOffsetY, newY))
                                    )
                                }
                                .onEnded { _ in
                                    guard currentScale > 1.05 else {
                                        finalOffset = .zero
                                        currentOffset = .zero
                                        return
                                    }
                                    finalOffset = currentOffset
                                }
                        )
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

    @ViewBuilder
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
                        .overlay(Circle().stroke(.white.opacity(0.4), lineWidth: 1.5))

                    VStack(alignment: .leading, spacing: 2) {
                        Text(photo.user.name)
                            .font(.system(size: 13, weight: .bold))
                            .glassVibrancy(.primary)
                            .lineLimit(1)
                        Text("@\(photo.user.username)")
                            .font(.system(size: 11))
                            .glassVibrancy(.secondary)
                            .lineLimit(1)
                    }
                }
            }
            .buttonStyle(.plain)

            Spacer()

            // Download Action
            Button(action: {
                downloadFeedbackTrigger += 1
                viewModel.trackDownload()
                if let url = URL(string: photo.urls.full) {
                    URLHelper.open(url)
                }
            }) {
                Image(systemName: "arrow.down.to.line")
                    .font(.system(size: 14, weight: .semibold))
                    .glassVibrancy(.primary)
                    .frame(width: 34, height: 34)
                    .background(Circle().fill(.white.opacity(0.12)))
            }

            // Toggle Inspector Button
            Button(action: {
                withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                    showInspector = true
                }
            }) {
                Image(systemName: "info.circle")
                    .font(.system(size: 15, weight: .semibold))
                    .glassVibrancy(.primary)
                    .frame(width: 34, height: 34)
                    .background(Circle().fill(.white.opacity(0.12)))
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background {
            Capsule()
                .fill(Color.black.opacity(0.42))
        }
        .glassCapsule(style: .ultraThin, showBorder: true, hasShadow: true)
        .padding(.horizontal, 16)
        .padding(.bottom, 24)
        .transition(.move(edge: .bottom).combined(with: .opacity))
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
                        .font(.system(size: 15, weight: .bold))
                        .glassVibrancy(.primary)
                    Text("@\(photo.user.username)")
                        .font(.system(size: 12))
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
                        .font(.system(size: 15))
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
                            .font(.system(size: 11, weight: .bold))
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
                            .fontWeight(.bold)
                        Spacer()
                    }
                    .foregroundColor(.black)
                    .padding(.vertical, 14)
                    .background(Color.white)
                    .cornerRadius(8)
                }

                // EXIF Glassmorphic Card
                if let exif = photo.exif, hasExif(exif) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("CAMERA & LENS SPECS")
                            .font(.system(size: 11, weight: .bold))
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
                            .font(.system(size: 11, weight: .bold))
                            .glassVibrancy(.secondary)
                            .tracking(1)

                        VStack(alignment: .leading, spacing: 10) {
                            if let name = location.name {
                                Text(name)
                                    .font(.system(size: 14, weight: .bold))
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
                            .font(.system(size: 11, weight: .bold))
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
        let num = Int(value)
        if num >= 1000000 {
            return String(format: "%.1fM", Double(num) / 1000000.0)
        } else if num >= 1000 {
            return String(format: "%.1fK", Double(num) / 1000.0)
        } else {
            return "\(num)"
        }
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
            if lat == 0.0 && lon == 0.0 {
                return false
            }
        }
        return location.name != nil || location.position != nil
    }
}

struct MetricCard: View {
    let label: String
    let value: String
    let systemImage: String

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: systemImage)
                .font(.system(size: 18))
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
    let label: String
    let value: String?

    var body: some View {
        if let value, !value.isEmpty {
            HStack {
                Text(label)
                    .font(.system(size: 13))
                    .glassVibrancy(.secondary)
                Spacer()
                Text(value)
                    .font(.system(size: 13, weight: .semibold))
                    .glassVibrancy(.primary)
            }
        }
    }
}

struct HistoricalStatsChart: View {
    let values: [StatsValue]

    var body: some View {
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
        }
        .chartXAxis(.hidden)
        .chartYAxis(.hidden)
        .frame(height: 120)
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
            Marker(name ?? "Photo Location", coordinate: CLLocationCoordinate2D(latitude: latitude, longitude: longitude))
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
