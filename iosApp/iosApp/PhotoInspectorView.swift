import Charts
import MapKit
import shared
import SwiftUI
#if os(iOS)
import UIKit
#elseif os(macOS)
import AppKit
#endif

struct PhotoInspectorView: View {
    let photo: Photo
    let viewModel: PhotoDetailsViewModel
    let onUserSelect: (String) -> Void
    let onTagSelect: (String) -> Void
    var onDownload: (() -> Void)?
    var heroNamespace: Namespace.ID?

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.containerWidth) private var containerWidth
    @State private var copyFeedbackTrigger = 0
    @State private var downloadBounceTrigger = 0

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            photographerCard

            VStack(alignment: .leading, spacing: 16) {
                if let description = photo.description_ ?? photo.altDescription {
                    Text(description)
                        .font(.body)
                        .lineSpacing(4)
                        .glassVibrancy(.primary)
                }

                HStack(spacing: 12) {
                    MetricCard(label: "Views", value: formatMetric(viewModel.stats?.views.total), systemImage: "eye.fill")
                    MetricCard(label: "Downloads", value: formatMetric(viewModel.stats?.downloads.total), systemImage: "arrow.down.circle.fill")
                    MetricCard(label: "Likes", value: formatMetric(viewModel.stats?.likes?.total), systemImage: "heart.fill")
                }

                if let viewsHist = viewModel.stats?.views.historical, !viewsHist.values.isEmpty {
                    historicalViewsSection(values: viewsHist.values)
                }

                splitDownloadAndBookmarkRow

                if let exif = photo.exif, hasExif(exif) {
                    exifBentoSection(exif: exif)
                }

                if !tonalSwatches.isEmpty {
                    tonalSwatchesSection
                }

                if let location = photo.location, hasLocation(location) {
                    locationSection(location: location)
                }

                if let tags = photo.tags, !tags.isEmpty {
                    relatedTagsSection(tags: tags)
                }
            }
        }
    }
}

// MARK: - Inspector Sections & Formatting

private extension PhotoInspectorView {
    var tonalSwatches: [PhotoTonalSwatch] {
        PhotoColorEngine.shared.deriveTonalSwatches(seedHex: photo.color)
    }

    var accentHex: String? {
        PhotoColorEngine.shared.deriveAccentHex(seedHex: photo.color, isDark: colorScheme == .dark)
    }

    var accentColor: Color? {
        accentHex.map { Color(hex: $0) }
    }

    var resolvedAccentColor: Color {
        accentColor ?? .accentColor
    }

    var photographerCard: some View {
        HStack(spacing: 12) {
            photographerAvatar

            VStack(alignment: .leading, spacing: 2) {
                Text(photo.user.name)
                    .font(.subheadline.weight(.bold))
                    .glassVibrancy(.primary)
                Text("@\(photo.user.username)")
                    .font(.caption)
                    .glassVibrancy(.secondary)
            }

            Spacer()

            Button {
                onUserSelect(photo.user.username)
            } label: {
                HStack(spacing: 4) {
                    Text("Profile")
                        .font(.caption.weight(.semibold))
                    Image(systemName: "arrow.up.right")
                        .font(.caption2.weight(.bold))
                }
                .glassVibrancy(.primary)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .matteCapsule(fillColor: GlassTheme.subtleFillColor)
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .matteSurface(cornerRadius: 12)
    }

    @ViewBuilder
    var photographerAvatar: some View {
        let baseAvatar = KFImage(URL(string: photo.user.profileImage.medium))
            .resizable()
            .scaledToFill()
            .frame(width: 44, height: 44)
            .clipShape(Circle())
            .overlay(Circle().stroke(GlassTheme.matteBorderColor, lineWidth: 1.5))

        #if os(iOS)
        if #available(iOS 18.0, *), let heroNamespace {
            baseAvatar.matchedTransitionSource(
                id: AppNavigationDefaults.userSourceID(photo.user.username),
                in: heroNamespace)
            { source in
                source.clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            }
        } else {
            baseAvatar
        }
        #else
        baseAvatar
        #endif
    }

    func historicalViewsSection(values: [StatsValue]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("HISTORICAL VIEWS (LAST 30 DAYS)")
                .font(.caption.weight(.bold))
                .glassVibrancy(.secondary)
                .tracking(1)

            HistoricalStatsChart(values: values, accentColor: accentColor)
                .padding(16)
                .matteSurface(cornerRadius: 12)
        }
    }

    var splitDownloadAndBookmarkRow: some View {
        HStack(spacing: 12) {
            HStack(spacing: 0) {
                Button {
                    triggerDownload(urlString: photo.urls.full)
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "arrow.down.doc.fill")
                            .symbolEffect(.bounce, value: downloadBounceTrigger)
                        Text("Full")
                            .font(.subheadline.weight(.bold))
                            .lineLimit(1)
                    }
                    .glassVibrancy(.primary)
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text("Download Full"))

                Rectangle()
                    .fill(GlassTheme.matteBorderColor)
                    .frame(width: 1, height: 24)

                Button {
                    triggerDownload(urlString: photo.urls.raw)
                } label: {
                    Text("RAW")
                        .font(.subheadline.weight(.bold))
                        .glassVibrancy(.primary)
                        .padding(.horizontal, 18)
                        .frame(minHeight: 48)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text("Download RAW"))
            }
            .matteSurface(cornerRadius: 12)

            Button {
                viewModel.toggleFavorite(fallbackPhoto: photo)
            } label: {
                Image(systemName: viewModel.isFavorite ? "bookmark.fill" : "bookmark")
                    .font(.title3.weight(.semibold))
                    .contentTransition(.symbolEffect(.replace))
                    .foregroundStyle(viewModel.isFavorite ? resolvedAccentColor : Color.primary)
                    .frame(width: 50, height: 48)
                    .matteSurface(
                        cornerRadius: 12,
                        fillColor: viewModel.isFavorite ? resolvedAccentColor.opacity(0.14) : nil)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(viewModel.isFavorite ? "Remove Bookmark" : "Bookmark"))
        }
    }

    func exifBentoSection(exif: Exif) -> some View {
        let columnCount = containerWidth >= 520 ? 4 : 2
        let columns = Array(repeating: GridItem(.flexible(), spacing: 10), count: columnCount)

        return VStack(alignment: .leading, spacing: 8) {
            Text("CAMERA & LENS SPECS")
                .font(.caption.weight(.bold))
                .glassVibrancy(.secondary)
                .tracking(1)

            if let cameraTitle = formatCamera(make: exif.make, model: exif.model) {
                HStack(spacing: 10) {
                    Image(systemName: "camera")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(accentColor ?? .secondary)
                    Text(cameraTitle)
                        .font(.subheadline.weight(.semibold))
                        .glassVibrancy(.primary)
                        .lineLimit(2)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .matteSurface(cornerRadius: 12)
            }

            LazyVGrid(columns: columns, spacing: 10) {
                ExifBentoCell(
                    systemImage: "camera.aperture",
                    label: "Aperture",
                    value: formatAperture(exif.aperture),
                    accentColor: accentColor)
                ExifBentoCell(
                    systemImage: "timer",
                    label: "Exposure Time",
                    value: formatExposureTime(exif.exposureTime),
                    accentColor: accentColor)
                ExifBentoCell(
                    systemImage: "scope",
                    label: "Focal Length",
                    value: formatFocalLength(exif.focalLength),
                    accentColor: accentColor)
                ExifBentoCell(
                    systemImage: "dial.medium",
                    label: "ISO",
                    value: exif.iso.map { "\($0)" } ?? "--",
                    accentColor: accentColor)
            }
        }
    }

    var tonalSwatchesSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("COLOR PALETTE")
                .font(.caption.weight(.bold))
                .glassVibrancy(.secondary)
                .tracking(1)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(tonalSwatches, id: \.hex) { swatch in
                        Button {
                            copyHexToClipboard(swatch.hex)
                            copyFeedbackTrigger += 1
                            viewModel.showCopiedColorToast(hex: swatch.hex)
                        } label: {
                            HStack(spacing: 8) {
                                Circle()
                                    .fill(Color(hex: swatch.hex))
                                    .frame(width: 16, height: 16)
                                    .overlay(Circle().strokeBorder(GlassTheme.matteBorderColor, lineWidth: 1))
                                Text(swatch.hex)
                                    .font(.caption.weight(.semibold).monospaced())
                                    .glassVibrancy(.primary)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .matteCapsule()
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(Text("Copy color \(swatch.hex)"))
                    }
                }
            }
        }
        .sensoryFeedback(.success, trigger: copyFeedbackTrigger)
    }

    func locationSection(location: Location) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("LOCATION")
                .font(.caption.weight(.bold))
                .glassVibrancy(.secondary)
                .tracking(1)

            VStack(alignment: .leading, spacing: 10) {
                if let name = location.name {
                    HStack(spacing: 8) {
                        Image(systemName: "mappin.and.ellipse")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(resolvedAccentColor)
                        Text(name)
                            .font(.subheadline.weight(.bold))
                            .glassVibrancy(.primary)
                    }
                }

                if let latValue = location.position?.latitude, let lonValue = location.position?.longitude {
                    let lat = Double(truncating: latValue)
                    let lon = Double(truncating: lonValue)
                    MapCardView(latitude: lat, longitude: lon, name: location.name, pinColorHex: accentHex)
                        .onTapGesture {
                            let encodedName = location.name?.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
                            if let url = URL(string: "maps://?q=\(encodedName)&ll=\(lat),\(lon)") {
                                URLHelper.open(url)
                            }
                        }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .matteSurface(cornerRadius: 12)
        }
    }

    func relatedTagsSection(tags: [Tag]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("RELATED TAGS")
                .font(.caption.weight(.bold))
                .glassVibrancy(.secondary)
                .tracking(1)

            FlowLayout(spacing: 8) {
                ForEach(tags, id: \.title) { tag in
                    Button {
                        onTagSelect(tag.title)
                    } label: {
                        Text(tag.title.uppercased())
                            .font(.caption2.weight(.bold))
                            .glassVibrancy(.primary)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .matteCapsule(fillColor: GlassTheme.subtleFillColor)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    func triggerDownload(urlString: String) {
        downloadBounceTrigger += 1
        onDownload?()
        viewModel.trackDownload(fallbackPhoto: photo)
        if let url = URL(string: urlString) {
            URLHelper.open(url)
        }
    }

    func copyHexToClipboard(_ hex: String) {
        #if os(iOS)
        UIPasteboard.general.string = hex
        #elseif os(macOS)
        NSPasteboard.general.clearContents()
        NSPasteboard.general.setString(hex, forType: .string)
        #endif
    }

    func formatMetric(_ value: Int32?) -> String {
        guard let value else { return "--" }
        return Int(value).formatted(.number.notation(.compactName))
    }

    func hasExif(_ exif: Exif) -> Bool {
        !(exif.make ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
            !(exif.model ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
            !(exif.exposureTime ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
            !(exif.aperture ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
            !(exif.focalLength ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
            exif.iso != nil
    }

    func formatCamera(make: String?, model: String?) -> String? {
        let cleanMake = make?.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanModel = model?.trimmingCharacters(in: .whitespacesAndNewlines)
        switch (cleanMake, cleanModel) {
        case (.some(let mk), .some(let md)) where !mk.isEmpty && !md.isEmpty:
            return md.localizedCaseInsensitiveContains(mk) ? md : "\(mk) \(md)"
        case (_, .some(let md)) where !md.isEmpty:
            return md
        case (.some(let mk), _) where !mk.isEmpty:
            return mk
        default:
            return nil
        }
    }

    func formatAperture(_ raw: String?) -> String {
        guard let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines), !trimmed.isEmpty else { return "--" }
        return trimmed.hasPrefix("f/") ? trimmed : "f/\(trimmed)"
    }

    func formatExposureTime(_ raw: String?) -> String {
        guard let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines), !trimmed.isEmpty else { return "--" }
        return trimmed.hasSuffix("s") ? trimmed : "\(trimmed)s"
    }

    func formatFocalLength(_ raw: String?) -> String {
        guard let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines), !trimmed.isEmpty else { return "--" }
        return trimmed.hasSuffix("mm") ? trimmed : "\(trimmed)mm"
    }

    func hasLocation(_ location: Location) -> Bool {
        if let latValue = location.position?.latitude, let lonValue = location.position?.longitude {
            if Double(truncating: latValue) == 0.0, Double(truncating: lonValue) == 0.0 {
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
        .matteSurface(cornerRadius: 12)
    }
}

struct ExifBentoCell: View {
    let systemImage: String
    let label: LocalizedStringKey
    let value: String
    let accentColor: Color?

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 6) {
                Image(systemName: systemImage)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(accentColor ?? .secondary)
                Text(label)
                    .font(.caption2.weight(.semibold))
                    .glassVibrancy(.secondary)
                    .lineLimit(1)
            }
            Text(value)
                .font(.subheadline.weight(.semibold))
                .glassVibrancy(.primary)
                .lineLimit(1)
                .minimumScaleFactor(0.85)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .matteSurface(cornerRadius: 12)
    }
}

struct HistoricalStatsChart: View {
    let values: [StatsValue]
    var accentColor: Color?
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

    private var markColor: Color {
        accentColor ?? .primary
    }

    var body: some View {
        if values.isEmpty {
            Text("No historical data available")
                .font(.caption)
                .glassVibrancy(.secondary)
                .frame(maxWidth: .infinity, minHeight: 120, alignment: .center)
        } else {
            VStack(alignment: .leading, spacing: 8) {
                calloutBanner
                chartContent
            }
            .sensoryFeedback(.impact(weight: .light), trigger: selectedIndex)
        }
    }

    private var calloutBanner: some View {
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
                    Image(systemName: "hand.draw").font(.caption2)
                    Text("Drag curve to inspect daily views").font(.caption2)
                }
                .glassVibrancy(.secondary)
                .transition(.opacity)
            }
            Spacer()
        }
        .frame(height: 20)
    }

    private var chartContent: some View {
        Chart {
            ForEach(values, id: \.date) { item in
                LineMark(x: .value("Date", item.date), y: .value("Views", item.value))
                    .foregroundStyle(markColor)
                    .interpolationMethod(.catmullRom)

                AreaMark(x: .value("Date", item.date), y: .value("Views", item.value))
                    .foregroundStyle(LinearGradient(colors: [markColor.opacity(0.22), .clear], startPoint: .top, endPoint: .bottom))
                    .interpolationMethod(.catmullRom)
            }

            if values.count == 1, let singleItem = values.first {
                PointMark(x: .value("Date", singleItem.date), y: .value("Views", singleItem.value))
                    .foregroundStyle(markColor)
                    .symbolSize(60)
            }

            if let selectedItem {
                RuleMark(x: .value("Date", selectedItem.date))
                    .foregroundStyle(markColor.opacity(0.35))
                    .lineStyle(StrokeStyle(lineWidth: 1, dash: [4, 4]))

                PointMark(x: .value("Date", selectedItem.date), y: .value("Views", selectedItem.value))
                    .symbol {
                        ZStack {
                            Circle().fill(markColor.opacity(0.25)).frame(width: 18, height: 18)
                            Circle().fill(markColor).frame(width: 8, height: 8).shadow(color: markColor.opacity(0.6), radius: 4)
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
    var pinColorHex: String?

    @State private var snapshotImage: PlatformImage?
    @Environment(\.colorScheme) private var colorScheme

    init(latitude: Double, longitude: Double, name: String?, pinColorHex: String? = nil) {
        self.latitude = latitude
        self.longitude = longitude
        self.name = name
        self.pinColorHex = pinColorHex
    }

    var body: some View {
        GeometryReader { geo in
            ZStack {
                if let snapshotImage {
                    #if os(iOS)
                    Image(uiImage: snapshotImage)
                        .resizable()
                        .scaledToFill()
                    #elseif os(macOS)
                    Image(nsImage: snapshotImage)
                        .resizable()
                        .scaledToFill()
                    #endif
                } else {
                    Rectangle()
                        .fill(GlassTheme.surfaceBackgroundColor)
                        .overlay {
                            ProgressView()
                                .tint(.secondary)
                        }
                }
            }
            .frame(width: geo.size.width, height: 150)
            .clipped()
            .task(id: "\(latitude)_\(longitude)_\(geo.size.width)_\(colorScheme)_\(pinColorHex ?? "default")") {
                await loadSnapshot(width: geo.size.width)
            }
        }
        .frame(height: 150)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func loadSnapshot(width: CGFloat) async {
        guard width > 0 else { return }
        let roundedLat = (latitude * 1000).rounded() / 1000
        let roundedLon = (longitude * 1000).rounded() / 1000
        let colorKey = pinColorHex ?? "default"
        let cacheKeyString = "map_snapshot_\(roundedLat)_\(roundedLon)_\(Int(width))x150_\(colorScheme == .dark ? "dark" : "light")_\(colorKey)"
        guard let cacheUrl = URL(string: "https://map.local/\(cacheKeyString)") else { return }

        if let cached = ImageMemoryCache.shared.image(for: cacheUrl) {
            snapshotImage = cached
            return
        }

        let coordinate = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        let options = makeSnapshotOptions(coordinate: coordinate, width: width)

        do {
            let snapshot = try await renderMapSnapshot(options: options)
            try Task.checkCancellation()
            let image = composePinnedImage(snapshot: snapshot, coordinate: coordinate, size: options.size)
            ImageMemoryCache.shared.insert(image, for: cacheUrl)
            snapshotImage = image
        } catch {
            // Snapshot failed or cancelled
        }
    }

    private func makeSnapshotOptions(coordinate: CLLocationCoordinate2D, width: CGFloat) -> MKMapSnapshotter.Options {
        let options = MKMapSnapshotter.Options()
        options.region = MKCoordinateRegion(
            center: coordinate,
            span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05))
        options.size = CGSize(width: width, height: 150)
        #if os(iOS)
        options.scale = UIScreen.main.scale
        options.traitCollection = UITraitCollection(userInterfaceStyle: colorScheme == .dark ? .dark : .light)
        #elseif os(macOS)
        options.appearance = NSAppearance(named: colorScheme == .dark ? .darkAqua : .aqua)
        #endif
        return options
    }

    private func renderMapSnapshot(options: MKMapSnapshotter.Options) async throws -> MKMapSnapshotter.Snapshot {
        let snapshotter = MKMapSnapshotter(options: options)
        return try await withCheckedThrowingContinuation { continuation in
            snapshotter.start { snapshot, error in
                if let error {
                    continuation.resume(throwing: error)
                } else if let snapshot {
                    continuation.resume(returning: snapshot)
                } else {
                    continuation.resume(throwing: URLError(.badServerResponse))
                }
            }
        }
    }

    private func composePinnedImage(
        snapshot: MKMapSnapshotter.Snapshot,
        coordinate: CLLocationCoordinate2D,
        size: CGSize) -> PlatformImage
    {
        #if os(iOS)
        let resolvedPinColor = pinColorHex.map { UIColor(Color(hex: $0)) } ?? .tintColor
        return UIGraphicsImageRenderer(size: size).image { _ in
            snapshot.image.draw(at: .zero)
            let point = snapshot.point(for: coordinate)
            let pinImage = UIImage(systemName: "mappin.circle.fill")?
                .withTintColor(resolvedPinColor, renderingMode: .alwaysOriginal)
            if let pinImage {
                let pinRect = CGRect(x: point.x - 12, y: point.y - 24, width: 24, height: 24)
                pinImage.draw(in: pinRect)
            }
        }
        #elseif os(macOS)
        let resolvedPinColor = pinColorHex.map { NSColor(Color(hex: $0)) } ?? .controlAccentColor
        let image = NSImage(size: size)
        image.lockFocus()
        snapshot.image.draw(at: .zero, from: .zero, operation: .copy, fraction: 1.0)
        let symbolConfig = NSImage.SymbolConfiguration(paletteColors: [resolvedPinColor])
        if let pinImage = NSImage(systemSymbolName: "mappin.circle.fill", accessibilityDescription: nil)?
            .withSymbolConfiguration(symbolConfig)
        {
            let point = snapshot.point(for: coordinate)
            let pinRect = NSRect(x: point.x - 12, y: point.y - 24, width: 24, height: 24)
            pinImage.draw(in: pinRect)
        }
        image.unlockFocus()
        return image
        #endif
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
