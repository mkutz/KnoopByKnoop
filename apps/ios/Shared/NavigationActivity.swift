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
    /// The progress about every 100 m, e.g. `800 m to 45 · end around 19:05`, see
    /// `knot-navigation` "Navigation with the screen off".
    var progress: String? = nil
    /// Whether the rider is close to the next knot, so the number to follow is the most prominent.
    var close = true
    /// The rider's place on the route, from 0 to 1, for the bar; `nil` until it is known.
    var riderFraction: Double? = nil
    /// The stops and ferries ahead on the bar, the most important first.
    var marks: [BarMark] = []
    /// Whether the system is in dark mode. From the app, as the Live Activity's own `colorScheme`
    /// is dark on the lock screen also when the system is light.
    var dark = false
    /// Whether the rider rides back along the route, see `knot-navigation` "Riding the wrong way".
    var wrongWay = false
  }

  /// A stop or ferry on the bar along the route.
  struct BarMark: Codable, Hashable {
    enum Kind: String, Codable { case stop, ferry, closedFerry }
    var fraction: Double
    var kind: Kind
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
