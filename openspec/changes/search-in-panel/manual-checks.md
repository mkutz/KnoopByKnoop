# Manual checks

## Scenarios

Checked 2026-11-09 in the simulator (iPhone 18 Pro, iOS 27), debug build, a route 02 → 06 → 02 in Siegburg, driven by a throwaway XCUITest runner outside the repository. The map's buttons are UIKit views on the MapLibre view and are not found by their labels in a UI test, so the runner tapped them by position.

| Scenario | iOS | Notes |
|---|---|---|
| Field above the sheet on iOS | ✅ simulator | Smallest and medium sheet: the column, magnifier on top, ends above the attribution and the sheet; nothing floats over the map ([default and largest text](screenshots/ios-column-default-and-largest-text.png), [medium, dark](screenshots/ios-sheet-medium-dark.png)) |
| Sheet pulled up on iOS | ✅ simulator | The sheet shows the waypoints |
| Typing on iOS | ✅ simulator | The sheet rises to full height, the field focused at its top with Cancel, the hit "Utrecht Centraal, Train station, Utrecht" below ([screenshot](screenshots/ios-search-open-and-cancelled.png), left) |
| One surface on iOS | ✅ simulator | The field and hits on the sheet's background |
| Searching with a hardware keyboard on iOS | ✅ simulator | The simulator has a hardware keyboard: no on-screen keyboard, the same layout |
| Back above the sheet on iOS | ✅ simulator | Picking the hit: the search closes, the sheet back at its height, the map moved to Utrecht Centraal with the marker ([screenshot](screenshots/ios-hit-picked.png)); Cancel: back as before ([screenshot](screenshots/ios-search-open-and-cancelled.png), right). Tapping the map: not tried, as the sheet covers it at full height |
| Searching in landscape on iOS | ✅ simulator | The magnifier with the other buttons, above the attribution |
| Typing in landscape on iOS | ✅ simulator | The field and hits at the top of the side panel; the map and attribution stay ([screenshot](screenshots/ios-landscape-search.png)) |
| Done typing in landscape on iOS | ✅ simulator | As in portrait |
| Search needs internet access | not checked | The disabled field with its message is the same view as before |
| Largest text size | ✅ simulator | The magnifier stays reachable at the top; the lower buttons go under the tall sheet, as they did before |

### Found and fixed

- **The field got no keyboard:** the focus set from the view behind the sheet did not reach into it; the field now focuses itself once the sheet has risen.
- **Picking a place lost its marker:** closing the search cleared it; only Cancel clears the search now.
- **The magnifier under the attribution and, in landscape, at the bottom edge:** the column now ends above the attribution, and above the bottom safe area in landscape.
- **The magnifier under the sheet at the largest text size:** it moved from the bottom to the top of the column, see the design; this differs from the mockup.

### Open

- The map's round buttons are not reached by their labels in UI tests, which suggests VoiceOver may not reach them either; that holds for all of them since before this change and is worth its own look.

## For the maintainer

On the iPhone: tap the magnifier, search a place and pick it; open it again and Cancel; check the map's buttons with the sheet at its smallest and at medium height, and in landscape.
