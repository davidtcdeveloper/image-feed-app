import SwiftUI

public struct SearchTopicCard: View {
    public let topic: String
    public let action: () -> Void
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast

    public init(topic: String, action: @escaping () -> Void) {
        self.topic = topic
        self.action = action
    }

    private var gradientColors: [Color] {
        switch topic.lowercased() {
        case "nature":
            [Color(hex: "134e5e"), Color(hex: "71b280")]
        case "travel":
            [Color(hex: "2b5876"), Color(hex: "4e4376")]
        case "architecture":
            [Color(hex: "3a6073"), Color(hex: "16222f")]
        case "wallpapers":
            [Color(hex: "614385"), Color(hex: "516395")]
        case "neon":
            [Color(hex: "ff007f"), Color(hex: "7928ca")]
        case "minimalist":
            [Color(hex: "232526"), Color(hex: "414345")]
        case "urban":
            [Color(hex: "4b6cb7"), Color(hex: "182848")]
        case "textures":
            [Color(hex: "8e2de2"), Color(hex: "4a00e0")]
        default:
            [Color(hex: "2c3e50"), Color(hex: "3498db")]
        }
    }

    private var iconName: String {
        switch topic.lowercased() {
        case "nature": "leaf.fill"
        case "travel": "airplane"
        case "architecture": "building.2.fill"
        case "wallpapers": "sparkles"
        case "neon": "bolt.fill"
        case "minimalist": "circle.circle"
        case "urban": "car.fill"
        case "textures": "square.stack.3d.up.fill"
        default: "tag.fill"
        }
    }

    public var body: some View {
        Button(action: action) {
            ZStack(alignment: .bottomLeading) {
                LinearGradient(
                    colors: gradientColors,
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing)

                // Atmospheric dark photographic scrim overlay
                LinearGradient(
                    colors: [Color.black.opacity(0.15), Color.black.opacity(0.58)],
                    startPoint: .top,
                    endPoint: .bottom)

                // Atmospheric thematic watermark icon
                HStack {
                    Spacer()
                    Image(systemName: iconName)
                        .font(.system(size: 38, weight: .bold))
                        .foregroundStyle(Color.white.opacity(0.16))
                        .offset(x: 4, y: 6)
                        .accessibilityHidden(true)
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(topic.uppercased())
                        .font(.subheadline.weight(.heavy))
                        .foregroundStyle(Color.white)
                        .tracking(1.2)
                        .shadow(color: Color.black.opacity(0.4), radius: 2, x: 0, y: 1)
                }
                .padding(12)
            }
            .frame(height: 72)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay {
                if reduceTransparency || colorSchemeContrast == .increased {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(GlassTheme.fallbackBorderColor, lineWidth: GlassTheme.fallbackBorderWidth)
                } else {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(
                            LinearGradient(
                                colors: [Color.white.opacity(0.35), Color.white.opacity(0.08)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing),
                            lineWidth: GlassTheme.specularBorderWidth)
                }
            }
            .shadow(color: GlassTheme.shadowColor, radius: 4, x: 0, y: 2)
        }
        .buttonStyle(SpringCardButtonStyle())
        .accessibilityLabel(topic.capitalized)
        .accessibilityAddTraits(.isButton)
    }
}
