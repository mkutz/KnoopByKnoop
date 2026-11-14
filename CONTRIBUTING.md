# Contributing

Thanks for your interest in Knoop by Knoop!


## Spec first

This project is spec-driven with [OpenSpec](https://github.com/Fission-AI/OpenSpec).
Every change in behavior starts as a change proposal in `openspec/changes/<change-id>/`:

1. **Propose:** `proposal.md` (why, what, out of scope), spec deltas in `specs/<capability>/spec.md`, and `tasks.md`.
   With Claude Code: `/opsx:propose "your idea"`.
2. **Discuss** the proposal in a pull request or issue before implementing it.
3. **Apply:** implement the tasks; each spec scenario gets a test, named after the scenario or marked `// Scenario: <name>`.
   Scenarios only checkable on the devices or by a workflow run are listed in `openspec/checked-by-hand.md` with how they are checked.
   `./gradlew checkScenarios -Pchange=<change-id>` shows the change's scenarios still without either.
   With Claude Code: `/opsx:apply`.
4. **Archive:** once merged, move the deltas into `openspec/specs/`. With Claude Code: `/opsx:archive`.

Validate specs with:

```bash
npx @fission-ai/openspec validate --all
```

The Specs workflow runs this and the scenario check on every push, also when only specs change.

Pure maintenance (dependency updates, refactorings, build changes) needs no proposal.


## Code

- `./gradlew check` must pass, including formatting. Fix formatting with `./gradlew spotlessApply`.
- Kotlin is formatted with ktfmt (Google style).
- Logic that both apps need belongs in `modules/core` and must not use platform APIs.
- Do not abbreviate names.


## Releasing

Releases are started by hand with the workflow **Release** in GitHub Actions, with the version as input.
`CHANGELOG.md` needs a section `## <version>`; its entries become the release notes.
The release only runs when the latest Build on `main` succeeded; later commits may only change Markdown files.
It uploads the Android App Bundle to Google Play's internal testing track, with the run number of the Release workflow as build number.
A platform whose secrets are not all set is skipped with a notice in the run, e.g. iOS until the App Store Connect secrets exist; the release fails only when neither platform can be released.

The release workflows need these repository secrets:

| Secret | Content |
|---|---|
| `ANDROID_UPLOAD_KEYSTORE_BASE64` | The upload keystore, base64-encoded |
| `ANDROID_UPLOAD_KEYSTORE_PASSWORD` | Password of the upload keystore |
| `ANDROID_UPLOAD_KEY_ALIAS` | Alias of the upload key in the keystore |
| `ANDROID_UPLOAD_KEY_PASSWORD` | Password of the upload key |
| `PLAY_SERVICE_ACCOUNT_JSON` | JSON key of the Google Play service account allowed to release |
| `APP_STORE_CONNECT_API_KEY` | Content of the App Store Connect API key file (`.p8`), a team key with the Admin role |
| `APP_STORE_CONNECT_API_KEY_ID` | The key's ID, shown with the key in App Store Connect |
| `APP_STORE_CONNECT_API_ISSUER_ID` | The Issuer ID shown above the keys in App Store Connect |
| `APPLE_TEAM_ID` | The Apple Developer team ID, in the membership details |

Only the release workflows may use secrets; a check in the Build workflow fails otherwise.
Without the keystore variables, local release builds are signed with the debug key.


## Store listing

What the App Store and Google Play show about the apps lives in `fastlane/`, in English, German, French and Dutch, see the spec `store-listing`:

- `fastlane/metadata/ios/<locale>/`: App Store name, subtitle, description, keywords, promotional text, support and privacy URLs; categories, copyright and the age rating (`app_rating_config.json`) one level up
- `fastlane/metadata/android/<locale>/`: Google Play title and short description; `full_description.txt` is a link to the App Store description, so both stores say the same; phone screenshots in `images/phoneScreenshots/`, the feature graphic and icon only for `en-US`, which Google Play shows for the other languages too (the graphic's source is `design/store/feature-graphic.svg`)
- `fastlane/screenshots/ios/<locale>/`: the iPhone screenshots, 1320 × 2868

The Build workflow checks that every language has every text within its store's limit and five screenshots per platform in sizes the stores take.

Change the listing in a commit to `main`, then start the workflow **Release store listing** by hand.
It replaces texts and screenshots on both stores; it uploads no build and submits nothing for review.
It uses the release secrets above; the App Store Connect key needs the Admin or App Manager role to write metadata.
For the App Store it writes to the version being prepared, or to the version given as input.

Retake the screenshots after changes to the planner panel, the navigation header or the map controls, see the spec `store-screenshots`.
They show the loop in `fastlane/screenshots/demo-route.knoop` with the app set to each language, at the coming Saturday 12:00 in Amsterdam, when all its ferries are open:

1. The planned route with the panel
2. Navigating between 03 and 21
3. The waypoint list
4. A tapped place with its details
5. The route in dark mode

To retake them:

1. Start the workflow **Store screenshots** by hand. It takes all 40 on an Android emulator and an iPhone simulator, in about 40 minutes, and needs no secrets; it commits nothing.
2. Review the contact sheet `contact-sheet.png` in its artifact `store-screenshots-<run>`: one row per platform and language. Check the language, the clock at 12:00, the map tiles and the open place card.
3. Copy the artifact's `fastlane/` folder over the repository's, commit the screenshots to `main`, and start **Release store listing**.

The positions and the place the screenshots use are in `fastlane/screenshots/demo.env`.
The scripts can also run locally: `.github/scripts/android-screenshots.sh <out-dir> [clock-epoch] [locales]` against the emulator, and the UI test `StoreScreenshots` in the scheme "KnoopByKnoop Screenshots" with `TEST_RUNNER_SCREENSHOT_LANGUAGE` set.

Set up by hand, once, in the consoles: App Privacy ("Data Not Collected"), price and availability, content rights (OpenStreetMap data) and the review notes in App Store Connect; the data safety form and the category Maps & Navigation in Play Console.
