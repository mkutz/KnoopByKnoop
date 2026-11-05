# Tasks

The code exists as an uncommitted test build. Each task checks it against the scenarios and removes what is only for testing.

## 1. Android

- [x] 1.1 `MainActivity`: `setShowWhenLocked` follows navigating, with the API 27 guard; the lock check once a second while started; no mention of a test build left. On the API 37 emulator with a PIN and a mocked ride, with screenshots: "Waking the phone on the ride", "Not navigating", "Another app on top", "Unlocking".
- [x] 1.2 `PlannerScreen`: the place search hidden over the lock screen. Checks: "The place search", and that nothing there opens sharing, export, the info sheet or planning without unlocking.
- [ ] 1.3 `HoldAction` with its own timed gesture, the fill and the screen reader's action; "Hold to stop" in all app languages. Checks on the emulator, finding the button from a screenshot before each press: "Before touching the stop button", "A touch on the stop button" several times, "Letting go early", "Holding the stop button" with a screenshot mid-hold, "Without animations" with the animator scale at 0, and "Stop with a screen reader" with TalkBack.
- [x] 1.4 Display: not kept on over the lock screen. Checks by `mHoldScreenWindow` and by waiting for the timeout: "Over the lock screen", "Coming back to the app", and the existing "Navigating with the app open", "Turning the display off", "Leaving the app", "Navigation ends", "Planning".
- [x] 1.5 "The notification": flicking up shows it and its Stop works, on the emulator with a screenshot.
- [ ] 1.6 "Audio playing" with media playing on the emulator, and on the maintainer's Pixel by their look.

## 2. Wrap-up

- [ ] 2.1 The maintainer's ride with the phone locked and a podcast on: the scenarios above on the road, whether a pocket touch does anything, the battery against "Battery use while navigating", and the unexplained stop of the design.
- [x] 2.2 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased.
