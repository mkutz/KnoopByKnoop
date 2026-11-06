# Tasks

## 1. Android

- [x] 1.1 `AndroidManifest.xml`: `locale|layoutDirection` in `MainActivity`'s `configChanges`; remove the `rememberSaveable` workaround for the info sheet only if it isn't needed for other restarts.
- [x] 1.2 List every string read from resources outside a composable's body (`getString`, `text(...)` in `AppViewModel`, `NavigationService`, notifications, snackbars) and make each follow the new locale. Record the list in `design.md`.
- [x] 1.3 On the emulator (Android 17), with a route planned: "Switching without a pause" (no "Skipped frames" in the log, the info sheet stays open, the map keeps its area), and "Choosing German on an English phone", "Back to the phone's language", "Kept after a restart", "Same as the system setting on Android" (changing the per-app setting while the app is open), with screenshots of the planner, info sheet, place menu, search and navigation header in all four languages.
- [x] 1.4 The same on an Android 12 emulator, where AppCompat applies the language itself.
- [x] 1.5 While navigating on the emulator: the scenarios of "Language change while navigating" again (notification words, off route, later notices), since the activity is no longer recreated.
- [x] 1.6 On the maintainer's Pixel, with their OK: eight switches with `dumpsys gfxinfo` and the log's skipped frames, compared with 2026-11-06 (110 of 543 slow, 99th percentile 500 ms, 30–38 frames skipped per switch).

## 2. Wrap-up

- [x] 2.1 CHANGELOG line, `manual-checks.md` with the results and a row for the maintainer, `./gradlew check` and `npx @fission-ai/openspec validate --all` pass.
