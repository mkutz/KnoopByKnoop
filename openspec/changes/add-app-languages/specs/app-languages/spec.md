# Spec Delta

## Purpose

The apps speak the rider's language: their texts, spoken notices, and the way numbers, times and dates are written follow the phone's language, for English, German, Dutch and French.

## ADDED Requirements

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
