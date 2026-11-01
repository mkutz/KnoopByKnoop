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

Not checked yet. In the simulator (iPhone 18 Pro, iOS 27), with the route 93 → 94, German set with `defaults write com.knoopbyknoop language de` and navigation started by a temporary launch flag (removed again), navigation started ("94 ist das Ende 573 m"), but moving the simulated location 590 m away, by a jump or along a path with `simctl location start`, never made it off route: the panel kept counting from 93 while the map showed the arrow 590 m north. Off route works on the iPhone (`show-route-progress`), so the simulator's fixes are likely not precise enough for navigation; this was not pursued further. The simulator's route, camera, language and location were restored.

### Steps for the maintainer

1. Pixel, on a ride: while the map follows you, street names and POI icons stay steady instead of flickering.
2. Pixel: rotate the map with two fingers: the compass shows at the top left below the search field, the battery icon is free.
3. iPhone: choose Deutsch in Info, start a route away from its start: the header shows "Abseits der Route, nächster" and the knot's badge (next to it or in the next line), also at a large text size (Settings → Display & Brightness → Text Size).
4. iPhone and Pixel in dark mode, off route: "Off route, next", the arrow and the dot on the strip are red, not pink.
