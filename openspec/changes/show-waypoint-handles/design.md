# Design

## Context

See `proposal.md` for why. Android `WaypointList` (`PlannerScreen.kt`) moves a row with `detectDragGesturesAfterLongPress` on the whole row and removes one with `SwipeToDismissBox`; both are disabled while navigating (the list is hidden then anyway). iOS `WaypointSheet` (`PlannerView.swift`) is a plain `List` with `onMove` (long press) and `onDelete` (swipe), not in edit mode. Rows are 48 dp / 44 pt high.

## Goals / Non-Goals

**Goals:**
- The gestures are visible without changing how they work.
- The same affordances on both platforms, in each platform's idiom.

**Non-Goals:**
- New ways to edit waypoints.

## Decisions

### Android: handle and × in the row
The row gets, at its end, a remove `IconButton` (Material Symbols "close", only when `canRemove`) and a drag handle (Material Symbols "drag_indicator") in a 48 dp touch target. The handle gets `detectDragGestures` without the long press, sharing the existing drag state and the same swap logic; the long-press drag on the whole row stays. TalkBack: the row gets `customActions` "Move up" / "Move down" (where possible) and "Remove" (where allowed), and the handle and the × are labeled "Move 30" and "Remove 30".
*Alternative:* a library for reorderable lists. A dependency for what the existing drag code already does.

### iOS: the list's own edit mode
The waypoint `List` is always in edit mode while planning (`.environment(\.editMode, .constant(.active))`), which shows the system's reorder grip on every row and the red remove control where `onDelete` is allowed (`deleteDisabled` with two waypoints). That is how iOS shows these affordances everywhere, and VoiceOver offers moving and deleting on such rows by itself. Rows stay at 44 pt; the badge and role move right by the remove control's width.
*Alternative:* a custom handle and × as on Android. It would fight the `List`'s own gestures.

## Risks / Trade-offs

- [The red remove control reads as alarming] → It is the standard iOS control for removing rows; removing a waypoint is easy to redo by tapping its knot.
- [The × is tapped by mistake] → Same as above; the route updates immediately, so the result is visible.
- [Rows get crowded on small phones] → Badge, role, × and handle fit in 360 dp; the role text is short ("Start", "Stop", "End").
