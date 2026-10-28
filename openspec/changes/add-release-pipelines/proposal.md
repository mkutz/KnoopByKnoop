# Proposal

## Why

All building and publishing happens in GitHub Actions. Builds already run for every pull request and push to `main`, but there is no way yet to get the apps to testers and the stores.

## What Changes

- A manually triggered release workflow takes a version, builds signed release artifacts for Android and iOS and uploads them to Google Play's internal testing track and TestFlight.
- Version name and build number are derived from the workflow input and the run number.
- Release notes come from `CHANGELOG.md`, like in ApproveJ.

Out of scope: promoting builds to production (done by hand in the store consoles), store listings and screenshots.

## Capabilities

### New Capabilities
- `release-delivery`: producing signed app releases and delivering them to the stores' test tracks

### Modified Capabilities

## Impact

- New workflows `release.yml` (entry point), `release-android.yml` and `release-ios.yml`
- Android release builds are signed with the upload key on CI and keep the debug key locally
- Repository secrets: Android upload keystore and Play service account; App Store Connect API key and team ID; Apple manages the signing certificate and profiles
- Requires a Google Play developer account and an Apple Developer Program membership; until they exist, the workflows can be written but not run
