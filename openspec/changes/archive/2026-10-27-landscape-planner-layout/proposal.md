# Proposal

## Why

On an iPhone in landscape, the waypoint sheet covers the whole screen: iOS shows sheets full screen in compact height and ignores their detents. The map cannot be seen at all, so nothing can be planned. On Android, the bottom sheet stays, but on a short landscape screen it leaves little room for the map. Riders often mount the phone in landscape on the handlebar or look at routes on a tablet.

## What Changes

- In landscape, the planner shows the map and the waypoint list **side by side**, both usable at the same time, on Android and iOS.
- In portrait, the planner stays as it is: the map with the waypoint sheet over it.
- Rotating keeps the route, the map's position and the selected network.

Out of scope: a tablet-specific layout beyond side by side, navigation mode, changes to the sheet's content.

## Capabilities

### New Capabilities

### Modified Capabilities
- `map-planning`: the planner's layout in landscape

## Impact

- `apps/ios`: `PlannerView` switches from the always-presented sheet to a split layout when the vertical size class is compact
- `apps/android`: `PlannerScreen` switches from `BottomSheetScaffold` to a row of map and list in landscape
- No core changes
