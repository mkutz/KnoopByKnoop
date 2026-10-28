import SwiftUI

extension Color {
  /// The green and orange of the app icon, see `design/icon`.
  static let knoopGreen = Color(red: 0x2E / 255, green: 0x9E / 255, blue: 0x5B / 255)
  static let knoopOrange = Color(red: 0xF2 / 255, green: 0x8C / 255, blue: 0x28 / 255)
  static let knoopDark = Color(red: 0x26 / 255, green: 0x32 / 255, blue: 0x38 / 255)
}

/// How a knot takes part in the route, as far as its badge shows it. Mirrors the core's `KnotRole`,
/// which the widget extension does not link.
enum BadgeRole: String, Codable, Hashable {
  case start, stop, end, passed, roundTrip
}

/// How a knot is drawn, on the map, in the sheet and in the Live Activity alike, see the design of
/// `improve-planner-map`. `nil` is a knot of the network that is not on the route.
struct BadgeStyle {
  let fill: Color
  let ring: Color
  let text: Color
  let large: Bool
  let thickRing: Bool

  init(_ role: BadgeRole?) {
    switch role {
    case .passed?: self = .init(fill: .white, ring: .knoopOrange, text: .knoopOrange, large: false, thickRing: true)
    case .stop?: self = .init(fill: .knoopOrange, ring: .white, text: .white, large: false, thickRing: false)
    case .start?: self = .init(fill: .knoopGreen, ring: .white, text: .white, large: true, thickRing: false)
    case .end?: self = .init(fill: .knoopDark, ring: .white, text: .white, large: true, thickRing: false)
    // Drawn as two halves, start green and end dark.
    case .roundTrip?: self = .init(fill: .knoopGreen, ring: .white, text: .white, large: true, thickRing: false)
    case nil: self = .init(fill: .white, ring: .knoopGreen, text: .knoopGreen, large: false, thickRing: false)
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

/// A round badge with the knot's number, as on the map; `scale` enlarges it, e.g. for the number
/// to follow in the Live Activity.
struct Badge: View {

  let ref: String
  let role: BadgeRole?
  var scale: CGFloat = 1

  var body: some View {
    let style = BadgeStyle(role)
    let side: CGFloat = (style.large ? 34 : 28) * scale
    Text(ref)
      .font(.system(size: (ref.count > 2 ? 10 : 12) * (style.large ? 1.2 : 1) * scale, weight: .bold))
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
      .overlay(Circle().stroke(style.ring, lineWidth: (style.thickRing ? 3 : 2) * scale))
  }
}
