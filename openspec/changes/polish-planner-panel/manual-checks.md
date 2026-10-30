# Manual checks

## Scenarios

Android checked 2026-10-30 on the emulator (`Medium_Phone_API_37.0`) with the bundled graphs, positions mocked with `adb emu geo fix`.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Info button | ✅ | ⏳ | Round ⓘ at the end of the network switch, styled like the zoom buttons, while planning ([light](screenshots/android-planning-light.png), [dark](screenshots/android-planning-dark.png)) and alone while navigating ([screenshot](screenshots/android-navigating-ferry-ahead.png)) |
| Info button with a screen reader | ✅ | ⏳ | `uiautomator dump`: a clickable button holding the icon with content description "Info", which TalkBack reads as one button; long press shows "Info" |
| Readable info button | ✅ | ⏳ | Measured from the screenshots: icon #2e9e5b on #ffffff 3.41:1 (light), on #2a2c2a 4.13:1 (dark) |
| Opening the info sheet, Failed update | unchanged | unchanged | Only the button changed |
| Listing waypoints, Compact waypoint list | ✅ | ⏳ | 03 → 02 → 30 → 31 (no ferries): four 48 dp rows with badge and role fit the expanded panel without scrolling ([screenshot](screenshots/android-planning-light.png)) |
| Waypoint list while navigating | ✅ | ⏳ | The panel shows the number to follow and the badges, no rows ([screenshot](screenshots/android-navigating-ferry-ahead.png)) |
| Closed for the season in the ferry list | ✅ | ⏳ | 76 → 02 → 20 at Renkum, started at 76 at 19:27: "Closed when you get there around 19:30, opens on 1 May at 10:00" in red, no status for now ([screenshot](screenshots/android-navigating-ferry-ahead.png)); spoken warning unchanged |
| Ferry passed in the list | ✅ | ⏳ | After 02: "Closed, opens on 1 May at 10:00", the status for now as while planning ([screenshot](screenshots/android-navigating-ferry-passed.png)) |
| Arrival in the ferry list, Arrival when the times do not tell, Warning in the ferry list | unit tests | unit tests | `FerryWarningTest`, JVM and iOS simulator |

### iOS steps for the maintainer

1. The map while planning: a round ⓘ in place of "Info", next to Cycling/Hiking, white with a green icon like the zoom buttons; dark in dark mode. Tapping it opens the info sheet.
2. VoiceOver on: the ⓘ is read as "Info, button".
3. Plan a route with four waypoints and no ferry, expand the sheet: four compact rows (badge and Start/Stop/End), all visible without scrolling; moving and removing still work.
4. Plan 76 → 02 → 20 at Renkum and start navigating near 76: the sheet shows no waypoint rows, the ⓘ stays at the top, and Renkumse Veer shows one red line "Closed when you get there around …, opens on 1 May at 10:00" above its times.
5. Optional, on a ride across a ferry: after the knot behind the ferry, its status for now is shown again.
