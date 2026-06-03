import SwiftUI
import MapKit
import Charts
import shared
import Kingfisher

struct PhotoDetailsView: View {
    let photoId: String
    let onUserSelect: (String) -> Void
    @State private var viewModel: PhotoDetailsViewModel
    @Environment(\.dismiss) private var dismiss

    init(photoId: String, onUserSelect: @escaping (String) -> Void) {
        self.photoId = photoId
        self.onUserSelect = onUserSelect
        self._viewModel = State(initialValue: PhotoDetailsViewModel(photoId: photoId))
    }

    var body: some View {
        ZStack {
            Color(hex: "0F0F11")
                .ignoresSafeArea()

            if viewModel.isLoading && viewModel.photo == nil {
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
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        // High resolution image section
                        GeometryReader { geo in
                            let aspectRatio = CGFloat(photo.width) / CGFloat(photo.height)
                            let imageUrl = photo.urls.raw + "&w=\(Int(geo.size.width))&q=85&auto=format"
                            
                            KFImage(URL(string: imageUrl))
                                .resizable()
                                .aspectRatio(aspectRatio, contentMode: .fill)
                                .frame(width: geo.size.width, height: geo.size.height)
                                .clipped()
                                .overlay(
                                    LinearGradient(
                                        colors: [.clear, .black.opacity(0.85)],
                                        startPoint: .top,
                                        endPoint: .bottom
                                    )
                                )
                        }
                        .aspectRatio(CGFloat(photo.width) / CGFloat(photo.height), contentMode: .fit)
                        .clipped()

                        VStack(alignment: .leading, spacing: 20) {
                            // Photographer Info Row
                            HStack(spacing: 12) {
                                KFImage(URL(string: photo.user.profileImage.medium))
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .frame(width: 44, height: 44)
                                    .clipShape(Circle())
                                    .overlay(Circle().stroke(.white.opacity(0.4), lineWidth: 1.5))

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(photo.user.name)
                                        .font(.system(size: 15, weight: .bold))
                                        .foregroundColor(.white)
                                    Text("@\(photo.user.username)")
                                        .font(.system(size: 12))
                                        .foregroundColor(.gray)
                                }

                                Spacer()

                                Button(action: {
                                    onUserSelect(photo.user.username)
                                }) {
                                    HStack(spacing: 4) {
                                        Text("Profile")
                                            .font(.system(size: 12, weight: .semibold))
                                        Image(systemName: "arrow.up.right")
                                            .font(.system(size: 10, weight: .bold))
                                    }
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(Color.white.opacity(0.15))
                                    .cornerRadius(16)
                                }
                            }
                            .padding(.top, -30) // Overlap slightly into gradient
                            .padding(.horizontal, 16)

                            VStack(alignment: .leading, spacing: 16) {
                                // Title / Description
                                if let description = photo.description ?? photo.altDescription {
                                    Text(description)
                                        .font(.system(size: 15))
                                        .lineSpacing(4)
                                        .foregroundColor(.white)
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
                                            .foregroundColor(.gray)
                                            .tracking(1)

                                        HistoricalStatsChart(values: viewsHist.values)
                                            .padding(16)
                                            .background(.ultraThinMaterial)
                                            .cornerRadius(12)
                                    }
                                }

                                // Action Download button
                                Button(action: {
                                    viewModel.trackDownload()
                                    if let url = URL(string: photo.urls.full) {
                                        UIApplication.shared.open(url)
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
                                            .foregroundColor(.gray)
                                            .tracking(1)

                                        VStack(spacing: 12) {
                                            ExifRowView(label: "Camera", value: formatCamera(make: exif.make, model: exif.model))
                                            ExifRowView(label: "Aperture", value: exif.aperture != nil ? "f/\(exif.aperture!)" : nil)
                                            ExifRowView(label: "Exposure Time", value: exif.exposureTime != nil ? "\(exif.exposureTime!)s" : nil)
                                            ExifRowView(label: "Focal Length", value: exif.focalLength != nil ? "\(exif.focalLength!)mm" : nil)
                                            ExifRowView(label: "ISO", value: exif.iso != nil ? "\(exif.iso!)" : nil)
                                        }
                                        .padding(16)
                                        .background(.ultraThinMaterial)
                                        .cornerRadius(12)
                                    }
                                }

                                // MapKit Coordinates Card
                                if let location = photo.location, hasLocation(location) {
                                    VStack(alignment: .leading, spacing: 8) {
                                        Text("LOCATION")
                                            .font(.system(size: 11, weight: .bold))
                                            .foregroundColor(.gray)
                                            .tracking(1)

                                        VStack(alignment: .leading, spacing: 10) {
                                            if let name = location.name {
                                                Text(name)
                                                    .font(.system(size: 14, weight: .bold))
                                                    .foregroundColor(.white)
                                            }

                                            if let latValue = location.position?.latitude, let lonValue = location.position?.longitude {
                                                let lat = Double(truncating: latValue)
                                                let lon = Double(truncating: lonValue)
                                                MapCardView(latitude: lat, longitude: lon, name: location.name)
                                                    .onTapGesture {
                                                        let urlString = "maps://?q=\(location.name?.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")&ll=\(lat),\(lon)"
                                                        if let url = URL(string: urlString) {
                                                            UIApplication.shared.open(url)
                                                        }
                                                    }
                                            }
                                        }
                                        .padding(16)
                                        .background(.ultraThinMaterial)
                                        .cornerRadius(12)
                                    }
                                }

                                // Related Tags / Badges Flow
                                if let tags = photo.tags, !tags.isEmpty {
                                    VStack(alignment: .leading, spacing: 8) {
                                        Text("RELATED TAGS")
                                            .font(.system(size: 11, weight: .bold))
                                            .foregroundColor(.gray)
                                            .tracking(1)

                                        FlowLayout(spacing: 8) {
                                            ForEach(tags, id: \.title) { tag in
                                                Text(tag.title.uppercased())
                                                    .font(.system(size: 10, weight: .bold))
                                                    .foregroundColor(.white)
                                                    .padding(.horizontal, 10)
                                                    .padding(.vertical, 6)
                                                    .background(Color.white.opacity(0.12))
                                                    .cornerRadius(14)
                                            }
                                        }
                                    }
                                }
                            }
                            .padding(.horizontal, 16)
                        }
                    }
                    .padding(.bottom, 32)
                }
                .ignoresSafeArea(edges: .top)
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(8)
                        .background(Circle().fill(Color.black.opacity(0.4)))
                }
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

    private func hasExif(_ exif: Exif) -> Bool {
        return exif.make != nil || exif.model != nil || exif.exposureTime != nil || exif.aperture != nil || exif.iso != nil
    }

    private func formatCamera(make: String?, model: String?) -> String? {
        guard make != nil || model != nil else { return nil }
        return "\(make ?? "") \(model ?? "")".trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func hasLocation(_ location: Location) -> Bool {
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
                .foregroundColor(.white.opacity(0.8))
            Text(value)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(.white)
            Text(label)
                .font(.system(size: 10, weight: .semibold))
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(Color(hex: "1E1E24"))
        .cornerRadius(12)
    }
}

struct ExifRowView: View {
    let label: String
    let value: String?

    var body: some View {
        if let value = value, !value.isEmpty {
            HStack {
                Text(label)
                    .font(.system(size: 13))
                    .foregroundColor(.gray)
                Spacer()
                Text(value)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.white)
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
                    y: .value("Views", item.value)
                )
                .foregroundStyle(.white)
                .interpolationMethod(.catmullRom)

                AreaMark(
                    x: .value("Date", item.date),
                    y: .value("Views", item.value)
                )
                .foregroundStyle(
                    LinearGradient(
                        colors: [.white.opacity(0.2), .clear],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
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
            span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
        )
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

// FlowLayout for dynamic SwiftUI wrapping badges
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
