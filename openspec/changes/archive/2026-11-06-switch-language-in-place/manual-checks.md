# Manual checks

## Scenarios

| Scenario | Android 17 emulator | Android 12 emulator | Notes |
|---|---|---|---|
| Switching without a pause | ✅ | – | Android 17: Deutsch in the info sheet; the sheet stayed open, no relaunch, no skipped frames, same process, the map kept its area and route 02 → 80. Android 12 rebuilds the screen as before, see the design |
| Choosing German on an English phone | ✅ | ✅ | "Darstellung", "Sprache", "Orte suchen", "Navigieren" |
| Back to the phone's language | ✅ | ✅ | English again at once |
| Kept after a restart | ✅ | ✅ | Dutch after a restart on Android 17, German after a reinstall on Android 12 |
| Same as the system setting on Android | ✅ | n/a | Dutch, French, English set in the system's per-app setting with the app open: each switched in place, no relaunch |
| Place menu in French | ✅ | – | "Partir d'ici / Ajouter une étape ici / Arriver ici" |
| Messages in the language | – | ✅ | Android 12, German, location denied: "Knoop by Knoop braucht Zugriff auf den Standort, …"; it was English before the fix |
| Language change while navigating: words and line outside the app | ✅ | – | Notification "Nächster 28, dann 27 · 2,7 km bis 28" at once, voice switched to German, navigation kept running |

## Measured on the maintainer's Pixel 7

Eight language switches each time, `dumpsys gfxinfo` and the log over the whole run, including the start.

| | Before | After |
|---|---|---|
| Screen rebuilds | one per switch | none |
| "Skipped frames" in the log | 30–38 per switch | none |
| Slow frames | 110 of 543 (20%) | 83 of 414 (20%) |
| 95th percentile | 200 ms | 150 ms |
| 99th percentile | 500 ms | 250 ms |

The maintainer found it "so much better". The share of slow frames stays at 20%: each switch still redraws every text and the menu.

## For the maintainer

| Check | Pixel | Notes |
|---|---|---|
| Language switch feels smooth | ✅ | 2026-11-06 |
