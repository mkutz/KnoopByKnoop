import KnoopByKnoopCore
import SwiftUI

/// Where the rider is on the route while navigating: the progress strip and the distances and
/// times ahead, see `knot-navigation` "Progress strip" and "Distances and times ahead".
struct ProgressPanel: View {

  let progress: KnoopByKnoopCore.ProgressView

  var body: some View {
    VStack(alignment: .leading, spacing: 8) {
      ProgressStripView(strip: progress.strip)
      Grid(alignment: .leading, horizontalSpacing: 12, verticalSpacing: 2) {
        ForEach(Array(progress.rows.enumerated()), id: \.offset) { _, row in
          GridRow {
            Text(row.label).foregroundStyle(.secondary)
            Text(row.knot).bold()
            Text(row.distance).gridColumnAlignment(.trailing)
            Text(row.time ?? "").gridColumnAlignment(.trailing)
            Text(row.arrival ?? "").gridColumnAlignment(.trailing)
          }
          .font(.callout)
          .lineLimit(1)
        }
      }
    }
  }
}

/// The route drawn to scale with the rider as a dot and the ridden part filled; labels that would
/// overlap a stronger one are left out, the marks of the other knots always drawn.
private struct ProgressStripView: View {

  let strip: ProgressStrip

  private static let badgeSpace: CGFloat = 30
  private static let height: CGFloat = 44
  /// The rider's color, as their position on the map.
  private static let riderBlue = Color(red: 0x1A / 255, green: 0x73 / 255, blue: 0xE8 / 255)

  var body: some View {
    GeometryReader { geometry in
      let inset = Self.badgeSpace / 2
      let lineWidth = geometry.size.width - Self.badgeSpace
      let y = Self.height / 2
      let riderX = inset + lineWidth * CGFloat(strip.rider)
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
        ForEach(Array(Self.shownLabels(strip.marks, lineWidth: lineWidth).enumerated()), id: \.offset) {
          _, mark in
          label(mark)
            .position(x: inset + lineWidth * CGFloat(mark.fraction), y: y)
        }
        // Above the badges, so the rider stays visible at a labeled knot.
        Circle()
          .fill(strip.offRoute ? Color.red : Self.riderBlue)
          .overlay(Circle().stroke(.white, lineWidth: 2))
          .frame(width: 14, height: 14)
          .position(x: riderX, y: y)
      }
    }
    .frame(height: Self.height)
    .accessibilityElement(children: .ignore)
    .accessibilityLabel(strip.description_)
  }

  @ViewBuilder private func label(_ mark: ProgressMark) -> some View {
    switch mark.kind {
    case .ferry: FerryBadge()
    case .start: KnotBadge(ref: mark.label, role: .start)
    case .stop: KnotBadge(ref: mark.label, role: .stop)
    default: KnotBadge(ref: mark.label, role: .end)
    }
  }

  /// The labeled marks that fit, the stronger ones first: end, stop, start, ferry.
  private static func shownLabels(_ marks: [ProgressMark], lineWidth: CGFloat) -> [ProgressMark] {
    var shown: [ProgressMark] = []
    for kind in [ProgressMarkKind.end, .stop, .start, .ferry] {
      for mark in marks where mark.kind == kind {
        let fits = shown.allSatisfy { lineWidth * abs(CGFloat($0.fraction - mark.fraction)) >= badgeSpace }
        if fits { shown.append(mark) }
      }
    }
    return shown
  }
}
