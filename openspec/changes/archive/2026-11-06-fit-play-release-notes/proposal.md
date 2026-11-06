# Proposal

## Why

The release of 0.10.0 on 2026-11-06 failed at the upload to Google Play: "The release created has notes in language en-US with length 2361, which is too long (max: 500)". The section's 13 entries are longer than Play allows.

## What Changes

- **Google Play:** the release notes are the section's entries in their order, as many whole entries as fit into Play's 500 characters, followed by a line pointing to the full changelog when entries were left out. An entry that is too long on its own is cut with "…".
- **`CHANGELOG.md`** stays complete; TestFlight keeps the whole section.

## Out of Scope

- Rewriting changelog entries for Play.
- Release notes in other languages.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `release-delivery`: "Release notes from the changelog" fits the Google Play notes into its limit.

## Impact

- `.github/scripts/play-release-notes.sh` with its test, run in the Build workflow; `release-android.yml` writes its output for Play.
