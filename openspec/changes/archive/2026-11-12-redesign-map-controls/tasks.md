## 1. Android

- [x] 1.1 `NetworkMap`: draw the buttons as group cards (search; position + nearest knots; zoom + and −) in a column aligned above a `bottomInset`, dropping the zoom then the position group when there is no room. Recenter stays while navigating.
- [x] 1.2 `PlannerScreen`: pass the sheet's live top edge (plus the FAB) as the inset. `NavigateButton` becomes a round FAB at the sheet's top right (Stop in the same place). Replace `MoreActions` by the action row below the knots. Make the gear a separate round button at the top right. Landscape: Navigate round at the end of the side panel's header, and the column at the bottom right of the map. Strings: "Settings". Check on the emulator in portrait and landscape, light and dark, with TalkBack.
- [x] 1.3 A `MapGlass` surface for everything over the map (network switch, gear, button groups, the tap note, the place card, the Navigate FAB, the loading notes). The tap note stands on the sheet's edge and the FAB and the column on top of it.

## 2. iOS

- [x] 2.1 `NetworkMapView`: group the UIKit buttons into glass stacks, positioned from a bottom inset that `PlannerView` feeds from the live sheet height, dropping groups when there is no room.
- [x] 2.2 `PlannerView`: Navigate as a round prominent glass button in the header, the action row instead of the More actions menu, and the gear as a separate glass circle at the top right. Landscape as on Android. Check in the simulator in portrait and landscape, light and dark, with VoiceOver.
- [x] 2.3 `floatingBackground` for the place card (instead of `.regularMaterial`), the loading notes and the gear. The tap note stands on the sheet's edge and the column on top of it.

## 3. Checks

- [x] 3.1 Add the new layout scenarios to `openspec/checked-by-hand.md` (all of "Map buttons in groups", `Zoom buttons as one control`, `Navigate in landscape`). Update the entries of the reworded ones (`Navigate stands out`, `Icon buttons with a screen reader`, `Info button`, …). Add a CHANGELOG Unreleased line. Run `./gradlew check`, `npx @fission-ai/openspec validate --all` and `./gradlew checkScenarios -Pchange=redesign-map-controls`. Install on both phones for the maintainer's look.

## Workflow follow-up

- The maintainer checks it on the phones, then archive.
