# Design

## Context

- **`add-store-listings` (paused at 11/19)** set up:
  - the fastlane layout
  - the demo route: a 12 km Kinderdijk loop, 14 → ferry → 31 → 03 → 21 → 04 → ferry → 62 → 63 → 61 → ferry → 14
  - the hand-taking steps in `CONTRIBUTING.md`
- **What hand-taking showed on 2026-11-13:**
  - The emulator's fused location follows `adb emu geo fix`.
  - The app reads a file opened from adb only from its own storage (`run-as`).
  - The route's auto-fit leaves knot 04 just left of the zoom buttons.
  - In the evening, Navigate asks about closed ferries first.
- **Ferry hours relevant to the route:**
  - Kinderdijk – Krimpen aan de Lek: Mo–Sa 06:00–24:00
  - Lekkerkerk – Nieuw-Lekkerland: Sa 09:00–19:00, closed on Sundays
  - Driehoeksveer Ridderkerk – Krimpen – Kinderdijk: Sa–Su 10:30–16:30, Mo–Fr with gaps, Dec 22–Jan 01 off

  Saturday 12:00 is inside every window, with time to spare.
- **Existing CI and setup:**
  - `build.yml` already builds the iOS app on `macos-latest` and Android on `ubuntu-latest`.
  - Neither app has UI test code yet.
  - The Android app reads positions from the fused provider.
  - The iOS app opens route files through `onOpenURL`.
  - The appearance setting defaults to following the system.

## Goals / Non-Goals

**Goals:**
- The same 40 screenshots whenever the workflow runs, without anyone dragging the map
- Real UI only: the apps get no screenshot mode

**Non-Goals:**
- Pixel-identical runs: map tiles and network data change; the review catches what matters.
- Screenshots on real devices

## Decisions

### Workflow shape

`.github/workflows/screenshots.yml`:
- runs by hand (`workflow_dispatch`, any branch)
- `permissions: contents: read`
- no secrets

It has three jobs:

1. **android** on `ubuntu-latest`:
   - Builds `assembleDebug`. Debug builds allow `run-as` and carry the same UI as releases.
   - Boots an API 36 Google APIs emulator with `reactivecircus/android-emulator-runner` (KVM), with a 1080 × 1920 screen at density 420.
   - Runs `android-screenshots.sh`.
2. **ios** on `macos-latest`:
   - Builds the app and the UI test target for testing, before the clock moves.
   - Creates an iPhone 17 Pro Max simulator and sets the clock.
   - Runs the UI test once per language.
   - Copies the attachments from the `.xcresult` out with `xcrun xcresulttool`.
3. **sheet** on `ubuntu-latest`:
   - Collects both artifacts into the fastlane layout.
   - Makes `contact-sheet.png` with ImageMagick `montage`: one row per platform and language, five columns.
   - Uploads one artifact, `store-screenshots-<run>`.

**Rejected:**
- fastlane `screengrab`/`snapshot` for Android: that needs instrumented tests in the app, and mocking fused locations from inside a test is unreliable. adb from outside already worked by hand.
- A pull request with the screenshots: the maintainer works on `main` and chose an artifact.

### The fixed moment

`.github/scripts/coming-saturday.sh [now-epoch]` prints the epoch of the next Saturday 12:00 Europe/Amsterdam strictly after now. It uses `TZ=Europe/Amsterdam date`, with GNU and BSD variants or `python3 zoneinfo`, so it runs on both runners. Its test covers the spec's scenarios.

- **A moment ahead, not behind:** certificates of GitHub, OpenFreeMap and Photon are valid from their issue date. A clock up to 7 days ahead stays inside their validity, while one behind can fall before a just-renewed certificate.
- **Android:**
  - `adb shell settings put global auto_time 0`
  - `adb shell cmd alarm set-time <ms>`
  - `adb shell cmd alarm set-timezone Europe/Amsterdam`

  The time is set before the app first starts. The hang seen on 2026-11-12 came from a jump under a running app with data.
- **iOS:**
  - `sudo systemsetup -setusingnetworktime off`
  - `sudo date <MMDDhhmmYYYY>` in UTC
  - the simulator's time zone through `TZ` in `SIMCTL_CHILD_TZ=Europe/Amsterdam`

  The build runs first, because a moved clock can upset incremental builds and code signing.
- **Closed ferry:** Navigate asks about every ferry closed on the way, naming it (`knot-navigation` "Ferry warning"). If that question appears before shot 2, the run fails with its text, which names the ferry. The panel's ferry lines need no parsing.

### Android script

`.github/scripts/android-screenshots.sh <out-dir>`, per language:

1. Use System UI demo mode: clock 12:00, full battery and wifi, mobile hidden, no notifications.
2. Grant location and notifications with `pm grant`.
3. Set the language with `cmd locale set-app-locales com.knoopbyknoop --locales <locale>`.
4. Clear the app's data (`pm clear`) per language, so the first language's state doesn't leak into the next. Copy the route into `files/` with `run-as`.
5. Start the app, wait until the network has loaded (no "Loading" text in the dump), then view the route file.
6. **Shot 1:** wait until the map tiles have loaded. This is a fixed wait, then two screenshots 3 s apart must match.
7. **Shot 3:** drag the panel up.
8. **Shot 2:**
   - Send a position on the course between 03 and 21, then a second one 30 m further, so the direction shows.
   - Tap Navigate. Navigate asks "ride to the start or start nearby"; tap "Start on the route nearby".
   - Wait for the header.
9. **Shot 4:** search the place's name, pick the result, then tap the centre of the map, where the place now is. Its card has to show.
10. **Shot 5:** `cmd uimode night yes`, back to the planner with the route.

Elements are found by their text in the language, from the app's own string resources: the script reads `values-<lang>/strings.xml`. Where a control has no text (icons), a `contentDescription` is added. That's an accessibility fix the apps should have anyway.

### iOS UI test

A `KnoopByKnoopScreenshots` UI test bundle target in `project.yml`, with one test and five steps. The language comes from `-AppleLanguages (<lang>) -AppleLocale <locale>` launch arguments, passed by the workflow through `xcodebuild test-without-building`. The workflow loops over the languages and passes the language through a `TEST_RUNNER_SCREENSHOT_LANGUAGE` environment variable, which the test turns into these arguments.

- **Route:** `XCUIApplication.open(_:)` with the file URL of the route, copied into the simulator's shared files by the workflow.
- **Positions:** `XCUIDevice.shared.location`.
- **Dark mode:** shots 1–4 come from one test method. The workflow then runs `xcrun simctl ui <device> appearance dark` and a second test method, which opens the route again and takes shot 5.
- **Screenshots:** `XCTAttachment(screenshot:)` with `lifetime = .keepAlways`, named `<n>-<name>`.
- **Accessibility:** controls without labels (icon buttons, panel handle) get `accessibilityIdentifier`s only where VoiceOver labels don't already identify them.

### Coordinates and place

- **The navigation position** is a point on the course between 03 and 21. It is read once from the published `cycling.geometry` with a throwaway core test, as the demo route was, and written into the script as constants.
- **The place for shot 4** is a highlighted place near the route with opening hours, preferably a campsite or a café at the Kinderdijk windmills. It is chosen from the place data at implementation time, and its name and position are written into both scripts.

## Risks / Trade-offs

- **Map tiles still loading when the screenshot is taken.** → Wait until two screenshots 3 s apart are identical, at most 60 s, then fail.
- **CI emulators are slow,** about 20 s to load the network on the local emulator. → Generous timeouts, and network loading checked by text.
- **macOS runner clock change breaks something** (Xcode, simulator, network). → Build before the change. If the simulator fails, fall back to `sudo date` on the hour only, keeping the real date, and set the run to fail on closed ferries.
- **Strings change and the Android script can't find a button.** → It reads them from the string resources, so renamed texts follow. A missing key fails the run, naming it.
- **Auto-fit frames the route with knot 04 under the map buttons.** → The screenshots show the app as it is. If this looks bad in review, fixing the fit is its own change.
- **macOS runner minutes cost ten times as much as Linux.** → The repository is public, so they're free. Runs are by hand.

## Migration Plan

- After the first good run, the maintainer reviews the contact sheet. The screenshots are committed together with the stashed screenshot check from `add-store-listings`. That ticks off its tasks 3.2–3.5, and the paused change resumes at 4.3.
- The hand-taking steps in `CONTRIBUTING.md` are replaced by "start the Store screenshots workflow".
