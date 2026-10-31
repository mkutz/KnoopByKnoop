# Proposal

## Why

An Android tester did not find out that the waypoints can be reordered: "Hatte keinen Anfasser gesehen, daher dachte ich das geht nicht". Moving a waypoint takes a long press, removing one a swipe, and neither is visible in the list. Both platforms hide these gestures the same way.

## What Changes

- Every waypoint row shows a drag handle (≡) at its end. Dragging the handle moves the waypoint right away; a long press anywhere on the row keeps working.
- Every row shows a remove button (×), also with only two waypoints: removing one of them leaves the other as the start (until now, two waypoints could only be cleared together). Swiping keeps working.
- Screen readers get "Move up", "Move down" and "Remove" as actions on each row, as dragging is not possible with them.

Out of scope:
- Adding waypoints from the list, or moving them on the map.
- Undo after removing a waypoint.
- The locate button and the dark map from the same round of feedback (their own changes).

## Capabilities

### New Capabilities

### Modified Capabilities
- `map-planning`: "Edit the waypoint list" shows how to move and remove waypoints

## Impact

- `apps/android` `PlannerScreen.kt` (`WaypointList`), new drawables for the handle and the remove icon
- `apps/ios` `PlannerView.swift` (`WaypointSheet`)
- `modules/core` `PlanningSession`: removing one of two waypoints
