## 1. Apps

- [x] 1.1 Android: `startNavigation` clears `tapNote` and counts `tapNotes` on, so a note still being worked out does not appear.
- [x] 1.2 iOS: the same in `AppModel.startNavigation`.

## 2. Checks

- [x] 2.1 Entry in `openspec/checked-by-hand.md`, CHANGELOG line; `./gradlew check`, validate, `checkScenarios -Pchange=close-tap-note-on-navigate`; check on the emulator and install on both phones.
