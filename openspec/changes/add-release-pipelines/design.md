# Design

## Context

See `proposal.md` for why. `build.yml` already builds and checks every push to `main`, but skips pushes that only change Markdown (`paths-ignore: '**.md'`), so a commit that only adds a `CHANGELOG.md` section has no Build run of its own. Android release builds are signed with the debug key so they can be installed locally, e.g. to measure startup times. On iOS, the app and the widget extension `KnoopByKnoopWidgets` use automatic signing with a personal team from `Signing.local.xcconfig`, which is not in the repository.

## Goals / Non-Goals

**Goals:**
- One manual trigger delivers both apps to the test tracks with the same version and build number.
- As few secrets as possible, and none that expire every year.

**Non-Goals:**
- Fastlane on Android, or a release tool of our own.
- Releasing from branches other than `main`, or releasing only one platform.

## Decisions

### Three workflows, one entry point
`release.yml` is the only workflow with a trigger (`workflow_dispatch` with a `version` input). It checks `main`, extracts the release notes, and calls `release-android.yml` and `release-ios.yml` with `workflow_call`, passing version, build number and notes, with `secrets: inherit`. The platforms run in parallel. If one fails, its job can be re-run without uploading the other one a second time.
*Alternative:* one workflow with two jobs. It would work as well, but the separate files make each platform readable on its own.

### Build number is the run number of `release.yml`
`github.run_number` goes up with every run, including failed runs, so it is always higher than the last upload on both platforms. It is used as `versionCode` and as `CURRENT_PROJECT_VERSION`. Re-running a job keeps the run number, which is right when that job's upload failed.
*Alternative:* asking the stores for the last build number. That needs API calls on both platforms and still has races.

### Green `main`
The release refuses to run unless it was started on `main`. It finds the latest completed Build run on `main` and requires its conclusion to be `success`. It also requires that the commits since that run change only Markdown files (`git diff --name-only <run sha> HEAD`), because those commits get no Build run of their own. Otherwise it stops before building ("Failing checks").
*Alternative:* requiring a successful Build run for the exact commit. That would make releasing a `CHANGELOG.md`-only commit impossible.

### Release notes from `CHANGELOG.md`
A small POSIX shell script, `.github/scripts/release-notes.sh <version>`, prints the lines between `## <version>` and the next `## ` heading, and fails when there is no such section or it is empty. A test script with sample changelogs covers "Release notes" and "Missing changelog section" and runs in `build.yml`. `release.yml` runs the script before anything else and passes its output on.
*Alternative:* a Kotlin task in `:pipeline`. It would bring JUnit, but the pipeline is about OpenStreetMap data, and a JVM start for twenty lines of text is overkill.

### Android: signing from the environment, upload with an action
The release signing config reads `KNOOPBYKNOOP_UPLOAD_KEYSTORE` (the path to the keystore file), `..._KEYSTORE_PASSWORD`, `..._KEY_ALIAS` and `..._KEY_PASSWORD`. When they are not set, it keeps the debug key. CI decodes the keystore from a base64 secret into `$RUNNER_TEMP`. The version comes in as Gradle properties (`-PversionName`, `-PversionCode`) with today's values as defaults. The AAB is uploaded with `r0adkll/upload-google-play` to the `internal` track; the release notes go in a `whatsnew/whatsnew-en-US` file. The AAB is also kept as a workflow artifact, for the first release that has to go to Play by hand.
*Alternative:* the Gradle Play Publisher plugin. It needs the service account during configuration and adds a plugin to the app build for something only CI does.

### iOS: cloud-managed signing with the API key
CI keeps automatic signing and passes `-allowProvisioningUpdates` together with the App Store Connect API key (`-authenticationKeyPath`, `-authenticationKeyID`, `-authenticationKeyIssuerID`) to `xcodebuild archive` and `-exportArchive`. Apple creates and manages the distribution certificate and the profiles for the app and the widget extension. The team ID is written into `Signing.local.xcconfig`, which the project already includes. The version is passed as `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION` build settings and applies to both targets. Upload and "What to Test" go through `fastlane pilot upload --changelog` with the same API key, because `xcrun altool` cannot set "What to Test".
*Alternative:* manual signing with a `.p12` certificate and two profiles as secrets. That means four more secrets and renewing them every year.

### Fork pull requests
GitHub gives fork pull requests no secrets unless a workflow uses `pull_request_target`. A step in `build.yml` fails when any workflow uses `pull_request_target`, or when a workflow other than the release workflows refers to a `secrets.` value other than `GITHUB_TOKEN`.

## Risks / Trade-offs

- [The API key needs the Admin role, so it can do anything in App Store Connect] → It lives only in repository secrets, which only the release workflows use (checked by the fork check).
- [Play's API cannot create an app's first release] → The first run's AAB is uploaded by hand (task 4.1). From the second run on, CI uploads.
- [Renaming `release.yml` resets `run_number`] → The file is not renamed; if it ever must be, an offset goes into the build number.
- [Play release notes are limited to 500 characters] → The upload fails visibly; changelog sections stay short.
- [Waiting for TestFlight processing can take several minutes] → `pilot` waits for it, and the iOS job gets a generous timeout.
