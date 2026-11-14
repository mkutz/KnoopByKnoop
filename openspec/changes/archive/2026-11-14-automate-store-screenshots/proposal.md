# Proposal

## Why

The store screenshots (`add-store-listings`) show times, ferry states and "open now", and these follow the device clock. Taken by hand they have to be shot on a Saturday between 10:30 and 16:30, with the map dragged into place for each of 40 shots. Taken in the evening, Navigate asks about closed ferries and the waypoint list says "Closed until tomorrow".

GitHub runners are thrown away after each run, so their clocks can be set freely. A workflow can take the same screenshots at the same moment every time, whenever it runs, and retake them after each UI change without anyone dragging maps.

## What Changes

- A new workflow, **Store screenshots**, is started by hand. It takes the five screenshots per platform in English, German, French and Dutch:
  1. the planned demo route with the panel
  2. navigating between knots 03 and 21
  3. the waypoint list
  4. a tapped place with its details
  5. the route in dark mode
- **Fixed clock:** both devices are set to the coming Saturday at 12:00 Amsterdam time, when all ferries of the demo route run. The run fails if the app shows one of them closed.
- **Android** is driven by a script over adb on a fresh emulator: open the route file, set the language, tap, send positions, take screenshots. No test code in the app.
- **iOS** gets a UI test target that does the same in an iPhone 17 Pro Max simulator. The runner's clock is set with `sudo`, and the simulator follows it.
- **The output** is one artifact with the 40 screenshots, laid out in the fastlane folders that `add-store-listings` uses, and a contact sheet for review. The workflow can't write to the repository. The maintainer reviews the screenshots, and they are committed to `main` by hand.
- **`add-store-listings` resumes** with this change's first run: its screenshot tasks 3.3–3.5 and the screenshot check (task 3.2, kept in a git stash) are done by committing that run's screenshots.

## Out of Scope

- Pushing to the stores; that's `release-store-listing.yml`.
- Scheduled runs: map tiles and network data change weekly, so the screenshots would change nearly every week.
- Captions, device frames and app preview videos.
- A clock override in the apps; the throwaway runners make it unnecessary.

## Capabilities

### New Capabilities

- `store-screenshots`: taking the store screenshots on CI at a fixed moment, in every app language, for review before they are committed.

### Modified Capabilities

None. `store-listing` is still a change of its own (`add-store-listings`) and not yet in `openspec/specs`; its requirements on screenshots stay as proposed there.

## Impact

- **New workflow:** `.github/workflows/screenshots.yml`. It needs no secrets: the builds are debug builds, and nothing is uploaded to a store.
- **New scripts:**
  - `.github/scripts/android-screenshots.sh`
  - `.github/scripts/coming-saturday.sh`, with its test
  - a contact sheet script
- **iOS:**
  - a `KnoopByKnoopScreenshots` UI test target and scheme in `apps/ios/project.yml`
  - possibly `accessibilityIdentifier`s on the controls the test taps
- **Android:** possibly `testTag`/`contentDescription`s for controls `uiautomator` can't otherwise find. These are useful for accessibility too.
- **Docs:** `CONTRIBUTING.md` "Store listing": hand-taking is replaced by the workflow.
