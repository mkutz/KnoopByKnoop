# Tasks

## 1. Android

- [x] 1.1 `HoldAction` as a 52 dp round button: red outline and icon, a red ring growing around it while held, the same gesture timing and semantics; a tap shows the "Hold to stop" tooltip above it. `NavigationHeader` uses it over the lock screen. Builds; `./gradlew check` passes.
- [x] 1.2 On the emulator, over the lock screen while navigating, with screenshots: "Before touching the stop button" (round, outlined, the size of the Stop in the app), "A touch on the stop button" (the hint shows, the ring fills a little, navigation continues), "Letting go early", "Holding the stop button" (the ring closes, navigation stops), "Without animations" (animations off: a tap doesn't stop, a hold does), and TalkBack for "Stop with a screen reader".

## 2. Release

- [ ] 2.1 Changelog line, and a manual check on the Pixel on a ride: wake the phone, tap the button for the hint, hold it to stop.
