# Tasks

## 1. Android

- [x] 1.1 `androidx.appcompat`, `MainActivity` as `AppCompatActivity` (with an AppCompat theme parent), `AppLocalesMetadataHolderService` with `autoStoreLocales`; the language choice in `InfoSheet.kt` with `AppCompatDelegate.setApplicationLocales`. Verify on the emulator, with screenshots: "Choosing German on an English phone", "Back to the phone's language", "Kept after a restart", "Same as the system setting on Android" (`cmd locale get-app-locales`), "Names of the languages", and "Notices in the chosen language" (the logged notice language)

## 2. iOS

- [x] 2.1 `@AppStorage("language")`, the locale environment, the bundle for code texts, `AppleLanguages`; the choice in the info sheet. Verify the project builds, check in the simulator in all four languages that every text switches without a restart, install it on the maintainer's iPhone, and write the steps in `openspec/changes/add-language-setting/manual-checks.md`

- [x] 2.2 Found on the iPhone on 2026-11-03: the words on the lock screen and in the Dynamic Island stayed in the language the widget extension started with ("Outside the app"). The Live Activity's state now carries the app's language, and the extension looks its words up in it. The extension now has English strings too, as it had no `en.lproj` to look them up in. The project builds; Xcode previews show the words and Stop in the state's language in all four languages. A preview cannot reproduce an extension stuck in an old language, so that goes to the maintainer's steps.

## 3. Wrap-up

- [x] 3.1 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; the CHANGELOG's 0.6.0 section mentions the setting, and its notes stay under 500 characters

## Workflow follow-up

- The maintainer checks iOS on the iPhone before `/opsx:archive add-language-setting`.
