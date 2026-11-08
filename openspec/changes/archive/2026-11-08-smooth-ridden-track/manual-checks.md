# Manual checks

## Scenarios

Checked 2026-11-06. Android: emulator `Medium_Phone_API_37.0`, Android 17, with locally built data. Core: `RiddenTrackTest`.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Spike out and back | ✅ | ✅ core | Emulator: 02 → 06 in Zange, every fourth fix 60 m to the side; no spike on the track. The newest point can be a spike until the next fix |
| Standing with scattered fixes | ✅ core | ✅ core | The emulator's fixes are too precise to show it |
| Scatter while walking slowly | ✅ core | ✅ core | |
| Impossible jump | ✅ core | ✅ core | |
| Out and back on the same path | ✅ core | ✅ core | Found with the first version, which erased such a detour point by point |
| Riding fast with good fixes | ✅ core | ✅ core | |
| Detour, Inaccurate fix, Navigation ends, Screen off, Riding along the route | ✅ core | ✅ core | Unchanged behavior; existing tests pass |

## For the maintainer

| Check | Pixel | iPhone | Notes |
|---|---|---|---|
| Walk through the Siegburg old town with the screen off, then look at the track | ⏳ | ⏳ | Compare with the screenshot of 2026-11-06 17:17 |
| The direction arrow while walking slowly with the screen on | ⏳ | ⏳ | Does it fall back to a dot too often? |

## Archived

Archived on 8 November 2026 at the maintainer's choice. On the ride of 8 November the track had a 250 m straight piece at the shortcut in Siegburg and two short strokes south of 21 that may be spikes; to be looked at as a fix of its own. The walk through the Siegburg old town was not done.
