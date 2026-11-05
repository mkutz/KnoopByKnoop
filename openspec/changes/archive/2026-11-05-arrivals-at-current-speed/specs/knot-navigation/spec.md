# Spec Delta

## MODIFIED Requirements

### Requirement: Ferry arrival estimate
While navigating, the app SHALL estimate when the rider gets to each ferry still ahead on the route, from the route distance to it and the rider's pace, renewed with every precise position on the route. The pace SHALL be the one over the last 5 km of the route, without the time the rider stood still or was off route. Until the rider has covered 2 km of the route, it SHALL assume 16 km/h cycling and 4.5 km/h hiking. The popover of a ferry ahead on the progress strip SHALL show the estimate, rounded to five minutes, in one line that starts with it and tells whether the ferry is open then, in place of its status for now, as in "At 14:30: open until 19:00": in green when it is open then, and only the estimate without a color when the operating times do not tell.

#### Scenario: Arrival in the ferry list
- **WHEN** the rider navigates a route with a ferry ahead that runs "Apr-Oct: Mo-Su 07:00-19:00" and that they get to around 14:30 on Wednesday, 16 June 2027
- **THEN** tapping the ferry on the strip shows "At 14:30: open until 19:00" in green above the ferry's times, and no status for now

#### Scenario: Arrival when the times do not tell
- **WHEN** the rider navigates a route with a ferry ahead without operating times in OpenStreetMap and gets there around 14:30
- **THEN** the ferry's popover shows "At 14:30" without a color

#### Scenario: Ferry passed in the list
- **WHEN** the rider has passed the knot after a ferry
- **THEN** the ferry's popover shows its status for now, as the ferry list while planning

#### Scenario: Ferry passed
- **WHEN** the rider has passed the knot after a ferry
- **THEN** the strip shows no distance and arrival for it, and the app gives no warning about it

#### Scenario: Phone in another time zone
- **WHEN** the phone is set to London time and the rider gets to a ferry at 18:15 London time, which is 19:15 in the Netherlands
- **THEN** the ferry's popover says "At 19:15", the strip "19:15" and any warning "around 19:15", compared with the ferry's times in Dutch time

#### Scenario: Slowing down
- **WHEN** the rider rode 20 km at 20 km/h and then the last 5 km at 12 km/h, and 10 km are left to the end at 18:00
- **THEN** the strip shows "18:50" below the end, at 12 km/h, not "18:30"

#### Scenario: Between two knots
- **WHEN** the rider has covered more than 2 km, slows down, and is still before the next knot
- **THEN** the arrivals on the strip and in the coming knots follow without the rider passing a knot

#### Scenario: A break
- **WHEN** the rider rode 10 km at 20 km/h, stands still for 30 minutes, and rides on
- **THEN** the arrivals move later while they stand, and when they ride on the pace is still 20 km/h

#### Scenario: A detour
- **WHEN** the rider is off route for 10 minutes and comes back on the route
- **THEN** those 10 minutes do not count for the pace

#### Scenario: Closed at the renewed estimate
- **WHEN** the rider slows down between two knots so that a ferry ahead is closed at the estimate
- **THEN** the strip shows the ferry with its red ring and its popover the warning at once, and the spoken warning comes at the next knot passed
