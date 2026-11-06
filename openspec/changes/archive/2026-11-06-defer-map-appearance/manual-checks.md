# Manual checks

## Scenarios

| Scenario | Android | Notes |
|---|---|---|
| Choosing the appearance in the info sheet | ✅ | Emulator 2026-11-06: with the sheet open and Dark chosen, the sheet turned dark and the map stayed light; on closing the sheet the map turned dark with the same area and route 02 → 80 |
| Switching while the app is open | ✅ | Emulator: sheet closed, system dark mode on: the map turned dark within 5 s (average brightness 226 → 54) |

## Measured on the maintainer's Pixel 7

Same steps both times: start the app, open the info sheet, switch Light → Dark → Light → Dark, close the sheet; `dumpsys gfxinfo` over the whole run, including the start.

| | Before | After |
|---|---|---|
| Frames | 868 | 688 |
| Slow frames | 59 (6.8%) | 36 (5.2%) |
| 95th percentile | 117 ms | 22 ms |
| 99th percentile | 200 ms | 200 ms |
| Slowest 1% on the GPU | 5 ms | 4 ms |

The remaining slow frames include the app's start and the one map reload when the sheet closes, which the totals can't tell apart.

## For the maintainer

| Check | Pixel | Notes |
|---|---|---|
| Does switching in the info sheet feel smooth now? | ✅ | Checked by the maintainer on the Pixel, 2026-11-06 |
