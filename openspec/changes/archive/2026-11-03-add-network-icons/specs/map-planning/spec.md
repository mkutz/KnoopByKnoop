## ADDED Requirements

### Requirement: Icons on the network switch
Each choice of the network switch SHALL show an icon next to its word: a bike for cycling and a hiker for hiking. The icon SHALL have the color of the word. The switch SHALL behave as without the icons.

#### Scenario: The icons
- **WHEN** the network switch is shown
- **THEN** cycling shows a bike next to its word, and hiking a hiker next to its word

#### Scenario: Selected network
- **WHEN** the user switches from cycling to hiking without a route
- **THEN** hiking shows as selected, with its icon and word, and cycling as not selected

#### Scenario: Switching with a route
- **WHEN** a route exists and the user taps the other network
- **THEN** the app asks first, and the selection only changes after the user confirms

#### Scenario: On a narrow phone
- **WHEN** the switch is shown on a phone 360 dp wide, in English, German, Dutch or French
- **THEN** both words and icons and the info button show in full, on one row

#### Scenario: Larger text
- **WHEN** the text is at 130% on a phone 360 dp wide, in German and in French
- **THEN** both words can be read in full, and the info button can be reached

#### Scenario: Very large text
- **WHEN** the text is larger than 130%, up to the largest size, on a phone 360 dp wide in German
- **THEN** both words stay on one line, and the info button can be reached, by scrolling the switch if it does not fit

#### Scenario: In landscape
- **WHEN** the app is in landscape
- **THEN** the switch shows the icons and does not cover the search field or the side panel

#### Scenario: Screen reader
- **WHEN** a screen reader reaches the switch
- **THEN** it announces the words of the two choices and which one is selected, and not the icons

#### Scenario: Readable in both appearances
- **WHEN** the switch is measured in light and in dark appearance
- **THEN** its words and icons have a contrast of at least 4.5:1 to what they are shown on
