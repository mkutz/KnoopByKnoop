# Tasks

## 1. iPhone only

- [x] 1.1 Set `TARGETED_DEVICE_FAMILY: "1"` for the app and widget targets in `apps/ios/project.yml`. Drop `UISupportedInterfaceOrientations~ipad` if present, and set `ITSAppUsesNonExemptEncryption: false`. Regenerate with `xcodegen` and build for the simulator. Verify: `grep TARGETED_DEVICE_FAMILY apps/ios/KnoopByKnoop.xcodeproj/project.pbxproj` shows only `1`.
- [x] 1.2 Add `.github/scripts/ios-device-family-test.sh` with `# Scenario: iPhone only`, which fails unless every target in `project.yml` is iPhone only. Run it from `build.yml`. Verify: it passes, and fails when one target is set back to `"1,2"`.
- [x] 1.3 Add "Installed on an iPad" to `openspec/checked-by-hand.md` under `store-listing`, checked on an iPad simulator. Verify: the app runs in iPhone mode there.

## 2. Store texts and their check

- [x] 2.1 Write `.github/scripts/store-listing-check.sh`. It checks every locale for every text file. It checks the limits from the spec, counted in characters with `python3`. It compares each locale's two descriptions. It names the file at fault. Verify: task 2.2.
- [x] 2.2 Add `.github/scripts/store-listing-check-test.sh` on fixture trees in a temp directory. It has tests for the scenarios "Missing text", "Text over the limit", "Limits count characters, not bytes" and "Different descriptions", each marked `# Scenario:`. Run both scripts from `build.yml`. Verify: `sh .github/scripts/store-listing-check-test.sh` passes.
- [x] 2.3 Write the English texts in `fastlane/metadata/ios/en-US/` and `fastlane/metadata/android/en-US/`:
  - name, subtitle, description, keywords, promotional text
  - URLs, with `full_description.txt` as a symlink to the iOS description
  - `primary_category`, `secondary_category`, `copyright`
  - `app_rating_config.json`

  Verify: the check passes for `en-US`, and the maintainer approves the English texts.
- [x] 2.4 Translate into `de-DE`, `fr-FR` and `nl-NL` in the apps' own terms. Verify: the check passes for all four, and the maintainer reads the German.
- [x] 2.5 Fix `PRIVACY.md` (shared routes are files, not links or QR codes; update "Last updated") and the share line in `README.md`. Verify: `grep -n 'QR' PRIVACY.md README.md` finds nothing.

## 3. Screenshots

- [x] 3.1 Make the demo route `.knoop` file (the Kinderdijk loop over two ferries). Keep it in `fastlane/screenshots/demo-route.knoop`. Verify: opening it on the emulator and in the simulator shows the route.
- [ ] 3.2 Extend the check with screenshot counts and sizes, read from the PNG header. Add tests for "Missing screenshot" and "Wrong screenshot size" to the check's test. Verify: the test passes.
- [ ] 3.3 Take the five Android screenshots in all four languages on the 1080 × 1920 emulator, with System UI demo mode. Put them in `fastlane/metadata/android/<locale>/images/phoneScreenshots/`. Move the feature graphic and icon from `design/store`. Verify: the check passes, and the maintainer looks at the English set.
- [ ] 3.4 Take the five iPhone screenshots in all four languages on the iPhone 17 Pro Max simulator, with `simctl status_bar override`. Put them in `fastlane/screenshots/ios/<locale>/`. Verify: the check passes, and the maintainer looks at the English set.
- [ ] 3.5 Add "Screenshots show the current apps" to `openspec/checked-by-hand.md`, checked by looking at the screenshots before each listing push. Remove the old screenshots from `design/store/screenshots`. Verify: `./gradlew checkScenarios` passes.

## 4. Listing workflow

- [x] 4.1 Add `.github/workflows/release-store-listing.yml`:
  - `workflow_dispatch`, `main` only
  - an iOS job with `deliver`: no binary, no submission, overwriting screenshots, rating config, logging the version it wrote to
  - an Android job with `supply`: listing and images only
  - each job skipping with a notice when its secrets are missing
  - covered by `check-workflow-secrets.sh` through its `release-` name

  Verify: actionlint and `sh .github/scripts/check-workflow-secrets-test.sh` pass in `build.yml`.
- [x] 4.2 Add "Listing update" and "No App Store Connect credentials for the listing" to `openspec/checked-by-hand.md`, checked by a run of the workflow. Describe in `CONTRIBUTING.md`:
  - how to change the listing
  - when to retake screenshots
  - the App Manager key the workflow needs
  - the steps done by hand in the consoles

  Verify: `./gradlew checkScenarios` passes.
- [ ] 4.3 Run the workflow with the maintainer. Verify:
  - Both consoles show the texts and screenshots in all four languages.
  - No build or release was changed.
  - The App Store version is not submitted.

## 5. By hand in the consoles (maintainer)

- [ ] 5.1 App Store Connect:
  - App Privacy "Data Not Collected"
  - price free, availability (all countries, or the maintainer's choice)
  - content rights: uses third-party content (OpenStreetMap, ODbL)
  - review contact and notes: no login needed; how to plan a route and navigate

  Verify: App Store Connect shows no missing items for the version.
- [ ] 5.2 Play Console: check that the data safety form matches `PRIVACY.md` (no data collected or shared) and the category is Maps & Navigation. Verify: the listing shows no warnings in Play Console.

## 6. Finish

- [ ] 6.1 Run `./gradlew check` and `./gradlew checkScenarios -Pchange=add-store-listings`. Verify: both pass.

## Workflow follow-up

- The maintainer submits the App Store version for review. Play goes to production after closed testing ends.
- Archive with `/opsx:archive` once 4.3 and 5.x are done.
