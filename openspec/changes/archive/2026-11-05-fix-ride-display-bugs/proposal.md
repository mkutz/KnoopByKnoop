# Proposal

## Why

The screen recordings of the test rides on 2026-11-01 (Pixel 7, German) show four display bugs on Android:

- While the map follows the rider, the labels and icons of the base map keep disappearing and reappearing, a few times a second (first video, 0:00–0:10).
- The map's compass button sits in the status bar, over the battery icon (second video, 0:00).
- Off route, the German header "Abseits der Route, nächster" wraps onto two lines, and the badge of the next knot is gone (second video, 12:48 and 17:35).
- In dark mode, the off-route red is a pale pink in the header, the arrow below it and the rider's dot on the strip (second video, 17:35).

## What Changes

- The base map's labels and icons stay steady while the map follows the rider.
- The compass button stays clear of the status bar, below the search field.
- The off-route header shows the next knot's badge in every language; where the text does not fit in one line, the badge follows it on the next.
- The off-route red stays a clear red in dark mode, readable on the dark panel.

## Out of Scope

- The other findings of the rides (ridden track on top, out-and-back legs, the rider's arrow size, which number is most prominent, the line back to the route, back on route on a parallel path, arrivals at the current speed, the lock-screen notification): each needs a decision and gets its own change.
- iOS, beyond checking the same situations: it hides the compass, and its system red stays red in dark mode.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: "Following the rider" gets a scenario for a steady base map; "Number to follow at large text sizes" also covers the off-route header in every language.
- `map-planning`: "Clear of the system bars" names the compass; "Light and dark appearance" gets a scenario for off route in dark mode.

## Impact

- Android: `NetworkMap.kt` (camera following, the rider's and the track's sources, the compass margins), `PlannerScreen.kt` (`OffRouteHeader`), `Theme.kt` (the dark `error` color).
- iOS: checks only, unless the German header loses its badge there too.
- Core: none.
