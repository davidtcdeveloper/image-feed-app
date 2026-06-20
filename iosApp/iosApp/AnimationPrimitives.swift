import SwiftUI

// MARK: - SpringCardButtonStyle

/// A custom `ButtonStyle` that handles scale transformations (`scaleEffect`)
/// using spring physics triggered on touch events (`configuration.isPressed`),
/// propagating standard scroll gestures seamlessly inside ScrollViews.
struct SpringCardButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
            .animation(.spring(response: 0.22, dampingFraction: 0.72, blendDuration: 0), value: configuration.isPressed)
    }
}

// MARK: - StaggeredRevealModifier

/// A custom `ViewModifier` that applies a bottom-up slide offset (from `y: 35` to `0`)
/// and opacity fade animation with a capped index-based delay to cascade entry.
struct StaggeredRevealModifier: ViewModifier {
    let index: Int
    @State private var isVisible = false

    func body(content: Content) -> some View {
        content
            .opacity(isVisible ? 1.0 : 0.0)
            .offset(y: isVisible ? 0 : 35)
            .onAppear {
                // Cap index-based delay calculation at 8 to prevent extreme lag for large scroll jumps
                let staggerDelay = Double(index % 8) * 0.045
                withAnimation(.spring(response: 0.48, dampingFraction: 0.82).delay(staggerDelay)) {
                    isVisible = true
                }
            }
    }
}

extension View {
    /// Applies a bottom-up slide offset and opacity fade animation with an index-based delay.
    func staggeredReveal(index: Int) -> some View {
        modifier(StaggeredRevealModifier(index: index))
    }
}

// MARK: - ShimmerModifier

/// A custom `ViewModifier` that overlays an infinite animating linear gradient across views using masks.
struct ShimmerModifier: ViewModifier {
    @State private var phase: CGFloat = -1.0
    let duration: Double = 1.4

    func body(content: Content) -> some View {
        content
            .overlay(
                GeometryReader { geo in
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0.3),
                            .init(color: .white.opacity(0.12), location: 0.5),
                            .init(color: .clear, location: 0.7)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    .rotationEffect(.degrees(20))
                    .offset(x: phase * geo.size.width * 1.5)
                    .frame(width: geo.size.width, height: geo.size.height)
                }
                .mask(content)
            )
            .onAppear {
                withAnimation(.linear(duration: duration).repeatForever(autoreverses: false)) {
                    phase = 1.0
                }
            }
    }
}

extension View {
    /// Overlays an infinite animating linear gradient across the view.
    func shimmer() -> some View {
        modifier(ShimmerModifier())
    }
}

// MARK: - PhotoCardSkeleton

/// A skeleton placeholder that replicates the PhotoCard layout with rounded card shape, avatar circle, and username block.
struct PhotoCardSkeleton: View {
    let height: CGFloat

    init(height: CGFloat = 200) {
        self.height = height
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.white.opacity(0.06))
                .frame(height: height)
            
            HStack(spacing: 6) {
                Circle()
                    .fill(Color.white.opacity(0.06))
                    .frame(width: 20, height: 20)
                RoundedRectangle(cornerRadius: 4)
                    .fill(Color.white.opacity(0.06))
                    .frame(width: 80, height: 10)
            }
            .padding(.horizontal, 4)
        }
        .shimmer()
    }
}

// MARK: - CollectionMosaicCardSkeleton

/// A skeleton placeholder that replicates the CollectionMosaicCard layout.
struct CollectionMosaicCardSkeleton: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 4) {
                RoundedRectangle(cornerRadius: 0)
                    .fill(Color.white.opacity(0.06))
                    .frame(height: 180)
                VStack(spacing: 4) {
                    RoundedRectangle(cornerRadius: 0)
                        .fill(Color.white.opacity(0.06))
                        .frame(height: 88)
                    RoundedRectangle(cornerRadius: 0)
                        .fill(Color.white.opacity(0.06))
                        .frame(height: 88)
                }
                .frame(width: 110)
            }
            .clipShape(RoundedRectangle(cornerRadius: 12))
            
            RoundedRectangle(cornerRadius: 4)
                .fill(Color.white.opacity(0.06))
                .frame(width: 180, height: 16)
            
            RoundedRectangle(cornerRadius: 4)
                .fill(Color.white.opacity(0.06))
                .frame(width: 260, height: 12)
        }
        .shimmer()
    }
}

// MARK: - UserProfileHeaderSkeleton

/// A skeleton placeholder that replicates the UserProfileHeader layout.
struct UserProfileHeaderSkeleton: View {
    var body: some View {
        VStack(spacing: 12) {
            Circle()
                .fill(Color.white.opacity(0.06))
                .frame(width: 88, height: 88)
            
            RoundedRectangle(cornerRadius: 4)
                .fill(Color.white.opacity(0.06))
                .frame(width: 140, height: 18)
            
            RoundedRectangle(cornerRadius: 4)
                .fill(Color.white.opacity(0.06))
                .frame(width: 90, height: 12)
            
            RoundedRectangle(cornerRadius: 4)
                .fill(Color.white.opacity(0.06))
                .frame(width: 240, height: 12)
                .padding(.top, 4)
        }
        .shimmer()
    }
}
