# Manual checks

## How it was checked

On 2026-11-13 on the emulator (`Medium_Phone_API_37.0`) with the debug build and the installed cycling data, and in the simulator (iPhone 18 Pro). The file opened was written by hand, without an id:

```
# Along the Sieg

- 02 Siegburg
  - 28
- 29 lunch at the river
```

On the emulator it was pushed to Downloads and opened through the documents provider, as Files does; in the simulator with `xcrun simctl openurl`.

## Scenarios

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Open a shared route | ✅ | ✅ | The route Siegburg 02 → 28 → 29 replaced the planned one and was shown whole ([Android](screenshots/android-opened-with-text.png), [iOS](screenshots/ios-opened-with-text.png)); on iOS it replaced a route in Slenaken |
| Own text for a stop | ✅ | ✅ | "End · lunch at the river" in the waypoint list; on Android also after a restart |
| Share a route | ✅ | open | Share offers both choices ([menu](screenshots/android-share-menu.png)); the share sheet with `knoop-by-knoop-cycling-02-29.knoop` ([sheet](screenshots/android-share-sheet.png)). The file read `# 02 Siegburg → 29 Troisdorf`, `- 02 59469043 Siegburg`, `  - 28`, `- 29 lunch at the river`. iOS: the menu needs a tap, for the phone |
| Undo an opened route | ✅ | open | Undo brought back the empty plan from before the file |
| Not a route file | ✅ | open | A letter saved as `.knoop`: "This file holds no route the app can open.", the plan unchanged ([screenshot](screenshots/android-not-a-route.png)) |
| Share while offline, Home flag shared as it is, Opened on the other network, Opened while navigating, Opened route on the other network | open | open | For the phones |

## Found and fixed

- **Android: "Loading the cycling knot network…" for good.** On the slow emulator the app twice stayed on the loading note with an empty panel. Measuring the stored base map suspends, and the state was read before it, so the copy written back after the network had loaded dropped it. Not caused by this change (it came with the offline base map); fixed in its own commit.

## For the maintainer

1. **Between the phones:** plan a route with a home flag and a café on the Pixel, Share → "Route for Knoop by Knoop", send it to yourself in a messenger, open it on the iPhone; and the other way round. Both show the same route, the flag at home, the café by name.
2. **iPhone:** Share shows the menu with both choices, and the share sheet with the file.
3. **A hiking file** (Share a hiking route) opened while cycling is shown: the app switches to hiking, and no Undo.
4. **While navigating:** open a file; the app says to stop first, and navigation goes on.
5. **In flight mode:** Share → "Route for Knoop by Knoop" still opens the share sheet.
6. **By hand:** write a list from a paper map in a notes app, save it as `.knoop` and open it.
