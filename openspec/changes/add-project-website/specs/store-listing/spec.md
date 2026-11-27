## MODIFIED Requirements

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

The App Store support URL SHALL be the website's help page and the privacy URL the website's privacy policy, both in the listing's language (see `project-website`).

A check in the build SHALL fail when a text is missing or over its limit, or when a support or privacy URL is not the website's page in the listing's language, and SHALL name the file at fault.

#### Scenario: Missing text
- **WHEN** the Dutch App Store subtitle is missing
- **THEN** the check fails and names the Dutch subtitle

#### Scenario: Text over the limit
- **WHEN** the German Google Play short description has 81 characters
- **THEN** the check fails and names that file and its length

#### Scenario: Limits count characters, not bytes
- **WHEN** the French App Store subtitle has 30 characters, some of them accented
- **THEN** the check passes

#### Scenario: Privacy URL in the listing's language
- **WHEN** the German App Store privacy URL points to the English privacy page
- **THEN** the check fails and names the German privacy URL
