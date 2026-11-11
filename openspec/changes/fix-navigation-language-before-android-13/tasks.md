# Tasks

## 1. Android

- [x] 1.1 Add `AppLocale` (`of` and `context`) and move `AppViewModel.text()` and its language onto it. The planner's texts stay as they are on the API 37 emulator.
- [x] 1.2 `NavigationService` and `Speaker` take the session's language, the channel's name and description, the Stop action, "starting navigation" and the voice's locale from `AppLocale`.
- [x] 1.3 `onConfigurationChanged` switches through one function to `AppLocale.of`, which ignores a language already in use, so a change of the phone's language keeps the one chosen in the app. (First planned as a signal from the info sheet, which is hidden while navigating.)

## 2. Checks

- [x] 2.1 API 31 emulator (`KbK_API_31`), app set to German on the English phone, the recorded ride `siegburg-going-on` replayed with `adb emu geo fix`:
  - "Navigation on Android 12": the notification shows "Beenden" and "1,6 km", and the log shows the notices spoken in DE.
  - "Phone's language changed while navigating on Android 12": change the phone's language to French while navigating, and the notification and the notices stay German.
- [x] 2.2 API 37 emulator: navigation still follows a language chosen in the system's per-app setting while navigating ("Words and line outside the app", "Later notices").
- [x] 2.3 List both scenarios in `openspec/checked-by-hand.md` with how they are checked: they need an emulator with an old Android version and cannot be unit tested.

## 3. Wrap-up

- [x] 3.1 A changelog line under Unreleased (create the section if missing): "Android 12 and older: navigation's notification and voice follow the language chosen in the app, not the phone's." `./gradlew check` and `npx @fission-ai/openspec validate --all` pass. Finish with `./gradlew checkScenarios -Pchange=fix-navigation-language-before-android-13`.
