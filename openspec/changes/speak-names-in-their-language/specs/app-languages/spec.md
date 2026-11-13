## ADDED Requirements

### Requirement: Names spoken in their language
A ferry's name in a spoken notice SHALL be spoken with a voice for the language of the name, as the graph gives it, and the rest of the notice with the notice's voice, as in "Spoken notices in the language". Where the phone has no voice for the name's language, or the name has no language, it SHALL be spoken with the notice's voice. Other audio SHALL stay lowered from the notice's start to its end, also between its parts.

#### Scenario: Dutch ferry name in an English notice
- **WHEN** the app's language is English and the rider is warned about the ferry "Renkumse Veer", whose name is Dutch, on a phone with a Dutch voice
- **THEN** "Renkumse Veer" is spoken with the Dutch voice and the rest of the warning with the English voice

#### Scenario: French ferry name in a German notice
- **WHEN** the app's language is German and the rider is warned about a ferry in Wallonia, whose name is French, on a phone with a French voice
- **THEN** the ferry's name is spoken with the French voice and the rest of the warning with the German voice

#### Scenario: Name in the notice's language
- **WHEN** the app's language is Dutch and the rider is warned about a ferry whose name is Dutch
- **THEN** the whole warning is spoken with the Dutch voice, as before

#### Scenario: No voice for the name's language
- **WHEN** the app's language is English, the ferry's name is German, and the phone has no German voice
- **THEN** the whole warning is spoken with the English voice, as before

#### Scenario: Name without a language
- **WHEN** the rider is warned about a ferry whose name has no language, e.g. from a graph file written before names had languages
- **THEN** the whole warning is spoken with the notice's voice, as before

#### Scenario: Music between the parts
- **WHEN** a warning with a name in another language is spoken while music plays
- **THEN** the music stays lowered until the end of the warning, also between its parts, and plays on at its volume afterwards

#### Scenario: Texts unchanged
- **WHEN** the rider is warned about a ferry whose name is in another language than the app's
- **THEN** the warning on screen, in the notification and in the Live Activity reads as before
