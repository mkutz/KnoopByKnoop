# Tasks

## 1. Core

- [x] 1.1 Test in `StartChoiceTest`: `Rough position at the start`, a position 70 m from 06 in each direction gives no choice, as the 200 m rule already rules it out.

## 2. iOS

- [x] 2.1 `PositionFinder.find` with `accuracy` and `maxAge`; a recent fix of the manager is used at once, otherwise it requests at that accuracy and resets to best after. "Show my position" keeps today's call.
- [x] 2.2 `AppModel.startNavigation`: `findingStart` set while finding, the fix's accuracy passed to `StartChoice.of`, a second tap ignored. `NavigateIconAction` shows the spinner with the label "Finding your position". Strings in de, nl, fr.

## 3. Android

- [x] 3.1 `currentLocation(request)` with a `CurrentLocationRequest`; `startNavigation` uses balanced power accuracy, 60 s max age, 10 s duration, and ignores fixes over 100 m. Others keep high accuracy.
- [x] 3.2 `AppState.findingStart`; `NavigateFab` and `NavigateButton` show a spinner and ignore taps while set, with the content description "Finding your position". Strings in four languages.

## 4. Checks

- [x] 4.1 List in `openspec/checked-by-hand.md`, with how they're checked: `Recent position` and `Position too old or too rough` (on the devices: Navigate right after "Show my position", and indoors after a while), and `Waiting for the position` (on the devices indoors, with TalkBack/VoiceOver). Add a CHANGELOG Unreleased line.
- [ ] 4.2 Run `./gradlew check`, `npx @fission-ai/openspec validate --all` and `./gradlew checkScenarios -Pchange=quicker-navigation-start`. Install on both phones; the maintainer tries Navigate indoors and outdoors.
