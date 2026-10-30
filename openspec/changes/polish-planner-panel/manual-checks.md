# Manual checks

## Scenarios

The ferry rows were folded into this change after the first round of checks, at the maintainer's request ("the ferries should not take more space than one line").

Android checked 2026-10-30 on the emulator (`Medium_Phone_API_37.0`) with the bundled graphs, positions mocked with `adb emu geo fix`.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Info button | ✅ | ⏳ | Round ⓘ at the end of the network switch, styled like the zoom buttons, while planning ([light](screenshots/android-planning-light.png), [dark](screenshots/android-planning-dark.png)) and alone while navigating ([screenshot](screenshots/android-navigating-ferry-ahead.png)) |
| Info button with a screen reader | ✅ | ⏳ | `uiautomator dump`: a clickable button holding the icon with content description "Info", which TalkBack reads as one button; long press shows "Info" |
| Readable info button | ✅ | ⏳ | Measured from the screenshots: icon #2e9e5b on #ffffff 3.41:1 (light), on #2a2c2a 4.13:1 (dark) |
| Opening the info sheet, Failed update | unchanged | unchanged | Only the button changed |
| Listing waypoints, Compact waypoint list | ✅ | ⏳ | 03 → 02 → 30 → 31 (no ferries): four 48 dp rows with badge and role fit the expanded panel without scrolling ([screenshot](screenshots/android-planning-light.png)) |
| Waypoint list while navigating | ✅ | ⏳ | The panel shows the number to follow and the badges, no rows ([screenshot](screenshots/android-navigating-ferry-ahead.png)) |
| Closed for the season in the ferry list | ✅ | ⏳ | 76 → 02 → 20 at Renkum, navigating at 20:10: "⛴ 76 → 02 · At 20:15: closed until 1 May 10:00" in red in one line, no status for now ([screenshot](screenshots/android-navigating-ferry-ahead.png)); spoken warning unchanged |
| Route over a ferry, Expanding a ferry | ✅ | ⏳ | 61 → 50 → 03, two legs of the Driehoeksveer: "⛴ 61 → 50 · Closed until tomorrow 10:30" and "⛴ 50 → 03 · …" ([screenshot](screenshots/android-ferries-collapsed.png)); tapping the first one shows its name "Driehoeksveer Ridderkerk - Krimpen aan de Lek - Kinderdijk", status, times, "Times according to OpenStreetMap" and Website, the second stays one line ([screenshot](screenshots/android-ferry-expanded.png)); tapping it again collapses it |
| Where the ferry is | ✅ | ⏳ | The lines start with the knots on both sides of the ferry in riding order ("⛴ 61 → 50", "⛴ 76 → 02"); the name only expanded |
| Shorter status texts (Ferry open now) | ✅ | ⏳ | "Closed until tomorrow 10:30", "Closed until 1 May 10:00"; all texts in `FerryStatusTest` |
| Ferry without known times | ✅ | ⏳ | 19 → 71 in Rotterdam over Waterbus 20: "⛴ Waterbus 20 · Operating times not known" in red ([screenshot](screenshots/android-ferry-without-times.png)); expanded: "Operating times not known, check before you go" ([screenshot](screenshots/android-ferry-without-times-expanded.png)) |
| Expanded state for screen readers | set, not observed | ⏳ | The row is a button with the state description "Expanded"/"Collapsed"; TalkBack's reading could not be captured on the emulator (no log of its speech) |
| Ferry passed in the list | ✅ | ⏳ | After 02: "Closed, opens on 1 May at 10:00", the status for now as while planning ([screenshot](screenshots/android-navigating-ferry-passed.png), taken before the ferry rows were collapsed; the same status now shows in the ferry's line) |
| Arrival in the ferry list, Arrival when the times do not tell, Warning in the ferry list | unit tests | unit tests | `FerryWarningTest`, JVM and iOS simulator |

### iOS steps for the maintainer

1. The map while planning: a round ⓘ in place of "Info", next to Cycling/Hiking, white with a green icon like the zoom buttons; dark in dark mode. Tapping it opens the info sheet.
2. VoiceOver on: the ⓘ is read as "Info, button".
3. Plan a route with four waypoints and no ferry, expand the sheet: four compact rows (badge and Start/Stop/End), all visible without scrolling; moving and removing still work.
4. Plan 76 → 02 → 20 at Renkum and start navigating near 76: the sheet shows no waypoint rows, the ⓘ stays at the top, and the ferry shows one red line "⛴ 76 → 02 · At …: closed until 1 May 10:00".
5. Ferries: plan 61 → 50 → 03 (Driehoeksveer, Krimpen aan de Lek): two one-line rows "⛴ 61 → 50 · Closed until …" and "⛴ 50 → 03 · …" with a chevron at the end; tap the first: the name "Driehoeksveer Ridderkerk - Krimpen aan de Lek - Kinderdijk", status, times, fee, "Times according to OpenStreetMap" and Website; the second stays one line; tap again to collapse. VoiceOver reads the row with "Expanded"/"Collapsed".
6. A ferry without times, e.g. Waterbus 20 (19 → 71 in Rotterdam): "Operating times not known" in red.
7. Optional, on a ride across a ferry: after the knot behind the ferry, its status for now is shown again.

### Found and fixed

- A long ferry name cut off the status entirely ("⛴ Driehoeksveer Ridderkerk - Krimpen aan de Lek - Ki…"). First the name gave way to the status; then, at the maintainer's request, the line shows where the ferry is ("⛴ 61 → 50") instead of the name, with shorter status texts ("Closed until tomorrow 10:30") and the arrival first while navigating ("At 20:15: closed until 1 May 10:00").
- The chevron sat right after a short line instead of at the end of the row.
