# Design

## Context

Per fix, both apps do the same steps (`NavigationService.onLocation`/`apply`, `Navigator.onLocation`/`apply`):

1. Ignore the fix if stopping (after arriving); on iOS also if its accuracy is negative.
2. `Navigation.update`, or `Navigation.start` for the first fix; if start gives `null`, stop.
3. `WayGuide.onFix(navigation, notices, fix, network, heading, nowMillis, speechLanguage)` → notices to speak and maybe a `WaySearch`.
4. Swap the `RiddenTrack`'s network to the route's, then add the fix.
5. Publish `NavigationState(navigation, fix, track, direction, guidedWay)`.
6. If arrived: stop location updates, stop once speech is silent; else update the notification or Live Activity and request `update.precision`.

A way search runs off the main thread: if the store's version changed, forget the router's squares; `router.way(...)`; back on the main thread, `WayGuide.found(way, navigation unless stopping)`, and publish again if still navigating, off route and not arrived. The same version check precedes the planner's `RoutePointWays` on both apps.

Clock: Android passes `SystemClock.elapsedRealtime()`, iOS wall-clock milliseconds, as `nowMillis`. `WayGuide` only compares them with each other, so both work.

## Goals / Non-Goals

**Goals:**
- Everything above except the platform calls decided in one place, testable with plain fixes.
- The apps shrink to adapters: platform events in, platform actions out.

**Non-Goals:**
- Changing what is shown or spoken.
- Moving threads into the core: the apps keep choosing where the way search runs.

## Decisions

### `NavigationSession`: fixes in, actions out

```kotlin
class NavigationSession(route: NavigationRoute, localTime: (Long) -> LocalMoment,
                        language: Language, speechLanguage: Language) {
  val state: NavigationState?            // null until the first fix
  fun onFix(fix: Fix, visible: Boolean, nowMillis: Long): SessionStep
  fun onWay(way: PathWay?): SessionStep  // a search's result
  fun inLanguage(language: Language, speechLanguage: Language): SessionStep
}

class SessionStep(
  val state: NavigationState?,   // to publish, null if nothing changed
  val notices: List<Notice>,     // to speak
  val search: WaySearch?,        // to run, then hand to onWay
  val precision: Precision?,     // to request, null to keep
  val end: SessionEnd,           // NONE, AFTER_SPEECH, NOW
)
```

`NavigationRoute` (knots, graph, geometry, stops) moves into the core; the apps' extra fields (badge roles on iOS) stay in a wrapper of their own.

The session holds the stopping state: after an `AFTER_SPEECH` step, every later fix and way gives an empty step. `onWay` returns a state to publish only when the way still applies (navigating, off route, not arrived), which is the check both apps do today.

*Alternative:* a callback interface the apps implement (`speak`, `search`, `publish`). Rejected: return values are easier to test and to call from Swift than Kotlin interfaces implemented in Swift.

*Why language through the session:* Android's `onConfigurationChanged` calls `inLanguage` on the navigation and republishes. iOS has no such call: a language chosen in the info sheet does not reach a running navigation there, though `app-languages` "Language setting" says it keeps "a running navigation as it is" and the notices follow the language. The session offers `inLanguage` for both; wiring it on iOS is a behavior change and left to its own change (see the proposal).

### `NavigationState` moves to the core

Identical on both apps today (navigation, position, track, direction, guided way). The session builds it, so the apps stop computing `track.directionAt(fix)` themselves.

### `WayFinder`: the router and its version check

```kotlin
class WayFinder(private val store: PathStore) {
  fun way(search: WaySearch): PathWay?          // forgets squares first if the store changed
  fun routePointWays(geometry: NetworkGeometry): RoutePointWays
}
```

Not thread-safe, like `PathRouter`: each app keeps one per queue it uses (navigation and planner), as today. It also carries the router's counters for the apps' log line.

### Bar marks and turn angles

`ProgressStrip.barMarks(limit: Int): List<BarMark>` with `BarMark(fraction, kind: CLOSED_FERRY | STOP | FERRY, stop's plate)` in the ranking both apps use; Android maps kinds to colors, iOS to its activity state. `TurnDirection.degrees` replaces the three Swift and Kotlin `turnDegrees` copies.

## Risks / Trade-offs

- [Navigation breaks on a ride after a refactor that tests didn't catch] → Replay tests in the core for the scenarios listed in the tasks, the emulator check with a mocked ride, and the maintainer's ride on both phones before archiving.
- [Swift interop of the new types is awkward] → Plain classes and enums only, no sealed hierarchies in the step; checked by building the iOS app after each task.
- [The step allocates per fix] → One small object per second at most; negligible next to `Navigation.update`.

## Migration Plan

One commit per task, each leaving both apps building and the tests green. Android first (the emulator can replay a ride), then iOS.
