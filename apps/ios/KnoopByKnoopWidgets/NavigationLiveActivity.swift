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
          if let progress = context.state.progress {
            Text(progress).font(.caption).foregroundStyle(.white.opacity(0.8))
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
        Text(context.state.follow ?? context.state.next)
          .bold()
          .foregroundStyle(Color.knoopGreen)
      }
    }
  }
}

/// The card on the lock screen, light or dark like the system, see `map-planning` "Lock screen in
/// dark mode". Its own background, as the text on the system's card was hard to read.
private struct LockScreenCard: View {

  let state: NavigationActivityAttributes.ContentState
  @Environment(\.colorScheme) private var colorScheme

  var body: some View {
    let dark = colorScheme == .dark
    HStack(alignment: .center, spacing: 16) {
      VStack(alignment: .leading, spacing: 6) {
        NextKnot(state: state, prominent: 1.8, textColor: dark ? .white : .knoopDark)
        if let progress = state.progress {
          Text(progress).font(.caption).foregroundStyle(dark ? Color.white.opacity(0.8) : Color.secondary)
        }
      }
      Spacer()
      StopButton()
    }
    .padding()
    .activityBackgroundTint(dark ? Color(white: 0.11).opacity(0.92) : Color.white.opacity(0.92))
    .activitySystemActionForegroundColor(.knoopGreen)
  }
}

/// "At 45 follow 12" with the knots as badges like on the map, 12 the largest; off route, at the
/// end of the route and while starting, the text with the next knot's badge.
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

private struct StopButton: View {
  var body: some View {
    Button(intent: StopNavigationIntent()) { Text("Stop") }
      .tint(.knoopGreen)
  }
}
