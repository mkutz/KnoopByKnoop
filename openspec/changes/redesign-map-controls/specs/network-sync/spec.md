## MODIFIED Requirements

### Requirement: Info sheet
While planning, the map SHALL offer an info sheet, opened with the info button, a round button with a gear at the top right of the map, apart from the network switch and announced as "Settings", with the data date of each installed network, the state of any download or update, the attribution required by OpenStreetMap, and the choice of the appearance (System, Light, Dark), see `map-planning` "Light and dark appearance".

#### Scenario: Opening the info sheet
- **WHEN** both networks are installed and the user opens the info sheet
- **THEN** it shows the data dates of the cycling and the hiking network and the OpenStreetMap attribution

#### Scenario: Failed update
- **WHEN** the last attempt to download newer data failed
- **THEN** the info sheet says so and offers to retry, while the installed data keeps working

#### Scenario: Info button
- **WHEN** the map is shown while planning
- **THEN** the info sheet is opened with a round gear button at the top right, on its own background like the map's button groups, not with a text button and not inside the network switch

#### Scenario: Info button with a screen reader
- **WHEN** a screen reader (TalkBack, VoiceOver) reaches the ⓘ button
- **THEN** it announces "Settings" as a button

#### Scenario: Readable info button
- **WHEN** the gear button is measured against its background, in light and in dark appearance
- **THEN** its icon has a contrast of at least 3:1

#### Scenario: No info button while navigating
- **WHEN** the rider is navigating
- **THEN** the map shows neither the gear button nor the network switch, and the info sheet is available again after navigation ends

#### Scenario: Appearance in the info sheet
- **WHEN** the user opens the info sheet
- **THEN** it shows the appearance with System, Light and Dark, the current choice selected, System by default
