import SwiftUI

/// Legacy 4-tab floating glass pill preserved strictly for iOS 17-25 (`#unavailable(iOS 26)`).
public struct FloatingTabCapsule: View {
    @Binding var selectedTab: Int
    var isMinimized: Bool
    @Namespace private var tabIndicatorNamespace
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast
    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    #endif

    public init(selectedTab: Binding<Int>, isMinimized: Bool = false) {
        self._selectedTab = selectedTab
        self.isMinimized = isMinimized
    }

    private var isCompactLayout: Bool {
        #if os(iOS)
        horizontalSizeClass != .regular
        #else
        false
        #endif
    }

    public var body: some View {
        HStack(spacing: 4) {
            ForEach(AppRootTab.allCases) { tab in
                tabButton(tab: tab)
            }
        }
        .padding(4)
        .background {
            if reduceTransparency {
                Capsule()
                    .fill(GlassTheme.fallbackBackgroundColor)
            } else {
                Capsule()
                    .fill(.regularMaterial)
            }
        }
        .overlay {
            if reduceTransparency || colorSchemeContrast == .increased {
                Capsule()
                    .strokeBorder(GlassTheme.fallbackBorderColor, lineWidth: GlassTheme.fallbackBorderWidth)
            } else {
                Capsule()
                    .strokeBorder(GlassTheme.specularBorderGradient, lineWidth: GlassTheme.specularBorderWidth)
            }
        }
        .shadow(
            color: GlassTheme.shadowColor,
            radius: GlassTheme.shadowRadius,
            x: GlassTheme.shadowX,
            y: GlassTheme.shadowY)
        .scaleEffect(isMinimized ? 0.88 : 1.0)
        .offset(y: isMinimized ? 22 : 0)
        .opacity(isMinimized ? 0.65 : 1.0)
        .animation(.spring(response: 0.35, dampingFraction: 0.75), value: isMinimized)
        .animation(.spring(response: 0.35, dampingFraction: 0.75), value: selectedTab)
        .sensoryFeedback(.impact(weight: .light), trigger: selectedTab)
    }

    private func tabButton(tab: AppRootTab) -> some View {
        let isSelected = selectedTab == tab.rawValue

        return Button {
            if selectedTab != tab.rawValue {
                withAnimation(.spring(response: 0.35, dampingFraction: 0.75)) {
                    selectedTab = tab.rawValue
                }
            }
        } label: {
            tabLabel(tab: tab, isSelected: isSelected)
                .glassVibrancy(isSelected ? .primary : .secondary)
                .padding(.horizontal, isCompactLayout ? 10 : 14)
                .padding(.vertical, isCompactLayout ? 7 : 10)
                .frame(minWidth: isCompactLayout ? 76 : nil)
                .background {
                    if isSelected {
                        Capsule()
                            .fill(Color.primary.opacity(0.14))
                            .matchedGeometryEffect(id: "ActiveTabIndicator", in: tabIndicatorNamespace)
                    }
                }
                .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(tab.titleKey))
        .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)
    }

    @ViewBuilder
    private func tabLabel(tab: AppRootTab, isSelected: Bool) -> some View {
        if isCompactLayout {
            VStack(spacing: 2) {
                Image(systemName: tab.systemImage)
                    .font(.system(size: 15, weight: isSelected ? .bold : .medium))
                Text(tab.titleKey)
                    .font(.caption2.weight(isSelected ? .bold : .medium))
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
            }
        } else {
            HStack(spacing: 6) {
                Image(systemName: tab.systemImage)
                    .font(.system(size: 14, weight: isSelected ? .bold : .medium))
                Text(tab.titleKey)
                    .font(.subheadline.weight(isSelected ? .bold : .medium))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
        }
    }
}
