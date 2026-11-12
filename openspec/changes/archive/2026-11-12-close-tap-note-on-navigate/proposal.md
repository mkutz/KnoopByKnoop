## Why

The note is one of the planning tools, and planning tools are no longer needed once the planned route is being ridden. Since a choice keeps the note open, it can still stand over the map when the rider starts navigating, where it only covers the map.

## What Changes

- Choosing Navigate closes the note at once, on Android and iOS, before the position is found.

## Capabilities

### New Capabilities

### Modified Capabilities
- `map-planning`: "Changing what a tapped knot became" also closes the note on Navigate.

## Impact

`AppViewModel.startNavigation` on Android and `AppModel.startNavigation` on iOS; no core change.
