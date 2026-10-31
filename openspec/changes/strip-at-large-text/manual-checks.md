# Manual checks

## Scenarios

Android checked 2026-10-31 on the emulator (`Medium_Phone_API_37.0`) on the round trip 93 → 29 (stop) → 94 (end) in Arnhem, positions mocked with `adb emu geo fix`, at font scales 1.0, 1.3 and 2.0.

The maintainer checked iOS on the iPhone on 2026-10-31 at the default and the largest text size: "Looks great". The overlap scenario was not seen on either platform.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Largest text size | ✅ | ✅ | Font scale 2.0: "2.2 km 17:55" below 29 and "4.0 km 18:00" below 94, complete ([screenshot](screenshots/android-navigating-2.0.png)); 1.3 ([screenshot](screenshots/android-navigating-1.3.png)) |
| Labels that would overlap at a large text size | not seen | ⏳ | On this route the labels did not overlap even at 2.0. The ranking is unchanged and works with the scaled width |
| Default text size | ✅ | ✅ | Font scale 1.0 as before ([screenshot](screenshots/android-navigating-1.0.png)) |
| Number to follow at the largest text size | ✅ | ✅ | Font scale 2.0: "At ⑨④ follow ④①" and Stop complete, "467 m" below them ([screenshot](screenshots/android-navigating-2.0.png)) |
| Number to follow at the default text size | ✅ | ✅ | Font scale 1.0: the distance in the same line, as before ([screenshot](screenshots/android-navigating-1.0.png)) |
| Route summary at the largest text size | ✅ | ✅ | Font scale 2.0: "4.1 km" and "about 15 min" complete, the actions below, all within the smallest sheet, which grew for them ([screenshot](screenshots/android-planning-2.0.png)) |
| Route summary at the default text size | ✅ | ✅ | Font scale 1.0: next to the actions in the 150 dp sheet, as before ([screenshot](screenshots/android-planning-1.0.png)) |
| Badge at the largest text size | ✅ | unchanged | Font scale 2.0: the numbers in the header, the strip, the badge row and the waypoint list are whole and centered ([navigating](screenshots/android-navigating-2.0.png), [planning](screenshots/android-planning-2.0.png)). iOS badges already use a fixed font size |

## Found and fixed

- Android: with the badge numbers' font size in dp, the numbers still sat low and were clipped at font scale 2.0, because the line height came from the text style in sp ([screenshot](screenshots/android-badges-2.0-before-line-height.png)). The line height is now in dp as well.
- Android: with the actions below the route summary at font scale 2.0, Navigate was behind the gesture bar in the 150 dp planning sheet. The smallest planning sheet now grows to show the summary and the actions, and stays 150 dp at the default size. It only follows a changed height while it is at its smallest, so a sheet pulled up stays up.
- Android: Text reports its longest word as its smallest width even without wrapping, so "4.1 km" over "about 15 min" first seemed to fit next to the actions; a route's summary is now compared in its full width.

### iOS steps for the maintainer

1. At the default text size, navigate a route with a stop and the end: the distances and arrivals below the strip look as before.
2. Set the largest text size (Settings → Accessibility → Display & Text Size → Larger Text, with Larger Accessibility Sizes on, at the largest) and navigate again: the distance and arrival below the end are complete, not cut off, and the navigation sheet is high enough for them.
3. On a route where a stop is close to the end (or at the largest size, where the labels need more room): where the stop's labels would overlap the end's, only the end's are shown.
4. At the largest size, while navigating: "At … follow …" and Stop are complete, and the distance to the next knot is in a line below them. At the default size it stays in the same line.
5. At the largest size, while planning a route: the distance and "about … min" are each in one line and complete, with the actions below them, and the smallest sheet is high enough to show the actions. At the default size the sheet looks as before. Without a route, "Tap a knot to set the start" still wraps next to the actions.
6. At the largest size, the knot badges in the header and on the strip show their numbers whole, at the same size as at the default text size.
