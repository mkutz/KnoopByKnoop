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

/// The route's ferries, each in one line with whether it is open now, expanded on tap to its
/// operating times, fee and website, see `map-planning` "Ferries on the route"; while `navigation`
/// runs, the line tells when the rider gets to each ferry ahead instead, see `knot-navigation`
/// "Ferry arrival estimate".
struct RouteFerries: View {

  let ferries: [RouteFerry]
  var navigation: Navigation? = nil

  /// The route positions of the expanded ferries; a new route starts collapsed.
  @State private var expanded: Set<Int32> = []

  var body: some View {
    // Updated at every full minute while shown, see "Ferry open now".
    TimelineView(.everyMinute) { context in
      list(now: Self.ferryMoment(context.date))
    }
    .onChange(of: ferries.map(\.fromPosition)) { _, _ in expanded = [] }
  }

  private func list(now: LocalMoment) -> some View {
    VStack(alignment: .leading, spacing: 0) {
      ForEach(Array(ferries.enumerated()), id: \.offset) { _, ferry in
        row(ferry, line: line(of: ferry, now: now), expanded: expanded.contains(ferry.fromPosition))
      }
    }
  }

  /// What a ferry's line tells after its name, in which color, and whether it is a status or
  /// arrival rather than the tagged times or their absence.
  private struct Line {
    let text: String
    let color: Color
    var isStatus = true
  }

  /// While navigating, the arrival takes the place of the status for now, see `knot-navigation`
  /// "Ferry arrival estimate"; without a status, the tagged times, see `map-planning` "Ferry open
  /// now".
  private func line(of ferry: RouteFerry, now: LocalMoment) -> Line {
    if let arrival = navigation?.ferryArrival(fromPosition: ferry.fromPosition) {
      return Line(text: arrival.text, color: Self.color(of: arrival.state))
    }
    if let status = ferry.statusAt(now: now) {
      return Line(text: status.text, color: status.open ? Color.actionIconGreen : Color.red)
    }
    return ferry.timesKnown
      ? Line(text: ferry.times, color: .secondary, isStatus: false)
      : Line(text: "Operating times not known", color: .red, isStatus: false)
  }

  private func row(_ ferry: RouteFerry, line: Line, expanded: Bool) -> some View {
    VStack(alignment: .leading, spacing: 2) {
      Button {
        if expanded { self.expanded.remove(ferry.fromPosition) } else { self.expanded.insert(ferry.fromPosition) }
      } label: {
        HStack(spacing: 0) {
          // The name gives way to the status, which answers the usual question, see "Ferry line
          // too long".
          Text("⛴ \(ferry.title)").font(.callout).foregroundStyle(.primary)
            .lineLimit(expanded ? nil : 1)
            .layoutPriority(0)
          if !expanded {
            Text(" · \(line.text)").font(.callout.weight(.medium)).foregroundStyle(line.color)
              .lineLimit(1)
              .layoutPriority(1)
          }
          Spacer(minLength: 8)
          Image(systemName: "chevron.down").font(.caption).foregroundStyle(.secondary)
            .rotationEffect(.degrees(expanded ? 180 : 0))
        }
        .frame(minHeight: 44)
        .contentShape(Rectangle())
      }
      .buttonStyle(.plain)
      .accessibilityValue(expanded ? "Expanded" : "Collapsed")
      if expanded {
        VStack(alignment: .leading, spacing: 2) {
          if line.isStatus { Text(line.text).font(.caption.weight(.medium)).foregroundStyle(line.color) }
          Text(ferry.times).font(.caption).foregroundStyle(ferry.timesKnown ? Color.secondary : Color.red)
          if let fee = ferry.fee { Text(fee).font(.caption).foregroundStyle(.secondary) }
          Text("Times according to OpenStreetMap").font(.caption).foregroundStyle(.secondary)
          if let website = ferry.ferry.website,
            let url = URL(string: website.contains("://") ? website : "https://\(website)")
          {
            Link("Website", destination: url).font(.caption)
          }
        }
        .padding(.leading, 20)
        .padding(.bottom, 8)
      }
    }
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
