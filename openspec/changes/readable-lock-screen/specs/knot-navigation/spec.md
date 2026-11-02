# Spec Delta

## MODIFIED Requirements

### Requirement: Navigation with the screen off
Navigation SHALL keep running with the screen off and while other apps are in use. While navigating, the app SHALL show outside the app what its panel shows: the next knot, far from it with the number to follow after it, close to it the number to follow there, see "Number to follow". With them, it SHALL show the distance to the next knot and the arrival at the end, renewed about every 100 m of the route, with a button to stop: on Android in an ongoing notification, from Android 16 as a Live Update with the route's progress; on iOS in a Live Activity on the lock screen and in the Dynamic Island, also with the route's progress. Showing them SHALL NOT need more location fixes than navigating does.

#### Scenario: Lock screen
- **WHEN** the rider is navigating, close to 45 with 12 to follow, and looks at the lock screen
- **THEN** it shows "At 45 follow 12" and, e.g., "150 m to 45 · end around 19:05"

#### Scenario: Far from the next knot outside the app
- **WHEN** the rider is 1.4 km from 29 with 25 to follow, and looks at the lock screen
- **THEN** it shows "Next 29, then 25" on Android and "Next 29", "then 25" with badges on iOS, and "1.4 km to 29 · end around 15:25"

#### Scenario: Dynamic Island
- **WHEN** the rider is navigating on an iPhone with a Dynamic Island and uses another app
- **THEN** the Dynamic Island shows the next knot and the number to follow there

#### Scenario: Off route outside the app
- **WHEN** the rider is off route and looks at the lock screen
- **THEN** it shows that they are off route, and the next knot

#### Scenario: Stop from the lock screen
- **WHEN** the rider taps Stop in the notification or the Live Activity
- **THEN** navigation stops, and the notification or Live Activity goes away

#### Scenario: Progress outside the app
- **WHEN** the rider passes a knot with the screen off, and the next knot is 800 m ahead and the end is reached around 19:05 at their pace
- **THEN** the notification or Live Activity shows "800 m to 45 · end around 19:05"

#### Scenario: Distance on the way
- **WHEN** the rider has ridden 100 m further towards 45 since the line was last renewed
- **THEN** the notification or Live Activity shows the new distance and the arrival at their current pace, without the rider passing a knot and without more location fixes

#### Scenario: Live Update
- **WHEN** the rider is navigating on Android 16 or newer
- **THEN** the notification shows a bar along the route with their place and marks for stops and ferries, the knot to look for as a large badge, and the same knot as a chip in the status bar: the next knot while far from it, the number to follow when close

#### Scenario: Progress bar in the Live Activity
- **WHEN** the rider is navigating on an iPhone and looks at the lock screen, or at the expanded Dynamic Island
- **THEN** the Live Activity shows a bar along the route with their place and marks for stops and ferries, and the end, renewed with the distance

#### Scenario: Live Activity in light and dark mode
- **WHEN** the rider navigates on an iPhone and the system is in light or in dark mode
- **THEN** the Live Activity's card on the lock screen is light or dark with it, with all its text readable, and follows a change of the mode with a later position

#### Scenario: Live Update turned off
- **WHEN** the rider has turned off Live Updates for the app, or the phone has Android 16 without its QPR2 update
- **THEN** the notification still shows the words, the distance and the bar, without the chip

#### Scenario: Older Android
- **WHEN** the rider is navigating on an Android version before 16
- **THEN** the notification shows the words, the distance and the arrival, and the knot to look for as a large badge
