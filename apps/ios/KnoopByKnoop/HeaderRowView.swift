import KnoopByKnoopCore
import SwiftUI

/// The header's row without words: badges, arrows and distances, see `knot-navigation` "Number to
/// follow", "Direction to the route" and "Header on the way to the route". VoiceOver reads
/// `row.spoken`.
struct HeaderRowView: View {

  let row: HeaderRow

  var body: some View {
    Group {
      if let toRoute = row as? HeaderRow.ToRoute, let guided = toRoute.guided {
        // On a guided way always one row, so the header keeps its height as the way is found or
        // lost, see `knot-navigation` "Header of steady height on a guided way".
        GuidedRow(toRoute: toRoute, guided: guided)
      } else {
        // The distance moves below the badges where it does not fit next to them, see
        // `knot-navigation` "Number to follow at large text sizes".
        // First the badges and arrows in a row with the distance below it, as the spec puts it; only
        // where even they do not fit, one below the other.
        ViewThatFits(in: .horizontal) {
          HStack(alignment: .center, spacing: 8) { content }
          VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .center, spacing: 8) { symbols }
            distance
          }
          VStack(alignment: .leading, spacing: 4) { content }
        }
      }
    }
    .accessibilityElement(children: .ignore)
    .accessibilityLabel(row.spoken)
  }

  /// The row's badges, arrows and symbols, without its distance.
  @ViewBuilder private var symbols: some View {
    if let close = row as? HeaderRow.Close {
      HeaderKnot(badge: close.next, scale: 1.4)
      PlainArrow(size: 26)
      SignPlate(badge: close.follow, turn: close.turn)
    } else if let far = row as? HeaderRow.Far {
      HeaderKnot(badge: far.next, scale: 2)
      HStack(spacing: 2) {
        Image(systemName: "chevron.right").font(.caption.bold()).foregroundStyle(.secondary)
        HeaderKnot(badge: far.follow, scale: 1.2)
      }
    } else if let end = row as? HeaderRow.End {
      HeaderKnot(badge: end.next, scale: 2)
    } else if let wrongWay = row as? HeaderRow.WrongWay {
      Image(systemName: "arrow.uturn.down").font(.title.bold())
      HeaderKnot(badge: wrongWay.next, scale: 1.6)
    } else if let toRoute = row as? HeaderRow.ToRoute {
      ToRouteSymbol(awayFromRoute: toRoute.offRoute, side: 38, tile: !toRoute.offRoute)
      Image(systemName: "location.north.fill")
        .font(.title)
        .rotationEffect(.degrees(toRoute.arrowDegrees))
      if let next = toRoute.next {
        HStack(spacing: 2) {
          Image(systemName: "chevron.right").font(.caption.bold()).foregroundStyle(.secondary)
          HeaderKnot(badge: next, scale: 1.2)
        }
      }
    }
  }

  /// The row's distance, as it is shown in the row.
  @ViewBuilder private var distance: some View {
    if let close = row as? HeaderRow.Close {
      Text(close.distance).font(.title3.weight(.semibold)).lineLimit(1)
    } else if let far = row as? HeaderRow.Far {
      Text(far.distance).font(.title.weight(.bold)).lineLimit(1)
    } else if let end = row as? HeaderRow.End {
      Text(end.distance).font(.title.weight(.bold)).lineLimit(1)
    } else if let wrongWay = row as? HeaderRow.WrongWay {
      Text(wrongWay.distance).font(.title.weight(.bold)).lineLimit(1)
    } else if let toRoute = row as? HeaderRow.ToRoute {
      Text(toRoute.distance).font(.title.weight(.bold)).lineLimit(1)
    }
  }

  @ViewBuilder private var content: some View {
    if let close = row as? HeaderRow.Close {
      HeaderKnot(badge: close.next, scale: 1.4)
      Text(close.distance).font(.title3.weight(.semibold)).lineLimit(1)
      PlainArrow(size: 26)
      SignPlate(badge: close.follow, turn: close.turn)
    } else if let far = row as? HeaderRow.Far {
      HeaderKnot(badge: far.next, scale: 2)
      Text(far.distance).font(.title.weight(.bold)).lineLimit(1)
      HStack(spacing: 2) {
        Image(systemName: "chevron.right").font(.caption.bold()).foregroundStyle(.secondary)
        HeaderKnot(badge: far.follow, scale: 1.2)
      }
    } else if let end = row as? HeaderRow.End {
      HeaderKnot(badge: end.next, scale: 2)
      Text(end.distance).font(.title.weight(.bold)).lineLimit(1)
    } else if let wrongWay = row as? HeaderRow.WrongWay {
      Image(systemName: "arrow.uturn.down").font(.title.bold())
      HeaderKnot(badge: wrongWay.next, scale: 1.6)
      Text(wrongWay.distance).font(.title.weight(.bold)).lineLimit(1)
    } else if let toRoute = row as? HeaderRow.ToRoute {
      ToRouteSymbol(awayFromRoute: toRoute.offRoute, side: 38, tile: !toRoute.offRoute)
      Image(systemName: "location.north.fill")
        .font(.title)
        .rotationEffect(.degrees(toRoute.arrowDegrees))
      Text(toRoute.distance).font(.title.weight(.bold)).lineLimit(1)
      if let next = toRoute.next {
        HStack(spacing: 2) {
          Image(systemName: "chevron.right").font(.caption.bold()).foregroundStyle(.secondary)
          HeaderKnot(badge: next, scale: 1.2)
        }
      }
    }
  }

  /// The row on a guided way: the tile, the turn with its distance and street, the distance left
  /// and the next knot, in one row whose street is cut short rather than wrapped.
  private struct GuidedRow: View {
    let toRoute: HeaderRow.ToRoute
    let guided: GuidedHeader
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
      HStack(alignment: .center, spacing: 8) {
        ToRouteSymbol(awayFromRoute: toRoute.offRoute, side: 38, tile: !toRoute.offRoute)
        Image(systemName: "location.north.fill")
          .font(.title2)
          .rotationEffect(.degrees(HeaderRowView.turnDegrees(guided.turn)))
        // The turn's distance always complete, at the largest text sizes too: the street is cut
        // short first, and at the accessibility sizes the distance left is left out, see
        // `knot-navigation` "Off route in German".
        VStack(alignment: .leading, spacing: 0) {
          Text(guided.turnDistance ?? guided.remaining).font(.title3.weight(.bold)).lineLimit(1)
            .fixedSize(horizontal: typeSize.isAccessibilitySize, vertical: false)
          if let street = guided.street {
            Text(street).font(.caption).foregroundStyle(.secondary).lineLimit(1).truncationMode(.tail)
          }
        }
        .layoutPriority(-1)
        if guided.turnDistance != nil && !typeSize.isAccessibilitySize {
          Text(guided.remaining).font(.title3).lineLimit(1).minimumScaleFactor(0.7)
        }
        if let next = toRoute.next {
          HStack(spacing: 2) {
            Image(systemName: "chevron.right").font(.caption.bold()).foregroundStyle(.secondary)
            HeaderKnot(badge: next, scale: 1.2)
          }
        }
      }
    }
  }

  /// The angle a turn on the guided way turns the arrow, from straight ahead.
  static func turnDegrees(_ turn: TurnDirection?) -> Double {
    switch turn {
    case .slightLeft?: -45
    case .left?: -90
    case .sharpLeft?: -135
    case .slightRight?: 45
    case .right?: 90
    case .sharpRight?: 135
    case .back?: 180
    default: 0
    }
  }
}

/// A knot of the header, filled or white as on its signs, with a picked knot's corner symbol.
private struct HeaderKnot: View {
  let badge: HeaderBadge
  let scale: CGFloat

  var body: some View {
    KnotBadge(ref: badge.ref, role: badge.role, plate: badge.plate, scale: scale, filled: badge.filled)
  }
}

/// The number to follow on a sign plate, with the arrow to the right of the number, like the knot's
/// arrow signs, see `knot-navigation` "Number to follow".
private struct SignPlate: View {
  let badge: HeaderBadge
  /// The turn the route makes at the knot, or a plain → where it cannot be told, see
  /// `knot-navigation` "Turn at the next knot".
  let turn: TurnDirection?

  var body: some View {
    let plate = Color(badge.plate.uiColor)
    let dutch = !badge.filled
    HStack(spacing: 2) {
      HeaderKnot(badge: badge, scale: 1.9)
      if let turn {
        Image(systemName: turn == .back ? "arrow.uturn.down" : "arrow.up")
          .font(.system(size: 18, weight: .bold))
          .foregroundStyle(dutch ? plate : .white)
          .rotationEffect(.degrees(turn == .back ? 0 : HeaderRowView.turnDegrees(turn)))
          .frame(width: 24, height: 24)
      } else {
        PlainArrow(size: 24, color: dutch ? plate : .white)
      }
    }
    .padding(.leading, 8)
    .padding(.trailing, 4)
    .padding(.vertical, 6)
    .background(RoundedRectangle(cornerRadius: 8).fill(dutch ? Color.white : plate))
    .overlay {
      if dutch { RoundedRectangle(cornerRadius: 8).strokeBorder(plate, lineWidth: 3) }
    }
  }
}

/// A plain → between the knot and the number to follow, until it shows the turn.
private struct PlainArrow: View {
  let size: CGFloat
  var color: Color? = nil

  var body: some View {
    Image(systemName: "arrow.right")
      .font(.system(size: size * 0.75, weight: .bold))
      .foregroundStyle(color.map { AnyShapeStyle($0) } ?? AnyShapeStyle(.primary))
      .frame(width: size, height: size)
  }
}

/// The to-the-route symbol: a small rider arrow joined by a dashed line to the orange route line, as
/// on the map; the arrow points away from the route off route, towards it on the way to it, see
/// `knot-navigation` "Direction to the route".
struct ToRouteSymbol: View {
  let awayFromRoute: Bool
  let side: CGFloat
  let tile: Bool
  @Environment(\.colorScheme) private var colorScheme

  var body: some View {
    Canvas { context, size in
      let unit = size.width / 24
      var route = Path()
      route.move(to: CGPoint(x: 9 * unit, y: 2.5 * unit))
      route.addCurve(
        to: CGPoint(x: 21.5 * unit, y: 13 * unit), control1: CGPoint(x: 12 * unit, y: 7 * unit),
        control2: CGPoint(x: 15 * unit, y: 11 * unit))
      context.stroke(route, with: .color(.knoopOrange), style: StrokeStyle(lineWidth: 3.2 * unit, lineCap: .round))
      var dashes = Path()
      dashes.move(to: CGPoint(x: 6 * unit, y: 18.5 * unit))
      dashes.addLine(to: CGPoint(x: 13 * unit, y: 9.5 * unit))
      context.stroke(
        dashes, with: .foreground,
        style: StrokeStyle(lineWidth: 2 * unit, lineCap: .round, dash: [2 * unit, 2.6 * unit]))
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
    .padding(tile ? 6 : 0)
    .background {
      if tile { RoundedRectangle(cornerRadius: 12).fill(Color(uiColor: .secondarySystemFill)) }
    }
    .accessibilityHidden(true)
  }
}
