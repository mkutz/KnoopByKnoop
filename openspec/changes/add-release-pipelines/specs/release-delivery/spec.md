# Spec Delta

## Purpose

Lets the maintainer ship a version of both apps to the stores' test tracks from GitHub Actions with a single manual trigger.

## ADDED Requirements

### Requirement: One trigger releases both apps
Releasing a version SHALL be started manually with the version as input and SHALL deliver both the Android and the iOS app.

#### Scenario: Successful release
- **WHEN** the maintainer starts a release for version 0.2.0
- **THEN** a signed Android App Bundle with version name 0.2.0 is uploaded to the Google Play internal testing track
- **AND** a signed iOS build with marketing version 0.2.0, including the Live Activity widget, is uploaded to TestFlight

#### Scenario: Build numbers increase
- **WHEN** two releases are started one after the other
- **THEN** the second release has a higher build number on both platforms than the first

### Requirement: Releases only from a green main
A release SHALL only be built from a commit on `main` whose checks passed.

#### Scenario: Failing checks
- **WHEN** the latest checks on `main` failed
- **THEN** the release stops before anything is uploaded

### Requirement: Release notes from the changelog
A release SHALL take its release notes from the section of `CHANGELOG.md` for its version.

#### Scenario: Release notes
- **WHEN** the maintainer starts a release for version 0.2.0 and `CHANGELOG.md` has a section `## 0.2.0`
- **THEN** the Google Play release notes and TestFlight's "What to Test" show that section's entries

#### Scenario: Missing changelog section
- **WHEN** `CHANGELOG.md` has no section for the version
- **THEN** the release stops before anything is built or uploaded

### Requirement: Secrets stay secret
Signing keys and store credentials SHALL only come from repository secrets and SHALL never appear in logs or artifacts.

#### Scenario: Pull requests from forks
- **WHEN** a workflow runs for a pull request from a fork
- **THEN** it has no access to signing keys or store credentials
