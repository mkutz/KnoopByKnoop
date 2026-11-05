# Spec Delta

## ADDED Requirements

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
