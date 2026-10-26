# Tasks

## 1. Accounts and secrets (manual)

- [ ] 1.1 Create the Google Play app `com.knoopbyknoop`, an upload keystore and a Play service account
- [ ] 1.2 Create the App ID `com.knoopbyknoop`, a distribution certificate, a provisioning profile and an App Store Connect API key
- [ ] 1.3 Store everything as repository secrets and document their names in CONTRIBUTING.md

## 2. Android

- [ ] 2.1 Release signing config in `apps/android` reading the keystore from environment variables
- [ ] 2.2 `release-android.yml`: build the AAB with version input and run number, upload to the internal track

## 3. iOS

- [ ] 3.1 `release-ios.yml` on a macOS runner: import certificate and profile, archive, export, upload to TestFlight
- [ ] 3.2 Set marketing version and build number from the inputs

## 4. Release workflow

- [ ] 4.1 A `release.yml` entry point that checks `main` is green ("Failing checks"), then calls both platform workflows
- [ ] 4.2 Verify "Successful release" and "Build numbers increase" with two releases to the test tracks
