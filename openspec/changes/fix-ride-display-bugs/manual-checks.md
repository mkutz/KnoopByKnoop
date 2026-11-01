# Manual checks

## Scenarios

Android checked 2026-11-01 on the emulator (`Medium_Phone_API_37.0`) with the debug build, on the route 93 → 94 in Arnhem, a position mocked once a second at 6 m/s along the course with `adb emu geo fix`, recorded with `screenrecord`. The strips show eight frames a quarter of a second apart.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Steady base map while following (cause) | ✅ | ⏳ | Before: labels and icons (Burgemeester Weertsstraat, Brouwerijweg, Lidl, Montessori College) disappear and reappear from frame to frame ([strip](screenshots/android-following-before.png)). With the camera kept still and the rider's and the track's sources still updated with each fix, they stay ([strip](screenshots/android-following-without-camera.png)): the camera animation restarted with every fix is the cause, the source updates are not, so the experiment without source updates was not needed |
| Steady base map while following | ✅ | ⏳ | After: one linear move per fix lasting the fix interval (`easeCamera`, 1 s); the same labels and icons in all frames while the map moves with the rider ([strip](screenshots/android-following-after.png)) |
| Riding with the map following, Looking around, Recentering | ✅ | ⏳ | Dragging the map during the ride stops following, the recenter button shows ([screenshot](screenshots/android-looking-around.png)); tapping it brings the map back to the rider, following again ([screenshot](screenshots/android-recentered.png)) |
| Compass below the status bar | ✅ | – | A throwaway build rotating the map by 30°, as `adb input` cannot rotate with two fingers. First try at the end: below the controls in portrait ([screenshot](screenshots/android-compass-right-first-try.png)), but behind the zoom buttons in landscape ([screenshot](screenshots/android-compass-landscape-under-zoom.png)). Now at the start below the network switch, the status bar clear, in portrait ([screenshot](screenshots/android-compass.png)) and landscape ([screenshot](screenshots/android-compass-landscape.png)). iOS hides the compass |
| Off route in German | ✅ | ⏳ | Off route 587 m north of 93, the app language set with `cmd locale set-app-locales`: at the default text size the badge 93 follows the text in English and French, and goes to the next line in German and Dutch ([strip](screenshots/android-off-route-header-languages.png)); at the largest font scale (2.0) the German text takes three lines, the badge and Beenden are complete ([screenshot](screenshots/android-off-route-header-german-largest.png)). The language and the font scale were restored |
| Off route in dark mode | ✅ | ⏳ | Dark `error` `#FF5449`, contrast 5.86:1 on `surface` `#141218`, 5.38:1 on `surfaceContainerLow` `#1D1B20` (the panel), 4.52:1 on `surfaceContainerHigh` `#2B2930`; Material's default `#FFB4AB` was the pink. "Off route, next 94", the arrow and the dot on the strip are red ([screenshot](screenshots/android-off-route-dark.png)); the closed Renkumse Veer on the strip, its ring and its arrival are red too ([screenshot](screenshots/android-ferry-warning-dark.png)). The Stop button keeps its own red |

The maintainer checked all four fixes on the Pixel 7 with the debug build on 2026-11-01: "All fixes look good on the Pixel".

### iOS

Checked 2026-11-01 in the simulator (iPhone 18 Pro, iOS 27, dark appearance), German set with `defaults write com.knoopbyknoop language de`, navigation 93 → 94 started by a temporary launch flag 30 s after the route showed (removed again), the location moved off the route along a path with `simctl location start`. The first attempts, starting navigation before the courses had loaded, could not detect off route, as specified for navigation without courses; that was the simulator problem noted before.

- Found: the badge was there, but the text was cut to "Abseits der Route, n…" ([screenshot](screenshots/ios-off-route-german-truncated.png)). Fixed like Android: text and badge in one line where they fit, else the text wrapping and the badge below it.
- Found at the largest accessibility size: Stop cut to "B…", and the distance and direction to "98…" and "S…". Giving Stop priority left the header a letter wide ([screenshot](screenshots/ios-off-route-german-stop-squeezed.png)). Fixed: at accessibility sizes, off route, Stop goes in its own line above the header, which takes the full width; the direction goes below the distance where both do not fit. The number to follow is unchanged.
- Now: complete at the default size, the largest regular size and the largest accessibility size ([three sizes](screenshots/ios-off-route-german-sizes.png), [top at the largest](screenshots/ios-off-route-german-accessibility.png)); the off-route red is the system red, clear in dark mode, in the header, the arrow and the dot on the strip.

The simulator's route, camera, language, text size and location were restored.

### Steps for the maintainer

1. Pixel, on a ride: while the map follows you, street names and POI icons stay steady instead of flickering.
2. Pixel: rotate the map with two fingers: the compass shows at the top left below the search field, the battery icon is free.
3. iPhone: choose Deutsch in Info, start a route away from its start: the header shows "Abseits der Route, nächster" over two lines and the knot's badge below it, with Beenden; at the largest accessibility size Beenden is above the header, and everything is complete.
4. iPhone and Pixel in dark mode, off route: "Off route, next", the arrow and the dot on the strip are red, not pink.
