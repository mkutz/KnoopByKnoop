## MODIFIED Requirements

### Requirement: Language setting
The info sheet SHALL offer the app's language: System, the default, which follows the phone as in "Language of the texts", and English, Deutsch, Nederlands and Français, each named in its own language. Choosing one SHALL switch the app's texts right away without restarting the app, and on iOS and on Android 13 and later without rebuilding the map, keeping the map's area, the route and a running navigation as they are, SHALL be kept until changed, and the spoken notices SHALL follow it as in "Spoken notices in the language". Navigation's notification and notices SHALL follow it on every supported Android version, also when the system only applies it to the app's screens.

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

#### Scenario: Outside the app
- **WHEN** the rider chose Deutsch on an English iPhone and navigates
- **THEN** the lock screen and the Dynamic Island show their words in German, also when an earlier choice was another language

#### Scenario: Names of the languages
- **WHEN** the rider opens the info sheet in any language
- **THEN** the choices read "English", "Deutsch", "Nederlands" and "Français", and "System" in the app's language

#### Scenario: Switching without a pause
- **WHEN** a route is planned, the map shows part of it, and the rider chooses another language in the info sheet on Android 13 or later
- **THEN** the texts change without the screen freezing, the info sheet stays open, and the map keeps its area, its knots and the route without loading them again

#### Scenario: Navigation on Android 12
- **WHEN** the rider chose Deutsch on an English phone with Android 12 and navigates
- **THEN** the notification shows its words and numbers in German, e.g. "Beenden" and "1,6 km", and the notices are spoken in German where the phone has a German voice

#### Scenario: Phone's language changed while navigating on Android 12
- **WHEN** the rider chose Deutsch in the app on an English phone with Android 12, navigates, and changes the phone's language to French in the settings
- **THEN** the notification and the notices stay German

