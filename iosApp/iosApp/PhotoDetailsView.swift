import Charts
import MapKit
import shared
import SwiftUI

struct PhotoDetailsView: View {
    let photoId: String
    let initialPhoto: Photo?
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

    @Namespace private var glassNamespace
    @State private var showInspector = true
    @State private var currentScale: CGFloat = 1.0
    @State private var finalScale: CGFloat = 1.0
    @State private var currentOffset: CGSize = .zero
    @State private var finalOffset: CGSize = .zero
    @State private var downloadFeedbackTrigger = 0

    private var activePhoto: Photo? {
        viewModel?.photo ?? initialPhoto
    }

    private var isDualPane: Bool {
        #if os(iOS)
        horizontalSizeClass == .regular
        #else
        true
        #endif
    }

    init(
        photoId: String,
        initialPhoto: Photo? = nil,
        initialViewModel: PhotoDetailsViewModel? = nil,
        heroNamespace: Namespace.ID,
        onDismiss: (() -> Void)? = nil,
        onUserSelect: @escaping (String) -> Void,
        onTagSelect: @escaping (String) -> Void)
    {
        self.photoId = photoId
        self.initialPhoto = initialPhoto
        self.heroNamespace = heroNamespace
        self.onDismiss = onDismiss
        self.onUserSelect = onUserSelect
        self.onTagSelect = onTagSelect
        self._viewModel = State(initialValue: initialViewModel)
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            mainContent
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
        .overlay(alignment: .top) {
            topOverlayControls
        }
        .sensoryFeedback(.success, trigger: downloadFeedbackTrigger)
        .sensoryFeedback(.impact(weight: .medium), trigger: viewModel?.favoriteFeedbackTrigger ?? 0)
        #if os(macOS)
        .background {
            Button("") {
                handleDismiss()
            }
            .keyboardShortcut(.escape, modifiers: [])
            .frame(width: 0, height: 0)
            .opacity(0)
            .accessibilityHidden(true)
        }
        #endif
        #if os(iOS)
        .sheet(isPresented: Binding(
            get: { showInspector && !isDualPane && activePhoto != nil },
            set: { showInspector = $0 }))
        {
            inspectorSheetContent
        }
        #endif
        .toolbar {
            #if os(macOS)
            ToolbarItem(placement: .navigation) {
                PlatformToolbarButton(systemName: "chevron.backward", accessibilityLabel: "Back") {
                    handleDismiss()
                }
            }
            #endif
        }
        .onDisappear {
            resetZoom()
            showInspector = false
        }
    }
}

// MARK: - PhotoDetailsView Content & Dismissal

private extension PhotoDetailsView {
    @ViewBuilder
    var mainContent: some View {
        if let photo = activePhoto {
            if isDualPane {
                dualPaneLayout(photo: photo, viewModel: viewModel)
            } else {
                compactPhotoLayout(photo: photo, viewModel: viewModel)
            }
        } else if let viewModel, !viewModel.isLoading, let error = viewModel.error {
            VStack(spacing: 16) {
                Text("Failed to load details")
                    .font(.headline)
                    .glassVibrancy(.primary)
                Text(error)
                    .font(.subheadline)
                    .glassVibrancy(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
                Button {
                    viewModel.loadDetails()
                } label: {
                    Text("Retry")
                        .font(.subheadline.weight(.semibold))
                        .glassVibrancy(.primary)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 10)
                        .glassBackground(style: .thin, shape: Capsule(), showBorder: true)
                }
                .buttonStyle(.plain)
            }
            .environment(\.colorScheme, .dark)
        } else {
            ProgressView()
                .tint(.white)
        }
    }

    #if os(iOS)
    @ViewBuilder
    var inspectorSheetContent: some View {
        if let viewModel, let photo = activePhoto {
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
                    },
                    heroNamespace: heroNamespace)
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

    func resetZoom() {
        currentScale = 1.0
        finalScale = 1.0
        currentOffset = .zero
        finalOffset = .zero
    }

    func handleDismiss() {
        withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
            resetZoom()
        }
        showInspector = false
        if let onDismiss {
            onDismiss()
        } else {
            dismiss()
        }
    }
}

// MARK: - Top Overlay & Glass Morphing Controls

private extension PhotoDetailsView {
    @ViewBuilder
    var topOverlayControls: some View {
        #if os(iOS)
        if #available(iOS 26.0, *), !reduceTransparency {
            GlassEffectContainer(spacing: 12) {
                topOverlayBarContent(topPadding: 60, showBackButton: true, showInfoButton: !isDualPane)
            }
        } else {
            topOverlayBarContent(topPadding: 60, showBackButton: true, showInfoButton: !isDualPane)
        }
        #else
        let showStandaloneBack = onDismiss != nil
        let showToast = viewModel?.favoriteToastBanner != nil
        if showStandaloneBack || showToast {
            if #available(macOS 26.0, *), !reduceTransparency {
                GlassEffectContainer(spacing: 12) {
                    topOverlayBarContent(topPadding: 16, showBackButton: showStandaloneBack, showInfoButton: false)
                }
            } else {
                topOverlayBarContent(topPadding: 16, showBackButton: showStandaloneBack, showInfoButton: false)
            }
        }
        #endif
    }

    func topOverlayBarContent(topPadding: CGFloat, showBackButton: Bool, showInfoButton: Bool) -> some View {
        ZStack(alignment: .top) {
            HStack {
                if showBackButton {
                    detailOrbButton(
                        systemName: "chevron.backward",
                        accessibilityLabel: "Back",
                        glassID: "details-back")
                    {
                        handleDismiss()
                    }
                }

                Spacer()

                if showInfoButton {
                    detailOrbButton(
                        systemName: showInspector ? "info.circle.fill" : "info.circle",
                        accessibilityLabel: "Photo Details",
                        glassID: "details-info")
                    {
                        withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                            showInspector.toggle()
                        }
                    }
                }
            }
            .padding(.horizontal, 16)

            if let banner = viewModel?.favoriteToastBanner {
                toastOverlayPill(banner: banner)
                    .padding(.horizontal, 64)
                    .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .padding(.top, topPadding)
    }

    @ViewBuilder
    func detailOrbButton(
        systemName: String,
        accessibilityLabel: LocalizedStringKey,
        glassID: String,
        action: @escaping () -> Void) -> some View
    {
        if #available(iOS 26.0, macOS 26.0, *), !reduceTransparency {
            Button(action: action) {
                Image(systemName: systemName)
                    .font(.system(size: 15, weight: .semibold))
                    .symbolRenderingMode(.hierarchical)
                    .contentTransition(.symbolEffect(.replace))
                    .foregroundStyle(.white)
                    .frame(width: GlassTheme.tactileOrbSize, height: GlassTheme.tactileOrbSize)
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .glassEffect(.regular.interactive(), in: Circle())
            .glassEffectID(glassID, in: glassNamespace)
            .environment(\.colorScheme, .dark)
            .accessibilityLabel(Text(accessibilityLabel))
        } else {
            GlassToolbarButton(
                systemName: systemName,
                accessibilityLabel: accessibilityLabel,
                action: action)
                .environment(\.colorScheme, .dark)
        }
    }

    @ViewBuilder
    func toastOverlayPill(banner: ToastBanner) -> some View {
        if #available(iOS 26.0, macOS 26.0, *), !reduceTransparency {
            GlassToastPill(banner: banner, usesNativeGlassEffect: true)
                .glassEffect(.regular, in: Capsule())
                .glassEffectID("details-toast", in: glassNamespace)
        } else {
            GlassToastPill(banner: banner)
        }
    }
}

// MARK: - Dual-Pane, Compact & Zoomable Canvas Layouts

private extension PhotoDetailsView {
    func dualPaneLayout(photo: Photo, viewModel: PhotoDetailsViewModel?) -> some View {
        GeometryReader { totalGeo in
            let inspectorWidth = max(320, min(420, totalGeo.size.width * 0.42))
            let canvasWidth = max(0, totalGeo.size.width - inspectorWidth)

            HStack(spacing: 0) {
                ZStack(alignment: .bottomLeading) {
                    ambientBackdrop(photo: photo)

                    GeometryReader { geo in
                        zoomablePhotoCanvas(photo: photo, containerSize: geo.size)
                    }

                    dualPaneAttributionPill(photo: photo)
                        .padding(16)
                }
                .frame(width: canvasWidth, height: totalGeo.size.height)
                .clipped()
                .ignoresSafeArea(edges: .top)

                ScrollView {
                    if let viewModel {
                        PhotoInspectorView(
                            photo: photo,
                            viewModel: viewModel,
                            onUserSelect: onUserSelect,
                            onTagSelect: onTagSelect,
                            onDownload: {
                                downloadFeedbackTrigger += 1
                            },
                            heroNamespace: nil)
                            .padding(20)
                            .padding(.bottom, 32)
                    } else {
                        ProgressView()
                            .tint(.primary)
                            .padding(.top, 40)
                    }
                }
                .satinScrollEdge()
                .frame(width: inspectorWidth)
                .background(GlassTheme.canvasBackgroundColor)
            }
        }
    }

    @ViewBuilder
    func dualPaneAttributionPill(photo: Photo) -> some View {
        let content = Button {
            onUserSelect(photo.user.username)
        } label: {
            HStack(spacing: 10) {
                attributionAvatar(photo: photo, size: 36, enableMatchedSource: true)

                VStack(alignment: .leading, spacing: 2) {
                    Text(photo.user.name)
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(.white)
                        .lineLimit(1)
                    Text("@\(photo.user.username)")
                        .font(.caption2)
                        .foregroundStyle(.white.opacity(0.74))
                        .lineLimit(1)
                }

                Image(systemName: "arrow.up.right")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(.white)
                    .padding(.leading, 4)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text("View Profile"))

        if #available(iOS 26.0, macOS 26.0, *), !reduceTransparency {
            content
                .background(Capsule().fill(Color.black.opacity(0.35)))
                .glassEffect(.regular.interactive(), in: Capsule())
                .environment(\.colorScheme, .dark)
        } else {
            content
                .background(Capsule().fill(Color.black.opacity(0.42)))
                .glassCapsule(style: .ultraThin, showBorder: true, hasShadow: true)
                .environment(\.colorScheme, .dark)
        }
    }

    func compactPhotoLayout(photo: Photo, viewModel: PhotoDetailsViewModel?) -> some View {
        GeometryReader { geo in
            ZStack(alignment: .bottom) {
                ambientBackdrop(photo: photo)

                zoomablePhotoCanvas(photo: photo, containerSize: geo.size)

                if !showInspector {
                    compactFloatingBarContainer(photo: photo, viewModel: viewModel, bottomSafeArea: geo.safeAreaInsets.bottom)
                        .frame(width: geo.size.width)
                }
            }
            .frame(width: geo.size.width, height: geo.size.height)
        }
        .ignoresSafeArea()
    }

    func zoomablePhotoCanvas(photo: Photo, containerSize: CGSize) -> some View {
        let aspectRatio = max(0.1, CGFloat(photo.width) / CGFloat(photo.height))
        let fittedSize = fittedImageSize(aspectRatio: aspectRatio, containerSize: containerSize)
        let imageUrl = photo.urls.getOptimalUrl(
            targetWidthPx: Int32(max(1, fittedSize.width) * AdaptiveLayoutHelper.displayScale()),
            quality: 85)

        return ZStack {
            KFImage(URL(string: imageUrl))
                .placeholder {
                    Rectangle().fill(Color(hex: photo.color ?? "1E1E24"))
                }
                .resizable()
                .aspectRatio(aspectRatio, contentMode: .fit)
                .frame(width: fittedSize.width, height: fittedSize.height)
                .matchedGeometryEffect(id: "photo-img-\(photoId)", in: heroNamespace)
                .scaleEffect(currentScale)
                .offset(currentOffset)
                .contentShape(Rectangle())
                .gesture(magnifyGesture(fittedSize: fittedSize, containerSize: containerSize))
                .simultaneousGesture(panGesture(fittedSize: fittedSize, containerSize: containerSize))
                .onTapGesture(count: 2) {
                    toggleDoubleTapZoom()
                }
        }
        .frame(width: containerSize.width, height: containerSize.height)
    }

    func fittedImageSize(aspectRatio: CGFloat, containerSize: CGSize) -> CGSize {
        let safeHeight = max(1, containerSize.height)
        let containerAspect = containerSize.width / safeHeight
        if containerAspect > aspectRatio {
            return CGSize(width: safeHeight * aspectRatio, height: safeHeight)
        }
        return CGSize(width: containerSize.width, height: containerSize.width / aspectRatio)
    }

    func magnifyGesture(fittedSize: CGSize, containerSize: CGSize) -> some Gesture {
        MagnifyGesture()
            .onChanged { value in
                currentScale = max(1.0, min(4.0, finalScale * value.magnification))
            }
            .onEnded { _ in
                finalScale = currentScale
                if finalScale <= 1.02 {
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                        resetZoom()
                    }
                } else {
                    let clamped = clampedOffset(finalOffset, scale: finalScale, fittedSize: fittedSize, containerSize: containerSize)
                    if clamped != finalOffset {
                        withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                            currentOffset = clamped
                            finalOffset = clamped
                        }
                    }
                }
            }
    }

    func panGesture(fittedSize: CGSize, containerSize: CGSize) -> some Gesture {
        DragGesture()
            .onChanged { value in
                guard currentScale > 1.05 else { return }
                let proposed = CGSize(
                    width: finalOffset.width + value.translation.width,
                    height: finalOffset.height + value.translation.height)
                currentOffset = clampedOffset(proposed, scale: currentScale, fittedSize: fittedSize, containerSize: containerSize)
            }
            .onEnded { _ in
                guard currentScale > 1.05 else {
                    finalOffset = .zero
                    currentOffset = .zero
                    return
                }
                finalOffset = currentOffset
            }
    }

    func toggleDoubleTapZoom() {
        withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
            if currentScale > 1.05 {
                resetZoom()
            } else {
                currentScale = 2.5
                finalScale = 2.5
            }
        }
    }

    func clampedOffset(_ offset: CGSize, scale: CGFloat, fittedSize: CGSize, containerSize: CGSize) -> CGSize {
        let maxOffsetX = max(0, (fittedSize.width * scale - containerSize.width) / 2)
        let maxOffsetY = max(0, (fittedSize.height * scale - containerSize.height) / 2)
        return CGSize(
            width: max(-maxOffsetX, min(maxOffsetX, offset.width)),
            height: max(-maxOffsetY, min(maxOffsetY, offset.height)))
    }
}

// MARK: - Compact Floating Action Bar & Backdrop

private extension PhotoDetailsView {
    @ViewBuilder
    func compactFloatingBarContainer(photo: Photo, viewModel: PhotoDetailsViewModel?, bottomSafeArea: CGFloat) -> some View {
        if #available(iOS 26.0, macOS 26.0, *), !reduceTransparency {
            GlassEffectContainer(spacing: 10) {
                compactFloatingBar(photo: photo, viewModel: viewModel, bottomSafeArea: bottomSafeArea, usesNativeGlass: true)
            }
            .transition(.move(edge: .bottom).combined(with: .opacity))
        } else {
            compactFloatingBar(photo: photo, viewModel: viewModel, bottomSafeArea: bottomSafeArea, usesNativeGlass: false)
                .transition(.move(edge: .bottom).combined(with: .opacity))
        }
    }

    func compactFloatingBar(
        photo: Photo,
        viewModel: PhotoDetailsViewModel?,
        bottomSafeArea: CGFloat,
        usesNativeGlass: Bool) -> some View
    {
        let barContent = HStack(spacing: 8) {
            compactAttributionButton(photo: photo)
            Spacer(minLength: 4)
            compactSplitDownloadPill(photo: photo, viewModel: viewModel)
            compactBookmarkButton(photo: photo, viewModel: viewModel)
            compactInspectorButton
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .environment(\.colorScheme, .dark)

        return Group {
            if #available(iOS 26.0, macOS 26.0, *), usesNativeGlass {
                barContent
                    .background(Capsule().fill(Color.black.opacity(0.35)))
                    .glassEffect(.regular.interactive(), in: Capsule())
                    .glassEffectID("compact-floating-bar", in: glassNamespace)
            } else {
                barContent
                    .background(Capsule().fill(Color.black.opacity(0.42)))
                    .glassCapsule(style: .ultraThin, showBorder: true, hasShadow: true)
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, max(24, bottomSafeArea + 12))
    }

    func compactAttributionButton(photo: Photo) -> some View {
        Button {
            onUserSelect(photo.user.username)
        } label: {
            HStack(spacing: 8) {
                attributionAvatar(photo: photo, size: 32, enableMatchedSource: true)

                VStack(alignment: .leading, spacing: 2) {
                    Text(photo.user.name)
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(.white)
                        .lineLimit(1)
                    Text("@\(photo.user.username)")
                        .font(.caption2)
                        .foregroundStyle(.white.opacity(0.74))
                        .lineLimit(1)
                }
            }
        }
        .buttonStyle(.plain)
    }

    func compactSplitDownloadPill(photo: Photo, viewModel: PhotoDetailsViewModel?) -> some View {
        HStack(spacing: 0) {
            Button {
                triggerCompactDownload(photo: photo, urlString: photo.urls.full, viewModel: viewModel)
            } label: {
                HStack(spacing: 4) {
                    Image(systemName: "arrow.down.to.line")
                        .font(.caption.weight(.semibold))
                        .symbolEffect(.bounce, value: downloadFeedbackTrigger)
                    Text("Full")
                        .font(.caption.weight(.bold))
                }
                .foregroundStyle(.white)
                .padding(.horizontal, 10)
                .frame(minHeight: 34)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("Download Full"))

            Rectangle()
                .fill(Color.white.opacity(0.22))
                .frame(width: 1, height: 18)

            Button {
                triggerCompactDownload(photo: photo, urlString: photo.urls.raw, viewModel: viewModel)
            } label: {
                Text("RAW")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 10)
                    .frame(minHeight: 34)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("Download RAW"))
        }
        .background(Capsule().fill(Color.white.opacity(0.12)))
        .clipShape(Capsule())
    }

    func compactBookmarkButton(photo: Photo, viewModel: PhotoDetailsViewModel?) -> some View {
        let accentColor: Color = {
            if let hex = PhotoColorEngine.shared.deriveAccentHex(seedHex: photo.color, isDark: true) {
                return Color(hex: hex)
            }
            return .accentColor
        }()

        return Button {
            viewModel?.toggleFavorite(fallbackPhoto: photo)
        } label: {
            Image(systemName: viewModel?.isFavorite == true ? "bookmark.fill" : "bookmark")
                .font(.system(size: 15, weight: .semibold))
                .contentTransition(.symbolEffect(.replace))
                .foregroundStyle(viewModel?.isFavorite == true ? accentColor : Color.white)
                .frame(width: 36, height: 36)
                .background(Circle().fill(Color.white.opacity(0.10)))
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(viewModel?.isFavorite == true ? "Remove Bookmark" : "Bookmark"))
    }

    var compactInspectorButton: some View {
        Button {
            withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                showInspector = true
            }
        } label: {
            Image(systemName: "info.circle")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: 36, height: 36)
                .background(Circle().fill(Color.white.opacity(0.10)))
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text("Inspect photo details"))
    }

    func triggerCompactDownload(photo: Photo, urlString: String, viewModel: PhotoDetailsViewModel?) {
        downloadFeedbackTrigger += 1
        viewModel?.trackDownload(fallbackPhoto: photo)
        if let url = URL(string: urlString) {
            URLHelper.open(url)
        }
    }

    @ViewBuilder
    func attributionAvatar(photo: Photo, size: CGFloat, enableMatchedSource: Bool) -> some View {
        let baseAvatar = KFImage(URL(string: photo.user.profileImage.medium))
            .resizable()
            .scaledToFill()
            .frame(width: size, height: size)
            .clipShape(Circle())
            .overlay(Circle().stroke(.white.opacity(0.4), lineWidth: 1.5))

        #if os(iOS)
        if #available(iOS 18.0, *), enableMatchedSource {
            baseAvatar.matchedTransitionSource(
                id: AppNavigationDefaults.userSourceID(photo.user.username),
                in: heroNamespace)
            { source in
                source.clipShape(RoundedRectangle(cornerRadius: size / 2, style: .continuous))
            }
        } else {
            baseAvatar
        }
        #else
        baseAvatar
        #endif
    }

    @ViewBuilder
    func ambientBackdrop(photo: Photo) -> some View {
        if reduceTransparency {
            Color.black.ignoresSafeArea()
        } else {
            Color.black
                .overlay {
                    KFImage(URL(string: photo.urls.small), targetWidthPx: 160)
                        .placeholder {
                            Rectangle().fill(Color(hex: photo.color ?? "1E1E24"))
                        }
                        .resizable()
                        .scaledToFill()
                        .scaleEffect(1.3)
                        .blur(radius: 28)
                }
                .overlay(Color.black.opacity(0.42))
                .clipped()
                .drawingGroup()
                .ignoresSafeArea()
                .allowsHitTesting(false)
        }
    }
}

struct GlassToastPill: View {
    let banner: ToastBanner
    var usesNativeGlassEffect: Bool = false

    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    var body: some View {
        let label = HStack(spacing: 8) {
            Image(systemName: banner.systemImage)
                .font(.caption.weight(.semibold))
            Text(banner.message)
                .font(.footnote.weight(.semibold))
                .lineLimit(2)
                .multilineTextAlignment(.center)
        }
        .foregroundStyle(.white)
        .padding(.horizontal, 14)
        .padding(.vertical, 9)
        .environment(\.colorScheme, .dark)
        .allowsHitTesting(false)

        if #available(iOS 26.0, macOS 26.0, *), usesNativeGlassEffect, !reduceTransparency {
            label.background(Capsule().fill(Color.black.opacity(0.35)))
        } else {
            label
                .background(Capsule().fill(Color.black.opacity(0.45)))
                .glassCapsule(style: .ultraThin, showBorder: true, hasShadow: true)
        }
    }
}

#if DEBUG
#Preview("Photo Details — Dark") {
    UIPreviewFixtures.makeView(for: .photoDetails)
        .preferredColorScheme(.dark)
}

#Preview("Photo Details — Light") {
    UIPreviewFixtures.makeView(for: .photoDetails)
        .preferredColorScheme(.light)
}
#endif
