# Manual checks

## Scenarios

Checked 2026-11-02, right after the network data of that morning (OpenStreetMap extract of 2026-10-31) was published. Both the emulator and the simulator still had the data of 2026-10-25 installed.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Data published since the last start | ✅ | ✅ simulator | First start of the new build: all four files from 2026-10-25 to 2026-10-31 (`installed.json`). On Android, `NetworkSyncWorker` ran once the route was restored |
| Two starts in a row | ✅ | ✅ simulator | Started again right away: Android ran the worker again (2.5 s); iOS requested `index.json` again (`log show`, `com.apple.network:connection`) |
| Nothing new | ✅ | ✅ simulator | The second start downloaded no file: the files kept their time; `NetworkSyncTest` "current data is installed" lists nothing to fetch |
| Newer data available | ✅ | ✅ simulator | As the first row |
| Route across an update | — | — | Unchanged; the restored route was kept in both |
| Offline start (design risk) | — | ⏳ | Not checked: the simulator can't go offline without the Mac. See the steps below |

### Steps for the maintainer

1. Install the new build on the Pixel and the iPhone and start it: the info sheet (ⓘ) shows 2026-10-31 for cycling and hiking.
2. After the next weekly data, swipe the app away and start it again: the info sheet shows the new date without waiting a day.
3. iPhone: start the app in flight mode, then swipe it away and start it with a connection: the info sheet does not keep showing a failed update.

### Archived 2026-11-05

Archived with the iPhone cells still marked ⏳ or ❌ above, at the maintainer's decision. What is left is on the ride sheet "Knoop by Knoop: Next Test Ride".
