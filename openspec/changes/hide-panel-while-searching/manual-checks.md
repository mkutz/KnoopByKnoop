# Manual checks

Checked 2026-11-06 on the Android emulator `Medium_Phone_API_37.0`, Android 17, in landscape, with 02 → 80 planned (13 km), typing "Amsterdam" in the search.

| Scenario | Android | Notes |
|---|---|---|
| Typing in the search in landscape | ✅ | Before: the panel's 13 km, Navigate and badges stayed in the strip above the keyboard. Now the map, the field and the first hit use the whole width; after closing the keyboard the panel is back with 13 km and 02 → 80 |
| Planning in landscape | ✅ | Map and panel side by side as before |
| Map controls in landscape | ✅ | Zoom and position buttons visible next to the hits once the keyboard is closed |

## For the maintainer

| Check | Pixel | Notes |
|---|---|---|
| Search in landscape | ⏳ | The panel steps aside while typing and comes back after |
