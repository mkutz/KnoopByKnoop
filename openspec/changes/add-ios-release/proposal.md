# Proposal

## Why

`add-release-pipelines` delivers the Android app to Google Play's internal testing track and already contains the iOS release workflow (`release-ios.yml`). That workflow cannot run yet: there is no Apple Developer Program membership, which waits for feedback from Android testers. Until then, every release run ends with a failed iOS job, which hides real failures.

## What Changes

- Releases also deliver the iOS app, including the Live Activity widget, to TestFlight, with the changelog section as "What to Test".
- While the App Store Connect secrets are not set, a release skips iOS with a notice instead of failing.
- The App Store Connect app, the API key and the iOS secrets are set up and documented.

Out of scope: App Store review and production releases, store listings and screenshots.

## Capabilities

### New Capabilities

### Modified Capabilities
- `release-delivery`: releases deliver iOS as well, and skip a platform whose store credentials are missing

## Impact

- `release.yml` decides per platform whether its secrets are set; `release-ios.yml` runs as written in `add-release-pipelines`
- Repository secrets: App Store Connect API key, key ID, issuer ID and team ID, documented in CONTRIBUTING.md
- Requires an Apple Developer Program membership; until then only the skipping can be done
- Must be archived after `add-release-pipelines`, which creates the `release-delivery` spec
