## MODIFIED Requirements

### Requirement: Number to follow
While navigating, the panel's header SHALL show what to look for on the signs, as one row of badges, arrows and distances, without words.

**Close to the next knot** (from when the rider is reminded of it, see "Reminder before a knot"), the row SHALL show, in this order:
1. the next knot's badge
2. an arrow
3. the badge of the number to follow there, as the most prominent element
4. the distance to the next knot

**Farther away**, the row SHALL show ↑ with the next knot's badge and the distance to it as the most prominent elements, then › and the badge of the number to follow there, smaller.

**When the next knot is the end**, the row SHALL show the end's badge with its corner symbol and the distance, and no number to follow.

**The distance** SHALL be along the route, or straight to the next knot while it is the first knot of the route.

**The badges** SHALL look like the knot's signs: in the Netherlands, the next knot filled and the number to follow white with a ring, as on the Dutch arrow signs; elsewhere, all filled, in the plate colour of `map-planning` "Route knots stand out". A knot the rider picked as a waypoint SHALL keep its corner symbol.

**Screen readers** SHALL get the row in words.

#### Scenario: Next knot in the middle of the route
- **WHEN** the next knot is 45, 150 m ahead, and the route continues to 12
- **THEN** the header shows the badge 45, an arrow, the badge 12 as the most prominent element, and "150 m", and no words

#### Scenario: Far from the next knot
- **WHEN** the next knot is 83, 1.2 km ahead, and the route continues to 30
- **THEN** the header shows ↑, the badge 83 and "1.2 km" as the most prominent elements, and › with a smaller badge 30

#### Scenario: Coming close
- **WHEN** the rider heading to 83 comes within 200 m of it and is reminded "83 ahead, then 30"
- **THEN** the header shows the badge 83, an arrow and the badge 30 as the most prominent element

#### Scenario: Knot right after the last one
- **WHEN** the rider passes 45 and the next knot 12 is 150 m after it
- **THEN** the header shows 12, an arrow and 31 right away

#### Scenario: Next knot is the end
- **WHEN** the next knot is 31, the end of the route, 900 m ahead
- **THEN** the header shows the badge 31 with the end's corner symbol and "900 m", and no knot after it

#### Scenario: Distance off route
- **WHEN** the rider is off route, 1.7 km straight from where they get back onto the route
- **THEN** the header shows the off-route row of "Direction to the route" with "1.7 km", not the number to follow

#### Scenario: Distance to the first knot
- **WHEN** the rider started away from the route 24, 45, 12, 31, came back within 30 m of its course 80 m straight from 24, and 24 is still the next knot
- **THEN** the header shows 24, an arrow, 45 and "80 m", and outside the app "80 m to 24", not "0 m"

#### Scenario: Dutch knots in the header
- **WHEN** the rider is close to the Dutch knot 95 with 1 to follow
- **THEN** 95 is a filled dark green plate and 1 is white with a dark green ring and number

#### Scenario: German knots in the header
- **WHEN** the rider is close to 25 in North Rhine-Westphalia with 32 to follow
- **THEN** 25 and 32 are both red plates with a white ring and number

#### Scenario: Picked knot in the header
- **WHEN** the number to follow is 30, a stop the rider picked
- **THEN** its badge has the stop's corner symbol

#### Scenario: Same layout in every language
- **WHEN** the rider is 1.2 km before 83 with 30 to follow, once with the app in English and once in German
- **THEN** the header looks the same and has the same height

#### Scenario: Far from the next knot with a screen reader
- **WHEN** a screen reader reaches the header while the next knot 83 is 1.2 km ahead and 30 follows
- **THEN** it announces "Next 83, 1.2 km, then 30"

#### Scenario: Close to the next knot with a screen reader
- **WHEN** a screen reader reaches the header while the rider is 150 m before 45 with 12 to follow
- **THEN** it announces "At 45 follow 12, 150 m"

### Requirement: Direction to the route
While navigating and off route without a guided way, the header SHALL show, on its dark background (see `map-planning` "Light and dark appearance"):
- the off-route symbol
- an arrow pointing to where the rider gets back onto the route (the end of the line in "Way to the route on the map"), relative to the rider's riding direction
- the straight distance to that place, as the most prominent element
- the next knot's badge, small, only while the rider is within 30 m of a course of the knot network

The header SHALL show no compass word. When the rider has not moved recently, the arrow SHALL point relative to north, as the map.

#### Scenario: Direction in the header
- **WHEN** the rider is off route without a guided way, rides north, and the place where they get back onto the course is 1.7 km to the north-east
- **THEN** the header is dark, with the off-route symbol, an arrow pointing to the front right and "1.7 km", and no compass word

#### Scenario: Back to the route, not to the knot
- **WHEN** the rider left the course of the leg to 02, 150 m from the course, and 02 is 1.4 km ahead along it
- **THEN** the header shows "150 m" and the arrow points to the course, not to 02

#### Scenario: Off the network
- **WHEN** the rider is off route on a road that is no path of the knot network
- **THEN** the header shows no knot badge

#### Scenario: Wrong turn within the network
- **WHEN** the rider passed 77, which is not on the route, and rides on along a course of the network
- **THEN** the header also shows the next knot 02 as a small badge

#### Scenario: Standing still
- **WHEN** the rider has not moved recently, has no guided way and the place where they get back onto the route is to the east
- **THEN** the arrow points to the right, as east on the map

#### Scenario: Without courses
- **WHEN** the network's courses are not installed and the rider is off route
- **THEN** the arrow and the distance point to the next knot

#### Scenario: Direction with a screen reader
- **WHEN** a screen reader reaches the header while off route without a guided way, 1.7 km from the route to the north-east, with 02 as the next knot
- **THEN** it announces "Off route, next 02, 1.7 km to the route, to the north-east"

### Requirement: Guided way in the header and outside the app
On a guided way, the header, the Android notification and the iOS Live Activity SHALL show the next turn with its distance, and the distance left along the way, in place of the straight distance. The header SHALL show them without words, on its dark background: the off-route symbol, the turn as an arrow with its distance, the street name of the turn when known, and the distance left along the way.

#### Scenario: In the header
- **WHEN** the rider is off route with next knot 02, a left turn into Weierstraße 120 m ahead and 1.9 km left along the way
- **THEN** the header shows the off-route symbol, a left-turn arrow with "120 m", "Weierstraße" and "1.9 km", and no words of the app

#### Scenario: On the lock screen
- **WHEN** the same rider looks at the lock screen
- **THEN** it shows "Off route, next 02" and "Left in 120 m · 1.9 km"

#### Scenario: Distance to the course
- **WHEN** the guided way ends on the course of the route, 300 m ahead
- **THEN** the distance left shown is "300 m", to the route and not to the next knot

#### Scenario: Distances ahead on a guided way
- **WHEN** the rider is on a guided way with 140 m left, ending on the course 1.1 km before 82
- **THEN** the progress panel shows 1.2 km to 82, and the knots after it and the arrival times count from there

#### Scenario: Header with a screen reader
- **WHEN** a screen reader reaches the header on a guided way
- **THEN** it announces "Off route, next 02, Left in 120 m, Weierstraße, 1.9 km"

### Requirement: Number to follow at large text sizes
In every app language and at large system text sizes, the header SHALL show its badges, arrows, distances and Stop completely. The badges SHALL keep their size, like the badges on the map. Where a distance does not fit in the row, it SHALL be shown in a line below it. The header SHALL have the same height in every app language at the same text size.

#### Scenario: Number to follow at the largest text size
- **WHEN** the system text size is at its largest and the rider is 150 m before 94 with 41 to follow
- **THEN** the header shows 94, the arrow, 41 and Stop completely, and "150 m" completely below them

#### Scenario: Next knot at the largest text size
- **WHEN** the system text size is at its largest and the rider is 467 m before 94 with 41 to follow
- **THEN** the header shows ↑, 94, "467 m", › 41 and Stop completely

#### Scenario: Number to follow at the default text size
- **WHEN** the system text size is the default
- **THEN** the distance stays in the row with the badges

#### Scenario: Off route in German
- **WHEN** the app is in German and the rider is off route, 1.7 km from the route
- **THEN** the header shows the off-route symbol, the arrow, "1.7 km" and Stop completely, at the same height as in English

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
- **THEN** the notification or Live Activity shows "Wrong direction, next 28" with its warning look, and the Dynamic Island shows the warning sign and 28

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

## ADDED Requirements

### Requirement: Header on the way to the route
After navigation started away from the route, until the rider is first on the route, the header SHALL show the way to the route like "Direction to the route", or like "Guided way in the header and outside the app" on a guided way, but with a start flag on a neutral tile in place of the off-route symbol and without the dark background, as this is no mistake. The next knot's badge SHALL be shown small only while the rider is within 30 m of a course of the knot network. After the rider has been on the route, leaving it SHALL show the off-route header.

#### Scenario: Starting at home
- **WHEN** the rider starts navigating at home, 800 m from the route to the east, on a street that is no path of the knot network
- **THEN** the header shows the start flag on a neutral background, an arrow to the right and "800 m", no knot badge, and is not dark

#### Scenario: Reaching the network on the way
- **WHEN** that rider reaches a course of the knot network on the way to the first knot 24
- **THEN** the header also shows 24 as a small badge

#### Scenario: On the route
- **WHEN** the rider comes within 30 m of the course
- **THEN** the header shows the number to follow as usual

#### Scenario: Leaving the route later
- **WHEN** the rider was on the route and leaves it by more than 100 m
- **THEN** the header is the dark off-route header

#### Scenario: On the way with a screen reader
- **WHEN** a screen reader reaches the header on the way to the route, 800 m to the east, with 24 as the first knot
- **THEN** it announces "To the route, next 24, 800 m, to the east"
