# Manual checks

## Scenarios

Android checked 2026-10-31 on the emulator (`Medium_Phone_API_37.0`, 411 dp; 360 dp with `wm density 480`), route 93 → 42 → 94 in Arnhem.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Export a route | ✅ | ✅ | More actions → Export GPX opens the share sheet with `knoop-by-knoop-cycling-93-94.gpx` ([screenshot](screenshots/android-share-sheet.png)) |
| Track along the network | ✅ | ✅ | File pulled from the cache ([file](screenshots/android-export-93-42-94.gpx)): well-formed (`xmllint`), one track of 25 points along the course; the GPX structure is covered by `GpxTest` |
| Knots as waypoints | ✅ | ✅ | 93 start, 42 stop, 93 knot, 94 end, in riding order |
| Round trip | ✅ | ✅ | `GpxTest` |
| No export without a complete route | ✅ | ✅ | Only a start: no More actions, only Clear |
| No export while navigating | ✅ | ✅ | Only Stop in the header ([screenshot](screenshots/android-navigating.png)) |
| Export while offline | ✅ | ✅ | Wi-Fi and mobile data off: the share sheet opens with the file |
| Navigate stands out | ✅ | ✅ | At 360 dp: "3.0 km", "about 11 min", More actions, Clear, Navigate in one row ([screenshot](screenshots/android-header-360dp.png)); the menu with Back to start and Export GPX ([screenshot](screenshots/android-menu-360dp.png)) |
| Icon buttons with a screen reader | ✅ | ✅ | Named "More actions" and "Clear"; the menu entries "Back to start" and "Export GPX" (uiautomator); TalkBack itself not observed |
| Readable actions | ✅ | ✅ | Icons measured from screenshots: menu entries 4.63:1, More actions 4.36:1 |
| Readable actions in dark mode | ✅ | ✅ | Menu entries 9.86:1, More actions 7.40:1 ([screenshot](screenshots/android-menu-dark.png)) |

### Found and fixed

- A fifth element did not fit the header at 360 dp: the distance wrapped and the time was cut off ([screenshot](screenshots/android-header-360dp-five.png)). Decided with the maintainer: Back to start and Export GPX moved into a More actions menu.

### Other apps

- iPhone, 2026-10-31, route 02 → 79 from Siegburg to Bonn: imported in Komoot (14.7 km, the line along the route, the knots as grey markers) and in Outdooractive (title "Cycling 02 → 79", the line from Siegburg to Bonn).

### iOS steps for the maintainer

1. Plan a route with a stop. The header shows the distance, a round ⋯ button, Clear and Navigate in one row.
2. Tap ⋯: Back to start and Export GPX, each with icon. Tap Export GPX: the share sheet opens over the planner sheet, which stays open, with `knoop-by-knoop-<network>-<start>-<end>.gpx`.
3. "Save to Files", then open the file in another app (e.g. Komoot, OsmAnd or a Garmin app): the line follows the route, the knots appear with their numbers.
4. Only a start set: no ⋯ button. While navigating: no ⋯ button.
5. Flight mode with a planned route: Export GPX still works.
6. VoiceOver: "More actions", "Clear", "Navigate"; in the menu "Back to start" and "Export GPX".
7. In dark mode, the ⋯ icon and the menu icons are clearly visible.
