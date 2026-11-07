import SwiftUI

extension NavigationActivityAttributes.HeaderKnotState {
  var plateColor: Color {
    Color(
      red: Double((colour >> 16) & 0xFF) / 255, green: Double((colour >> 8) & 0xFF) / 255,
      blue: Double(colour & 0xFF) / 255)
  }
}

/// A knot of the header outside the app, filled or white as on its signs, with a picked knot's
/// corner symbol, see `knot-navigation` "Navigation with the screen off".
struct SignKnot: View {
  let knot: NavigationActivityAttributes.HeaderKnotState
  var side: CGFloat = 28

  var body: some View {
    let plate = knot.plateColor
    Group {
      if knot.ref.isEmpty {
        Image(systemName: "flag.fill").font(.system(size: side * 0.42, weight: .bold))
      } else {
        Text(knot.ref).font(.system(size: side * (knot.ref.count > 2 ? 0.36 : 0.44), weight: .bold))
          .minimumScaleFactor(0.6)
      }
    }
    .foregroundStyle(knot.filled ? Color.white : plate)
    .frame(width: side, height: side)
    .background(Circle().fill(knot.filled ? plate : Color.white))
    .overlay { if !knot.filled { Circle().strokeBorder(plate, lineWidth: side * 0.08) } }
    .overlay {
      if knot.filled && knot.innerRing {
        Circle().stroke(.white, lineWidth: side * 0.055).padding(side * 0.12)
      }
    }
    .overlay(alignment: .topTrailing) {
      if let symbol = Self.cornerSymbol(knot.role) {
        Image(systemName: symbol)
          .font(.system(size: side * 0.27, weight: .black))
          .foregroundStyle(.white)
          .frame(width: side * 0.5, height: side * 0.5)
          .background(Circle().fill(Color(red: 0x1B / 255, green: 0x1F / 255, blue: 0x1C / 255)))
          .overlay(Circle().stroke(.white, lineWidth: 1))
          .offset(x: side * 0.13, y: -side * 0.13)
      }
    }
    .accessibilityHidden(true)
  }

  private static func cornerSymbol(_ role: BadgeRole?) -> String? {
    switch role {
    case .start?: "play.fill"
    case .stop?: "flag.fill"
    case .end?: "stop.fill"
    case .roundTrip?: "arrow.clockwise"
    default: nil
    }
  }
}

/// The number to follow on its sign plate, the arrow right of it, like the knot's arrow signs.
struct SignPlateView: View {
  let knot: NavigationActivityAttributes.HeaderKnotState
  var side: CGFloat = 40

  var body: some View {
    let plate = knot.plateColor
    let dutch = !knot.filled
    HStack(spacing: side * 0.04) {
      SignKnot(knot: knot, side: side)
      Image(systemName: "arrow.right")
        .font(.system(size: side * 0.42, weight: .bold))
        .foregroundStyle(dutch ? plate : .white)
    }
    .padding(.leading, side * 0.18)
    .padding(.trailing, side * 0.1)
    .padding(.vertical, side * 0.14)
    .background(RoundedRectangle(cornerRadius: side * 0.2).fill(dutch ? Color.white : plate))
    .overlay {
      if dutch { RoundedRectangle(cornerRadius: side * 0.2).strokeBorder(plate, lineWidth: side * 0.07) }
    }
    .accessibilityHidden(true)
  }
}

/// The to-the-route symbol: a small rider arrow joined by a dashed line to the orange route line;
/// the arrow points away from the route off route, towards it on the way to it.
struct ToRouteGlyph: View {
  let awayFromRoute: Bool
  let dashes: Color
  var side: CGFloat = 32

  var body: some View {
    Canvas { context, size in
      let unit = size.width / 24
      var route = Path()
      route.move(to: CGPoint(x: 9 * unit, y: 2.5 * unit))
      route.addCurve(
        to: CGPoint(x: 21.5 * unit, y: 13 * unit), control1: CGPoint(x: 12 * unit, y: 7 * unit),
        control2: CGPoint(x: 15 * unit, y: 11 * unit))
      context.stroke(route, with: .color(.knoopOrange), style: StrokeStyle(lineWidth: 3.2 * unit, lineCap: .round))
      var line = Path()
      line.move(to: CGPoint(x: 6 * unit, y: 18.5 * unit))
      line.addLine(to: CGPoint(x: 13 * unit, y: 9.5 * unit))
      context.stroke(line, with: .color(dashes), style: StrokeStyle(lineWidth: 2 * unit, lineCap: .round, dash: [2 * unit, 2.6 * unit]))
      var arrow = Path()
      arrow.move(to: CGPoint(x: 0, y: -4.6 * unit))
      arrow.addLine(to: CGPoint(x: 3.6 * unit, y: 4 * unit))
      arrow.addLine(to: CGPoint(x: 0, y: 2 * unit))
      arrow.addLine(to: CGPoint(x: -3.6 * unit, y: 4 * unit))
      arrow.closeSubpath()
      var placed = context
      placed.translateBy(x: 5.5 * unit, y: 19 * unit)
      placed.rotate(by: .degrees(awayFromRoute ? 218 : 38))
      placed.fill(arrow, with: .color(Color(red: 0x1A / 255, green: 0x73 / 255, blue: 0xE8 / 255)))
      placed.stroke(arrow, with: .color(.white), style: StrokeStyle(lineWidth: 1.2 * unit, lineJoin: .round))
    }
    .frame(width: side, height: side)
    .accessibilityHidden(true)
  }
}
