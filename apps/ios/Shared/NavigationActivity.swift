import ActivityKit
import AppIntents

/// The Live Activity while navigating, see `knot-navigation` "Navigation with the screen off".
/// Shared by the app, which starts and updates it, and the widget extension, which shows it. The
/// app passes plain strings, so the extension does not link the core.
struct NavigationActivityAttributes: ActivityAttributes {

  struct ContentState: Codable, Hashable {
    /// The next knot's number, e.g. `45`.
    var next: String
    /// The number to follow at the next knot, or `nil` at the end of the route.
    var follow: String?
    var offRoute: Bool
    /// How the next knot and the one to follow take part in the route, for their badges.
    var nextRole: BadgeRole?
    var followRole: BadgeRole?
    /// What to show in full, e.g. `At 45 follow 12`, `31 is the end` or `Off route, next 45`.
    var text: String
    /// The progress as of the last knot passed, e.g. `800 m to 45 · end around 19:05`, see
    /// `knot-navigation` "Navigation with the screen off".
    var progress: String? = nil
  }
}

/// Stops navigating from the Live Activity. Runs in the app, which sets `stop`.
struct StopNavigationIntent: LiveActivityIntent {

  static var title: LocalizedStringResource = "Stop navigation"

  @MainActor static var stop: (() -> Void)?

  func perform() async throws -> some IntentResult {
    await MainActor.run { Self.stop?() }
    return .result()
  }
}
