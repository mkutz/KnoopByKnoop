## 1. Android

- [x] 1.1 Navigate as Material's floating action button: default shape and 56 dp size in `NavigateFab`, without `CircleShape`, the fixed size and the white border; `fabRoom` from the new size. Check on the emulator: "Navigate stands out", "Above Navigate on Android", "Note on the panel's edge".
- [x] 1.2 Stop in the same slot: one floating button composable showing Navigate while planning and Stop (red, stop icon, named "Stop", tooltip on a long press) while navigating in portrait; `buttonsBottom` keeps its room while navigating; `NavigationHeader` without Stop in portrait, with Stop in landscape. Check on the emulator: "Stop stands out", "Stop in every language" (German), "Stop with a screen reader" (TalkBack), "Above Stop on Android", "Stop in landscape on Android", and the header at the largest text size in German.
- [x] 1.3 The hold button over the lock screen as the outlined rounded square in the floating button's place, the ring drawn along its outline by length. Check on the emulator: "Before touching the stop button", "A touch on the stop button", "Letting go early", "Without animations", "Holding the stop button", "Stop with a screen reader", "Unlocking".

## 2. Checks

- [x] 2.1 Update `openspec/checked-by-hand.md` for the changed and new scenarios ("Above Stop on Android", "Stop in landscape on Android"), and add a CHANGELOG Unreleased line.
- [x] 2.2 Run `./gradlew check` and `npx @fission-ai/openspec validate --all`, then `./gradlew checkScenarios -Pchange=material-navigate-stop-buttons`.
- [ ] 2.3 The maintainer rides with it on the Pixel: reaching Stop, stopping by accident, the map covered in the lower right.
