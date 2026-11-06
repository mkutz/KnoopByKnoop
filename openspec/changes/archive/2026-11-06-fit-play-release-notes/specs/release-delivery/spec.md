## MODIFIED Requirements

### Requirement: Release notes from the changelog
A release SHALL take its release notes from the section of `CHANGELOG.md` for its version. For Google Play, which allows at most 500 characters, the notes SHALL be the section's entries in their order, as many whole entries as fit, followed by a pointer to the full changelog when entries were left out.

#### Scenario: Release notes
- **WHEN** the maintainer starts a release for version 0.2.0 and `CHANGELOG.md` has a section `## 0.2.0`
- **THEN** the Google Play release notes show that section's entries

#### Scenario: Missing changelog section
- **WHEN** `CHANGELOG.md` has no section for the version
- **THEN** the release stops before anything is built or uploaded

#### Scenario: Long release notes
- **WHEN** the section for 0.10.0 has 13 entries with 2,361 characters
- **THEN** the Google Play release notes have at most 500 characters: the first entries that fit, whole, and a line pointing to the full changelog, and the upload succeeds
