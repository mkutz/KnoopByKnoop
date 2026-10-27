# Design

## Context

- **Android:** `WaypointSheet` (`PlannerScreen.kt`) is a `Column`: a `Row` with the summary (distance and a `FlowRow` of badges, from `show-ferry-schedules` also the ferry list) next to a `Column` with Clear and Back to start, then `WaypointList`, a `LazyColumn` whose height is fixed to at most five rows, with its own long-press drag and swipe to dismiss. The `Column` does not scroll, so a tall summary pushes the list out of the sheet.
- **iOS:** `WaypointSheet` (`PlannerView.swift`) is a `VStack`: an `HStack` with the summary (`WrappingRow` of badges, the ferry list) and the buttons, then a `List` with `onMove` and `onDelete`. When the `VStack` is taller than the sheet, it is cut off at the top as well as the bottom, which hides the buttons too.

## Goals / Non-Goals

**Goals:**
- Distance and buttons always reachable.
- Badges, ferries and waypoints reachable by scrolling, with moving and removing waypoints unchanged.
- A compact top of the panel, so the smallest sheet still shows the route at a glance.

**Non-Goals:**
- Collapsing passed knots or the ferry list.

## Decisions

### A fixed header with one line of badges, one scrolling body
The header holds the distance on the left and Clear and Back to start on the right, as now, and below them the badges in one line that scrolls sideways. The body holds the ferries and the waypoints, and scrolls as one.

The maintainer suggested the single line. On its own it is not enough: a 550 km test route crossed seven ferries, and their list alone was taller than the sheet. So the ferries and waypoints still scroll. Alternatives: badges wrapping inside the scrolling body (built first during apply; it worked, but on long routes the badges took the whole sheet and the route could not be seen at a glance), and a collapsed ferry list (hides what users need to check before the trip). Both rejected.

The texts without a complete route ("Tap a knot to set the start", "No route for …") stay in the header, where the distance is otherwise.

### iOS: the ferries in the `List`
The badges are a horizontal `ScrollView` in the header. The ferries become the first section of the waypoint `List`, as one row without separators and without move or delete. The waypoints are the second section with `onMove` and `onDelete`, so dragging and swiping work as now. A `ScrollView` around a `List` would not scroll the list itself, and `onMove` needs a `List`.

### Android: one `LazyColumn`
The badges are a `Row` with `horizontalScroll` in the header. `WaypointList`'s `LazyColumn` becomes the body: its first item holds the ferries, the waypoint rows follow. Its height is no longer fixed to five rows; it fills the rest of the sheet. The drag offsets in `WaypointList` are measured in rows from the dragged row, so an item above the waypoint rows does not change them; the item keys stay the waypoint indices, with a separate key for the summary item.

### Removing waypoints must not crash
During apply, swiping a waypoint away at the end of the long, scrolled list crashed the app in Material's `SwipeToDismissBox` ("The offset was read before being initialized"); the same crash was in the log from before this change. The rows are keyed by their index, so after a removal another waypoint's row took over the dismissed row's state. The rows get keys that change when the waypoints change, so every row starts with a fresh swipe state.

## Risks / Trade-offs

- [A single line shows only part of a long route at once] → it scrolls sideways; the map shows the whole route.
- [Android's drag uses `animateItem`, which also animates the summary item when the route changes] → the summary item gets no `animateItem`.
