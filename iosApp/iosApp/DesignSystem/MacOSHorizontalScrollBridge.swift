import SwiftUI
#if os(macOS)
import AppKit
#endif

// MARK: - macOS Horizontal Scroll Wheel & Trackpad Bridge

@MainActor
public final class MacOSHorizontalScrollCoordinator {
    public var scrollFraction: CGFloat = 0
    public var stepIndex: Int?
    #if os(macOS)
    weak var scrollView: NSScrollView?
    #endif

    public init() {}

    public func currentFraction() -> CGFloat {
        #if os(macOS)
        if let scrollView, let documentView = scrollView.documentView {
            let clipView = scrollView.contentView
            let maxOffsetX = max(0, documentView.bounds.width - clipView.bounds.width)
            if maxOffsetX > 0 {
                return min(max(0, clipView.bounds.origin.x / maxOffsetX), 1.0)
            }
        }
        #endif
        return scrollFraction
    }
}

#if os(macOS)
public final class HorizontalScrollInterceptorView: NSView {
    var isEnabled: Bool = true
    weak var coordinator: MacOSHorizontalScrollCoordinator?
    private nonisolated(unsafe) var eventMonitor: Any?

    override public func hitTest(_ point: NSPoint) -> NSView? {
        nil
    }

    override public func viewDidMoveToWindow() {
        super.viewDidMoveToWindow()
        if window != nil {
            coordinator?.scrollView = resolveHorizontalScrollView()
            startMonitoringIfNeeded()
        } else {
            stopMonitoring()
        }
    }

    func startMonitoringIfNeeded() {
        guard eventMonitor == nil else { return }
        eventMonitor = NSEvent.addLocalMonitorForEvents(matching: .scrollWheel) { [weak self] event in
            guard let self else { return event }
            return handleScrollWheel(event)
        }
    }

    func stopMonitoring() {
        if let monitor = eventMonitor {
            NSEvent.removeMonitor(monitor)
            eventMonitor = nil
        }
    }

    private func handleScrollWheel(_ event: NSEvent) -> NSEvent? {
        guard isEnabled,
              let window,
              event.window === window,
              !isHiddenOrHasHiddenAncestor,
              let scrollView = resolveHorizontalScrollView(),
              let documentView = scrollView.documentView
        else {
            return event
        }

        coordinator?.scrollView = scrollView

        let locationInScrollView = scrollView.convert(event.locationInWindow, from: nil)
        guard scrollView.bounds.contains(locationInScrollView) else {
            return event
        }

        let clipView = scrollView.contentView
        let maxOffsetX = max(0, documentView.bounds.width - clipView.bounds.width)
        guard maxOffsetX > 0 else {
            return event
        }

        let dominantDelta = abs(event.scrollingDeltaX) >= abs(event.scrollingDeltaY)
            ? event.scrollingDeltaX
            : event.scrollingDeltaY
        guard dominantDelta != 0 else {
            return nil
        }

        let multiplier: CGFloat = event.hasPreciseScrollingDeltas ? 1.0 : 14.0
        let deltaX = dominantDelta * multiplier
        let currentOrigin = clipView.bounds.origin
        let targetX = min(max(0, currentOrigin.x - deltaX), maxOffsetX)

        if targetX != currentOrigin.x {
            clipView.scroll(to: NSPoint(x: targetX, y: currentOrigin.y))
            scrollView.reflectScrolledClipView(clipView)
            coordinator?.scrollFraction = targetX / maxOffsetX
            coordinator?.stepIndex = nil
        }

        return nil
    }

    private func resolveHorizontalScrollView() -> NSScrollView? {
        if let enclosing = enclosingScrollView {
            return enclosing
        }
        var current: NSView? = superview
        while let candidate = current {
            if let sv = candidate as? NSScrollView {
                return sv
            }
            current = candidate.superview
        }
        return nil
    }

    deinit {
        if let monitor = eventMonitor {
            NSEvent.removeMonitor(monitor)
        }
    }
}

public struct MacOSHorizontalScrollBridge: NSViewRepresentable {
    public var isEnabled: Bool
    public var coordinator: MacOSHorizontalScrollCoordinator?

    public init(isEnabled: Bool = true, coordinator: MacOSHorizontalScrollCoordinator? = nil) {
        self.isEnabled = isEnabled
        self.coordinator = coordinator
    }

    public func makeNSView(context: Context) -> HorizontalScrollInterceptorView {
        let view = HorizontalScrollInterceptorView()
        view.isEnabled = isEnabled
        view.coordinator = coordinator
        return view
    }

    public func updateNSView(_ nsView: HorizontalScrollInterceptorView, context: Context) {
        nsView.isEnabled = isEnabled
        nsView.coordinator = coordinator
    }

    public static func dismantleNSView(_ nsView: HorizontalScrollInterceptorView, coordinator: ()) {
        nsView.stopMonitoring()
    }
}
#endif

public extension View {
    /// Enables horizontal scrolling via vertical mouse wheel and trackpad gestures on macOS.
    func macOSHorizontalScroll(isEnabled: Bool = true, coordinator: MacOSHorizontalScrollCoordinator? = nil) -> some View {
        #if os(macOS)
        background(MacOSHorizontalScrollBridge(isEnabled: isEnabled, coordinator: coordinator))
        #else
        self
        #endif
    }
}
