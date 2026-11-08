## ADDED Requirements

### Requirement: Waking the screen for notices
While navigating with the app not on screen, the app SHALL alert outside the app, so that a locked phone shows what the panel shows, at these notices:
- the reminder before the next knot;
- off route;
- riding the wrong way;
- a ferry on the route closing or closed;
- arriving.

The alert SHALL be silent, without a setting. Other changes outside the app SHALL NOT alert.

#### Scenario: Reminder before a knot
- **WHEN** the rider navigates with the phone locked and comes within the reminder distance of 45, with 12 to follow there
- **THEN** the screen wakes and the lock screen shows the number to follow at 45, without a sound of the app other than the spoken notice

#### Scenario: Off route
- **WHEN** the rider navigates with the phone locked and leaves the route
- **THEN** the screen wakes and shows that they are off route, with the way back

#### Scenario: Riding the wrong way
- **WHEN** the rider navigates with the phone locked and is told they ride the wrong way
- **THEN** the screen wakes and shows it

#### Scenario: A ferry closing
- **WHEN** the rider is told a ferry on the route closes before they reach it
- **THEN** the screen wakes and shows the route's progress as usual

#### Scenario: Arriving
- **WHEN** the rider arrives at the end with the phone locked
- **THEN** the screen wakes and shows the arrival

#### Scenario: Riding on between notices
- **WHEN** the rider rides on with the phone locked and the distance to the next knot is renewed
- **THEN** the screen stays off

#### Scenario: App on screen
- **WHEN** the app is on screen while a notice is spoken
- **THEN** nothing else alerts, the panel shows the notice as before
