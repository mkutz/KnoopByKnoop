# Proposal

## Why

While navigating, the map shows the rider as a dot, without the direction they are riding in, and it only moves to the rider when navigation starts or the app is opened. Riding on, the rider leaves the visible area. After zooming out or looking ahead, there is no quick way back. Riders also can't see where they have actually been, e.g. on a detour. The maintainer wants the navigation view to fix this.

## What Changes

- While navigating, the map follows the rider as they ride.
- Moving or zooming the map stops following. A recenter button then appears and brings the map back to the rider at the navigation zoom, following again. There is no automatic return: the map stays where the user put it until they tap the button.
- The rider's position is an arrow pointing in the riding direction, from their most recent movement, not from the compass or the course the phone reports. Standing still, it is a dot as before. The map stays north-up.
- The track ridden during this navigation is drawn as a line that looks different from the planned route. It exists only while navigating and is gone after Stop or arriving.
- The location is not sampled more often for any of this: arrow and track come from the fixes navigation already gets.

Out of scope: rotating the map with the riding direction (heading-up), saving, sharing or exporting tracks, the compass, and the lock screen, notification and Live Activity.

## Capabilities

### New Capabilities

### Modified Capabilities
- `knot-navigation`: new requirements for following the rider with a recenter button, the riding direction, and the ridden track. Its main spec is created when `add-knot-navigation` is archived, so that change must be archived first.

## Impact

- Core: a new immutable ridden track with its filtering, and the riding direction from its most recent movement. Unit tests for both.
- Android:
  - `NavigationService`: keeps the ridden track
  - `NetworkMap.kt`: the arrow as a rotated symbol, a track layer, following and gesture detection
  - `PlannerScreen.kt`: the recenter button
- iOS:
  - `Navigator.swift`: keeps the ridden track
  - `NetworkMapView.swift`: the same as on Android
  - `PlannerView.swift`: the recenter button
