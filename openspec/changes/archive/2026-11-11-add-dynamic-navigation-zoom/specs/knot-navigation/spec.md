# Spec Delta

## MODIFIED Requirements

### Requirement: Following the rider
While navigating with the app open, the map SHALL follow the rider, keeping their position in view as they ride, until the user moves or zooms the map. Then the map SHALL stay where the user put it and a recenter button SHALL appear, which brings the map back to the rider at the zoom for what lies ahead (see "Zoom while following") and follows them again. The map SHALL NOT return to the rider on its own. While following, on the route and off it, the base map's labels and icons SHALL stay steady.

#### Scenario: Riding with the map following
- **WHEN** the rider rides along the route and has not touched the map since navigation started
- **THEN** the map moves with them, and their position stays in view

#### Scenario: Looking around
- **WHEN** the rider drags or zooms the map while navigating
- **THEN** the map stops following and stays where they put it, and the recenter button appears

#### Scenario: Recentering
- **WHEN** the rider taps the recenter button
- **THEN** the map moves to their position at the zoom for what lies ahead, follows them again, and the button disappears

#### Scenario: Recentering near a knot
- **WHEN** the rider moved the map away and taps the recenter button 150 m before the next knot
- **THEN** the map moves to their position zoomed in on the knot, as described in "Zoom at a knot"

#### Scenario: No automatic return
- **WHEN** the rider moved the map and does not touch it for several minutes while riding on
- **THEN** the map stays where they put it, and the recenter button stays

#### Scenario: No automatic zoom after looking around
- **WHEN** the rider zoomed the map by hand and then rides up to a knot
- **THEN** the map keeps the zoom they chose and doesn't zoom in on the knot

#### Scenario: Off route while following
- **WHEN** the rider leaves the route while the map follows them
- **THEN** the map shows their position and the end of the line to the route together, as described in "Way to the route on the map"

#### Scenario: Steady base map while following
- **WHEN** the map follows the rider past a supermarket and a parking with their icons on the base map
- **THEN** the icons and street names stay shown while they are in view, without disappearing and reappearing with every position

#### Scenario: Steady base map off route
- **WHEN** the map follows a rider who is off route or on the way to the first knot, showing them and the end of the line
- **THEN** the icons and street names stay shown while they are in view, as while following along the route

### Requirement: Way to the route on the map
While navigating and off route, the map SHALL show a dashed straight line from the rider's position to the nearest point of the route ahead of them: on the course of the leg to the next knot, after the last place the rider was on the route. Without the network's courses, or before the first knot, the line SHALL go to the next knot. On a guided way, the map SHALL show a solid line along that way instead of the dashed line. While the map follows the rider, it SHALL keep both the rider and the end of that line in view, except close before a tricky spot on a guided way (see "Zoom at a tricky spot").

#### Scenario: Line to the route
- **WHEN** the rider heading to 02 left the course to the west and is off route 150 m from it, with 02 1.7 km ahead
- **THEN** the map shows a dashed line from their position to the nearest point of the course ahead of where they left it, about 150 m long, not to 02

#### Scenario: Route behind the rider
- **WHEN** the rider is off route and the nearest point of the leg's course lies before the place where they left the route
- **THEN** the line goes to that place where they left the route, not back along the part they already rode

#### Scenario: Line to the knot
- **WHEN** the network's courses are not installed, or the rider is off route before the first knot 24
- **THEN** the map shows a dashed line from their position to the next knot

#### Scenario: Both in view
- **WHEN** the rider is off route 400 m from the course and the map follows them
- **THEN** the map shows both their position and the end of the line

#### Scenario: Doubtful junction on the way back
- **WHEN** the rider is off route on a guided way 1.2 km long and comes within 150 m of a doubtful junction on it
- **THEN** the map zooms in on the junction, and the end of the line may leave the view until they are past it

#### Scenario: Back on route
- **WHEN** the rider comes back on route
- **THEN** the dashed line is gone and the map follows the rider as before

#### Scenario: Line along the guided way
- **WHEN** the rider is off route on a guided way
- **THEN** the map shows a solid line along the way and no dashed line

#### Scenario: Back to the dashed line
- **WHEN** the rider is off route and the guided way can't be replaced after leaving it
- **THEN** the map shows the dashed line again

## ADDED Requirements

### Requirement: Zoom while following
While the map follows the rider on the route, its zoom SHALL follow what lies ahead: zoomed in at a tricky spot, zoomed in at a knot, briefly zoomed out after a knot, and otherwise the navigation zoom, where the next few hundred meters of the route and their streets can be read. When more than one applies, the tricky spot SHALL win over the knot, and the knot over the zoom-out after a knot. The rider SHALL stay in the middle of the map, as before.

#### Scenario: Riding between knots
- **WHEN** the rider is on the route, 1.5 km from the next knot, with no tricky spot within 150 m ahead
- **THEN** the map follows them at the navigation zoom

#### Scenario: Tricky spot at a knot
- **WHEN** the rider comes within 200 m of knot 45, and a tricky spot begins 50 m after 45
- **THEN** the map zooms in on the tricky spot, with 45 in view on the way to it

#### Scenario: Off route
- **WHEN** the rider is off route without a tricky spot close ahead on a guided way
- **THEN** the map keeps them and the end of the way to the route in view, as described in "Way to the route on the map", and doesn't zoom in on the next knot

### Requirement: Zoom at a knot
From the reminder before the next knot (see "Reminder before a knot"), the map SHALL zoom in so that the rider, the knot and the first 100 m of the route after it are in view, to show where to expect the signpost and which way the route goes on. It SHALL NOT come closer than about 300 m across the screen's narrower side. The zoom SHALL last until the knot is passed.

#### Scenario: Approaching a knot
- **WHEN** the rider heading to 45, with 12 after it, comes within 200 m of 45
- **THEN** the map zooms in so their position, 45, and the first 100 m of the route towards 12 are in view

#### Scenario: Coming closer
- **WHEN** the rider rides on from 200 m to 50 m before 45
- **THEN** 45 and the first 100 m of the route after it stay in view, and the zoom doesn't change with every position

#### Scenario: Knot at the end of the route
- **WHEN** the rider comes within 200 m of 31, the end of the route
- **THEN** the map zooms in so their position and 31 are in view

### Requirement: Zoom out after passing a knot
After passing a knot, the map SHALL zoom out for about 10 s, just far enough that the rider and the next knot are in view, but not farther out than about 3 km across the screen's narrower side, and then go back to the navigation zoom. When the next knot is within 200 m, the map SHALL zoom in on it at once instead. A tricky spot within 150 m ahead SHALL skip or end the zoom-out.

#### Scenario: Next knot 800 m ahead
- **WHEN** the rider passes 45, and the next knot 12 is 800 m ahead
- **THEN** the map zooms out so their position and 12 are in view, and after about 10 s goes back to the navigation zoom

#### Scenario: Next knot far ahead
- **WHEN** the rider passes 45, and the next knot 12 is 6 km ahead
- **THEN** the map zooms out to about 3 km across, with 12 not yet in view, and after about 10 s goes back to the navigation zoom

#### Scenario: Next knot close
- **WHEN** the rider passes 45, and the next knot 12 is 150 m after it
- **THEN** the map doesn't zoom out and zooms in on 12 right away

#### Scenario: Tricky spot right after the knot
- **WHEN** the rider passes 45, and a tricky spot begins 60 m after it
- **THEN** the map doesn't zoom out and zooms in on the tricky spot

#### Scenario: Last knot passed
- **WHEN** the rider arrives at the end of the route
- **THEN** the map doesn't zoom out, as there is no next knot

### Requirement: Tricky spots on the route
A stretch of the route's course SHALL count as a tricky spot when it has two or more sharp turns, of 60° or more, within 100 m of each other. On a guided way, a doubtful junction (see "Doubtful junctions") SHALL count as a tricky spot too. Without the network's courses, the route has no tricky spots of its own.

#### Scenario: Zigzag
- **WHEN** the route's course turns right by 80°, then 40 m on left by 90°
- **THEN** the two turns are one tricky spot

#### Scenario: One sharp turn
- **WHEN** the route's course turns by 90° once, with no other sharp turn within 100 m
- **THEN** it isn't a tricky spot

#### Scenario: Gentle bends
- **WHEN** the route's course bends by 30° several times within 100 m
- **THEN** it isn't a tricky spot

#### Scenario: Without courses
- **WHEN** the network's courses are not installed
- **THEN** the route has no tricky spots, and the map only zooms at knots

### Requirement: Zoom at a tricky spot
When the rider comes within 150 m of a tricky spot ahead, the map SHALL zoom in so that the rider, the whole tricky spot and 30 m of the way after it are in view, no closer than about 200 m across the screen's narrower side. The zoom SHALL last until the rider is 20 m past the spot.

#### Scenario: Approaching a zigzag
- **WHEN** the rider comes within 150 m of a zigzag on the route
- **THEN** the map zooms in so their position, both turns and the way after them are in view

#### Scenario: Past the spot
- **WHEN** the rider is 20 m past the last turn of the zigzag, with the next knot 1 km ahead
- **THEN** the map goes back to the navigation zoom

#### Scenario: Doubtful junction off route
- **WHEN** the rider is off route on a guided way and comes within 150 m of a doubtful junction on it
- **THEN** the map zooms in on the junction, as on the route

### Requirement: Smooth zoom changes
A change of zoom SHALL be one even move of about 1 s. During one of the zoom rules, the zoom SHALL stay the same while the map follows the rider. The base map's labels SHALL stay steady as described in "Following the rider".

#### Scenario: Zooming in at a knot
- **WHEN** the rider comes within 200 m of the next knot
- **THEN** the map zooms in in one even move, without jumps, while it keeps following them

#### Scenario: Labels while zoomed in
- **WHEN** the map is zoomed in at a knot and follows the rider towards it
- **THEN** the street names and icons stay shown while they are in view, without disappearing and reappearing with every position
