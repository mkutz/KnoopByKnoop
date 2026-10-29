# Spec Delta

## MODIFIED Requirements

### Requirement: One trigger releases the apps
Releasing a version SHALL be started manually with the version as input and SHALL deliver both the Android and the iOS app.

#### Scenario: Successful release
- **WHEN** the maintainer starts a release for version 0.2.0
- **THEN** a signed Android App Bundle with version name 0.2.0 is uploaded to the Google Play internal testing track
- **AND** a signed iOS build with marketing version 0.2.0, including the Live Activity widget, is uploaded to TestFlight

#### Scenario: Build numbers increase
- **WHEN** two releases are started one after the other
- **THEN** the second release has a higher build number on both platforms than the first

### Requirement: Release notes from the changelog
A release SHALL take its release notes from the section of `CHANGELOG.md` for its version.

#### Scenario: Release notes
- **WHEN** the maintainer starts a release for version 0.2.0 and `CHANGELOG.md` has a section `## 0.2.0`
- **THEN** the Google Play release notes and TestFlight's "What to Test" show that section's entries

#### Scenario: Missing changelog section
- **WHEN** `CHANGELOG.md` has no section for the version
- **THEN** the release stops before anything is built or uploaded

## ADDED Requirements

### Requirement: Platforms without store credentials are skipped
A release SHALL skip a platform whose store credentials are not set, say so in the run, and still deliver the other platform.

#### Scenario: No App Store Connect credentials
- **WHEN** the maintainer starts a release and the App Store Connect secrets are not set
- **THEN** the Android app is delivered, the iOS release is skipped with a notice that its credentials are missing, and the release run succeeds

#### Scenario: Credentials set
- **WHEN** the maintainer starts a release and the secrets of both platforms are set
- **THEN** no platform is skipped
