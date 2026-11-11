# app-languages Specification

## Purpose
The apps speak the rider's language: their texts, spoken notices, and the way numbers, times and dates are written follow the phone's language, for English, German, Dutch and French.

## Requirements

### Requirement: Language of the texts
The apps SHALL show all their own texts in the language chosen for the app on the phone, its per-app language or else the system language, when it is German, Dutch or French, and in English for any other language. Texts from OpenStreetMap, such as place names and ferry names, SHALL be shown as they are.

#### Scenario: German phone
- **WHEN** the phone's language is German and the rider is 800 m from 45 with 12 to follow
- **THEN** the panel shows "Bei 45 folge 12" and "800 m"

#### Scenario: Dutch phone
- **WHEN** the phone's language is Dutch and a cycling route of 3.5 km is planned
- **THEN** the panel shows "3,5 km" and "ongeveer 13 min" and the action "Navigeren"

#### Scenario: French phone
- **WHEN** the phone's language is French and the rider is off route
- **THEN** the panel shows "Hors itinéraire, suivant 02"

#### Scenario: Other language
- **WHEN** the phone's language is Spanish
- **THEN** the apps show their texts in English

#### Scenario: Per-app language
- **WHEN** the system language is English and the app's own language is set to German in the system settings
- **THEN** the apps show their texts in German

#### Scenario: Large text in German
- **WHEN** the phone's language is German, the system text size is at its largest, and the rider plans a route and navigates it
- **THEN** the number to follow, the distance to the next knot, Stop, the route's distance and time, Navigate and the search field are shown completely, and nothing is clipped at the sides of the panel

#### Scenario: Place names stay
- **WHEN** the phone's language is German and the rider searches "Arnhem Centraal"
- **THEN** the hit is shown as "Arnhem Centraal", as the place search returns it

### Requirement: Numbers, times and dates in the language
Distances with decimals, times and dates SHALL be written as usual in the language: a decimal comma in German, Dutch and French, the 24-hour clock in all four, and dates with the month's name in the language.

#### Scenario: Decimal comma
- **WHEN** the phone's language is German and the stop is 5.2 km ahead
- **THEN** the strip shows "5,2 km"

#### Scenario: Date of a ferry's next opening
- **WHEN** the phone's language is German and a ferry is closed for the season until 1 April 07:00
- **THEN** its status says "Geschlossen bis 1. April 07:00"

#### Scenario: English unchanged
- **WHEN** the phone's language is English
- **THEN** the texts, distances, times and dates are as before, e.g. "5.2 km" and "Closed until 1 April 07:00"

### Requirement: Spoken notices in the language
The spoken notices SHALL be in the app's language, spoken with a voice for that language, and SHALL say the knot numbers in it. Where the phone has no voice for the language, they SHALL be spoken in English.

#### Scenario: German notice
- **WHEN** the app's language is German and the rider passes 24 with 45 next
- **THEN** the app says "Nächster Knotenpunkt 45" with a German voice, the number spoken in German

#### Scenario: Dutch arrival
- **WHEN** the app's language is Dutch and the rider passes the end
- **THEN** the app says "Je bent aangekomen" with a Dutch voice

#### Scenario: No voice for the language
- **WHEN** the app's language is French and the phone has no French voice
- **THEN** the notices are spoken in English, as before

### Requirement: Texts outside the app in the language
The notification while navigating (Android), the Live Activity (iOS), the screen reader texts and the system's location permission prompt SHALL be in the app's language too.

#### Scenario: Notification in German
- **WHEN** the app's language is German and the rider navigates with the screen off
- **THEN** the notification shows e.g. "Bei 45 folge 12" and "0,8 km bis 45 · Ende gegen 19:05"

#### Scenario: Screen reader in Dutch
- **WHEN** the app's language is Dutch and a screen reader reaches 30 in the list of coming knots
- **THEN** it announces "Knooppunt 30, tussenstop, 5,2 km, 18:40"

#### Scenario: Location prompt in French
- **WHEN** the app's language is French and the app asks for location access
- **THEN** the system prompt shows the app's reason in French

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
