import KnoopByKnoopCore
import SwiftUI

/// Marks a ferry between two knots of the route summary, see `map-planning`.
struct FerryBadge: View {

  var body: some View {
    Text("⛴")
      .font(.system(size: 14))
      .frame(width: 28, height: 28)
      .background(Circle().fill(Color.accentColor.opacity(0.2)))
  }
}

/// The route's ferries with whether they are open now, their operating times, fee and website, see
/// `map-planning`; while `navigation` runs, also when the rider gets to each ferry ahead, see
/// `knot-navigation` "Ferry arrival estimate".
struct RouteFerries: View {

  let ferries: [RouteFerry]
  var navigation: Navigation? = nil

  var body: some View {
    // Updated at every full minute while shown, see "Ferry open now".
    TimelineView(.everyMinute) { context in
      list(now: Self.ferryMoment(context.date))
    }
  }

  private func list(now: LocalMoment) -> some View {
    VStack(alignment: .leading, spacing: 6) {
      Text("Ferries on this route").font(.subheadline.bold())
      ForEach(Array(ferries.enumerated()), id: \.offset) { _, ferry in
        VStack(alignment: .leading, spacing: 2) {
          Text("⛴ \(ferry.title)").font(.callout)
          // While navigating, the arrival line takes the place of the status for now, see
          // `knot-navigation` "Ferry arrival estimate".
          if let arrival = navigation?.ferryArrival(fromPosition: ferry.fromPosition) {
            Text(arrival.text).font(.caption.weight(.medium))
              .foregroundStyle(Self.color(of: arrival.state))
          } else if let status = ferry.statusAt(now: now) {
            Text(status.text).font(.caption.weight(.medium))
              .foregroundStyle(status.open ? Color.actionIconGreen : Color.red)
          }
          Text(ferry.times).font(.caption).foregroundStyle(ferry.timesKnown ? Color.secondary : Color.red)
          if let fee = ferry.fee { Text(fee).font(.caption).foregroundStyle(.secondary) }
          if let website = ferry.ferry.website,
            let url = URL(string: website.contains("://") ? website : "https://\(website)")
          {
            Link("Website", destination: url).font(.caption)
          }
        }
      }
    }
    .padding(.top, 4)
  }

  private static func color(of state: FerryArrivalState) -> Color {
    switch state {
    case .open: Color.actionIconGreen
    case .warning: Color.red
    default: Color.primary
    }
  }

  /// The networks' local time: the Netherlands, Belgium and Nordrhein-Westfalen share it, whatever
  /// time zone the phone is set to.
  private static let ferryCalendar: Calendar = {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(identifier: "Europe/Amsterdam")!
    return calendar
  }()

  static func ferryMoment(_ date: Date) -> LocalMoment {
    let parts = ferryCalendar.dateComponents([.year, .month, .day, .hour, .minute], from: date)
    return LocalMoment(
      day: CalendarDay(year: Int32(parts.year!), month: Int32(parts.month!), day: Int32(parts.day!)),
      minute: Int32(parts.hour! * 60 + parts.minute!))
  }
}
