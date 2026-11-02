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
        DynamicIslandExpandedRegion(.leading) {
          NextKnot(state: context.state, prominent: 1.5, textColor: .white).padding(.leading, 4)
        }
        DynamicIslandExpandedRegion(.trailing) {
          StopButton()
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
        if context.state.offRoute {
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
        if context.state.offRoute {
          Text(context.state.next).bold()
        } else if let follow = context.state.follow {
          Text(follow).bold().foregroundStyle(Color.knoopGreen)
        } else {
          Image(systemName: "flag.checkered")
        }
      } minimal: {
        // The one number left when the island is shared: the knot the app shows largest.
        Text(context.state.close && !context.state.offRoute ? context.state.follow ?? context.state.next : context.state.next)
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
    HStack(alignment: .center, spacing: 16) {
      VStack(alignment: .leading, spacing: 6) {
        NextKnot(state: state, prominent: 1.8, textColor: dark ? .white : .knoopDark)
        if let progress = state.progress {
          Text(progress).font(.caption).foregroundStyle(Color(white: dark ? 0.8 : 0.35))
        }
        RouteBar(state: state, track: Color(white: dark ? 0.35 : 0.8))
      }
      Spacer()
      StopButton()
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
          Circle().fill(state.offRoute ? Color.red : Color.blue)
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

  var body: some View {
    content.foregroundStyle(textColor)
  }

  @ViewBuilder private var content: some View {
    if state.next.isEmpty {
      Text(state.text).font(.title3.bold())
    } else if state.offRoute {
      HStack(spacing: 8) {
        Text("Off route, next").font(.title3.bold()).foregroundStyle(.red)
        Badge(ref: state.next, role: state.nextRole, scale: 1.3)
      }
    } else if let follow = state.follow, !state.close {
      VStack(alignment: .leading, spacing: 4) {
        HStack(spacing: 8) {
          Text("Next").font(.title3)
          Badge(ref: state.next, role: state.nextRole, scale: prominent)
        }
        HStack(spacing: 6) {
          Text("then").font(.subheadline)
          Badge(ref: follow, role: state.followRole, scale: 1)
        }
      }
    } else if let follow = state.follow {
      HStack(spacing: 8) {
        Text("At").font(.title3)
        Badge(ref: state.next, role: state.nextRole, scale: 1.3)
        Text("follow").font(.title3)
        Badge(ref: follow, role: state.followRole, scale: prominent)
      }
    } else {
      HStack(spacing: 8) {
        Badge(ref: state.next, role: state.nextRole, scale: prominent)
        Text("is the end").font(.title3)
      }
    }
  }
}

/// Red with white text as the app's Stop, opaque, so it reads the same on every background.
private struct StopButton: View {
  var body: some View {
    Button(intent: StopNavigationIntent()) {
      Label("Stop", systemImage: "stop.fill")
        .font(.subheadline.bold())
        .foregroundStyle(.white)
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color.actionRed, in: Capsule())
    }
    .buttonStyle(.plain)
  }
}
