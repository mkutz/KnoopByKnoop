# Proposal

## Why

A tester's feedback of 2026-11-01 (Pixel 9): the map opened with no knots around, and the panel said "Tap a knot to set the start". "Maaaybe when the map has no knots in the local area and user clicks on tap a knot to see… notify the user that he has no knot network in his area… At least for me this was confusing since I haven't had fast the idea to zoom out on the map."

Anyone outside the Netherlands, Belgium and Nordrhein-Westfalen gets the same: an empty map, and a prompt they cannot follow. The same happens when zoomed out too far for knots to show. The maintainer wants a way to get to the network, like the to-me button.

## What Changes

- **A button when no knots are in view:** while planning, when the map shows no knots, a round button appears below the to-me button. Tapping it shows the nearest knots.
- **The panel says so:** with no waypoints set, "No knots in view" and a "Show the nearest knots" button replace "Tap a knot to set the start".
- **From where the rider is:** the nearest knot is found from the rider's position where location access is allowed, otherwise from the middle of the map. The button does not ask for location access.
- **Where the map goes:** so the rider and the nearest knot are both in view, with the knots shown. If they are too far apart for that, to the nearest knot.
- **The rest of the time, nothing changes:** with knots in view the map has no extra button, and the panel asks to tap a knot.

## Out of Scope

- **Other networks:** only the selected network, cycling or hiking, is searched.
- **Telling the distance or the area the network covers.**
- **Navigating:** as with the to-me button, not while navigating.
- **Knots hidden behind the top controls:** a knot behind the search field or the network switch counts as in view. The panel at its smallest is left out on both platforms.
- **Other points of that feedback:** the language change and appearance lag in Info, and crash reporting.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`: a new requirement "Nearest knots".

## Impact

- **Core:** `KnotLocator.noKnotsIn` and `nearestPosition`. Tests are in `PlanningTest`.
- **Android:** `NetworkMap` (the button, the map's area when it stops moving, moving the map), `AppViewModel`, `PlannerScreen` (the panel), and new strings in four languages.
- **iOS:** the same in `NetworkMapView`, `AppModel`, `PlannerView` and `Localizable.xcstrings`.
