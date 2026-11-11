# Tasks

## 1. Check first

- [x] 1.1 A throwaway iPhone build: an alerting Live Activity update with a silent sound file, 10 s after leaving the app. Result 2026-11-08 on the maintainer's ride: the Dynamic Island expanded while unlocked, a locked screen did not light up. Led to notifications instead, see the design.
- [x] 1.2 A throwaway iPhone build: one silent local notification 10 s after locking, once. On the maintainer's iPhone: does the screen light up, is it silent, what does the Watch show? Stop and reconsider if the screen does not light up.
  - 2026-11-11: built and installed on the maintainer's iPhone, not committed. It asks for notification permission at launch. On the first trip to the background while navigating it posts "Wake check: 45 ahead, then 12" 10 s later, without sound and with interruption level active. Result from the maintainer: the locked screen lit up. The first time the notification showed on the lock screen next to the Live Activity. On a repeat it was not shown there, but could be found in the Notification Center. So 2.1 posts each notice as a new notification, after removing the previous one, instead of replacing it under the same identifier.

## 2. Apps

- [ ] 2.1 iOS: ask for notification permission when navigation starts the first time; post the silent notification for each update with notices while the app is not on screen, as a new notification after removing the previous one (a replaced one was not shown again in the check of 1.2); remove it when navigation ends; strings in all four languages. Builds with `xcodebuild`.
- [ ] 2.2 Android: the "Navigation alerts" channel without sound or vibration, the alerting post for each update with notices and back; strings in all four languages. Builds.
- [ ] 2.3 Check on the emulator with the screen off and a mocked ride: the screen turns on with each notice, not in between.

## 3. Release

- [ ] 3.1 Changelog line, and manual checks for the maintainer's ride on both phones: the screen lights up with each spoken notice, silently, not in between; and the Watch.
