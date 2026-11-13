## MODIFIED Requirements

### Requirement: Navigation with the screen off
Navigation SHALL keep running with the screen off and while other apps are in use. While navigating, the app SHALL show outside the app what its header shows (see "Number to follow", "Direction to the route", "Header on the way to the route"), without words, in the same way in every app language, renewed about every 100 m of the route and with every change of state, with a button to stop. Showing them SHALL NOT need more location fixes than navigating does.

**On iOS**, a Live Activity on the lock screen and in the expanded Dynamic Island SHALL show:
- the header's row
- a bar along the route with the rider's place, marks for stops and ferries, and the end arrival at its end

Off route and riding the wrong way, its card SHALL take the opposite of its usual appearance, light on a dark lock screen and dark on a light one.

**The Dynamic Island** has room for little, so it SHALL show one element: the number to follow on its plate when close, the next knot when far, the light tile with the arrow off route or riding the wrong way, and the plain arrow on the way to the route. The compact island SHALL show that element on the leading side and the distance on the trailing side, made smaller rather than cut off; the minimal island the element alone.

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

### Requirement: Direction outside the app
While navigating and off route without a guided way, the Android notification and the iOS Live Activity SHALL show an arrow towards the place where the rider gets back onto the route, relative to the riding direction, and the straight distance to it, as the header does (see "Direction to the route"), without a compass word. They SHALL be updated when the distance changed by 100 m or more, or the arrow by 45° or more, without more location fixes than navigating does.

#### Scenario: Lock screen off route
- **WHEN** the rider is off route without a guided way and with the screen off, riding south, with the route 1.7 km to the north-east
- **THEN** the lock screen shows an arrow pointing back to the left, "1.7 km", and no compass word

#### Scenario: Updated on the way
- **WHEN** the rider got 100 m closer to the route
- **THEN** the lock screen shows the new distance

#### Scenario: Back on route outside the app
- **WHEN** the rider comes back on route
- **THEN** the lock screen shows the knots and the bar as before, in its usual appearance

### Requirement: Guided way in the header and outside the app
On a guided way, the header, the Android notification and the iOS Live Activity SHALL show the next turn with its distance, and the distance left along the way, in place of the straight distance. The header SHALL show them without words, on its dark background: the to-the-route symbol, the turn as an arrow with its distance, the street name of the turn when known, and the distance left along the way.

#### Scenario: In the header
- **WHEN** the rider is off route with next knot 02, a left turn into Weierstraße 120 m ahead and 1.9 km left along the way
- **THEN** the header shows the to-the-route symbol, a left-turn arrow with "120 m", "Weierstraße" and "1.9 km", and no words of the app

#### Scenario: On the lock screen
- **WHEN** the same rider looks at the lock screen
- **THEN** it shows the to-the-route symbol, a left-turn arrow with "120 m" and "1.9 km", without the app's words; on Android the title reads "↰ 120 m · 1.9 km"

#### Scenario: Distance to the course
- **WHEN** the guided way ends on the course of the route, 300 m ahead
- **THEN** the distance left shown is "300 m", to the route and not to the next knot

#### Scenario: Distances ahead on a guided way
- **WHEN** the rider is on a guided way with 140 m left, ending on the course 1.1 km before 82
- **THEN** the progress panel shows 1.2 km to 82, and the knots after it and the arrival times count from there

#### Scenario: Header with a screen reader
- **WHEN** a screen reader reaches the header on a guided way
- **THEN** it announces "Off route, next 02, Left in 120 m, Weierstraße, 1.9 km"

### Requirement: Riding the wrong way
While navigating with the network's courses, a rider within 30 m of the course of the current leg who has ridden at least 150 m back along it from the furthest place they reached on that leg, and has moved at least 100 m in a straight line from that place, SHALL be told they are riding the wrong way. The app SHALL say it once and show it until they turn around, pass the next knot or leave the course. It SHALL NOT plan a new route.

#### Scenario: Riding back along the route
- **WHEN** the rider heading to 28 joined the route 2.0 km before 28 and has ridden 150 m back along it, away from 28
- **THEN** the app says "Wrong direction, turn around, next 28" once

#### Scenario: Wrong direction in the panel
- **WHEN** the rider is riding the wrong way, 2.2 km along the route from 28
- **THEN** the header is dark and shows a U-turn symbol, the badge 28 and "2.2 km", without words, the rider's dot on the strip is a ring with ✕, and the map shows no dashed line and keeps following them

#### Scenario: Wrong direction outside the app
- **WHEN** the rider is riding the wrong way with the screen off or in another app
- **THEN** the Live Activity's card has the opposite appearance with a U-turn symbol, 28 and the distance, the Dynamic Island shows the U-turn symbol on a light tile and 28, and the Android notification's title reads "↩ 28 · 2.2 km" with the U-turn symbol as its large icon and "↩28" as its chip

#### Scenario: In the app's language
- **WHEN** the app is in German and the rider is riding the wrong way
- **THEN** the app says "Falsche Richtung, bitte wenden, nächster Knotenpunkt 28", and the header looks as in English

#### Scenario: Turning around
- **WHEN** the rider who was told they ride the wrong way turns around and has ridden 50 m along the route towards 28
- **THEN** the app says "Back on route, next 28" and shows the route's usual header

#### Scenario: Turning back shortly
- **WHEN** the rider rides 100 m back along the route, e.g. to pick something up, and goes on
- **THEN** the app says nothing

#### Scenario: A place that jumps
- **WHEN** the course doubles back past itself, and the nearest place on it jumps 300 m back while the rider has moved 20 m
- **THEN** the app says nothing

#### Scenario: Leaving the course while riding the wrong way
- **WHEN** the rider riding the wrong way gets more than 100 m away from the course
- **THEN** the app says they are off route, as on any other way off the course

#### Scenario: Passing the next knot
- **WHEN** the rider riding the wrong way passes the next knot by another way
- **THEN** the app gives the usual notice for the knot after it, and the warning is gone

#### Scenario: Riding back again later
- **WHEN** the rider turned around, rode on, and later rides 150 m back along the route again
- **THEN** the app says it again

#### Scenario: On the way back of an out-and-back route
- **WHEN** the route goes 02 → 01 → 02, and the rider passed 01 and rides the same way back
- **THEN** the app does not say they ride the wrong way

#### Scenario: Following the branch to another signpost
- **WHEN** the rider follows the course to a signpost of the next knot that is not on the main course
- **THEN** the app does not say they ride the wrong way

#### Scenario: Before the first knot
- **WHEN** the first knot is still the next knot
- **THEN** the app does not say they ride the wrong way

#### Scenario: Without geometry
- **WHEN** the network's courses are not installed
- **THEN** the app does not say they ride the wrong way
