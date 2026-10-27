import KnoopByKnoopCore
import SwiftUI

extension Color {
  /// The green and orange of the app icon, see `design/icon`.
  static let knoopGreen = Color(red: 0x2E / 255, green: 0x9E / 255, blue: 0x5B / 255)
  static let knoopOrange = Color(red: 0xF2 / 255, green: 0x8C / 255, blue: 0x28 / 255)
  static let knoopDark = Color(red: 0x26 / 255, green: 0x32 / 255, blue: 0x38 / 255)
}

/// How a knot is drawn, on the map and in the sheet alike, see the design of `improve-planner-map`.
/// `nil` is a knot of the network that is not on the route.
private struct BadgeStyle {
  let fill: Color
  let ring: Color
  let text: Color
  let large: Bool
  let thickRing: Bool

  init(_ role: KnotRole?) {
    switch role {
    case .passed?: self = .init(fill: .white, ring: .knoopOrange, text: .knoopOrange, large: false, thickRing: true)
    case .stop?: self = .init(fill: .knoopOrange, ring: .white, text: .white, large: false, thickRing: false)
    case .start?: self = .init(fill: .knoopGreen, ring: .white, text: .white, large: true, thickRing: false)
    case .end?: self = .init(fill: .knoopDark, ring: .white, text: .white, large: true, thickRing: false)
    // Drawn as two halves, start green and end dark, see image(_:_:) and KnotBadge.
    case .roundTrip?: self = .init(fill: .knoopGreen, ring: .white, text: .white, large: true, thickRing: false)
    default: self = .init(fill: .white, ring: .knoopGreen, text: .knoopGreen, large: false, thickRing: false)
    }
  }

  private init(fill: Color, ring: Color, text: Color, large: Bool, thickRing: Bool) {
    self.fill = fill
    self.ring = ring
    self.text = text
    self.large = large
    self.thickRing = thickRing
  }
}

enum KnotBadges {

  /// The map image name of a badge; the map's symbol layers build the same names from GeoJSON.
  static func name(_ role: KnotRole?, _ ref: String) -> String {
    role.map { "route-\($0.name.lowercased())-\(ref)" } ?? "knot-\(ref)"
  }

  /// A round badge with the knot's number, drawn locally so numbers show without network access.
  static func image(_ role: KnotRole?, _ ref: String) -> UIImage {
    let style = BadgeStyle(role)
    let side: CGFloat = style.large ? 28 : 22
    let size = CGSize(width: side, height: side)
    return UIGraphicsImageRenderer(size: size).image { context in
      let circle = CGRect(origin: .zero, size: size).insetBy(dx: 1.5, dy: 1.5)
      UIColor(style.fill).setFill()
      context.cgContext.fillEllipse(in: circle)
      if role == .roundTrip {
        // The right half is the end's color: start and end of a round trip in one badge.
        let half = UIBezierPath(
          arcCenter: CGPoint(x: circle.midX, y: circle.midY), radius: circle.width / 2, startAngle: -.pi / 2,
          endAngle: .pi / 2, clockwise: true)
        UIColor(Color.knoopDark).setFill()
        half.fill()
      }
      UIColor(style.ring).setStroke()
      context.cgContext.setLineWidth(style.thickRing ? 3 : 2)
      context.cgContext.strokeEllipse(in: circle)
      let fontSize: CGFloat = (ref.count > 2 ? 7 : 9) * (style.large ? 1.25 : 1)
      let attributes: [NSAttributedString.Key: Any] = [
        .font: UIFont.boldSystemFont(ofSize: fontSize), .foregroundColor: UIColor(style.text),
      ]
      let text = ref as NSString
      let textSize = text.size(withAttributes: attributes)
      text.draw(
        at: CGPoint(x: (size.width - textSize.width) / 2, y: (size.height - textSize.height) / 2),
        withAttributes: attributes)
    }
  }

  /// An arrow pointing along the line it is placed on, for the riding direction.
  static func arrow() -> UIImage {
    UIGraphicsImageRenderer(size: CGSize(width: 16, height: 16)).image { context in
      let path = UIBezierPath()
      path.move(to: CGPoint(x: 5, y: 3.5))
      path.addLine(to: CGPoint(x: 11.5, y: 8))
      path.addLine(to: CGPoint(x: 5, y: 12.5))
      path.lineWidth = 2.75
      path.lineCapStyle = .round
      path.lineJoinStyle = .round
      UIColor.white.setStroke()
      path.stroke()
    }
  }
}

/// The same badge as on the map, for the sheet.
struct KnotBadge: View {

  let ref: String
  let role: KnotRole?

  var body: some View {
    let style = BadgeStyle(role)
    let side: CGFloat = style.large ? 34 : 28
    Text(ref)
      .font(.system(size: (ref.count > 2 ? 10 : 12) * (style.large ? 1.2 : 1), weight: .bold))
      .foregroundStyle(style.text)
      .frame(width: side, height: side)
      .background(
        Circle().fill(
          role == .roundTrip
            ? AnyShapeStyle(
              LinearGradient(
                stops: [.init(color: .knoopGreen, location: 0.5), .init(color: .knoopDark, location: 0.5)],
                startPoint: .leading, endPoint: .trailing))
            : AnyShapeStyle(style.fill)))
      .overlay(Circle().stroke(style.ring, lineWidth: style.thickRing ? 3 : 2))
  }
}

/// Lays out its views in rows that wrap, like words in a text.
struct WrappingRow: Layout {

  var spacing: CGFloat = 4

  func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
    let rows = arrange(subviews, width: proposal.width ?? .infinity)
    return CGSize(
      width: rows.map(\.width).max() ?? 0,
      height: rows.map(\.height).reduce(0, +) + spacing * CGFloat(max(rows.count - 1, 0)))
  }

  func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
    var y = bounds.minY
    for row in arrange(subviews, width: bounds.width) {
      var x = bounds.minX
      for index in row.indices {
        let size = subviews[index].sizeThatFits(.unspecified)
        subviews[index].place(
          at: CGPoint(x: x, y: y + (row.height - size.height) / 2), proposal: ProposedViewSize(size))
        x += size.width + spacing
      }
      y += row.height + spacing
    }
  }

  private func arrange(_ subviews: Subviews, width: CGFloat) -> [(indices: [Int], width: CGFloat, height: CGFloat)] {
    var rows: [(indices: [Int], width: CGFloat, height: CGFloat)] = []
    for index in subviews.indices {
      let size = subviews[index].sizeThatFits(.unspecified)
      if let last = rows.last, last.width + spacing + size.width <= width {
        rows[rows.count - 1] = (last.indices + [index], last.width + spacing + size.width, max(last.height, size.height))
      } else {
        rows.append(([index], size.width, size.height))
      }
    }
    return rows
  }
}
