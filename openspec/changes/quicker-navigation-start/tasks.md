# Tasks

## 1. Core

- [ ] 1.1 `StartChoice.of` takes `accuracyMeters` (default 0) and widens `AT_START_METERS` by it. Tests in `StartChoiceTest`: `Rough position at the start` (70 m from 06, 80 m accuracy: no choice), and that the existing scenarios still hold with accuracy 0.

## 2. iOS

- [ ] 2.1 `PositionFinder.find` with `accuracy` and `maxAge`; a recent fix of the manager is used at once, otherwise it requests at that accuracy and resets to best after. "Show my position" keeps today's call.
- [ ] 2.2 `AppModel.startNavigation`: `findingStart` set while finding, the fix's accuracy passed to `StartChoice.of`, a second tap ignored. `NavigateIconAction` shows the spinner with the label "Finding your position". Strings in de, nl, fr.

## 3. Android

- [ ] 3.1 `currentLocation(request)` with a `CurrentLocationRequest`; `startNavigation` uses balanced power accuracy, 60 s max age, 10 s duration, and ignores fixes over 100 m. Others keep high accuracy.
- [ ] 3.2 `AppState.findingStart`; `NavigateFab` and `NavigateButton` show a spinner and ignore taps while set, with the content description "Finding your position". Strings in four languages.

## 4. Checks

- [ ] 4.1 List in `openspec/checked-by-hand.md`, with how they're checked: `Recent position` and `Position too old or too rough` (on the devices: Navigate right after "Show my position", and indoors after a while), and `Waiting for the position` (on the devices indoors, with TalkBack/VoiceOver). Add a CHANGELOG Unreleased line.
- [ ] 4.2 Run `./gradlew check`, `npx @fission-ai/openspec validate --all` and `./gradlew checkScenarios -Pchange=quicker-navigation-start`. Install on both phones; the maintainer tries Navigate indoors and outdoors.
