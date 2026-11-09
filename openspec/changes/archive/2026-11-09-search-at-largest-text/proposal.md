# Proposal

## Why

Taking `search-in-panel`'s screenshots in the simulator on 2026-11-09 showed two problems on the iPhone at the largest text size (Accessibility XXXL):

- The first tap on the magnifier after the app starts does not open the search. The waypoint sheet drops lower instead, with the route's "15 km" cut off at its top, which also breaks `map-planning` "Route summary at large text sizes". A second tap opens the search.
- With the search open, the top of the field is cut off under the sheet's grabber.

At the default text size neither happens. The maintainer chose to handle this in a change of its own and archive `search-in-panel`.

## What Changes

- The first tap on the magnifier opens the search at any text size.
- The search field shows completely below the sheet's grabber.
- Closing the search brings the sheet back with the route's distance and time complete.

## Out of Scope

- **Android:** not seen there; checked in `search-in-panel` at the default size only, so task 2.1 checks it too.
- **The map's lower buttons under the sheet** at the largest size: accepted in `search-in-panel`.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `place-search`: a new requirement that the search works at the largest text size.

## Impact

- **iOS:** `PlannerView.swift` (the sheet's detents and content while opening and closing the search), `PlaceSearchView.swift` (room below the grabber).
- **Core:** none.
