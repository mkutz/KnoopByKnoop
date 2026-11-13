import KnoopByKnoopCore
import SwiftUI

/// The panel with fewer than two waypoints: "Plan a ride", the start and the end slot, a hint and
/// the ways to begin, see `map-planning` "Panel before a route".
struct EmptyRoute: View {

  let description: RouteDescriptionStartOnly?
  let noKnotsInView: Bool
  let onSearch: (() -> Void)?
  let onNearest: () -> Void
  let onClear: () -> Void
  /// Undo, after an edit, also with no waypoint left, see `map-planning` "Undo route edits".
  let onUndo: (() -> Void)?
  @Environment(\.dynamicTypeSize) private var dynamicTypeSize

  var body: some View {
    let waypoint = description?.waypoint
    let endFirst = description?.endFirst == true
    let start = endFirst ? nil : waypoint
    let end = endFirst ? waypoint : nil
    let startLabel = String(localized: "waypoint.start", defaultValue: "Start", bundle: AppLanguage.bundle)
    let endLabel = String(localized: "waypoint.end", defaultValue: "End", bundle: AppLanguage.bundle)
    VStack(alignment: .leading, spacing: 10) {
      Text("Plan a ride").font(.title2.bold())
      HStack(spacing: 8) {
        EmptyRouteSlot(label: startLabel, knot: start, role: .start, next: start == nil)
        DottedLine().frame(height: 2)
        EmptyRouteSlot(label: endLabel, knot: end, role: .end, next: start != nil && end == nil)
      }
      // Read with the slots, see `EmptyRouteSlot`. One below the other at the largest text sizes, so
      // the place is not cut, see "Empty panel at large text sizes".
      Group {
        if dynamicTypeSize.isAccessibilitySize {
          VStack(alignment: .leading, spacing: 2) {
            Text(verbatim: Self.label(startLabel, start))
            Text(verbatim: Self.label(endLabel, end))
          }
        } else {
          HStack {
            Text(verbatim: Self.label(startLabel, start)).frame(maxWidth: .infinity, alignment: .leading)
            Text(verbatim: Self.label(endLabel, end)).frame(maxWidth: .infinity, alignment: .trailing)
          }
          .lineLimit(1)
        }
      }
      .font(.caption)
      .foregroundStyle(.secondary)
      .fixedSize(horizontal: false, vertical: true)
      .accessibilityHidden(true)
      Group {
        if waypoint == nil && noKnotsInView {
          Text("No knots in view")
        } else if waypoint == nil {
          Text("Tap a numbered knot on the map for the start.")
        } else if endFirst {
          Text("Now tap a knot for the start.")
        } else {
          Text("Now tap a knot for the end.")
        }
      }
      .font(.callout)
      .foregroundStyle(.secondary)
      .fixedSize(horizontal: false, vertical: true)
      ViewThatFits(in: .horizontal) {
        HStack(spacing: 8) { buttons(waypoint: waypoint) }
        VStack(alignment: .leading, spacing: 8) { buttons(waypoint: waypoint) }
      }
    }
  }

  @ViewBuilder private func buttons(waypoint: Knot?) -> some View {
    if let onSearch {
      Button("Search a place", systemImage: "magnifyingglass", action: onSearch)
        .buttonStyle(.bordered)
        .buttonBorderShape(.capsule)
        .fixedSize(horizontal: false, vertical: true)
        .tint(.actionIconGreen)
    }
    if waypoint != nil {
      Button("Clear", systemImage: "trash", action: onClear)
        .buttonStyle(.bordered)
        .buttonBorderShape(.capsule)
        .fixedSize(horizontal: false, vertical: true)
        .tint(.actionIconGreen)
    } else if noKnotsInView {
      // The prominent action with no knots in view, see `map-planning` "Nearest knots".
      Button("Nearest knots", systemImage: "signpost.right", action: onNearest)
        .buttonStyle(.borderedProminent)
        .buttonBorderShape(.capsule)
        .fixedSize(horizontal: false, vertical: true)
        .tint(.actionGreen)
    } else {
      Button("Nearest knots", systemImage: "signpost.right", action: onNearest)
        .buttonStyle(.bordered)
        .buttonBorderShape(.capsule)
        .fixedSize(horizontal: false, vertical: true)
        .tint(.actionIconGreen)
    }
    if let onUndo {
      IconAction(title: "Undo", systemImage: "arrow.counterclockwise", action: onUndo)
    }
  }

  static func placeName(of knot: Knot) -> String? {
    knot.routePoint?.placeName ?? knot.place?.nameIn(language: AppLanguage.current)
  }

  private static func label(_ role: String, _ knot: Knot?) -> String {
    guard let knot, let place = placeName(of: knot) else { return role }
    return "\(role) · \(place)"
  }
}

/// A slot for the start or the end: the waypoint's badge when set, else a dashed circle, which
/// breathes when the next tap fills it, unless Reduce Motion is on. Read with its label and place
/// as one, e.g. "Start, 48, De Rips" or "End, not set yet".
private struct EmptyRouteSlot: View {

  let label: String
  let knot: Knot?
  let role: KnotRole
  let next: Bool
  @Environment(\.accessibilityReduceMotion) private var reduceMotion
  @State private var breathing = false

  var body: some View {
    ZStack {
      if let knot {
        KnotBadge(
          ref: knot.ref, role: role, plate: .of(knot.signRegion), scale: 1.15,
          place: knot.routePoint?.placeKind)
      } else {
        let color: Color = next ? .actionIconGreen : .secondary
        Circle()
          .fill(Color.actionGreen.opacity(next && breathing ? 0.18 : 0))
          .frame(width: 52, height: 52)
        Circle()
          .fill(Color(uiColor: .systemBackground).opacity(0.6))
          .overlay(Circle().strokeBorder(color, style: StrokeStyle(lineWidth: 2, lineCap: .round, dash: [5, 4])))
          .overlay(Text(verbatim: "?").font(.headline).dynamicTypeSize(...DynamicTypeSize.xLarge).foregroundStyle(color))
          .frame(width: 40, height: 40)
      }
    }
    .frame(width: 52, height: 52)
    .accessibilityElement(children: .ignore)
    .accessibilityLabel(Text(verbatim: spoken))
    .onAppear {
      guard next, !reduceMotion else { return }
      withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) { breathing = true }
    }
  }

  private var spoken: String {
    guard let knot else {
      return "\(label), \(String(localized: "not set yet", bundle: AppLanguage.bundle))"
    }
    let place = EmptyRoute.placeName(of: knot)
    return ([label] + (knot.isRoutePoint ? [] : [knot.ref]) + [place].compactMap { $0 }).joined(separator: ", ")
  }
}

/// The dotted line between the start and the end slot.
private struct DottedLine: View {
  var body: some View {
    GeometryReader { geometry in
      Path { path in
        path.move(to: CGPoint(x: 0, y: geometry.size.height / 2))
        path.addLine(to: CGPoint(x: geometry.size.width, y: geometry.size.height / 2))
      }
      .stroke(Color.secondary.opacity(0.6), style: StrokeStyle(lineWidth: 2, lineCap: .round, dash: [0.1, 6]))
    }
  }
}
