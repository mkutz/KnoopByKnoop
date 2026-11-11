# Tasks

## 1. Android

- [ ] 1.1 Add `AppLocale` (`of` and `context`) and move `AppViewModel.text()` and its language onto it. The planner's texts stay as they are on the API 37 emulator.
- [ ] 1.2 `NavigationService` and `Speaker` take the session's language, the channel's name and description, the Stop action, "starting navigation" and the voice's locale from `AppLocale`.
- [ ] 1.3 `Navigating` carries the chosen locale; the info sheet sets it after `setApplicationLocales`. The service applies a change through the same function as `onConfigurationChanged`, which ignores a language already in use.

## 2. Checks

- [ ] 2.1 API 31 emulator (`KbK_API_31`), app set to German on the English phone, the recorded ride `siegburg-going-on` replayed with `adb emu geo fix`:
  - "Navigation on Android 12": the notification shows "Beenden" and "1,6 km", and the log shows the notices spoken in DE.
  - "Choosing a language while navigating on Android 12": start in English, choose Deutsch in the info sheet, and the notification switches at once, with nothing spoken for the change.
- [ ] 2.2 API 37 emulator: the same two checks still pass ("Words and line outside the app", "Later notices"), with the change applied only once.
- [ ] 2.3 List both scenarios in `openspec/checked-by-hand.md` with how they are checked: they need an emulator with an old Android version and cannot be unit tested.

## 3. Wrap-up

- [ ] 3.1 A changelog line under Unreleased (create the section if missing): "Android 12 and older: navigation's notification and voice follow the language chosen in the app, not the phone's." `./gradlew check` and `npx @fission-ai/openspec validate --all` pass. Finish with `./gradlew checkScenarios -Pchange=fix-navigation-language-before-android-13`.
