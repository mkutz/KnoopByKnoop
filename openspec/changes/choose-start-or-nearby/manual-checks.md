# Manual checks

Android on the emulator (`Medium_Phone_API_37.0`), 2026-11-11, route 06 → 02 → 28 near Siegburg, position set to Siegburg-Dreesch (50.80134, 7.21105), about 250 m east of the course of 06 → 02.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Asked near the course | ✅ | ⏳ | "Where do you want to start?" with "Start on the route nearby · 145 m" and "Ride to 06 · 4.4 km" ([screenshot](screenshots/android-asked.png)). Core: `StartChoiceTest` |
| Starting nearby | ✅ | ⏳ | A start flag on the course, "To the start, 145 m to the south-west", the way to it, 02 at 2.4 km on the strip ([screenshot](screenshots/android-started-nearby.png)). Core: `StartChoiceTest`, ride `dreesch-nearby` |
| The plan after the ride | ✅ | ⏳ | After Stop the planner shows start flag, 02, 28, 5.0 km; `route.json` starts with the route point ([screenshot](screenshots/android-plan-after.png)) |
| Question dismissed | ✅ | ⏳ | Back closes it: no navigation, the plan as it was ([screenshot](screenshots/android-dismissed.png)) |
| Riding to the start | ✅ | ⏳ | "To the start, 06, 4.4 km to the north", then the recorded ride `dreesch-to-06` replayed to fix 340: only turn notices, no "Back on route, next 02", "Wrong direction" or "Turn back now" on Hauptstraße, still 06 next at 1.4 km ([screenshot](screenshots/android-ride-to-06.png)). Core: ride `dreesch-to-06` |
| Position unknown | — | — | Not checked: the emulator always has a position. Core path: no choice without a fix |

iOS builds; the checks on the iPhone are the maintainer's.

Found on the way: the emulator's screen had gone off once, which swallowed a tap; `svc power stayon true` again.
