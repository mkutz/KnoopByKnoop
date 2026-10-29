# Manual checks

Emulator `Medium_Phone_API_37.0`, 2026-10-29, on the route 08 → 31 → 97 at the Kaag. Appearance switched with `adb shell cmd uimode night yes|no`, navigation mode and display cutout with the system's overlays (`cmd overlay enable-exclusive --category …navbar.threebutton|gestural`, `cmd overlay enable …cutout.emulation.tall`), all restored afterwards.

| Scenario | Android | Notes |
|---|---|---|
| Status bar on the light map | ✅ | Clock and icons dark above the light map ([screenshot](screenshots/android-top-light.png)); before, they were white and hardly visible |
| Status bar in dark mode | ✅ | Light icons when started in dark mode, and after switching to dark while the app is open ([screenshot](screenshots/android-top-dark.png)); back to dark icons after switching to light |
| Tapping the top of the search field | ✅ | The search field now starts below the status bar (y = 95 px instead of reaching into it); `adb shell input tap` 4 dp below its upper edge focuses it and opens the keyboard (`mInputShown=true`) |
| Landscape with a display cutout | ✅ | Tall cutout on the left: search field and network switch clear of it; with gestures, the attribution above the home bar; with three buttons, the side panel clear of the buttons, its background behind them, and the header still one row ([screenshots](screenshots/android-landscape-cutout.png): gestures, three buttons; [dark](screenshots/android-landscape-dark.png)) |

## Found and fixed

- **Android: header in two rows in landscape with three-button navigation.** The side panel at first took the navigation bar's width from its 360 dp, so "5.2 km" wrapped. The panel now keeps 360 dp for its content and extends behind the navigation bar.
- **Android: icons dark after switching to dark mode, in a first attempt.** With `enableEdgeToEdge()`'s automatic detection, a screenshot 6 s after switching still showed dark icons; it may have been taken too early. The appearance is now passed from the new configuration, and the light icons after switching are confirmed.
