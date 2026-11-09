# Tasks

## 1. Apps

- [x] 1.1 iOS: `PlaceSearchModel` passes `AppLanguage.code`. In the simulator with the app in German and the phone in English: "Nederland" reads "Niederlande" ("App language differs from the phone's", "German app").
- [x] 1.2 Android: the search passes the app's language as its texts use it. On the emulator, English app: "Köln" reads "Cologne" ("English app"); Dutch app: "Netherlands" reads "Nederland" ("Dutch app").

## 2. Release

- [x] 2.1 Changelog line, and a check on the maintainer's iPhone: a search in German reads "Köln".

## Notes

- 1.1, simulator, phone in English, app set to German, 2026-11-09: "Nederland" reads "Niederlande", "Cologne" reads "Köln".
- 1.2, emulator, 2026-11-09: English app, "Koln" (adb can't type "ö") reads "Cologne"; Dutch app, "Netherlands" reads "Nederland". Typing right after the app started, before the network was loaded, gave no hits until the next letter, on Android as before this change.
- 2.1, the maintainer, 2026-11-09: checked on the Pixel in German and the iPhone in English, "Works well".
