# Tasks

## 1. System bar icons

- [x] 1.1 `enableEdgeToEdge()` from a `LaunchedEffect` keyed on the appearance, so it runs at start and on every `uiMode` change; on the emulator the status bar shows dark icons on the light map and light icons in dark mode, also after `cmd uimode night yes|no` while the app is open ("Status bar on the light map", "Status bar in dark mode")

## 2. Insets

- [x] 2.1 Top controls (search field and its hits, network switch, notes) clear of the status bar and cutout with `safeDrawing` insets; on the emulator a tap 4 dp below the search field's upper edge focuses it and opens the keyboard ("Tapping the top of the search field")
- [x] 2.2 Zoom buttons, attribution and the landscape side panel clear of the cutout and the navigation bar; checked in landscape with the emulator's display cutout setting, with gesture and with three-button navigation ("Landscape with a display cutout")

## 3. Checks

- [x] 3.1 Manual checks in `manual-checks.md` with screenshots for all four scenarios, portrait and landscape, light and dark; restore the emulator's navigation mode, cutout and appearance afterwards
- [x] 3.2 `./gradlew check` passes, CI passes
