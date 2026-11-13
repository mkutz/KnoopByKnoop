## MODIFIED Requirements

### Requirement: Number to follow at large text sizes
In every app language and at large system text sizes, the header SHALL show its badges, arrows, distances and, where the header holds it, Stop completely. The badges SHALL keep their size, like the badges on the map. Where a distance does not fit in the row, it SHALL be shown in a line below it. The header SHALL have the same height in every app language at the same text size.

#### Scenario: Number to follow at the largest text size
- **WHEN** the system text size is at its largest and the rider is 150 m before 94 with 41 to follow
- **THEN** the header shows 94, the arrow, 41 and, where the header holds it, Stop completely, and "150 m" completely below them

#### Scenario: Next knot at the largest text size
- **WHEN** the system text size is at its largest and the rider is 467 m before 94 with 41 to follow
- **THEN** the header shows 94, "467 m", › 41 and, where the header holds it, Stop completely

#### Scenario: Number to follow at the default text size
- **WHEN** the system text size is the default
- **THEN** the distance stays in the row with the badges

#### Scenario: Off route in German
- **WHEN** the app is in German and the rider is off route, 1.7 km from the route
- **THEN** the header shows the to-the-route symbol, the arrow, "1.7 km" and, where the header holds it, Stop completely, at the same height as in English

### Requirement: Navigation over the lock screen
On Android, while navigating with the app on top when the phone is locked, the app SHALL show the navigation over the lock screen when the phone is woken, without unlocking. Only the navigation SHALL be reachable there: the place search SHALL be hidden, and anything else SHALL need unlocking. Stopping SHALL need holding the stop button for about a second. The button SHALL have the place, shape and size of the usual Stop button, on Android in portrait the floating action button's rounded square at the panel's top right edge, and SHALL look different from it: outlined, with a ring along its edge that fills while it is held, and without a label on it, so its behavior shows without reading it. A short tap SHALL show the hint "Hold to stop". The notification SHALL stay available. When navigation ends, the lock screen SHALL show as usual.

#### Scenario: Waking the phone on the ride
- **WHEN** the rider locks the phone with the app on top while navigating, and wakes it
- **THEN** the map and the header show without unlocking, with the current distances

#### Scenario: Audio playing
- **WHEN** the same rider has audio playing
- **THEN** the navigation shows over the lock screen in the same way, with no media player above it, and pulling the notifications down shows the player and the navigation's notification

#### Scenario: The place search
- **WHEN** the navigation shows over the lock screen
- **THEN** the place search isn't shown

#### Scenario: Before touching the stop button
- **WHEN** the navigation shows over the lock screen
- **THEN** the stop button has the place, rounded-square shape and size of the solid red Stop button in the app, and is outlined with an empty ring along its edge, unlike it

#### Scenario: A touch on the stop button
- **WHEN** the rider taps the stop button over the lock screen
- **THEN** "Hold to stop" shows above the button for a few seconds, the ring fills a little and empties again, and navigation continues

#### Scenario: Letting go early
- **WHEN** the rider lets go of the stop button before it is full
- **THEN** the ring empties again, and navigation continues

#### Scenario: Without animations
- **WHEN** the phone's animations are turned off and the rider taps the stop button
- **THEN** navigation continues, and it still takes a hold of about a second to stop

#### Scenario: Holding the stop button
- **WHEN** the rider holds the stop button over the lock screen
- **THEN** the ring fills along the button's edge over about a second, and when it is closed, navigation stops and the lock screen shows as usual

#### Scenario: Stop with a screen reader
- **WHEN** a screen reader reaches the stop button over the lock screen
- **THEN** it announces "Hold to stop", and its action stops navigation

#### Scenario: Unlocking
- **WHEN** the rider unlocks the phone while the navigation shows over the lock screen
- **THEN** within about a second the place search and the usual Stop button are back, and navigation continues

#### Scenario: The notification
- **WHEN** the rider pulls the notifications down while the navigation shows over the lock screen
- **THEN** the notification shows with its Stop button, and stopping there works as before

#### Scenario: The lock screen behind the navigation
- **WHEN** the rider flicks the screen up while the navigation shows over the lock screen
- **THEN** the lock screen behind it shows, with the notification

#### Scenario: Another app on top
- **WHEN** the rider locks the phone with another app on top while navigating
- **THEN** the lock screen shows as usual, with the notification

#### Scenario: Not navigating
- **WHEN** the rider locks the phone with the app on top while not navigating
- **THEN** the lock screen shows as usual
