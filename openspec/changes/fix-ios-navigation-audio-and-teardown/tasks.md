# Tasks

## 1. Music back up after a notice

- [x] 1.1 `Navigator`: count pending utterances, deactivate the audio session with `.notifyOthersOnDeactivation` when the count reaches 0, log failures and retry once after 0.5 s; `stop()` stops the synthesizer; both iOS targets build ("Music playing")

## 2. Closing the app ends navigation

- [x] 2.1 `AppDelegate.applicationWillTerminate` calls `Navigator.shared.stop()` and waits up to 1 s for the Live Activity to end; the start-time cleanup stays; both iOS targets build ("Closing the app while navigating")
- [x] 2.2 The start-time cleanup only after a navigation the app did not end (a flag in `UserDefaults`), ending its session after 1 s; both iOS targets build ("No location use after navigation")

## 3. Checks

- [ ] 3.1 Manual checks in `manual-checks.md` by the maintainer on the iPhone: "Music playing" (music on, Navigate, the first notice lowers the music, and it is back to full volume right after); "Closing the app while navigating" (swipe the app away while navigating: the indicator and the Live Activity go away right then, and the route is there at the next start); "Stopping in the app" still works
- [x] 3.2 Both iOS targets build, `./gradlew check` passes, CI passes
