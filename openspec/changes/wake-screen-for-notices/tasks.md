# Tasks

## 1. Check first

- [ ] 1.1 A throwaway iPhone build: an alerting Live Activity update with a silent sound file, 10 s after locking. On the maintainer's iPhone: does the screen wake, is it silent, what does the Watch show? Stop and reconsider if the screen does not wake.

## 2. Core

- [ ] 2.1 `NavigationUpdate.alerts` from the notice kinds of the design. Tests: one per notice in the requirement, and none for NEXT, BACK_ON_ROUTE and the renewed distance.

## 3. Apps

- [ ] 3.1 iOS: `Navigator` passes the `AlertConfiguration` with `silence.caf` on alerting updates, only when the app is not on screen. Builds with `xcodebuild`.
- [ ] 3.2 Android: the "Navigation alerts" channel, the alerting post and back; strings in all four languages. Builds.
- [ ] 3.3 Check on the emulator with the screen off and a mocked ride: the screen turns on at the reminder and off route, not in between.

## 4. Release

- [ ] 4.1 Changelog line, and manual checks for the maintainer's ride on both phones: the screen wakes before a knot and off route, silent, not in between; and the Watch.
