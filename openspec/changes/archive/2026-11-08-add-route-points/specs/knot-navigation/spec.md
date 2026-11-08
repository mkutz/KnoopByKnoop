# Spec Delta

## ADDED Requirements

### Requirement: Navigating with route points
A route point SHALL be passed, reached and aimed at like a knot, with the same radius, also around the end of its way where the way stops short of the point, and SHALL count for reminders, distances and arrival. Everywhere a number would name it, on screen, outside the app and in the voice, it SHALL be named as the start, the stop or the end in the app's language. Navigating SHALL NOT compute a route point's way anew.

#### Scenario: Starting at the point
- **WHEN** the rider starts navigating at a route point and the first knot is 02, then 28
- **THEN** the first notice is the usual "Next 02, then 28"

#### Scenario: Starting away from the point
- **WHEN** the rider starts navigating 400 m from the route point at the start
- **THEN** they are off route with the way to the start, in words instead of a number

#### Scenario: Joining further along
- **WHEN** the rider joins the route between the route point and 02 without reaching the point
- **THEN** they are on route with 02 next

#### Scenario: A stop next
- **WHEN** the rider passes 28 and a route point is the next stop, then 27
- **THEN** the notice names the stop and 27, and the same words are on the lock screen and spoken

#### Scenario: The last knot before the end
- **WHEN** the rider passes 81 and the end is a route point
- **THEN** the notice names the end instead of a number, and the same words are on the lock screen and spoken

#### Scenario: Reminder and arrival
- **WHEN** the rider nears a route point at the end and then comes within the radius
- **THEN** the reminder says that the end is coming, and arrival is announced as for a knot

#### Scenario: A point the way does not reach
- **WHEN** the way along paths ends 60 m from the route point, as the point is inside a park, and the rider reaches the end of the way
- **THEN** the point counts as reached

#### Scenario: Off route with the end next
- **WHEN** the rider is off route and the route point at the end is next
- **THEN** the notice gives the way to the end in words, and the map's dashed line goes to the route ahead, on the way to the point, as in "Way to the route on the map"

#### Scenario: On the strip
- **WHEN** a route with route points is navigated
- **THEN** the strip marks them with the flag, and its distances count only the ridden part of their connections and their ways

#### Scenario: A round trip from a route point
- **WHEN** the route starts and ends at the same route point and the rider leaves it
- **THEN** leaving does not count as arriving, and arrival is announced only after the waypoints before the end were passed

#### Scenario: All languages
- **WHEN** the app's language is German, Dutch or French
- **THEN** the wording for the start, stop and end is in that language, in the texts and the voice

### Requirement: Riding the way to a route point
On a way along paths to a route point, the rider SHALL be told the distance and direction with the line on the map, with no directions at each turn, and leaving the way SHALL count as off route as on a course. On a straight line, which cannot be followed, the rider SHALL NOT be told they are off route, and SHALL be told the distance and direction to where it leads.

#### Scenario: Following a way along paths
- **WHEN** the rider rides along the way to a stop
- **THEN** they stay on route, and the distance to the stop counts down along the way

#### Scenario: Leaving a way along paths
- **WHEN** the rider moves more than the off route distance away from the way
- **THEN** they are off route, as on a course

#### Scenario: A straight line
- **WHEN** the next stretch is a straight line of 800 m to a stop, and the rider rides around a building 200 m from it
- **THEN** they are not told they are off route, and see the distance and direction to the stop

#### Scenario: Turning round at a detour
- **WHEN** the rider reaches a stop at the end of a way and turns back along it
- **THEN** the turn is not taken for riding the wrong way, and the next knot is named

#### Scenario: Without internet access
- **WHEN** the rider navigates a route with a way along paths to a route point, without internet access
- **THEN** the way and its guidance work as with internet access
