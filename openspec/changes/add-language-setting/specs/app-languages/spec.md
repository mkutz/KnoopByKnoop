# Spec Delta

## ADDED Requirements

### Requirement: Language setting
The info sheet SHALL offer the app's language: System, the default, which follows the phone as in "Language of the texts", and English, Deutsch, Nederlands and Français, each named in its own language. Choosing one SHALL switch the app's texts right away without restarting the app, SHALL be kept until changed, and the spoken notices SHALL follow it as in "Spoken notices in the language".

#### Scenario: Choosing German on an English phone
- **WHEN** the phone's language is English and the rider chooses Deutsch in the info sheet
- **THEN** the info sheet and the planner show their texts in German right away, e.g. "Darstellung" and "Navigieren"

#### Scenario: Back to the phone's language
- **WHEN** the rider chose Deutsch and then chooses System on an English phone
- **THEN** the texts are English again right away

#### Scenario: Kept after a restart
- **WHEN** the rider chose Nederlands and starts the app again
- **THEN** the texts are Dutch

#### Scenario: Notices in the chosen language
- **WHEN** the rider chose Français on a German phone and navigates
- **THEN** the notices are spoken in French where the phone has a French voice

#### Scenario: Same as the system setting on Android
- **WHEN** the rider chose Deutsch in the app on Android 13 or later
- **THEN** the system's per-app language setting of the app shows German, and choosing a language there shows in the app's setting too

#### Scenario: Names of the languages
- **WHEN** the rider opens the info sheet in any language
- **THEN** the choices read "English", "Deutsch", "Nederlands" and "Français", and "System" in the app's language
