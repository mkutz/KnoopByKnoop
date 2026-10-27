# Manual checks

Checked 2026-10-27 with release builds on the Android emulator (`Medium_Phone_API_37.0`). iOS needs taps and airplane mode, so it is checked by the maintainer on an iPhone (⏳ until then).

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Normal start | ✅ | ⏳ | Splash screen, then the map with the note "Loading the cycling knot network…", then knots, straight lines and courses; no screen without a map ([frames, 4 per second](screenshots/android-normal-start-frames.png)) |
| Planning before the courses are shown | ✅ | ⏳ | During a normal start the courses follow the knots within about 0.1 s, too fast to tap; checked on a first start in airplane mode instead: knots 10 and 26 tapped, route planned with straight lines ([screenshot](screenshots/android-planned-before-courses.png)) |
| Courses do not move the map | ✅ | ⏳ | Going online, the courses and the base map were added by themselves; route, position and zoom unchanged, the knots stay at the same place on the screen ([screenshot](screenshots/android-courses-added.png)) |
