import ActivityKit
import AppIntents
import SwiftUI
import WidgetKit

@main
struct KnoopByKnoopWidgets: WidgetBundle {
  var body: some Widget { NavigationLiveActivity() }
}

/// What to look for on the signs on the lock screen and in the Dynamic Island, without words, with
/// Stop, see `knot-navigation` "Navigation with the screen off".
struct NavigationLiveActivity: Widget {

  var body: some WidgetConfiguration {
    ActivityConfiguration(for: NavigationActivityAttributes.self) { context in
      LockScreenCard(state: context.state)
    } dynamicIsland: { context in
      DynamicIsland {
        DynamicIslandExpandedRegion(.leading, priority: 1) {
          RowView(state: context.state, color: .white, scale: 0.85)
            .padding(.leading, 4)
            .dynamicIsland(verticalPlacement: .belowIfTooWide)
        }
        DynamicIslandExpandedRegion(.trailing) {
          StopButton(words: context.state.words)
        }
        DynamicIslandExpandedRegion(.bottom) {
          // In from the sides, where the island's rounded corners cut off the bar's ends.
          BarWithArrival(state: context.state, track: Color(white: 0.35), color: .white)
            .padding(.horizontal, 8)
        }
      } compactLeading: {
        CompactLeading(state: context.state)
      } compactTrailing: {
        if let row = context.state.row {
          // Smaller rather than cut off, e.g. "2.3 km" next to another app's activity.
          Text(row.remaining ?? row.distance).font(.subheadline.bold()).lineLimit(1)
            .minimumScaleFactor(0.6)
        }
      } minimal: {
        Minimal(state: context.state)
      }
    }
  }
}

/// The card on the lock screen, light or dark like the system, opposite while off route or riding
/// the wrong way, see `knot-navigation` "Navigation with the screen off". It draws its own
/// background and only opaque colors: on the lock screen, `colorScheme` is dark also when the system
/// is light, and a per-state `activityBackgroundTint` is reported to update unreliably.
private struct LockScreenCard: View {

  let state: NavigationActivityAttributes.ContentState

  var body: some View {
    let dark = state.dark != (state.row?.inverted ?? false)
    let color: Color = dark ? .white : .knoopDark
    // Stop only beside the knots, so the bar has the card's whole width.
    VStack(alignment: .leading, spacing: 8) {
      HStack(alignment: .center, spacing: 12) {
        RowView(state: state, color: color, scale: 1)
        Spacer(minLength: 0)
        StopButton(words: state.words)
      }
      BarWithArrival(state: state, track: Color(white: dark ? 0.35 : 0.8), color: color)
    }
    .padding()
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(dark ? Color(white: 0.11) : Color.white)
    .activityBackgroundTint(state.dark ? Color(white: 0.11) : Color.white)
    .activitySystemActionForegroundColor(.knoopGreen)
  }
}

/// The header's row: badges, arrows and distances, as in the app.
private struct RowView: View {

  let state: NavigationActivityAttributes.ContentState
  let color: Color
  let scale: CGFloat

  var body: some View {
    Group {
      if let row = state.row {
        HStack(alignment: .center, spacing: 8 * scale) { content(row) }
      } else {
        Text(state.text).font(.title3.bold())
      }
    }
    .foregroundStyle(color)
    .lineLimit(1)
    .minimumScaleFactor(0.7)
  }

  @ViewBuilder private func content(_ row: NavigationActivityAttributes.HeaderRowState) -> some View {
    switch row.kind {
    case .close:
      if let next = row.next { SignKnot(knot: next, side: 30 * scale) }
      Text(row.distance).font(.callout.weight(.semibold))
      Image(systemName: "arrow.right").font(.headline.bold())
      if let follow = row.follow { SignPlateView(knot: follow, side: 40 * scale, turnDegrees: row.turnDegrees) }
    case .far:
      if let next = row.next { SignKnot(knot: next, side: 44 * scale) }
      Text(row.distance).font(.title2.bold())
      if let follow = row.follow {
        Image(systemName: "chevron.right").font(.caption.bold()).opacity(0.7)
        SignKnot(knot: follow, side: 26 * scale)
      }
    case .end:
      if let next = row.next { SignKnot(knot: next, side: 44 * scale) }
      Text(row.distance).font(.title2.bold())
    case .wrongWay:
      Image(systemName: "arrow.uturn.down").font(.title2.bold())
      if let next = row.next { SignKnot(knot: next, side: 36 * scale) }
      Text(row.distance).font(.title2.bold())
    case .toRoute:
      ToRouteGlyph(awayFromRoute: row.offRoute, dashes: color, side: 32 * scale)
      if let turn = row.turnDegrees {
        Image(systemName: "location.north.fill").font(.title3).rotationEffect(.degrees(turn))
        if let distance = row.turnDistance { Text(distance).font(.callout) }
        Text(row.remaining ?? row.distance).font(.title2.bold())
      } else {
        Image(systemName: "location.north.fill").font(.title2).rotationEffect(.degrees(row.arrowDegrees))
        Text(row.distance).font(.title2.bold())
      }
      if let next = row.next {
        Image(systemName: "chevron.right").font(.caption.bold()).opacity(0.7)
        SignKnot(knot: next, side: 26 * scale)
      }
    }
  }
}

/// The bar along the route with the arrival at the end beside it.
private struct BarWithArrival: View {
  let state: NavigationActivityAttributes.ContentState
  let track: Color
  let color: Color

  var body: some View {
    HStack(spacing: 8) {
      RouteBar(state: state, track: track)
      if let arrival = state.row?.endArrival {
        Text(arrival).font(.subheadline.bold()).foregroundStyle(color).fixedSize()
      }
    }
  }
}

/// The Dynamic Island's leading side: the row in miniature, see `knot-navigation` "Navigation with
/// the screen off".
/// The one element of the island on the leading side, as the island has room for little, see
/// `knot-navigation` "Navigation with the screen off".
private struct CompactLeading: View {
  let state: NavigationActivityAttributes.ContentState

  var body: some View {
    Minimal(state: state)
  }
}

/// The island's one element: the plate close to a knot, the next knot far from it, and the arrow off
/// route, see "Dynamic Island with room for one number".
private struct Minimal: View {
  let state: NavigationActivityAttributes.ContentState

  var body: some View {
    if let row = state.row {
      switch row.kind {
      case .close:
        if let follow = row.follow {
          SignKnot(knot: follow, side: 18)
            .padding(2)
            .background(
              RoundedRectangle(cornerRadius: 4).fill(follow.filled ? follow.plateColor : .white))
        }
      case .far, .end:
        if let next = row.next { SignKnot(knot: next, side: 24) }
      case .wrongWay, .toRoute:
        WarningTile(row: row, side: 24)
      }
    } else {
      NumberOrFlag(ref: state.next).bold()
    }
  }
}

/// Off route and riding the wrong way, the arrow or the U-turn on a light tile, as the island is
/// black; on the way to the route the plain arrow.
private struct WarningTile: View {
  let row: NavigationActivityAttributes.HeaderRowState
  let side: CGFloat

  var body: some View {
    let symbol =
      Image(systemName: row.kind == .wrongWay ? "arrow.uturn.down" : "location.north.fill")
      .font(.system(size: side * 0.55, weight: .bold))
      .rotationEffect(.degrees(row.kind == .wrongWay ? 0 : row.turnDegrees ?? row.arrowDegrees))
    if row.inverted {
      symbol
        .foregroundStyle(Color.knoopDark)
        .frame(width: side, height: side)
        .background(RoundedRectangle(cornerRadius: side * 0.3).fill(Color(white: 0.95)))
    } else {
      symbol.foregroundStyle(.white).frame(width: side, height: side)
    }
  }
}

/// The route as a bar, the part ridden green, with the rider, the stops and ferries ahead, see
/// `knot-navigation` "Navigation with the screen off". Nothing until the rider's place is known.
private struct RouteBar: View {

  let state: NavigationActivityAttributes.ContentState
  /// The part of the route still ahead.
  let track: Color

  var body: some View {
    if let rider = state.riderFraction {
      GeometryReader { geometry in
        let width = geometry.size.width
        ZStack(alignment: .leading) {
          Capsule().fill(track).frame(height: 4)
          Capsule().fill(Color.knoopGreen).frame(width: max(4, width * rider), height: 4)
          ForEach(state.marks, id: \.self) { mark in
            Circle().fill(color(of: mark)).frame(width: 8, height: 8)
              .offset(x: width * mark.fraction - 4)
          }
          Circle().fill(Color.knoopDark).overlay(Circle().strokeBorder(.white, lineWidth: 1))
            .frame(width: 10, height: 10)
            .offset(x: width - 10)
          // Off route a ring with ✕, not red, see `knot-navigation` "Progress strip".
          Group {
            if state.offRoute || state.wrongWay {
              Circle().fill(.white)
                .overlay(Circle().strokeBorder(Color.knoopDark, lineWidth: 2))
                .overlay(Image(systemName: "xmark").font(.system(size: 6, weight: .black)).foregroundStyle(Color.knoopDark))
            } else {
              Circle().fill(Color.blue).overlay(Circle().strokeBorder(.white, lineWidth: 2))
            }
          }
          .frame(width: 12, height: 12)
            .offset(x: max(0, min(width - 12, width * rider - 6)))
        }
        .frame(maxHeight: .infinity)
      }
      .frame(height: 12)
      .accessibilityHidden(true)
    }
  }

  private func color(of mark: NavigationActivityAttributes.BarMark) -> Color {
    switch mark.kind {
    case .stop: .knoopOrange
    case .ferry: .blue
    // Dark, not red, so it never reads like a German knot.
    case .closedFerry: .knoopDark
    }
  }
}

extension NavigationActivityAttributes.ContentState {

  /// The words in the app's language, see `app-languages` "Language setting". The extension runs
  /// apart from the app and keeps the language it started with, which can be an old one.
  fileprivate var words: Bundle {
    language.flatMap { Bundle.main.path(forResource: $0, ofType: "lproj") }.flatMap(Bundle.init(path:)) ?? .main
  }
}

/// Red with white text as the app's Stop, opaque, so it reads the same on every background.
private struct StopButton: View {

  let words: Bundle

  var body: some View {
    // An icon of the same size in every language, read as "Stop" by VoiceOver.
    Button(intent: StopNavigationIntent()) {
      Image(systemName: "stop.fill")
        .font(.system(size: 16, weight: .bold))
        .foregroundStyle(.white)
        .frame(width: 40, height: 40)
        .background(Color.actionRed, in: Circle())
    }
    .buttonStyle(.plain)
    .accessibilityLabel(Text(String(localized: "Stop", bundle: words)))
  }
}

/// A knot's number, or a flag for a route point, which has none, see `knot-navigation` "Navigating
/// with route points".
struct NumberOrFlag: View {
  let ref: String

  var body: some View {
    if ref.isEmpty { Image(systemName: "flag.fill") } else { Text(ref) }
  }
}
