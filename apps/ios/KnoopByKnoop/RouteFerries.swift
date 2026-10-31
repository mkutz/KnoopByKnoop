import KnoopByKnoopCore
import SwiftUI

/// Marks a ferry between two knots of the route summary, see `map-planning`.
struct FerryBadge: View {

  /// Closed at the rider's arrival: a red ring, see `knot-navigation` "Ferry warning".
  var warning = false

  var body: some View {
    Text("⛴")
      .font(.system(size: 14))
      .frame(width: 28, height: 28)
      // Opaque like the knots' badges: the light tint on the background color, not on what is behind.
      .background(Circle().fill(Color.accentColor.opacity(0.2)))
      .background(Circle().fill(Color(uiColor: .systemBackground)))
      .overlay(Circle().stroke(Color.red, lineWidth: warning ? 3 : 0))
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

  /// One list row per ferry: a row sends taps anywhere in it to its first link, so with one row
  /// for all, tapping a ferry without a website opened the website of another.
  var body: some View {
    ForEach(Array(ferries.enumerated()), id: \.offset) { _, ferry in
      // Updated at every full minute while shown, see "Ferry open now".
      TimelineView(.everyMinute) { context in
        let now = Self.ferryMoment(context.date)
        row(ferry, line: line(of: ferry, now: now), expanded: expanded.contains(ferry.fromPosition))
      }
      .listRowSeparator(.hidden)
      .listRowBackground(Color.clear)
    }
    .onChange(of: ferries.map(\.fromPosition)) { _, _ in expanded = [] }
  }

  /// What a ferry's line tells after its name, in which color, and whether it is a status or
  /// arrival rather than the tagged times or their absence.
  struct Line {
    let text: String
    let color: Color
    var isStatus = true
  }

  /// While navigating, the arrival takes the place of the status for now, see `knot-navigation`
  /// "Ferry arrival estimate"; without a status, the tagged times, see `map-planning` "Ferry open
  /// now".
  private func line(of ferry: RouteFerry, now: LocalMoment) -> Line {
    Self.line(of: ferry, now: now, navigation: navigation)
  }

  static func line(of ferry: RouteFerry, now: LocalMoment, navigation: Navigation?) -> Line {
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
          // Where the ferry is on the route rather than its name, which is often long, see
          // `map-planning` "Ferries on the route".
          Text("⛴ \(ferry.place)").font(.callout).foregroundStyle(.primary)
            .layoutPriority(1)
          if !expanded {
            Text(" · \(line.text)").font(.callout.weight(.medium)).foregroundStyle(line.color)
              .lineLimit(1)
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
        FerryDetails(ferry: ferry, line: line)
        .padding(.leading, 20)
        .padding(.bottom, 8)
      }
    }
  }

  static func color(of state: FerryArrivalState) -> Color {
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

/// A ferry's name, its status or arrival, operating times, fee and website: the expanded ferry row,
/// and the popover of a ferry on the progress strip, see `knot-navigation` "Ferry popover".
struct FerryDetails: View {

  let ferry: RouteFerry
  let line: RouteFerries.Line

  var body: some View {
    VStack(alignment: .leading, spacing: 2) {
      Text(ferry.title).font(.callout)
      if line.isStatus { Text(line.text).font(.caption.weight(.medium)).foregroundStyle(line.color) }
      Text(ferry.times).font(.caption).foregroundStyle(ferry.timesKnown ? Color.secondary : Color.red)
      if let fee = ferry.fee { Text(fee).font(.caption).foregroundStyle(.secondary) }
      Text("Times according to OpenStreetMap").font(.caption).foregroundStyle(.secondary)
      if let website = ferry.ferry.website,
        let url = URL(string: website.contains("://") ? website : "https://\(website)")
      {
        Link("Website", destination: url).font(.caption).buttonStyle(.borderless)
      }
    }
  }
}
