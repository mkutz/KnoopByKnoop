# Manual checks

Emulator `Medium_Phone_API_37.0`, 2026-10-29, on the route 08 → 31 → 97 at the Kaag. Appearance switched with `adb shell cmd uimode night yes|no`, navigation mode and display cutout with the system's overlays (`cmd overlay enable-exclusive --category …navbar.threebutton|gestural`, `cmd overlay enable …cutout.emulation.tall`), all restored afterwards.

| Scenario | Android | Notes |
|---|---|---|
| Status bar on the light map | ✅ | Clock and icons dark above the light map ([screenshot](screenshots/android-top-light.png)); before, they were white and hardly visible |
| Status bar in dark mode | ✅ | Light icons when started in dark mode ([screenshot](screenshots/android-top-dark.png)), dark icons after switching to light ([screenshot](screenshots/android-top-light.png)); three rounds of switching dark and light while the app is open, each followed by the bar icons |
| Tapping the top of the search field | ✅ | The search field now starts below the status bar (y = 95 px instead of reaching into it); `adb shell input tap` 4 dp below its upper edge focuses it and opens the keyboard (`mInputShown=true`) |
| Landscape with a display cutout | ✅ | Tall cutout on the left: search field and network switch clear of it; with gestures, the attribution above the home bar; with three buttons, the side panel clear of the buttons, its background behind them, and the header still one row ([screenshots](screenshots/android-landscape-cutout.png): gestures, three buttons; [dark](screenshots/android-landscape-dark.png)) |

## Found and fixed

- **Android: header in two rows in landscape with three-button navigation.** The side panel at first took the navigation bar's width from its 360 dp, so "5.2 km" wrapped. The panel now keeps 360 dp for its content and extends behind the navigation bar.
- **Android: bar icons did not always follow a switch while the app is open.** Set in `onConfigurationChanged`, the app's window had the right appearance, but the system bars kept the old icons until the window was laid out again, e.g. after the screen woke up; one check that looked fine had been after such a wake-up. The icons are now set from the UI once it shows the new appearance, and three rounds of switching were followed each time.
- **Emulator: status bar twice as high after the cutout check.** After the "tall" cutout was turned off again, the status bar kept its height (126 instead of 63 px) until the emulator restarted, which made the search field look too low in a first dark screenshot. The screenshots here are from after the restart.
