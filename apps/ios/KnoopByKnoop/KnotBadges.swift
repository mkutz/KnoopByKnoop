import KnoopByKnoopCore
import SwiftUI

extension BadgeRole {
  init(_ role: KnotRole) {
    switch role {
    case .start: self = .start
    case .stop: self = .stop
    case .end: self = .end
    case .roundTrip: self = .roundTrip
    default: self = .passed
    }
  }
}

enum KnotBadges {

  /// The map image name of a badge; the map's symbol layers build the same names from GeoJSON.
  static func name(_ role: KnotRole?, _ ref: String) -> String {
    role.map { "route-\($0.name.lowercased())-\(ref)" } ?? "knot-\(ref)"
  }

  /// A round badge with the knot's number, drawn locally so numbers show without network access.
  static func image(_ role: KnotRole?, _ ref: String) -> UIImage {
    let style = BadgeStyle(role.map(BadgeRole.init))
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

  var body: some View { Badge(ref: ref, role: role.map(BadgeRole.init)) }
}
