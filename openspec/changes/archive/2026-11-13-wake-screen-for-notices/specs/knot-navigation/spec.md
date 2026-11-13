## ADDED Requirements

### Requirement: Waking the screen for notices
While navigating with the app not on screen, every spoken notice SHALL also show on the screen: a locked phone SHALL light up and show the notice with what the panel shows. The alert SHALL be silent apart from the spoken notice, and SHALL NOT need a setting. Changes outside the app without a spoken notice SHALL NOT alert.

#### Scenario: Reminder before a knot
- **WHEN** the rider navigates with the phone locked and is reminded of 45, with 12 to follow there
- **THEN** the screen lights up and shows the notice and the number to follow at 45, without a sound other than the spoken notice

#### Scenario: Passing a knot
- **WHEN** the rider passes 45 with the phone locked and hears "Next 12, then 31"
- **THEN** the screen lights up and shows it

#### Scenario: Off route and back
- **WHEN** the rider leaves the route and later comes back to it, with the phone locked
- **THEN** the screen lights up at "Off route" and at "Back on route"

#### Scenario: A ferry closing
- **WHEN** the rider is told a ferry on the route closes before they get there
- **THEN** the screen lights up and shows that notice

#### Scenario: Riding on between notices
- **WHEN** the rider rides on with the phone locked and only the distance to the next knot is renewed
- **THEN** the screen stays off

#### Scenario: App on screen
- **WHEN** the app is on screen while a notice is spoken
- **THEN** nothing else alerts, the panel shows the notice as before
