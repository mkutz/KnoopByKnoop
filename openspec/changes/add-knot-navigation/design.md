# Design

## Context

See proposal.md for the motivation and specs/knot-navigation/spec.md for the behavior.

- **Core:** a complete route gives its knots in riding order (`RouteSummary.Complete.knots`), and `NetworkGeometry.courseOf` gives the course of each leg in riding direction. Without installed courses, the apps use `NetworkGeometry.straightLines`, which must not count as a course for off-route detection. `KnotLocator` finds the nearest knot fast. `PlanningSession` is immutable and owned by `AppViewModel` (Android) and `AppModel` (iOS).
- **Apps:** neither app uses the device's location yet. Android declares only `INTERNET` and `ACCESS_NETWORK_STATE`, with `minSdk` 26 and `targetSdk` 37. iOS targets iOS 17 and has a single app target in `project.yml`.
- **Constraint:** low battery use outweighs visual polish, and the logic stays in the platform-free core.

## Goals / Non-Goals

**Goals:**
- All decisions about progress, off route, notices and how precisely to sample are made in the core, as pure functions tested on the JVM. The apps only deliver fixes, speak, and show texts.
- Location access "while in use" is enough on both platforms; no "always" permission.

**Non-Goals:**
- Keeping navigation across a force-quit or a restart of the phone. The navigation state lives in memory.
- Map-matching or snapping the position to the course. Distances to knots and to the course are enough.

## Decisions

### The core: a navigation state machine
A new `Navigation` in the core is immutable, like `PlanningSession`. It is created from the route's knots, the course per leg (or none without geometry) and the first fix, and every further fix returns a `NavigationUpdate`: the new navigation, the notices to give, and the precision to sample with next.

```
  Fix(latitude, longitude, accuracyMeters, timeMillis)
        |
        v
  Navigation.update(fix) --> NavigationUpdate(
                               navigation,            next knot, off route, reminded, arrived
                               notices: List<Notice>, Passed, Reminder, OffRoute, BackOnRoute, Arrived
                               precision: Precision)  COARSE, FINE, LIVE
```

- **Passing** is entering the 30 m circle around a knot, checked on the line between the previous and the current fix rather than on the fixes alone. At 30 km/h and a fix every 5 s, consecutive fixes are about 40 m apart and can both lie outside the circle. Starting inside a circle does not count as entering it, so the start of a round trip is not taken for its end.
- **Skipping ahead** looks for the first occurrence after the next knot, so a route that passes a knot twice is followed in order.
- **Off route** by course needs a fix with an accuracy of 50 m or better that is more than 100 m from the course of the current leg and its neighbors. A knot not on the route counts as passed like a route knot, by entering its 30 m circle (found with `KnotLocator`). Off route and back on route are announced once each.
- **Start** follows the spec: at a route knot within 30 m, the knot after it is next; on the course within 100 m, the end of that leg is next (assuming the riding direction of the route); otherwise the nearest route knot is next and the rider is off route.
- **Notices** carry the text to show and the text to speak. The core spells numbers for speech ("08" → "8") so both apps speak them alike.

*Settled during apply:*
- **Coarse fixes** are fixes with an accuracy worse than 50 m. Passing is checked on the line from the last precise fix only if it is at most 30 s old; otherwise on the fix alone, so a long coarse stretch does not draw a line across knots.
- **Round trip:** the end of a round trip (the same knot as the start) only counts as passed once it is the next knot. Otherwise riding away from the start and back, or skipping ahead to it, would end the navigation right away ("Round trip").
- **Back on route after a knot that is not on the route** needs a precise fix within 100 m of the course and more than 100 m from that knot, so a junction next to the route does not flip between off and back on route.
- **Starting without courses** is never off route: without a course, the app cannot tell, in line with "Without geometry". The nearest route knot is next.
- **Texts:** the end of the route is shown as "31 is the end"; the reminder before it is "31 ahead, the end"; back on route is "Back on route, next 45". Starting gives the same notice as passing a knot ("Next 45, then 12"), or "Off route, next 45".

*Alternative:* keeping the logic in each app. Rejected: it would be written twice and could only be tested on devices.

### Precision instead of intervals
The platforms control location very differently: Android takes an interval and a priority, iOS a desired accuracy and a distance filter. The core therefore asks for one of three precisions, and each app maps them:

| Precision | When | Android (fused provider) | iOS (`CLLocationManager`) |
|---|---|---|---|
| `COARSE` | more than 400 m from the next knot, on route | high accuracy, every 30 s (balanced power at first, see below) | accuracy 100 m, distance filter 100 m |
| `FINE` | within 400 m of the next knot, off route, or a coarse fix far from the course to be confirmed | high accuracy, every 3 s | best accuracy, distance filter 10 m |
| `LIVE` | the app is visible | high accuracy, every 1 s | best for navigation, no filter |

`COARSE` mostly does without GPS, which is where the battery is saved. 400 m leaves enough room to have a precise fix before the 200 m reminder, even after a slow first GPS fix. Coarse fixes are never used to count a knot as passed or the rider as off route; a coarse fix that looks off route switches to `FINE` to confirm it. The thresholds are constants in the core and are tuned with the battery measurement.

*Found during apply:* on the Android emulator, `COARSE` with balanced power gave no fixes at all, as balanced power uses Wi-Fi and cell towers and the emulator has neither; navigation then never saw the next knot. The same can happen on a phone in the countryside. A timer that asks for `FINE` after a missing fix does not help: with the screen off and no fixes, the device sleeps and so does the timer. Android's `COARSE` therefore uses high accuracy every 30 s instead of balanced power; the battery use of that is measured once a physical Android phone is available. iOS keeps its mapping: with an accuracy of 100 m, Core Location uses GPS where needed.

*Alternative:* geofences from the operating system around the next knots. Rejected: their delay and precision are not controlled by the app, and in rural areas they can be minutes late.

### Android: a foreground service of type location
A `NavigationService` (foreground service, type `location`) runs while navigating. It is started from the visible app, so `ACCESS_FINE_LOCATION` "while in use" is enough; `ACCESS_BACKGROUND_LOCATION` is not requested. It holds the `Navigation`, gets fixes from the fused location provider (`play-services-location`), and shows an ongoing notification with "At 45 follow 12" and a Stop action. `AppViewModel` and the service share the navigation through a process-wide holder, so the planner shows the same state.

New permissions: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS`. If notifications are denied, navigation still runs and speaks, without the notification.

*Alternative:* the platform `LocationManager`, without Google Play services. Rejected for now: the fused provider combines Wi-Fi, cell and GPS for the coarse precision, which is the cheapest way to sample far from a knot.

### iOS: background location and a Live Activity
Navigation keeps running in the background with `allowsBackgroundLocationUpdates`, a `CLBackgroundActivitySession` (iOS 17) and the `location` background mode, started while the app is visible, so "when in use" is enough. Notices are spoken with `AVSpeechSynthesizer` in an audio session of category `.playback`, mode `.voicePrompt` and the option `.duckOthers`. The session is deactivated after each notice so other audio comes back. The spike showed that the `audio` background mode is needed as well: with `location` alone, the iPhone did not speak while locked (see `spike.md`). Location updates are not paused automatically (`pausesLocationUpdatesAutomatically = false`, activity type `.otherNavigation`), so a rider who waits at a ferry keeps getting notices.

The Live Activity lives in a new widget extension target in `project.yml`, with views for the lock screen and the Dynamic Island (compact, minimal and expanded). The app passes plain strings in the activity's content state, so the extension does not link the core. Stop is a `LiveActivityIntent` that runs in the app.

### Android speech
`TextToSpeech` with `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` and a transient audio focus that may duck, released after each notice. The language is English (`Locale.UK` falling back to `Locale.ENGLISH`).

### The planner while navigating
A Navigate button appears in the panel's header when the route is complete. While navigating, the header shows "At 45 follow 12" and Stop, the waypoint list cannot be edited, and taps on knots are ignored. The map draws the rider's position from the same fixes as a point on its own layer, rather than with MapLibre's location component, so there is only one location stream. The next knot is highlighted on the map.

### Reference devices for "Battery use while navigating"
The maintainer's iPhone, on a real ride of at least one hour with the screen off, reading the battery level at start and end. The Android emulator cannot measure battery: Android behavior is checked on the emulator with simulated routes, and its battery use is measured once a physical Android phone is available.

## Risks / Trade-offs

- **[Risk] "While in use" may not be enough for background location on one platform.** → The spike (task 1) checks it on both platforms before anything else is built; if it fails, the proposal is revised before going on.
- **[Risk] The Live Activity ends after about 8 hours.** → Spoken notices keep working. When the app is opened again while navigating, it starts a new Live Activity.
- **[Risk] Coarse fixes in rural areas can be off by several hundred meters.** → They are only used to choose the precision. If a coarse fix is worse than 400 m, the app samples with `FINE`.
- **[Risk] A missed knot between two fine fixes.** → Passing is checked on the line between fixes, not on the fixes themselves.
- **[Risk] The 5 %/h budget may not be met with the first thresholds.** → The thresholds are core constants; the measurement on the iPhone tunes them without changing the spec.
- **[Trade-off] No Android battery measurement yet.** → Accepted; the iPhone measurement validates the sampling approach, which is shared.
- **[Trade-off] Google Play services become a dependency of the Android app.** → Accepted for the battery gain.
- **Store review:** both stores ask about location in the background. Navigating a route the user planned and started is the standard justification. Google Play needs the foreground service declaration with a short video.

## Migration Plan

None: navigation is new, and the planner works as before when it is not started.
