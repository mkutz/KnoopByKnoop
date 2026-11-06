# Manual checks

Checked 2026-11-06 on the Android emulator `Medium_Phone_API_37.0`, Android 17, in landscape, with 02 → 80 planned (13 km), typing "Amsterdam" in the search.

| Scenario | Android | Notes |
|---|---|---|
| Typing in the search in landscape | ✅ | Before: the panel's 13 km, Navigate and badges stayed in the strip above the keyboard. Now the panel slides aside and a chip "02 → 80, 13 km" shows at the top right; tapping the chip closed the keyboard and the panel slid back with 13 km and 02 → 80. Screen reader: "Route of 13 km, closes the keyboard". Recorded as a GIF for the maintainer, who approved it |
| Planning in landscape | ✅ | Map and panel side by side as before |
| Map controls in landscape | ✅ | Zoom and position buttons visible next to the hits once the keyboard is closed |

## For the maintainer

| Check | Pixel | Notes |
|---|---|---|
| Search in landscape | – | Not possible on the Pixel, its rotation sensor is broken; the maintainer accepted the emulator recording instead (2026-11-06) |
