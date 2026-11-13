## Why

A mistap or a wrong removal while planning can only be put right by hand today; the only way back is the Undo in the note after a tap, which is gone after 6 seconds and covers nothing else. A general Undo in the panel takes back any edit of the route, so planning can be tried out freely.

## What Changes

- An Undo icon button in the panel's action row, after Back to start, Export GPX and Clear, and in the empty panel's buttons, shown when there is an edit to take back and not while navigating.
- Each Undo takes back one edit, many steps back (at least 50), with no redo. Edits are taps (with the note's role choices as part of the tap), Start/Stop/End here, removing and moving waypoints, Back to start and Clear. The courses coming in and data updates are not edits.
- The edits are kept across a restart, beside the route. Switching the network discards them with the route.
- **BREAKING (UI)**: the note after a tap loses its own Undo; Undo in the panel takes the tap back and closes the note.
- Clear stays without a question, as today; Undo brings a cleared route back.

## Capabilities

### New Capabilities

### Modified Capabilities
- `map-planning`: new requirement "Undo route edits"; "Changing what a tapped knot became" without the note's Undo; "Clear actions in the planner panel" and "Panel before a route" with Undo.

## Impact

- Core: a new `RouteHistory` (snapshots of the route before each edit, as JSON), with tests.
- Android: `AppViewModel` records edits and undoes them, `NetworkStorage` keeps `route-history.json`; `PlannerScreen` and `EmptyRoute` show Undo; the note's Undo and its string go.
- iOS: the same in `AppModel`, `NetworkStorage`, `PlannerView` and `EmptyRoute`.
- Strings "Undo" exist already from the note.
