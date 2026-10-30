# Tasks

## 1. Core: one arrival line

- [ ] 1.1 `FerryArrival` with the one-line text and the state `OPEN`, `WARNING` or `UNKNOWN`, from the evaluation the warnings use; verified by `FerryWarningTest` "Arrival in the ferry list", "Arrival when the times do not tell", "Warning in the ferry list", "Closed for the season in the ferry list" and the existing warning tests, which keep the spoken texts unchanged
- [ ] 1.2 `./gradlew :modules:core:jvmTest :modules:core:iosSimulatorArm64Test` pass

## 2. Android

- [ ] 2.1 Round ⓘ button with `contentDescription` "Info" and long-press tooltip, styled like the zoom buttons, in place of the text button; verified on the emulator in planning and navigation, light and dark, with screenshots, and by the TalkBack announcement in `uiautomator dump` (content description "Info", clickable)
- [ ] 2.2 Waypoint rows 48 dp; no waypoint list while navigating; verified on the emulator: four waypoints without ferries fit the expanded panel without scrolling ("Compact waypoint list"), and the panel while navigating shows no rows ("Waypoint list while navigating"), with screenshots
- [ ] 2.3 `RouteFerries.kt` shows the arrival line in place of the status for now, colored by its state; verified on the emulator with the Renkum route ("Closed for the season in the ferry list") and a ferry passed by mocked positions ("Ferry passed in the list"), with screenshots
- [ ] 2.4 Contrast of the ⓘ icon measured in light and dark from the screenshots (at least 3:1, "Readable info button"), recorded in `manual-checks.md`

## 3. iOS

- [ ] 3.1 `info.circle` button with `accessibilityLabel("Info")` and the zoom buttons' background; compact rows (44 pt) and no waypoint list while navigating; the arrival line in `RouteFerries.swift`; the project builds with `xcodebuild`
- [ ] 3.2 Steps for the maintainer's iPhone checks (all scenarios of this change, VoiceOver included) in `manual-checks.md`

## 4. Wrap-up

- [ ] 4.1 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; CHANGELOG entry under "Unreleased"

## Workflow follow-up

- The maintainer checks iOS on the iPhone before `/opsx:archive polish-planner-panel`.
