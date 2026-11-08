## MODIFIED Requirements

### Requirement: Navigation over the lock screen
On Android, while navigating with the app on top when the phone is locked, the app SHALL show the navigation over the lock screen when the phone is woken, without unlocking. Only the navigation SHALL be reachable there: the place search SHALL be hidden, and anything else SHALL need unlocking. Stopping SHALL need holding the stop button for about a second. The button SHALL be round and the size of the usual Stop button, and SHALL look different from it: outlined, with a ring around it that fills while it is held, so its behavior shows without reading it. A short tap SHALL show the hint "Hold to stop". The notification SHALL stay available. When navigation ends, the lock screen SHALL show as usual.

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
- **THEN** the stop button is round and outlined, with an empty ring, the size of the solid red Stop button in the app and unlike it

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
- **THEN** the ring fills around the button over about a second, and when it is closed, navigation stops and the lock screen shows as usual

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
