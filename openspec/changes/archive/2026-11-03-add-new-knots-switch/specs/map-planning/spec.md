## MODIFIED Requirements

### Requirement: Plan by tapping knots
Tapping a knot SHALL make it the start if there is no start yet, the end if there is no end yet, and otherwise what the switch in "What new knots become" says: a stop in the leg where it adds the least distance, or the new end. Tapping a knot that already is a waypoint SHALL change nothing, so an accidental double tap does not create a round trip and the start does not turn into a stop; round trips are made with the action to return to the start. A tap SHALL select the knot nearest to the tapped position, within a tolerance that keeps knots easy to hit.

#### Scenario: Start and end
- **WHEN** the user taps knot 12 and then knot 45
- **THEN** 12 is the start, 45 the end, and the route between them is shown on the map

#### Scenario: Adding a stop
- **WHEN** a route from 12 to 45 exists, the switch is on Between and the user taps knot 30
- **THEN** 30 is added as a stop where it adds the least distance, and the route passes through it

#### Scenario: Tapping the last waypoint again
- **WHEN** only the start 12 is set and the user taps 12 again
- **THEN** 12 stays the only waypoint, and the next tapped knot becomes the end

#### Scenario: Tapping the start of a route again
- **WHEN** a route from 12 to 45 exists and the user taps 12 again
- **THEN** the waypoints stay 12 and 45

#### Scenario: Tapping next to the network
- **WHEN** the user taps where no knot is within the tolerance
- **THEN** the route does not change

#### Scenario: Knots with equal numbers
- **WHEN** two knots 30 are close to each other, e.g. on both banks of a river, and the user taps one of them
- **THEN** exactly the tapped knot becomes part of the route

## ADDED Requirements

### Requirement: What new knots become
Once a route exists, the top of the planner panel SHALL show a switch "Add knots: Between | At end", also at the panel's smallest height. Each choice SHALL show its word and an icon in the color of that word, with an arrow pointing to where the knot goes. On Between, a tapped knot SHALL become a stop. On At end, it SHALL become the new end and the old end a stop; a round trip SHALL stay one. Between SHALL be the default, and the app SHALL remember the choice. The switch SHALL only affect later taps.

#### Scenario: Extending the route
- **WHEN** a route from 12 to 45 exists, the switch is on At end and the user taps knot 28
- **THEN** the waypoints are 12, 45 and 28, with 28 as the end and 45 as a stop

#### Scenario: Looking around
- **WHEN** the switch is on At end and the user taps 12, 45, 28 and 7 one after the other
- **THEN** the waypoints are 12, 45, 28 and 7 in that order

#### Scenario: A knot beside the route
- **WHEN** a route from 12 to 45 passes close to knot 30, the switch is on At end and the user taps 30
- **THEN** the waypoints are 12, 45 and 30, as the switch says, also though the route rides on to 45 first

#### Scenario: Round trip at the end
- **WHEN** the waypoints are 12, 45 and 12, the switch is on At end and the user taps knot 28
- **THEN** the waypoints are 12, 45, 28 and 12

#### Scenario: A knot that cannot be reached
- **WHEN** a route from 12 to 45 exists, the switch is on At end and the user taps a knot that cannot be reached from 45
- **THEN** it becomes the end, and the route reports the leg as unreachable, as for any waypoint

#### Scenario: First two taps
- **WHEN** no route exists yet and the user taps knots 12 and 45, whatever the remembered choice
- **THEN** 12 is the start and 45 the end, and the switch appears only then

#### Scenario: Default
- **WHEN** the app was never told otherwise
- **THEN** the switch is on Between

#### Scenario: Remembered
- **WHEN** the user chose At end, clears the route or restarts the app, and plans a new route
- **THEN** the switch shows At end

#### Scenario: Switching changes nothing yet
- **WHEN** a route with the stops 30 and 7 exists and the user changes the switch
- **THEN** the waypoints and the route stay as they are

#### Scenario: The icons
- **WHEN** the switch is shown
- **THEN** the arrow of Between points at the line between two knots, the arrow of At end points past the last knot, and neither uses the colors of the knots' roles

#### Scenario: In the smallest panel
- **WHEN** a route is planned and the panel is at its smallest height, on a phone 360 dp wide
- **THEN** the switch can be seen and used, and the header stays one row

#### Scenario: In landscape
- **WHEN** a route is planned in landscape
- **THEN** the switch is at the top of the side panel

#### Scenario: While navigating
- **WHEN** the rider is navigating
- **THEN** the switch is not shown

#### Scenario: Larger text
- **WHEN** the text is at 130% on a phone 360 dp wide
- **THEN** the label and both choices can be read in full, the label above the choices if needed

#### Scenario: Screen reader
- **WHEN** a screen reader reaches the switch
- **THEN** it announces "Add knots", the words of the two choices and which one is selected, and not the icons

#### Scenario: Readable in both appearances
- **WHEN** the switch is measured in light and in dark appearance
- **THEN** its labels and icons have a contrast of at least 4.5:1 to what they are shown on
