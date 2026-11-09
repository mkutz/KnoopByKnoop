## ADDED Requirements

### Requirement: Hits in the app's language
The places found SHALL be named in the app's language, the one its own texts are in, whatever language the search was typed in. Where the search service has no names in that language, as for Dutch, the places SHALL be named as on the spot.

#### Scenario: German app
- **WHEN** the app's language is German and the user searches "Cologne"
- **THEN** the hit reads "Köln"

#### Scenario: English app
- **WHEN** the app's language is English and the user searches "Köln"
- **THEN** the hit reads "Cologne"

#### Scenario: Dutch app
- **WHEN** the app's language is Dutch and the user searches "Keulen"
- **THEN** the hit reads "Köln", as named on the spot

#### Scenario: App language differs from the phone's
- **WHEN** the phone's language is English, German is chosen as the app's language, and the user searches "Nederland"
- **THEN** the hit reads "Niederlande"
