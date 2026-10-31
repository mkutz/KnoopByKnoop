# Manual checks

## Scenarios

The maintainer checked iOS on the iPhone on 2026-10-31, navigating off route after the dimming fix: "Looks great now". Landscape, with the zoom fix, checked on the iPhone too: "Works in landscape now". The other iOS rows were checked on the iPhone afterwards, as were the music getting quieter during spoken notices and coming back after them, with the audio session off the main thread.

Android checked 2026-10-31 on the emulator (`Medium_Phone_API_37.0`), positions mocked with `adb emu geo fix` in Arnhem on the round trip 93 → 29 (stop) → 94 (end). The emulator had no courses, so off route could not be triggered there.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Starting with the smallest panel | ✅ | ✅ | "At ⑨④ follow ④① 573 m", the strip with "2.3 km 17:15" below 29 and "4.1 km 17:25" below 94, clear of the gesture bar ([screenshot](screenshots/android-start-from-smallest.png)) |
| Starting with a high panel | ✅ | ✅ | The planning sheet dragged up to the waypoint list; Navigate brings it down to the header and strip ([screenshot](screenshots/android-start-from-raised.png)) |
| Dragging the panel | ✅ | ✅ | No handle while navigating; dragging down or up moves the map, not the sheet ([screenshot](screenshots/android-dragging.png)) |
| Ending navigation | ✅ (Stop) | ✅ | After Stop, back to the 150 dp planning sheet with the handle ([screenshot](screenshots/android-after-stop.png)). Arriving ends navigation through the same state change as Stop |
| Navigating in landscape | ✅ | ✅ | Side panel unchanged ([screenshot](screenshots/android-landscape.png)) |
| Larger font sizes | ✅ | ✅ | Font scale 1.3 fits; switching to 1.0 while navigating brings the sheet down with the header ([screenshot](screenshots/android-font-scale-1.3-to-1.0.png)). At 2.0 the sheet grows, but the strip's labels are clipped in their fixed 64 × 40 dp boxes ([screenshot](screenshots/android-font-scale-2.0.png)). That is in the strip itself and was there before this change |

## Found and fixed

- iOS: while navigating, the map behind the sheet was dimmed and its controls greyed out, because background interaction was only enabled up through `.medium`, which is not a detent while navigating. It is now enabled up through the navigation detent (iPhone, first round).
- Android: after the header's height changed while navigating (here: the font size), the map reserved the new height, but the sheet stayed at its old position, with a gap above it and the arrivals cut off. The Material 3 sheet doesn't follow a changed peek height by itself, so the app now brings it to the peek height again whenever that height changes.
- iOS, found during these checks but not caused by this change: in landscape, off route, the map zoomed out to the whole region to show the rider and the next knot (`knot-navigation` "Both in view"). The fit used portrait margins (120 pt top, 260 pt bottom), which left almost no room in the low landscape map; fitting the route while planning had the same problem. The margins now depend on the layout. Android fits with an even 160 px margin and was not affected.

### iOS steps for the maintainer

1. Plan a route with a stop and the end, leave the sheet at its smallest height, and tap Navigate. Once the position is there, the sheet shows "At … follow …" and the whole strip with the distance and arrival below the stop and the end, without dragging. Nothing is cut off at the bottom and nothing hides behind the home indicator. The place search sits right above the sheet. The map is not dimmed, and it and its zoom buttons respond to touches.
2. Drag the sheet down and up: it stays put, and there is no grabber.
3. Tap Stop: the sheet goes back to its smallest planning height with the grabber, and can be dragged to half and full height again.
4. Drag the planning sheet to half the screen and tap Navigate: the sheet comes down to the header and strip.
5. Ride or walk to the end (or a short route): after "You've arrived", the sheet is back at its smallest planning height.
6. Set a large text size (Settings → Display & Brightness → Text Size, or Accessibility → Larger Text) and navigate: the sheet grows with the header and strip.
7. While navigating, step off the route: the "Off route" header changes height, and the sheet follows without a gap above it.
8. Landscape while navigating: the side panel is unchanged. Off route, the map shows the rider and the next knot at a close zoom, not the whole region. While planning, the route fits the map in landscape too.
