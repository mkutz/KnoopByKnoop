# Design

## Context

- **Core:** `StartChoice.of(session, geometry, position)` returns null within `AT_START_METERS` (30 m) of the start, and otherwise when the course isn't closer. Tests are in `StartChoiceTest`.
- **iOS:** `AppModel.startNavigation` calls `positionFinder.find(timeout: .seconds(10))`. `PositionFinder` uses its own `CLLocationManager` with `kCLLocationAccuracyBest` and `requestLocation()`. "Show my position" also uses it, with 15 s. `NavigateIconAction` in `PlannerActions.swift` is the round Navigate.
- **Android:** `AppViewModel.startNavigation` wraps `currentLocation()`, which is `getCurrentLocation(PRIORITY_HIGH_ACCURACY)`, in `withTimeoutOrNull(START_POSITION_TIMEOUT_MILLIS)` (10 s). "Show my position" and "Nearest knots" use `currentLocation()` too. `NavigateFab` (portrait) and `NavigateButton` (landscape) are in `PlannerScreen.kt`.
- Nothing marks the wait on either platform.

## Goals / Non-Goals

**Goals:**
- An answer within a second or two outdoors, and at once when the phone has a recent fix.
- Visible feedback from the tap on.
- The same decisions as today for good fixes; no question caused only by a rough fix.

**Non-Goals:**
- Changing the location used while navigating, "Show my position" or "Nearest knots".
- Changing the 10 s limit.

## Decisions

- **A recent fix first.**
  - iOS: `CLLocationManager.location`, if `timestamp` is at most 60 s old and `horizontalAccuracy` is between 0 and 100 m.
  - Android: `getCurrentLocation` with a `CurrentLocationRequest` of `maxUpdateAgeMillis = 60_000`. Fused location returns a cached fix itself when one fits, so no separate `lastLocation` call is needed. A result with accuracy above 100 m counts as no fix.
- **About 100 m otherwise.**
  - iOS: `PositionFinder.find(timeout:accuracy:maxAge:)` sets `desiredAccuracy = kCLLocationAccuracyHundredMeters` for this request and back to best afterwards. `requestLocation()` returns once that accuracy is reached.
  - Android: `Priority.PRIORITY_BALANCED_POWER_ACCURACY` with `durationMillis = 10_000`.
  - Both: "Show my position" keeps calling with today's settings, which stay the defaults.
- **Accuracy in the core.** `StartChoice.of(..., accuracyMeters: Double = 0.0)` compares the start distance with `AT_START_METERS + accuracyMeters`. Nothing else changes: the course and the start are both measured from the same rough point, so their comparison holds.
- **Feedback.**
  - iOS: `AppModel.findingStart` (`@Published`).
  - Android: `AppState.findingStart`.
  - Both: it is set before the search and cleared when the choice or the ferry check takes over. Navigate shows a `ProgressView` / `CircularProgressIndicator` in white on its green, same size, ignores taps while `findingStart` is set, and its accessibility label becomes "Finding your position".
  - Alternative: a note over the map. Rejected, as the spinner is where the finger is.
- **Strings:** "Finding your position" in four languages.

## Risks / Trade-offs

- **A rough fix near the route but far from the start.** Up to 100 m off, the "ride to the start or nearby" distances in the question are rough too. They are shown in whole tens or hundreds of metres already, and the rider sees the map.
- **A cached fix from before a short ride to the start.** At most 60 s old, so at most a few hundred metres off by bike. The worst case is an unneeded question, which the rider answers with "Ride to the start".
