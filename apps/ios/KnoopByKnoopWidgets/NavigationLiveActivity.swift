import ActivityKit
import AppIntents
import SwiftUI
import WidgetKit

@main
struct KnoopByKnoopWidgets: WidgetBundle {
  var body: some Widget { NavigationLiveActivity() }
}

/// The number to follow at the next knot on the lock screen and in the Dynamic Island, with Stop,
/// see `knot-navigation` "Navigation with the screen off".
struct NavigationLiveActivity: Widget {

  var body: some WidgetConfiguration {
    ActivityConfiguration(for: NavigationActivityAttributes.self) { context in
      LockScreenCard(state: context.state)
    } dynamicIsland: { context in
      DynamicIsland {
        // The first to get room, and below the camera if it needs more: by default this region is too
        // narrow for "At 16 follow 17", which squeezed the words to nothing.
        DynamicIslandExpandedRegion(.leading, priority: 1) {
          NextKnot(state: context.state, prominent: 1.5, textColor: .white, oneLine: true)
            .padding(.leading, 4)
            .dynamicIsland(verticalPlacement: .belowIfTooWide)
        }
        DynamicIslandExpandedRegion(.trailing) {
          StopButton(words: context.state.words)
        }
        DynamicIslandExpandedRegion(.bottom) {
          VStack(alignment: .leading, spacing: 6) {
            if let progress = context.state.progress {
              Text(progress).font(.caption).foregroundStyle(Color(white: 0.8))
            }
            // In from the sides, where the island's rounded corners cut off the bar's ends.
            RouteBar(state: context.state, track: Color(white: 0.35)).padding(.horizontal, 8)
          }
        }
      } compactLeading: {
        // Room for about three characters: "31 →" and "97" for at 31 follow 97, "97" and a flag at
        // the end, a warning sign and the next knot off route.
        if context.state.offRoute || context.state.wrongWay {
          Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(.red)
        } else if context.state.follow != nil {
          HStack(spacing: 2) {
            Text(context.state.next).bold()
            Image(systemName: "arrow.right").font(.caption2.bold())
          }
        } else {
          Text(context.state.next).bold()
        }
      } compactTrailing: {
        if context.state.offRoute || context.state.wrongWay {
          Text(context.state.next).bold()
        } else if let follow = context.state.follow {
          Text(follow).bold().foregroundStyle(Color.knoopGreen)
        } else {
          Image(systemName: "flag.checkered")
        }
      } minimal: {
        // The one number left when the island is shared: the knot the app shows largest.
        Text(context.state.close && !context.state.offRoute && !context.state.wrongWay ? context.state.follow ?? context.state.next : context.state.next)
          .bold()
          .foregroundStyle(Color.knoopGreen)
      }
    }
  }
}

/// The card on the lock screen, light or dark like the system, see `map-planning` "Lock screen in
/// dark mode". It draws its own background and only opaque colors, chosen together by the system's
/// mode as the app reports it: on the lock screen, `colorScheme` is dark also when the system is
/// light, and iOS showed a light card for `activityBackgroundTint` with content for dark.
private struct LockScreenCard: View {

  let state: NavigationActivityAttributes.ContentState

  var body: some View {
    let dark = state.dark
    // Stop only beside the knots, so the distance and the bar have the card's whole width.
    VStack(alignment: .leading, spacing: 6) {
      HStack(alignment: .center, spacing: 16) {
        NextKnot(state: state, prominent: 1.8, textColor: dark ? .white : .knoopDark)
        Spacer(minLength: 0)
        StopButton(words: state.words)
      }
      if let progress = state.progress {
        Text(progress).font(.caption).foregroundStyle(Color(white: dark ? 0.8 : 0.35))
      }
      RouteBar(state: state, track: Color(white: dark ? 0.35 : 0.8))
    }
    .padding()
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(dark ? Color(white: 0.11) : Color.white)
    .activityBackgroundTint(dark ? Color(white: 0.11) : Color.white)
    .activitySystemActionForegroundColor(.knoopGreen)
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
          Circle().fill(state.offRoute || state.wrongWay ? Color.red : Color.blue)
            .overlay(Circle().strokeBorder(.white, lineWidth: 2))
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
    case .closedFerry: .red
    }
  }
}

/// "At 45 follow 12" with the knots as badges like on the map, 12 the largest, and far from the
/// knot "Next 45" and "then 12", 45 the largest, see `knot-navigation` "Number to follow"; off
/// route, at the end of the route and while starting, the text with the next knot's badge.
private struct NextKnot: View {

  let state: NavigationActivityAttributes.ContentState
  /// How much larger than on the map the number to follow is.
  let prominent: CGFloat
  /// Dark on the light lock screen card, light on the dark one and in the black Dynamic Island.
  let textColor: Color
  /// For the Dynamic Island, where more height pushes the bar into the rounded corners: far from a
  /// knot, "Next" and "then" in one line, and off route no more lines than fit.
  var oneLine = false

  var body: some View {
    content.foregroundStyle(textColor)
  }

  @ViewBuilder private var content: some View {
    if state.next.isEmpty {
      Text(state.text).font(.title3.bold())
    } else if state.offRoute || state.wrongWay {
      HStack(spacing: 8) {
        Text(
          state.wrongWay
            ? String(localized: "Wrong direction, next", bundle: state.words)
            : String(localized: "Off route, next", bundle: state.words)
        )
        .font(.title3.bold())
        .foregroundStyle(.red)
        // On the lock screen, as many lines as it takes; in the Dynamic Island, more than two would
        // push the bar into its rounded corners, so the end is cut off there.
        .fixedSize(horizontal: false, vertical: !oneLine)
        Badge(ref: state.next, role: state.nextRole, scale: 1.3)
      }
    } else if let follow = state.follow, !state.close, oneLine {
      HStack(spacing: 8) {
        Text(String(localized: "Next", bundle: state.words)).font(.title3).fixedSize()
        Badge(ref: state.next, role: state.nextRole, scale: prominent)
        Text(String(localized: "then", bundle: state.words)).font(.subheadline).fixedSize()
        Badge(ref: follow, role: state.followRole, scale: 1)
      }
    } else if let follow = state.follow, !state.close {
      VStack(alignment: .leading, spacing: 4) {
        HStack(spacing: 8) {
          Text(String(localized: "Next", bundle: state.words)).font(.title3).fixedSize()
          Badge(ref: state.next, role: state.nextRole, scale: prominent)
        }
        HStack(spacing: 6) {
          Text(String(localized: "then", bundle: state.words)).font(.subheadline).fixedSize()
          Badge(ref: follow, role: state.followRole, scale: 1)
        }
      }
    } else if let follow = state.follow, oneLine {
      // The island puts the region below the camera if this is too wide.
      atFollowInOneLine(follow)
    } else if let follow = state.follow {
      // At large text sizes "Bei 94 folge 41" does not fit beside Stop, and squeezed into one line
      // the words broke apart; then "Bei 94" and "folge 41" in two lines.
      ViewThatFits(in: .horizontal) {
        atFollowInOneLine(follow)
        VStack(alignment: .leading, spacing: 4) {
          HStack(spacing: 8) {
            Text(String(localized: "At", bundle: state.words)).font(.title3).fixedSize()
            Badge(ref: state.next, role: state.nextRole, scale: 1.3)
          }
          HStack(spacing: 8) {
            Text(String(localized: "follow", bundle: state.words)).font(.title3).fixedSize()
            Badge(ref: follow, role: state.followRole, scale: prominent)
          }
        }
      }
    } else {
      HStack(spacing: 8) {
        Badge(ref: state.next, role: state.nextRole, scale: prominent)
        // Breaks between words, never inside one.
        Text(String(localized: "is the end", bundle: state.words)).font(.title3)
          .fixedSize(horizontal: false, vertical: true)
      }
    }
  }

  private func atFollowInOneLine(_ follow: String) -> some View {
    HStack(spacing: 8) {
      Text(String(localized: "At", bundle: state.words)).font(.title3).fixedSize()
      Badge(ref: state.next, role: state.nextRole, scale: 1.3)
      Text(String(localized: "follow", bundle: state.words)).font(.title3).fixedSize()
      Badge(ref: follow, role: state.followRole, scale: prominent)
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
    Button(intent: StopNavigationIntent()) {
      Label(String(localized: "Stop", bundle: words), systemImage: "stop.fill")
        .font(.subheadline.bold())
        .foregroundStyle(.white)
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color.actionRed, in: Capsule())
    }
    .buttonStyle(.plain)
  }
}
