# Manual checks

## Scenarios

Checked 2026-11-05 with the published path file of 3 November, 102.7 MB, on a build of committed code plus these changes only. Android: emulator `Medium_Phone_API_37.0`, Android 17. iOS: iPhone 18 Pro Max simulator, iOS 27. Route 02 → 80 from Siegburg, 13 km, positions mocked along the way found, in German.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Off by default | ✅ | ✅ | No path files and no status in the panel. Network traffic itself wasn't watched; `PathStoreTest` covers that nothing is loaded |
| Reading the explanation | ✅ | ✅ | German on both; the maintainer finds it reads well on the iPhone. Dutch and French not looked at |
| Turning it on with a route | ✅ | ✅ | By touch. iOS: 15 squares, 1.4 MB, within 4 s; Android 1.5 MB within 14 s |
| Kept across starts | ✅ | ✅ | Status "loaded" after a restart |
| Turning it off | ✅ | ✅ | Only the empty list of squares stays, no status in the panel |
| Opening the info sheet with path data | ✅ | ✅ | "Daten vom 2026-11-03" |
| Without network access | ✅ | – | Restart with network off: status still "loaded", way of 1,011 m found. The simulator can't go offline from here |
| Starting away from the route | ✅ | ✅ | Off route at home, way to the signpost |
| Line along the guided way | ✅ | ✅ | Solid red, no dashed line |
| In the header, On the lock screen | ✅ | header ✅ | Android notification: "Rechts in 30 m · 680 m". iOS lock screen below |
| Turn notices | ✅ | ✅ | In the log, with "In 70 Metern scharf links" as speech. Not listened to |
| Beyond the path data, Back to the dashed line | ✅ | ✅ | 15 km from 02: dashed line, "15 km, Nord" |
| No way, no path data, far away | ✅ | ✅ | 23 km away: "24 km Richtung Nordost bis 02" |
| Reaching the path data | ✅ | ✅ | Back near the route: a new way within 10 s |
| Back on route with a guided way | ✅ | ✅ | "Zurück auf der Route", then "Nächster 28, dann 27" |
| Screen off | ✅ | unit test | Precision only changes with the app being visible |
| Switching between street and path | ✅ | – | Emulator, 2026-11-05, build of 1c1cdf5, German, four mocked rides off route towards 02 in Siegburg: "In 100 Metern auf den Radweg wechseln", "In 70 Metern links abbiegen, auf die Straße", "Jetzt rechts abbiegen, auf den Radweg". Across the four rides the switch added one notice of its own; the others were turns said before, now with the switch. The one way through the Grimmelsgasse roundabout turned right there, so the ride's "geradeaus" wasn't reproduced. In the town centre the guided way has about 7 notices per km, from its turns rather than from switches. For the maintainer: the roundabout again, see the ride sheet |
| Not enough memory | ✅ | – | Forcing the error: no crash, straight line. iOS has no such error to catch |

Everything else in the delta specs is covered by `PathStoreTest`, `PathRouterTest`, `GuidedWayTest`, `WayGuideTest`, `NetworkIndexTest`, `PathExtractionTest` and `MainTest`.

## Measured

Pixel 7, release settings, a 12.5 km way, the whole area's file: 242 to 257 ms with the screen on; 992 to 1,080 ms first and 397 to 466 ms after that with the screen off, at foreground priority as an approximation of the service; 21 MB in use after collecting, 58 MB peak. Along routes of about 250 km: at most 87 MB kept. The iPhone is still to do, see task 2.3.

## Workflow

Run for real on 2026-11-05: 14 minutes for the build and 1.3 for publishing. Range requests give 206 and the directory's checksum matches the index. The failure paths weren't run.

## For the maintainer

1. A real ride from home to the first knot, with the phone locked, on both phones: turn the switch on, plan, wait for "Wege für diese Route sind geladen", then ride.
2. A wrong turn on purpose: the way, "Umkehren" when it starts behind you, and the notices by ear.
3. Flight mode with the squares in, on the iPhone especially, which is untested offline.
4. A long route on mobile data: how long until it says loaded.
5. Battery on the ride, against "Battery use while navigating".
6. iPhone lock screen and Dynamic Island on a guided way.

## Store forms, for the maintainer to change

Only with the switch on, and only as the rough area of a planned route, with no position. Whether that counts as location data is your call; this is how I would answer, to be on the safe side, not legal advice:

- **Play data safety:** Approximate location, shared with GitHub, optional for the user, for app functionality, not used for tracking, encrypted in transit.
- **App Store privacy labels:** Coarse Location, for app functionality, not linked to the user, not used for tracking.
