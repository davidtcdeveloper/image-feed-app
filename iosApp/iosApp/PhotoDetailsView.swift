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
    @State private var viewModel: PhotoDetailsViewModel?
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
        self._viewModel = State(initialValue: nil)
    }

    var body: some View {
        ZStack {
            Color.black
                .ignoresSafeArea()

            if let viewModel {
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
                        dualPaneLayout(photo: photo, viewModel: viewModel)
                    } else {
                        compactPhotoLayout(photo: photo, viewModel: viewModel)
                    }
                }
            } else {
                ProgressView()
                    .tint(.white)
            }
        }
        .task(id: photoId) {
            if viewModel == nil {
                viewModel = PhotoDetailsViewModel(photoId: photoId)
            }
        }
        .navigationTitle("")
        .navigationBarBackButtonHidden(true)
        #if os(iOS)
        .toolbar(.hidden, for: .navigationBar, .tabBar)
        #endif
        .onChange(of: viewModel?.shouldDismiss) { _, shouldDismiss in
            if shouldDismiss == true {
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
            get: { showInspector && !isDualPane && viewModel?.photo != nil },
            set: { showInspector = $0 }))
        {
            if let viewModel, let photo = viewModel.photo {
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
    func dualPaneLayout(photo: Photo, viewModel: PhotoDetailsViewModel) -> some View {
        GeometryReader { totalGeo in
            let inspectorWidth = max(320, min(420, totalGeo.size.width * 0.42))

            HStack(spacing: 0) {
                // Left: Photo Canvas
                ZStack(alignment: .bottomLeading) {
                    ambientBackdrop(photo: photo)

                    GeometryReader { geo in
                        let aspectRatio = max(0.1, CGFloat(photo.width) / CGFloat(photo.height))
                        #if os(iOS)
                        let displayScale = UIScreen.main.scale
                        #elseif os(macOS)
                        let displayScale = NSScreen.main?.backingScaleFactor ?? 2.0
                        #endif
                        let imageUrl = photo.urls.getOptimalUrl(targetWidthPx: Int32(geo.size.width * displayScale), quality: 85)

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

    func compactPhotoLayout(photo: Photo, viewModel: PhotoDetailsViewModel) -> some View {
        GeometryReader { geo in
            let containerWidth = geo.size.width
            let aspectRatio = max(0.1, CGFloat(photo.width) / CGFloat(photo.height))
            let calculatedHeight = containerWidth / aspectRatio
            #if os(iOS)
            let displayScale = UIScreen.main.scale
            #elseif os(macOS)
            let displayScale = NSScreen.main?.backingScaleFactor ?? 2.0
            #endif
            let imageUrl = photo.urls.getOptimalUrl(targetWidthPx: Int32(containerWidth * displayScale), quality: 85)

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
                    compactFloatingBar(photo: photo, viewModel: viewModel)
                }
            }
        }
        .ignoresSafeArea()
    }

    private func compactFloatingBar(photo: Photo, viewModel: PhotoDetailsViewModel) -> some View {
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
