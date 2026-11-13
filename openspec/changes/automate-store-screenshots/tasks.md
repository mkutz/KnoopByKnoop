# Tasks

## 1. The fixed moment

- [x] 1.1 Write `.github/scripts/coming-saturday.sh [now-epoch]`. It prints the epoch seconds of the next Saturday 12:00 Europe/Amsterdam strictly after now, using `python3` with `zoneinfo` so it is the same on Linux and macOS. Verify: task 1.2.
- [x] 1.2 Add `.github/scripts/coming-saturday-test.sh` with tests for "Coming Saturday", "Run on a Saturday afternoon" and "Summer time", each marked `# Scenario:`. Run it from `build.yml`. Verify: `sh .github/scripts/coming-saturday-test.sh` passes.

## 2. Positions and the place

- [ ] 2.1 With a throwaway core test on the published data (deleted afterwards), find the following, and write them as constants in a shared `fastlane/screenshots/demo.env`, read by both scripts:
  - two points 30 m apart on the course between 03 and 21
  - a highlighted place near the route with opening hours that are open on Saturday at 12:00 (a campsite or café near the windmills), with its name and position

  Verify: the points lie on the course, and the place's hours say open at that moment.

## 3. Android

- [ ] 3.1 Add `contentDescription`s to the Android controls the script must tap that have no text, in all four languages. Candidates are the panel handle and Navigate. Verify: `uiautomator dump` on the emulator lists them, and `./gradlew :apps:android:assembleDebug` passes.
- [ ] 3.2 Write `.github/scripts/android-screenshots.sh <out-dir>` as in the design. For each language it:
  - uses System UI demo mode
  - clears the app's data, grants permissions and sets the language
  - copies the route in with `run-as` and opens it
  - waits for the tiles
  - takes the five shots, failing with the question's text if Navigate asks about a closed ferry

  Verify: on the local emulator, with its clock set to a Saturday at 12:00 before the app starts, it writes 20 screenshots of 1080 × 1920 in the fastlane layout, in the right languages.
- [ ] 3.3 Run the script on the local emulator with the clock on a Sunday at 12:00, when the Lekkerkerk ferry doesn't run. Add "Closed ferry" to `openspec/checked-by-hand.md` under `store-screenshots`, checked this way. Verify: the run fails naming Lekkerkerk – Nieuw Lekkerland.

## 4. iOS

- [ ] 4.1 Add the `KnoopByKnoopScreenshots` UI test target and its scheme to `apps/ios/project.yml`, and `accessibilityIdentifier`s where a control has no unique label. Verify: `xcodegen` and `xcodebuild build-for-testing` for the simulator pass.
- [ ] 4.2 Write the UI test:
  - one method for shots 1–4 and one for shot 5
  - the language from `SCREENSHOT_LANGUAGE`
  - the route opened by URL, positions through `XCUIDevice.shared.location`
  - screenshots as kept attachments
  - failing with the question's text on a closed-ferry question

  Verify: run locally in an iPhone 17 Pro Max simulator for German while it is daytime, then extract the attachments. You get five screenshots of 1320 × 2868 with German texts.

## 5. Workflow

- [ ] 5.1 Add `.github/workflows/screenshots.yml` as in the design:
  - three jobs: android, ios, sheet
  - `permissions: contents: read`, no secrets
  - clocks set from `coming-saturday.sh`
  - one artifact with the fastlane layout and `contact-sheet.png`

  Add `.github/scripts/screenshots-workflow-test.sh`, marked `# Scenario: No write access`. It fails if the workflow asks for more than read permissions or uses secrets. Run it from `build.yml`. Verify: actionlint and the test pass.
- [ ] 5.2 Add "Screenshots for review" and "Screenshot in the wrong language" to `openspec/checked-by-hand.md`, checked by a run of the workflow. Verify: `./gradlew checkScenarios -Pchange=automate-store-screenshots` passes.
- [ ] 5.3 Push and run the workflow. Verify:
  - The artifact has 40 screenshots in the fastlane layout, and the contact sheet shows them.
  - The status bars show 12:00, and the ferries show as open.
  - The maintainer reviews the contact sheet.

## 6. Hand-over to add-store-listings

- [ ] 6.1 After the maintainer's OK:
  - copy the artifact's screenshots into `fastlane/`
  - remove `design/store/screenshots`
  - `git stash pop` the screenshot check from `add-store-listings`
  - commit them together, and tick `add-store-listings` tasks 3.2–3.5

  Verify: `sh .github/scripts/store-listing-check-test.sh` and `sh .github/scripts/store-listing-check.sh` pass.
- [ ] 6.2 Replace the hand-taking steps in `CONTRIBUTING.md` "Store listing" with "start the workflow Store screenshots, review the contact sheet, commit the screenshots". Verify: the section names the workflow and the review.
- [ ] 6.3 Run `./gradlew checkScenarios -Pchange=automate-store-screenshots` and `./gradlew check`. Verify: both pass.

## Workflow follow-up

- Archive this change with `/opsx:archive`, then resume `add-store-listings` at task 4.3 (the first listing push).
