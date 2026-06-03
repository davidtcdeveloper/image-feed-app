import SwiftUI

public struct KFImage: View {
    private let url: URL?
    private var placeholderView: AnyView? = nil
    private var fadeDuration: Double = 0.25
    private var isResizable = false
    
    public init(_ url: URL?) {
        self.url = url
    }
    
    // Custom placeholder builder
    public func placeholder<Content: View>(@ViewBuilder _ content: @escaping () -> Content) -> KFImage {
        var copy = self
        copy.placeholderView = AnyView(content())
        return copy
    }
    
    // Custom fade modifier
    public func fade(duration: Double) -> KFImage {
        var copy = self
        copy.fadeDuration = duration
        return copy
    }
    
    // Custom resizable modifier
    public func resizable() -> KFImage {
        var copy = self
        copy.isResizable = true
        return copy
    }
    
    public var body: some View {
        if let url = url {
            AsyncImage(url: url) { phase in
                switch phase {
                case .empty:
                    if let placeholder = placeholderView {
                        placeholder
                    } else {
                        ProgressView()
                            .tint(.white.opacity(0.5))
                    }
                case .success(let image):
                    if isResizable {
                        image
                            .resizable()
                    } else {
                        image
                    }
                case .failure:
                    if let placeholder = placeholderView {
                        placeholder
                    } else {
                        Color(white: 0.1)
                    }
                @unknown default:
                    Color.clear
                }
            }
        } else {
            if let placeholder = placeholderView {
                placeholder
            } else {
                Color(white: 0.1)
            }
        }
    }
}
