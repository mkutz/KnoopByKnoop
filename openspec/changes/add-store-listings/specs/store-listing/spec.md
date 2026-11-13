# Spec Delta

## Purpose
Keeps what the App Store and Google Play show about the apps in the repository, complete in every app language and the same on both stores, and brings it to the stores with one manual trigger.

## ADDED Requirements

### Requirement: Store texts for every app language
The repository SHALL hold the store texts of both stores for every language the apps offer: English, German, French and Dutch. For the App Store these are the name, subtitle, description, keywords, promotional text, support URL and privacy URL. For Google Play they are the title, short description and full description. Each text SHALL fit its store's limit:

| Store | Text | Limit (characters) |
|---|---|---|
| App Store | name | 30 |
| App Store | subtitle | 30 |
| App Store | keywords | 100 |
| App Store | promotional text | 170 |
| App Store | description | 4000 |
| Google Play | title | 30 |
| Google Play | short description | 80 |
| Google Play | full description | 4000 |

A check in the build SHALL fail when a text is missing or over its limit, and SHALL name the file at fault.

#### Scenario: Missing text
- **WHEN** the Dutch App Store subtitle is missing
- **THEN** the check fails and names the Dutch subtitle

#### Scenario: Text over the limit
- **WHEN** the German Google Play short description has 81 characters
- **THEN** the check fails and names that file and its length

#### Scenario: Limits count characters, not bytes
- **WHEN** the French App Store subtitle has 30 characters, some of them accented
- **THEN** the check passes

### Requirement: The same description on both stores
A language's App Store description and Google Play full description SHALL be the same text, and the build check SHALL fail when they differ.

#### Scenario: Different descriptions
- **WHEN** the English Google Play full description differs from the English App Store description
- **THEN** the check fails and names both files

### Requirement: Screenshots for every app language
The repository SHALL hold five screenshots per platform for every app language, in the order the stores show them. The iPhone screenshots SHALL be 1320 × 2868 pixels, portrait, the size Apple requires for the largest iPhones. The Android phone screenshots SHALL be portrait with a 9:16 aspect ratio and between 320 and 3840 pixels on each side. The build check SHALL fail on a missing screenshot or a size the store does not take.

#### Scenario: Missing screenshot
- **WHEN** the German iPhone screenshots are only four
- **THEN** the check fails and names the German iPhone screenshots

#### Scenario: Wrong screenshot size
- **WHEN** an English Android screenshot is 1080 × 1920 and another is 1179 × 2556
- **THEN** the check passes for the first and fails for the second, naming it and its size

#### Scenario: Screenshots show the current apps
- **WHEN** the listing is pushed to the stores
- **THEN** each screenshot shows the app of the same platform as released, in the screenshot's language, with a clean status bar and no personal data

### Requirement: One trigger updates both store listings
The maintainer SHALL update both store listings by starting one workflow by hand. It takes the texts and screenshots from `main`. It SHALL replace what the stores show with them. It SHALL NOT upload a build, change a release or submit anything for review. Like a release, it SHALL skip a store whose credentials are not set, say so in the run, and still update the other store.

#### Scenario: Listing update
- **WHEN** the maintainer starts the store listing workflow
- **THEN** the App Store's version being prepared and Google Play's main store listing show the texts and screenshots from `main`, in all four languages
- **AND** no build was uploaded, no release changed and nothing submitted for review

#### Scenario: No App Store Connect credentials for the listing
- **WHEN** the maintainer starts the store listing workflow and the App Store Connect secrets are not set
- **THEN** Google Play's listing is updated, the App Store is skipped with a notice that its credentials are missing, and the run succeeds

### Requirement: The iOS app is for iPhone
The iOS app SHALL be built for iPhone only. On an iPad, the App Store SHALL offer it as an iPhone app, running in iPhone mode.

#### Scenario: iPhone only
- **WHEN** the iOS app is built
- **THEN** its device family is iPhone only

#### Scenario: Installed on an iPad
- **WHEN** the app is installed on an iPad
- **THEN** it runs in iPhone mode, with the same layout as on an iPhone
