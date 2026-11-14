## MODIFIED Requirements

### Requirement: Navigation with the screen off
Navigation SHALL keep running with the screen off and while other apps are in use. While navigating, the app SHALL show outside the app what its header shows (see "Number to follow", "Direction to the route", "Header on the way to the route"), without words, in the same way in every app language, renewed about every 100 m of the route and with every change of state, with a button to stop. Showing them SHALL NOT need more location fixes than navigating does.

**On iOS**, a Live Activity on the lock screen and in the expanded Dynamic Island SHALL show:
- the header's row
- a bar along the route with the rider's place, marks for stops and ferries, and the end arrival at its end

Off route and riding the wrong way, its card SHALL take the opposite of its usual appearance, light on a dark lock screen and dark on a light one.

**The Dynamic Island** has room for little, so it SHALL show one element: the number to follow on its plate when close, the next knot when far, the light tile with the arrow off route or riding the wrong way, and the plain arrow on the way to the route. The compact island SHALL show that element on the leading side and the distance on the trailing side, made smaller rather than cut off; the minimal island the element alone. In the compact and the minimal island, a badge SHALL show without the corner symbol of the start, a stop or the end; the expanded island and the lock screen keep it. The compact island's distance SHALL be large and bold, with digits of equal width, so the island keeps its width while the distance counts down.

**On Android**, an ongoing notification SHALL show:
- a title of numbers, arrow characters (↑ ↗ → ↘ ↓ ↙ ← ↖ for directions, → between a knot and the number to follow, › before the knot after) and distances
- a second line with the end arrival after ■
- a large icon with the knot to look for as its badge or plate, or the to-the-route symbol: on a dark disc off route, on a light one on the way to the route

From Android 16 it SHALL be a Live Update with the route's progress and a status chip of at most 7 characters. Off route and on the way to the route, the chip's icon SHALL be the to-the-route symbol.

#### Scenario: Far from the next knot in German
- **WHEN** the app is in German, the rider is far from 29 with 25 to follow, and the system's text is larger than its default
- **THEN** the notification's title shows "29 · 1.4 km › 25" completely, as in English

#### Scenario: Lock screen
- **WHEN** the rider is navigating, close to 45 with 12 to follow, and looks at the lock screen
- **THEN** the Live Activity shows 45 with "150 m", →, and 12 on its sign plate, and the bar with "19:05" at its end, and no words

#### Scenario: Far from the next knot outside the app
- **WHEN** the rider is 1.4 km from 29 with 25 to follow, and looks at the lock screen
- **THEN** it shows "29 · 1.4 km › 25" and "■ 15:25" on Android, and on iOS the badge 29, "1.4 km", › and a smaller badge 25, with the bar ending in "15:25"

#### Scenario: Dynamic Island
- **WHEN** the rider is navigating on an iPhone with a Dynamic Island, close to 45 with 12 to follow, and uses another app
- **THEN** the compact Dynamic Island shows 12 on its plate and "150 m", and nothing is cut off

#### Scenario: Stop next in the Dynamic Island
- **WHEN** the rider uses another app while navigating, far from 71, which is the end of the route, 4.0 km away
- **THEN** the compact island shows 71 without the end's corner symbol and "4.0 km", and the minimal island 71 without it, and nothing is cut off

#### Scenario: Corner symbols in the expanded island
- **WHEN** the same rider long-presses the island, or looks at the lock screen
- **THEN** the badge 71 shows with the end's corner symbol, as in the app

#### Scenario: Distance counting down in the Dynamic Island
- **WHEN** the rider uses another app while navigating towards 45, and the distance counts down from "180 m" to "110 m"
- **THEN** the compact island's distance is about as large as the badge, readable at a glance, and the island keeps its width

#### Scenario: Dynamic Island with room for one number
- **WHEN** the Dynamic Island is shared, e.g. with a screen recording, so it has room for one element
- **THEN** it shows the number to follow on its plate when close, the next knot when far

#### Scenario: Dynamic Island shared with another app
- **WHEN** the rider listens to a podcast while navigating, far from 28 with 27 after it, 2.3 km away
- **THEN** the island shows 28 and "2.3 km" in full, without cut badges or "…"

#### Scenario: Off route outside the app
- **WHEN** the rider is off route, 1.7 km from the route to the front left, with 02 next, on a path of the network, and looks at the lock screen
- **THEN** the iPhone's card has the opposite appearance and shows the to-the-route symbol, the arrow, "1.7 km" and › 02, and Android shows "↖ 1.7 km › 02" with the to-the-route symbol on a dark disc as the large icon

#### Scenario: Off route in the Dynamic Island
- **WHEN** the same rider uses another app on an iPhone with a Dynamic Island
- **THEN** the compact island shows the arrow on a light tile and "1.7 km", and the minimal island the light tile with the arrow

#### Scenario: On the way to the route outside the app
- **WHEN** the rider started navigating at home, 800 m from the route to the right, and looks at the lock screen
- **THEN** the iPhone's card keeps its usual appearance with the to-the-route symbol, the arrow and "800 m", and Android shows "→ 800 m" with the to-the-route symbol on a light disc

#### Scenario: Stop from the lock screen
- **WHEN** the rider taps Stop in the notification or the Live Activity
- **THEN** navigation stops, and the notification or Live Activity goes away

#### Scenario: Progress outside the app
- **WHEN** the rider passes a knot with the screen off, and the next knot 45 is 800 m ahead with 12 to follow and the end is reached around 19:05 at their pace
- **THEN** the notification or Live Activity shows 45, "800 m", › 12 and "19:05"

#### Scenario: Distance on the way
- **WHEN** the rider has ridden 100 m further towards 45 since the line was last renewed
- **THEN** the notification or Live Activity shows the new distance and the arrival at their current pace, without the rider passing a knot and without more location fixes

#### Scenario: Live Update
- **WHEN** the rider is navigating on Android 16 or newer
- **THEN** the notification shows a bar along the route with their place, stops as points in their plate colour and a ferry they may miss as a dark point, the knot to look for as a large badge, and a chip in the status bar: "→12" when close to 45 with 12 to follow, "83" when far from 83

#### Scenario: Chip off route
- **WHEN** the rider is off route, 1.7 km from the route to the front left, on Android 16
- **THEN** the chip shows the to-the-route symbol and "↖1.7 km"

#### Scenario: Progress bar in the Live Activity
- **WHEN** the rider is navigating on an iPhone and looks at the lock screen, or at the expanded Dynamic Island
- **THEN** the Live Activity shows a bar along the route with their place and marks for stops and ferries, and the end, renewed with the distance, with the markers at both ends of the bar complete

#### Scenario: Live Activity in light and dark mode
- **WHEN** the rider navigates on an iPhone and the system is in light or in dark mode
- **THEN** the Live Activity's card on the lock screen is light or dark with it, opposite while off route or riding the wrong way, with all its content readable, and follows a change of the mode with a later position

#### Scenario: Live Update turned off
- **WHEN** the rider has turned off Live Updates for the app, or the phone has Android 16 without its QPR2 update
- **THEN** the notification still shows the title, the end arrival and the bar, without the chip

#### Scenario: Older Android
- **WHEN** the rider is navigating on an Android version before 16
- **THEN** the notification shows the same title and end arrival, and the knot to look for as a large badge
