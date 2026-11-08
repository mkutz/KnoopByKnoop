# Tasks

## 1. Check first

- [x] 1.1 A throwaway iPhone build: an alerting Live Activity update with a silent sound file, 10 s after leaving the app. Result 2026-11-08 on the maintainer's ride: the Dynamic Island expanded while unlocked, a locked screen did not light up. Led to notifications instead, see the design.
- [ ] 1.2 A throwaway iPhone build: one silent local notification 10 s after locking, once. On the maintainer's iPhone: does the screen light up, is it silent, what does the Watch show? Stop and reconsider if the screen does not light up.

## 2. Apps

- [ ] 2.1 iOS: ask for notification permission when navigation starts the first time; post the silent notification for each update with notices while the app is not on screen, replacing the previous one; remove it when navigation ends; strings in all four languages. Builds with `xcodebuild`.
- [ ] 2.2 Android: the "Navigation alerts" channel without sound or vibration, the alerting post for each update with notices and back; strings in all four languages. Builds.
- [ ] 2.3 Check on the emulator with the screen off and a mocked ride: the screen turns on with each notice, not in between.

## 3. Release

- [ ] 3.1 Changelog line, and manual checks for the maintainer's ride on both phones: the screen lights up with each spoken notice, silently, not in between; and the Watch.
