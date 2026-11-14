# Tasks

## 1. Keep typed text

- [x] 1.1 Add `fieldAfterQuery` to `PlaceSearchField.kt`, and use it with a remembered set of sent texts in place of the plain comparison in the `LaunchedEffect`. Clear also empties the field directly. Verify: `./gradlew :apps:android:assembleDebug` passes.
- [x] 1.2 Add `PlaceSearchFieldTest` in `apps/android/src/test` with tests for "Earlier text echoed late" and "Cleared from the app", each named after its scenario. Verify: `./gradlew :apps:android:testDebugUnitTest --tests '*PlaceSearchFieldTest*'` passes.
- [x] 1.3 On the emulator, open the search and type "Blue Lotus Nieuw-Lekkerland" in one `adb shell input text`, then dump the field. Add "Typing fast" to `openspec/checked-by-hand.md` under `place-search`, checked this way. Verify: the field holds the whole text in English and German.
- [x] 1.4 Add a changelog entry under Unreleased: "Android: typing fast into the search no longer loses letters". Verify: `grep -n 'loses letters' CHANGELOG.md`.

## 2. Finish

- [x] 2.1 Run `./gradlew checkScenarios -Pchange=keep-typed-search-text` and `./gradlew check`. Verify: both pass.

## Workflow follow-up

- Archive with `/opsx:archive`, then resume `automate-store-screenshots`: screenshot 4 by search again, one search per language submitted with Enter.
