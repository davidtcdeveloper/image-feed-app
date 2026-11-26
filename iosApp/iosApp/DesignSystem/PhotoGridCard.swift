import shared
import SwiftUI

/// Unified masonry photo grid cell used across Feed, Favorites, Search, Collection Details, and User Profile.
struct PhotoGridCard<MenuContent: View>: View {
    @Environment(\.containerWidth) private var containerWidth
    @Environment(\.heroNamespace) private var envHeroNamespace

    let photo: Photo
    let columnCount: Int
    var heroNamespace: Namespace.ID?
    var isHeroSource: Bool = true
    var showAttribution: Bool = true
    var fadeDuration: TimeInterval = 0.25
    let onSelect: () -> Void
    var onUserSelect: ((String) -> Void)?
    var onAppear: (() -> Void)?
    private let hasContextMenu: Bool
    private let contextMenuContent: () -> MenuContent

    private var resolvedNamespace: Namespace.ID? {
        heroNamespace ?? envHeroNamespace
    }

    init(
        photo: Photo,
        columnCount: Int,
        heroNamespace: Namespace.ID? = nil,
        isHeroSource: Bool = true,
        showAttribution: Bool = true,
        fadeDuration: TimeInterval = 0.25,
        onSelect: @escaping () -> Void,
        onUserSelect: ((String) -> Void)? = nil,
        onAppear: (() -> Void)? = nil,
        @ViewBuilder contextMenu: @escaping () -> MenuContent)
    {
        self.photo = photo
        self.columnCount = columnCount
        self.heroNamespace = heroNamespace
        self.isHeroSource = isHeroSource
        self.showAttribution = showAttribution
        self.fadeDuration = fadeDuration
        self.onSelect = onSelect
        self.onUserSelect = onUserSelect
        self.onAppear = onAppear
        self.hasContextMenu = MenuContent.self != EmptyView.self
        self.contextMenuContent = contextMenu
    }

    var body: some View {
        let itemWidth = AdaptiveLayoutHelper.calculateItemWidthPx(
            containerWidth: containerWidth,
            columnCount: columnCount)
        let imageUrl = photo.urls.getOptimalUrl(targetWidthPx: Int32(itemWidth))
        let aspectRatio = contentRatio(photoWidth: Int(photo.width), photoHeight: Int(photo.height))

        let baseCard = ZStack(alignment: .bottomLeading) {
            photoImage(imageUrl: imageUrl, aspectRatio: aspectRatio)
                .contentShape(Rectangle())
                .onTapGesture {
                    NavigationPreviewSeeder.seedPhoto(photo)
                    onSelect()
                }

            if showAttribution, let onUserSelect {
                Button {
                    NavigationPreviewSeeder.seedUser(photo.user)
                    onUserSelect(photo.user.username)
                } label: {
                    HStack(spacing: 6) {
                        avatarImage

                        Text(photo.user.name)
                            .font(.caption2.weight(.semibold))
                            .glassVibrancy(.primary)
                            .lineLimit(1)
                            .truncationMode(.tail)
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 5)
                    .glassCapsule(style: .lightweight, showBorder: true, hasShadow: false)
                    .environment(\.colorScheme, .dark)
                }
                .buttonStyle(.plain)
                .contentShape(Capsule())
                .padding(8)
            }
        }
        .feedScrollTransition()
        .onAppear {
            onAppear?()
        }

        if hasContextMenu {
            baseCard.contextMenu {
                contextMenuContent()
            }
        } else {
            baseCard
        }
    }

    @ViewBuilder
    private var avatarImage: some View {
        let baseAvatar = KFImage(URL(string: photo.user.profileImage.small))
            .resizable()
            .scaledToFill()
            .frame(width: 20, height: 20)
            .clipShape(Circle())

        #if os(iOS)
        if #available(iOS 18.0, *), let resolvedNamespace {
            baseAvatar.matchedTransitionSource(
                id: AppNavigationDefaults.userSourceID(photo.user.username),
                in: resolvedNamespace)
            { source in
                source.clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            }
        } else {
            baseAvatar
        }
        #else
        baseAvatar
        #endif
    }

    @ViewBuilder
    private func photoImage(imageUrl: String, aspectRatio: CGFloat) -> some View {
        let baseImage = KFImage(URL(string: imageUrl))
            .placeholder {
                ZStack {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(Color(hex: photo.color ?? "1E1E24"))
                    ProgressView()
                        .tint(.primary.opacity(0.6))
                }
                .aspectRatio(aspectRatio, contentMode: .fit)
            }
            .fade(duration: fadeDuration)
            .resizable()
            .aspectRatio(aspectRatio, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: AppNavigationDefaults.zoomTransitionCornerRadius, style: .continuous))

        #if os(iOS)
        if #available(iOS 18.0, *) {
            if let resolvedNamespace {
                baseImage.matchedTransitionSource(
                    id: AppNavigationDefaults.photoSourceID(photo.id),
                    in: resolvedNamespace)
                { source in
                    source.clipShape(RoundedRectangle(cornerRadius: AppNavigationDefaults.zoomTransitionCornerRadius, style: .continuous))
                }
            } else {
                baseImage
            }
        } else if let resolvedNamespace {
            baseImage.matchedGeometryEffect(
                id: "photo-img-\(photo.id)",
                in: resolvedNamespace,
                isSource: isHeroSource)
        } else {
            baseImage
        }
        #else
        baseImage
        #endif
    }

    private func contentRatio(photoWidth: Int, photoHeight: Int) -> CGFloat {
        guard photoWidth > 0, photoHeight > 0 else { return 1.0 }
        return CGFloat(photoWidth) / CGFloat(photoHeight)
    }
}

extension PhotoGridCard where MenuContent == EmptyView {
    init(
        photo: Photo,
        columnCount: Int,
        heroNamespace: Namespace.ID? = nil,
        isHeroSource: Bool = true,
        showAttribution: Bool = true,
        fadeDuration: TimeInterval = 0.25,
        onSelect: @escaping () -> Void,
        onUserSelect: ((String) -> Void)? = nil,
        onAppear: (() -> Void)? = nil)
    {
        self.init(
            photo: photo,
            columnCount: columnCount,
            heroNamespace: heroNamespace,
            isHeroSource: isHeroSource,
            showAttribution: showAttribution,
            fadeDuration: fadeDuration,
            onSelect: onSelect,
            onUserSelect: onUserSelect,
            onAppear: onAppear,
            contextMenu: { EmptyView() })
    }
}
