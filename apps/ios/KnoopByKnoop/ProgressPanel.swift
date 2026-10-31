import KnoopByKnoopCore
import SwiftUI

/// Where the rider is on the route while navigating: the progress strip with the distances and
/// arrivals below its marks and the ferries, which open a popover, see `knot-navigation` "Progress
/// strip", "Distances and arrivals on the strip", "Ferries on the strip" and "Ferry popover".
struct ProgressPanel: View {

  let progress: KnoopByKnoopCore.ProgressView
  let ferries: [RouteFerry]
  let navigation: Navigation

  var body: some View {
    ProgressStripView(strip: progress.strip, ferries: ferries, navigation: navigation)
  }
}

/// The route drawn to scale with the rider as a dot and the ridden part filled; labels that would
/// overlap a stronger one are left out, the marks of the other knots always drawn.
private struct ProgressStripView: View {

  let strip: ProgressStrip
  let ferries: [RouteFerry]
  let navigation: Navigation

  /// The route position of the ferry whose popover is open.
  @State private var opened: Int32?

  private static let badgeSpace: CGFloat = 30
  private static let height: CGFloat = 44
  /// The room for a mark's distance and arrival, e.g. `280 km` over `04:30`; growing with the
  /// text, see `knot-navigation` "Strip labels at large text sizes".
  @ScaledMetric(relativeTo: .callout) private var labelWidth: CGFloat = 70
  @ScaledMetric(relativeTo: .callout) private var labelHeight: CGFloat = 40
  /// The rider's color, as their position on the map.
  private static let riderBlue = Color(red: 0x1A / 255, green: 0x73 / 255, blue: 0xE8 / 255)

  var body: some View {
    GeometryReader { geometry in
      let inset = Self.badgeSpace / 2
      let lineWidth = geometry.size.width - Self.badgeSpace
      let y = Self.height / 2
      let riderX = inset + lineWidth * CGFloat(strip.rider)
      let badges = Self.shown(strip.marks, lineWidth: lineWidth, space: Self.badgeSpace) {
        $0.kind != .knot
      }
      ZStack(alignment: .topLeading) {
        // For screen readers, the strip is one element, except the ferries, which are buttons.
        ZStack(alignment: .topLeading) {
          Path { path in
            path.move(to: CGPoint(x: inset, y: y))
            path.addLine(to: CGPoint(x: inset + lineWidth, y: y))
          }
          .stroke(Color.secondary.opacity(0.3), style: StrokeStyle(lineWidth: 4, lineCap: .round))
          Path { path in
            path.move(to: CGPoint(x: inset, y: y))
            path.addLine(to: CGPoint(x: riderX, y: y))
          }
          .stroke(Color.actionGreen, style: StrokeStyle(lineWidth: 4, lineCap: .round))
          Path { path in
            for mark in strip.marks where mark.kind == .knot {
              let x = inset + lineWidth * CGFloat(mark.fraction)
              path.move(to: CGPoint(x: x, y: y - 5))
              path.addLine(to: CGPoint(x: x, y: y + 5))
            }
          }
          .stroke(Color.secondary, lineWidth: 1)
        }
        .frame(width: geometry.size.width, height: Self.height)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(strip.description_)
        ForEach(Array(badges.enumerated()), id: \.offset) { _, mark in
          badge(mark)
            .position(x: inset + lineWidth * CGFloat(mark.fraction), y: y)
        }
        let labeled = Self.shown(badges, lineWidth: lineWidth, space: labelWidth) {
          $0.distance != nil
        }
        ForEach(Array(labeled.enumerated()), id: \.offset) { _, mark in
          // Centered below the mark, but within the strip at both ends.
          let x = min(
            max(inset + lineWidth * CGFloat(mark.fraction), labelWidth / 2),
            geometry.size.width - labelWidth / 2)
          VStack(spacing: 0) {
            Text(mark.distance ?? "").font(.callout.bold())
            if let arrival = mark.arrival {
              Text(arrival)
                .font(mark.state != nil ? .callout.weight(.medium) : .callout)
                .foregroundStyle(Self.arrivalColor(mark.state))
            }
          }
          .lineLimit(1)
          .frame(width: labelWidth)
          .position(x: x, y: Self.height + labelHeight / 2)
          .accessibilityHidden(true)
        }
        // Above the badges, so the rider stays visible at a labeled knot.
        Circle()
          .fill(strip.offRoute ? Color.red : Self.riderBlue)
          .overlay(Circle().stroke(.white, lineWidth: 2))
          .frame(width: 14, height: 14)
          .position(x: riderX, y: y)
          .allowsHitTesting(false)
          .accessibilityHidden(true)
      }
    }
    .frame(height: Self.height + labelHeight)
    .accessibilityElement(children: .contain)
  }

  @ViewBuilder private func badge(_ mark: ProgressMark) -> some View {
    if mark.kind == .ferry, let ferry = ferries.first(where: { $0.fromPosition == mark.position?.int32Value }) {
      Button {
        opened = ferry.fromPosition
      } label: {
        FerryBadge(warning: mark.state == .warning).frame(width: 44, height: 44).contentShape(Circle())
      }
      .buttonStyle(.plain)
      .accessibilityLabel(Self.description(of: ferry, navigation: navigation))
      .popover(
        isPresented: Binding(
          get: { opened == ferry.fromPosition },
          set: { if !$0 { opened = nil } })
      ) {
        FerryPopover(ferry: ferry, navigation: navigation)
          .presentationCompactAdaptation(.popover)
          .appAppearance()
      }
    } else {
      Group {
        switch mark.kind {
        case .start: KnotBadge(ref: mark.label, role: .start)
        case .stop: KnotBadge(ref: mark.label, role: .stop)
        default: KnotBadge(ref: mark.label, role: .end)
        }
      }
      .accessibilityHidden(true)
    }
  }

  private static func arrivalColor(_ state: FerryArrivalState?) -> Color {
    switch state {
    case .warning: .red
    case .open: .actionIconGreen
    default: .secondary
    }
  }

  /// E.g. `Ferry 76 → 02, at 21:10 closed until 1 May 10:00`, see "Ferry popover".
  private static func description(of ferry: RouteFerry, navigation: Navigation) -> String {
    let status =
      navigation.ferryArrival(fromPosition: ferry.fromPosition)?.accessibilityText
      ?? ferry.statusAt(now: RouteFerries.ferryMoment(Date())).map { status in
        status.text.prefix(1).lowercased() + status.text.dropFirst()
      }
    return ["Ferry \(ferry.place)", status].compactMap { $0 }.joined(separator: ", ")
  }

  /// How important a mark is where its badge or its distance would overlap another's: lower first.
  private static func rank(_ mark: ProgressMark) -> Int {
    switch mark.kind {
    case .end: 0
    case .ferry where mark.state == .warning: 1
    case .stop: 2
    case .start: 3
    case .ferry: 4
    default: Int.max
    }
  }

  /// The marks that pass `include` and fit with `space` around them, the more important ones first.
  private static func shown(
    _ marks: [ProgressMark], lineWidth: CGFloat, space: CGFloat, include: (ProgressMark) -> Bool
  ) -> [ProgressMark] {
    var shown: [ProgressMark] = []
    for mark in marks.filter(include).sorted(by: { rank($0) < rank($1) }) {
      if shown.allSatisfy({ lineWidth * abs(CGFloat($0.fraction - mark.fraction)) >= space }) {
        shown.append(mark)
      }
    }
    return shown.sorted { $0.fraction < $1.fraction }
  }
}

/// A ferry's details next to its badge on the strip, see `knot-navigation` "Ferry popover".
private struct FerryPopover: View {

  let ferry: RouteFerry
  let navigation: Navigation

  var body: some View {
    VStack(alignment: .leading, spacing: 4) {
      Text("⛴ \(ferry.place)").font(.headline)
      FerryDetails(
        ferry: ferry,
        line: RouteFerries.line(of: ferry, now: RouteFerries.ferryMoment(Date()), navigation: navigation))
    }
    .padding()
    .frame(maxWidth: 320, alignment: .leading)
  }
}
