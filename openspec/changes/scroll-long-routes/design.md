# Design

## Context

- **Android:** `WaypointSheet` (`PlannerScreen.kt`) is a `Column`: a `Row` with the summary (distance and a `FlowRow` of badges, from `show-ferry-schedules` also the ferry list) next to a `Column` with Clear and Back to start, then `WaypointList`, a `LazyColumn` whose height is fixed to at most five rows, with its own long-press drag and swipe to dismiss. The `Column` does not scroll, so a tall summary pushes the list out of the sheet.
- **iOS:** `WaypointSheet` (`PlannerView.swift`) is a `VStack`: an `HStack` with the summary (`WrappingRow` of badges, the ferry list) and the buttons, then a `List` with `onMove` and `onDelete`. When the `VStack` is taller than the sheet, it is cut off at the top as well as the bottom, which hides the buttons too.

## Goals / Non-Goals

**Goals:**
- Distance and buttons always reachable.
- Badges, ferries and waypoints reachable by scrolling, with moving and removing waypoints unchanged.

**Non-Goals:**
- Making the knot sequence shorter.

## Decisions

### A fixed header, one scrolling body
The header row holds the distance on the left and Clear and Back to start on the right, as now; it no longer holds the badges. The body holds the badges, the ferries and the waypoints, and scrolls as one. Alternative: let only the badges scroll in a box of limited height. That leaves two scrolling areas in a small sheet, one inside the other, which is hard to use. Rejected.

The texts without a complete route ("Tap a knot to set the start", "No route for …") stay in the header, where the distance is otherwise.

### iOS: everything in the one `List`
The badges and the ferries become the first section of the waypoint `List`, as one row without separators and without move or delete. The waypoints are the second section with `onMove` and `onDelete`, so dragging and swiping work as now. A `ScrollView` around a `List` would not scroll the list itself, and `onMove` needs a `List`.

### Android: one `LazyColumn`
`WaypointList`'s `LazyColumn` becomes the body: its first item holds the badges and the ferries, the waypoint rows follow. Its height is no longer fixed to five rows; it fills the rest of the sheet. The drag offsets in `WaypointList` are measured in rows from the dragged row, so an item above the waypoint rows does not change them; the item keys stay the waypoint indices, with a separate key for the summary item.

## Risks / Trade-offs

- [The sheet in its smallest height only shows the header] → the badges start right below it, and pulling the sheet up shows more, as now.
- [Android's drag uses `animateItem`, which also animates the summary item when the route changes] → the summary item gets no `animateItem`.
