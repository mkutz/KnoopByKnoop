# Spec Delta

## MODIFIED Requirements

### Requirement: Info sheet
The map SHALL offer an info sheet, opened with a round info button (ⓘ) at the end of the network switch, with the data date of each installed network, the state of any download or update, and the attribution required by OpenStreetMap.

#### Scenario: Opening the info sheet
- **WHEN** both networks are installed and the user opens the info sheet
- **THEN** it shows the data dates of the cycling and the hiking network and the OpenStreetMap attribution

#### Scenario: Failed update
- **WHEN** the last attempt to download newer data failed
- **THEN** the info sheet says so and offers to retry, while the installed data keeps working

#### Scenario: Info button
- **WHEN** the map is shown, while planning or navigating
- **THEN** the info sheet is opened with a round ⓘ button, styled like the zoom buttons, not with a text button

#### Scenario: Info button with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) reaches the ⓘ button
- **THEN** it announces "Info" as a button

#### Scenario: Readable info button
- **WHEN** the ⓘ button is measured against its background, in light and in dark appearance
- **THEN** its icon has a contrast of at least 3:1
