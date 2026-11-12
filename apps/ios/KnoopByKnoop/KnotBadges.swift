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

extension KnotPlate {
  /// The plate of a knot in `region`, the app's green without one, see `map-planning` "Route knots
  /// stand out".
  static func of(_ region: SignRegion?) -> KnotPlate { KnotPlate.companion.of(region: region) }

  static var none: KnotPlate { KnotPlate.companion.NONE }

  var uiColor: UIColor {
    let value = UInt32(truncatingIfNeeded: colour)
    return UIColor(
      red: CGFloat((value >> 16) & 0xFF) / 255, green: CGFloat((value >> 8) & 0xFF) / 255,
      blue: CGFloat(value & 0xFF) / 255, alpha: 1)
  }
}

extension UIColor {
  /// Corner symbols and marks for problems, never red, see `map-planning` "Route knots stand out".
  static let knoopInk = UIColor(red: 0x1B / 255, green: 0x1F / 255, blue: 0x1C / 255, alpha: 1)
}

extension Color {
  static let knoopInk = Color(UIColor.knoopInk)
}

/// How a knot is drawn, on the map and in the sheet alike, see `map-planning` "Route knots stand
/// out": a waypoint as a filled plate in its sign region's colour, a passed knot white with a ring in
/// that colour, and a knot not on the route (`role` `nil`) with a grey ring.
struct PlateStyle {
  let fill: UIColor
  let ring: UIColor?
  let innerRing: Bool
  let text: UIColor
  let large: Bool

  init(_ role: KnotRole?, plate: KnotPlate, dark: Bool, filled: Bool? = nil) {
    let colour = plate.uiColor
    // In the navigation header, filled or white as on the knot's signs, see `knot-navigation`
    // "Number to follow".
    if let filled {
      fill = filled ? colour : .white
      ring = filled ? nil : colour
      innerRing = filled && plate.innerRing
      text = filled ? .white : colour
      large = false
      return
    }
    switch role {
    case nil:
      fill = dark ? UIColor(red: 0x2A / 255, green: 0x2C / 255, blue: 0x2A / 255, alpha: 1) : .white
      ring = dark
        ? UIColor(red: 0x8A / 255, green: 0x92 / 255, blue: 0x8C / 255, alpha: 1)
        : UIColor(red: 0x9A / 255, green: 0xA1 / 255, blue: 0x9C / 255, alpha: 1)
      innerRing = false
      text = dark
        ? UIColor(red: 0xC9 / 255, green: 0xCE / 255, blue: 0xC9 / 255, alpha: 1)
        : UIColor(red: 0x6B / 255, green: 0x73 / 255, blue: 0x6D / 255, alpha: 1)
      large = false
    case .passed?:
      fill = .white
      ring = colour
      innerRing = false
      text = colour
      large = false
    default:
      fill = colour
      ring = nil
      innerRing = plate.innerRing
      text = .white
      large = role != .stop
    }
  }

  /// The corner symbol's diameter, and how far its centre sits from the badge's, per badge diameter.
  static let cornerSize: CGFloat = 0.5
  static let cornerOffset: CGFloat = 0.38

  /// The SF Symbol of a waypoint's corner symbol, `nil` for other knots.
  static func cornerSymbol(_ role: KnotRole?) -> String? {
    switch role {
    case .start?: "play.fill"
    case .stop?: "flag.fill"
    case .end?: "stop.fill"
    case .roundTrip?: "arrow.clockwise"
    default: nil
    }
  }
}

enum KnotBadges {

  /// The map image name of a badge; the map's symbol layers build the same names from GeoJSON.
  /// Knots not on the route are grey everywhere, so only route knots carry their plate.
  static func name(_ role: KnotRole?, _ ref: String, _ plate: KnotPlate = .none, place: String? = nil) -> String {
    role.map { "route-\($0.name.lowercased())-\(plate.key)-\(ref)-\(place ?? "")" } ?? "knot-\(ref)"
  }

  /// Room around the plate for the corner symbol, on every side so the plate stays centred.
  private static let cornerRoom: CGFloat = 5

  /// A round badge with the knot's number, drawn locally so numbers show without network access,
  /// with a waypoint's corner symbol.
  /// `place` is the kind key of a route point made from a place, drawn instead of the flag, see
  /// `map-planning` "Route points from a place".
  static func image(
    _ role: KnotRole?, _ ref: String, _ plate: KnotPlate = .none, dark: Bool = false, place: String? = nil
  ) -> UIImage {
    let placeKind = PlaceKind.companion.ofKey(kindKey: place)
    let style = PlateStyle(role, plate: plate, dark: dark)
    let plateSide: CGFloat = style.large ? 28 : 22
    let side = plateSide + 2 * cornerRoom
    let size = CGSize(width: side, height: side)
    return UIGraphicsImageRenderer(size: size).image { context in
      let circle = CGRect(origin: .zero, size: size).insetBy(dx: cornerRoom + 1, dy: cornerRoom + 1)
      if let placeKind {
        UIColor.white.setFill()
        context.cgContext.fillEllipse(in: circle)
        PlaceIcons.fill(placeKind).setFill()
        context.cgContext.fillEllipse(in: circle.insetBy(dx: 1.5, dy: 1.5))
        let configuration = UIImage.SymbolConfiguration(pointSize: circle.width * 0.42, weight: .semibold)
        if let glyph = UIImage(systemName: PlaceIcons.symbol(placeKind), withConfiguration: configuration)?
          .withTintColor(.white, renderingMode: .alwaysOriginal)
        {
          glyph.draw(at: CGPoint(x: circle.midX - glyph.size.width / 2, y: circle.midY - glyph.size.height / 2))
        }
      } else {
      style.fill.setFill()
      context.cgContext.fillEllipse(in: circle)
      if let ring = style.ring {
        ring.setStroke()
        context.cgContext.setLineWidth(role == nil ? 1.5 : 2)
        context.cgContext.strokeEllipse(in: circle.insetBy(dx: 0.75, dy: 0.75))
      }
      if style.innerRing {
        UIColor.white.setStroke()
        context.cgContext.setLineWidth(circle.width * 0.055)
        context.cgContext.strokeEllipse(in: circle.insetBy(dx: circle.width * 0.1, dy: circle.width * 0.1))
      }
      let fontSize: CGFloat = (ref.count > 2 ? 7 : 9) * (style.large ? 1.25 : 1)
      let attributes: [NSAttributedString.Key: Any] = [
        .font: UIFont.boldSystemFont(ofSize: fontSize), .foregroundColor: style.text,
      ]
      if ref.isEmpty {
        // A route point has no number: a flag, as in the list.
        let configuration = UIImage.SymbolConfiguration(pointSize: fontSize * 1.1, weight: .bold)
        if let flag = UIImage(systemName: "flag.fill", withConfiguration: configuration)?
          .withTintColor(style.text, renderingMode: .alwaysOriginal)
        {
          flag.draw(at: CGPoint(x: (side - flag.size.width) / 2, y: (side - flag.size.height) / 2))
        }
      } else {
        let text = ref as NSString
        let textSize = text.size(withAttributes: attributes)
        text.draw(
          at: CGPoint(x: (side - textSize.width) / 2, y: (side - textSize.height) / 2),
          withAttributes: attributes)
      }
      }
      if let symbol = PlateStyle.cornerSymbol(role) {
        let radius = circle.width * PlateStyle.cornerSize / 2
        let center = CGPoint(
          x: circle.midX + circle.width * PlateStyle.cornerOffset,
          y: circle.midY - circle.width * PlateStyle.cornerOffset)
        let disc = CGRect(x: center.x - radius, y: center.y - radius, width: 2 * radius, height: 2 * radius)
        UIColor.knoopInk.setFill()
        context.cgContext.fillEllipse(in: disc)
        UIColor.white.setStroke()
        context.cgContext.setLineWidth(0.75)
        context.cgContext.strokeEllipse(in: disc)
        let configuration = UIImage.SymbolConfiguration(pointSize: radius * 1.15, weight: .black)
        if let glyph = UIImage(systemName: symbol, withConfiguration: configuration)?
          .withTintColor(.white, renderingMode: .alwaysOriginal)
        {
          glyph.draw(at: CGPoint(x: center.x - glyph.size.width / 2, y: center.y - glyph.size.height / 2))
        }
      }
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

extension KnotBadges {

  /// The rider's position with a riding direction: a navigation arrow pointing north, rotated by the
  /// map, 44 pt from its tip to its base, see `knot-navigation` "Riding direction on the map".
  static func riderArrow() -> UIImage {
    // The path below is 23 long, from its tip at 3 to its base at 26.
    let scale: CGFloat = 44 / 23
    return UIGraphicsImageRenderer(size: CGSize(width: 30 * scale, height: 30 * scale)).image { context in
      context.cgContext.scaleBy(x: scale, y: scale)
      let path = UIBezierPath()
      path.move(to: CGPoint(x: 15, y: 3))
      path.addLine(to: CGPoint(x: 25.5, y: 26))
      path.addLine(to: CGPoint(x: 15, y: 21))
      path.addLine(to: CGPoint(x: 4.5, y: 26))
      path.close()
      UIColor.systemBlue.setFill()
      path.fill()
      path.lineWidth = 2.5
      path.lineJoinStyle = .round
      UIColor.white.setStroke()
      path.stroke()
    }
  }
}

/// The same badge as on the map, for the sheet; `scale` enlarges it, e.g. to stand out.
struct KnotBadge: View {

  let ref: String
  let role: KnotRole?
  var plate: KnotPlate = .none
  var scale: CGFloat = 1
  var filled: Bool? = nil
  /// For a route point made from a place, its kind key, drawn instead of the flag, see
  /// `map-planning` "Route points from a place".
  var place: String? = nil
  @Environment(\.colorScheme) private var colorScheme

  var body: some View {
    let placeKind = PlaceKind.companion.ofKey(kindKey: place)
    let style = PlateStyle(role, plate: plate, dark: colorScheme == .dark, filled: filled)
    let side: CGFloat = (style.large ? 34 : 28) * scale
    Group {
      if let placeKind {
        Image(systemName: PlaceIcons.symbol(placeKind)).font(.system(size: side * 0.42, weight: .semibold))
          .foregroundStyle(.white)
          .frame(width: side, height: side)
          .background(Circle().fill(Color(PlaceIcons.fill(placeKind))))
          .overlay(Circle().strokeBorder(.white, lineWidth: 1.5 * scale))
          .accessibilityHidden(true)
      } else if ref.isEmpty {
        Image(systemName: "flag.fill").font(.system(size: 12 * (style.large ? 1.2 : 1) * scale, weight: .bold))
          .accessibilityHidden(true)
      } else {
        Text(ref).font(.system(size: (ref.count > 2 ? 10 : 12) * (style.large ? 1.2 : 1) * scale, weight: .bold))
      }
    }
    .foregroundStyle(Color(style.text))
    .frame(width: side, height: side)
    .background(Circle().fill(placeKind == nil ? Color(style.fill) : .clear))
    .overlay {
      if placeKind == nil, let ring = style.ring { Circle().strokeBorder(Color(ring), lineWidth: 2 * scale) }
    }
    .overlay {
      if placeKind == nil, style.innerRing {
        Circle().stroke(.white, lineWidth: side * 0.055).padding(side * 0.12)
      }
    }
    .overlay(alignment: .topTrailing) {
      if let symbol = PlateStyle.cornerSymbol(role) {
        Image(systemName: symbol)
          .font(.system(size: side * PlateStyle.cornerSize * 0.55, weight: .black))
          .foregroundStyle(.white)
          .frame(width: side * PlateStyle.cornerSize, height: side * PlateStyle.cornerSize)
          .background(Circle().fill(Color.knoopInk))
          .overlay(Circle().stroke(.white, lineWidth: 1))
          .offset(
            x: side * (PlateStyle.cornerOffset + PlateStyle.cornerSize / 2 - 0.5),
            y: -side * (PlateStyle.cornerOffset + PlateStyle.cornerSize / 2 - 0.5))
          .accessibilityHidden(true)
      }
    }
  }
}

