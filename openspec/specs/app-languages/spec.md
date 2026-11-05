# app-languages Specification

## Purpose
The apps speak the rider's language: their texts, spoken notices, and the way numbers, times and dates are written follow the phone's language, for English, German, Dutch and French.

## Requirements

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

#### Scenario: Outside the app
- **WHEN** the rider chose Deutsch on an English iPhone and navigates
- **THEN** the lock screen and the Dynamic Island show their words in German, also when an earlier choice was another language

#### Scenario: Names of the languages
- **WHEN** the rider opens the info sheet in any language
- **THEN** the choices read "English", "Deutsch", "Nederlands" and "Français", and "System" in the app's language

### Requirement: Language change while navigating
When the app's language changes while navigating, navigation's texts SHALL follow at once, without restarting navigation, and later notices SHALL be spoken in the new language where the phone has a voice for it, otherwise in English. The route, the progress and the pace SHALL stay as they were, and the change itself SHALL NOT be spoken.

#### Scenario: Words and line outside the app
- **WHEN** the rider navigates in English, 150 m from 45 with 12 to follow, and the app's language changes to German
- **THEN** the notification shows "Bei 45 folge 12" and "150 m bis 45", without waiting for the next position

#### Scenario: Off route
- **WHEN** the rider is off route in English, and the app's language changes to German
- **THEN** the panel and the notification show "Abseits der Route, nächster" with the distance and the direction in German

#### Scenario: Later notices
- **WHEN** the language changed to German, and the rider then passes a knot
- **THEN** the notice is spoken in German

#### Scenario: No voice for the new language
- **WHEN** the language changes to French on a phone without a French voice
- **THEN** the texts are in French, and later notices are spoken in English

#### Scenario: Ferry ahead
- **WHEN** a ferry is ahead and the language changes to German
- **THEN** its popover and any later warning are in German, and a ferry already warned about is not warned about again

#### Scenario: Nothing else changes
- **WHEN** the language changes while navigating
- **THEN** the next knot, the place on the strip and the pace stay as they were, and nothing is spoken for the change
