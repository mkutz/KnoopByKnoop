## Why

Planning tools are no longer needed once the planned route is being ridden. Some of them can still be open when the rider chooses Navigate and then stay over the map or act on it during the ride: the card of a tapped place with "Add to route", a spot or place still being added, the marker of a search hit, and a map move for "Show my position" or "Nearest knots" that has not arrived yet. `close-tap-note-on-navigate` does this for the note after a tap; this change does it for the rest.

## What Changes

- Choosing Navigate closes every planning tool still open, at once, before the position is found, on Android and iOS:
  - the card of a tapped place, with its "Add to route";
  - a tapped spot or place still being added: it is not added;
  - the search and the marker of a chosen search hit;
  - a map move for "Show my position" or "Nearest knots" not yet made: the map does not move for it.
- The tap note is left to `close-tap-note-on-navigate`.
- Tapping a place while navigating still shows its card with the name only, as specified.

## Capabilities

### New Capabilities

### Modified Capabilities
- `map-planning`: a new requirement "Planning tools close on Navigate".

## Impact

`AppViewModel.startNavigation` and `PlannerScreen` on Android, `AppModel.startNavigation` and `PlannerView` on iOS. No core change, no data change.
