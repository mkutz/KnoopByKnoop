# Tasks

## 1. Android

- [x] 1.1 Release signing config in `apps/android` reading the upload keystore from environment variables, and the debug key when they are not set, so local release builds keep working
- [x] 1.2 `release-android.yml` (called with `workflow_call`): build the AAB with the version input and build number, upload it with the release notes to the internal track

## 2. iOS

- [x] 2.1 `release-ios.yml` (called with `workflow_call`) on a macOS runner: archive and export with automatic signing, `-allowProvisioningUpdates` and the App Store Connect API key for both targets, upload to TestFlight
- [x] 2.2 Set marketing version and build number from the inputs for both targets
- [x] 2.3 The release notes as TestFlight's "What to Test"

## 3. Release workflow

- [x] 3.1 Extract the section for a version from `CHANGELOG.md`, failing when there is none; tests for "Release notes" and "Missing changelog section"
- [x] 3.2 `release.yml` (`workflow_dispatch` with the version): checks `main` is green ("Failing checks"), extracts the release notes, then calls both platform workflows with version, build number (`github.run_number`) and notes
- [x] 3.3 A check in `build.yml` that no workflow uses `pull_request_target` and that store secrets appear only in the release workflows ("Pull requests from forks")
- [x] 3.4 `CHANGELOG.md` gets a section for the first release

## 4. Accounts, secrets and first releases

- [x] 4.1 Create the Google Play app `com.knoopbyknoop`, an upload keystore and a Play service account
- [x] 4.2 Store the Android secrets as repository secrets and document their names in CONTRIBUTING.md
- [x] 4.3 Verify "Successful release", "Build numbers increase" and "Release notes" with two releases to the internal testing track

The App Store Connect setup, the iOS secrets and the TestFlight releases moved to `add-ios-release`.
