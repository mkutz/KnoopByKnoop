# Manual checks

## Scenarios

Android checked 2026-10-31 on the emulator (`Medium_Phone_API_37.0`) with the route 03 → 02 → 30 → 31; gestures sent with `adb shell input`.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Visible handle, Visible remove button | ✅ | ⏳ | Every row shows × and ≡ at its end ([screenshot](screenshots/android-handles.png)) |
| Moving by the handle | ✅ | ⏳ | A plain swipe on the handle of 30 up by one row, without holding: 03, 30, 02, 31 |
| Long press still moves | ✅ | ⏳ | Press on the row of 02 for one second, then drag up: 03, 02, 30, 31 |
| Remove | ✅ | ⏳ | × of 30: 03, 02, 31, each row still with × |
| No remove button with two waypoints | ✅ | ⏳ | × of 02: 03, 31, without × ([screenshot](screenshots/android-two-waypoints.png)) |
| Reordering with a screen reader | set, not observed | ⏳ | `uiautomator dump` shows the labels "Move 03" and "Remove 02" on handle and ×; the row's custom actions "Move up", "Move down", "Remove" are set in the semantics, but `uiautomator` does not list custom actions and the project has no UI tests to trigger them |

### iOS steps for the maintainer

1. Plan a route with four waypoints and expand the sheet: every row shows the reorder grip (≡) on the right and the red remove control on the left.
2. Drag a grip: the waypoint moves, the route follows.
3. Tap a remove control and confirm: the waypoint is gone. With two waypoints left, no remove control.
4. VoiceOver on a row: the actions (swipe up/down) offer moving and deleting.
5. A route with a ferry while planning: tapping a ferry row still expands it, and its website link still works (the list is in edit mode now).
6. While navigating, the waypoint list stays hidden as before.
