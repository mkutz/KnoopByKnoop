# Tasks

## 1. iOS

- [ ] 1.1 In `NetworkMapView.swift`, return a container from `makeUIView` with the `MLNMapView` and the button column as siblings. Pin the column to the container, and reach the map view through the coordinator in `updateUIView`. Verify: the app builds; in the simulator the buttons look and work as before (search, My position, zoom, Recenter while navigating).
- [ ] 1.2 Add `MapAccessibilityTest` to the UI test target, marked `// Scenario: Map buttons with VoiceOver`. It launches the app in English, opens the demo route and asserts that buttons labelled "Search places", "My position", "Zoom in" and "Zoom out" exist and are hittable. Verify: it fails before 1.1 and passes after it.
- [ ] 1.3 Check with VoiceOver on in the simulator or on the iPhone, in German too. Add "Map buttons with VoiceOver" to `openspec/checked-by-hand.md` under `map-planning`, checked this way and by the UI test. Verify: VoiceOver reads "Orte suchen", "Meine Position", "Vergrößern", "Verkleinern".

## 2. Android

- [ ] 2.1 Add "Map buttons with TalkBack" to `openspec/checked-by-hand.md` under `map-planning`, checked with `uiautomator dump`, which lists what TalkBack reads. Verify: the dump on the emulator lists the four buttons with their descriptions.

## 3. Finish

- [ ] 3.1 Add a changelog entry under Unreleased: "iPhone: VoiceOver reaches the map's buttons: search, position, nearest knots, zoom and Recenter". Run `./gradlew checkScenarios -Pchange=reach-map-buttons-with-voiceover` and `./gradlew check`. Verify: both pass.

## Workflow follow-up

- Archive with `/opsx:archive`, then resume `automate-store-screenshots` at task 4.2.
