import Foundation
import shared

extension AppError {
    var localizedMessage: String {
        switch self {
        case is AppError.Network:
            return String(localized: "No internet connection. Please check your network.")
        case is AppError.Timeout:
            return String(localized: "The server took too long to respond. Please try again.")
        case is AppError.NotFound:
            return String(localized: "The requested item could not be found.")
        case let server as AppError.Server:
            let format = String(localized: "Server error (%lld). Please try again later.")
            return String(format: format, Int(server.code))
        case let unknown as AppError.Unknown:
            if let raw = unknown.rawMessage, !raw.isEmpty {
                return raw
            }
            return String(localized: "An unexpected error occurred.")
        default:
            return String(localized: "An unexpected error occurred.")
        }
    }
}
