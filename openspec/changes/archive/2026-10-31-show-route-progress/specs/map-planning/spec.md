# Spec Delta

## MODIFIED Requirements

### Requirement: Show the route
The app SHALL show the planned route on the map along its actual course, together with its knot sequence, its total distance and the time it takes at an assumed pace of 16 km/h cycling and 4.5 km/h hiking, rounded as in `knot-navigation` "Rounded times and distances". Unreachable legs SHALL be pointed out. The knot sequence SHALL show each knot as a badge with its number, like on the map, and SHALL make the chosen waypoints clearly stand out from the knots the route only passes.

#### Scenario: Route summary
- **WHEN** the route goes from 12 via 7 to 45 over 3.5 km, with 12 and 45 as waypoints
- **THEN** the app shows 3.5 km with "about 13 min" below it, and the badges 12, 7 and 45 in this order, with 12 and 45 highlighted as start and end and 7 shown as a passed knot

#### Scenario: Unreachable leg
- **WHEN** a waypoint cannot be reached from the previous one
- **THEN** the app marks that leg as unreachable instead of showing a route summary

#### Scenario: Hiking time
- **WHEN** a hiking route is 3.5 km long
- **THEN** the app shows 3.5 km with "about 47 min" below it

#### Scenario: Long route time
- **WHEN** a cycling route is 40.0 km long
- **THEN** the app shows 40 km with "about 2 h 30 min" below it

### Requirement: Ferries on the route
The route's knot list SHALL mark every ferry between the two knots it connects, and, while planning, SHALL list the ferries of the route, each in one line with where it is on the route, the numbers of the knots on both sides in riding order, and its status, cut off with "…" at the end where it does not fit. Tapping a ferry SHALL expand it to its name, its full status, its operating times as tagged, its fee and its website as far as they are known, with a note that they are according to OpenStreetMap; tapping it again SHALL collapse it. A ferry without known operating times SHALL be listed as such, so the user knows to check it.

#### Scenario: Route over a ferry
- **WHEN** the route crosses a river by a ferry that runs "Apr-Oct: Mo-Su 07:00-19:00" for €1.50
- **THEN** the knot list shows a ferry between the two knots on both banks, and lists the ferry in one line; expanded, it shows those times and that fee

#### Scenario: Ferry without known times
- **WHEN** the route uses a ferry without operating times in OpenStreetMap
- **THEN** the ferry's line says "Operating times not known" in red, and expanded, it notes to check them before going

#### Scenario: Several ferries
- **WHEN** the route uses two ferries
- **THEN** both are listed in the order the route reaches them

#### Scenario: Route without ferries
- **WHEN** the route uses no ferry
- **THEN** the knot list shows no ferries and no ferry list

#### Scenario: Offline
- **WHEN** the device has no internet access
- **THEN** the ferries and their details are shown as online

#### Scenario: Expanding a ferry
- **WHEN** the route uses two ferries and the user taps the first one
- **THEN** the first one shows its name, status, times, fee and website and the note "according to OpenStreetMap", the second one stays one line, and tapping the first one again collapses it

#### Scenario: Where the ferry is
- **WHEN** the route goes from 76 across Renkumse Veer to 02 and on to 20
- **THEN** the ferry's line starts with "⛴ 76 → 02", and its name "Renkumse Veer" is shown when it is expanded

#### Scenario: Ferries while navigating
- **WHEN** the rider starts navigating a route with ferries
- **THEN** the ferry list is replaced by the ferries on the progress strip, see `knot-navigation` "Ferries on the strip"

### Requirement: Ferry open now
The ferry list while planning, and the popover of a passed ferry while navigating, SHALL show for each ferry with known operating times whether it is open now according to OpenStreetMap and, where that can be told too, when that changes next, in the local time of the ferry's region. The status SHALL be shown in the ferry's line and the operating times as tagged when it is expanded, and the status SHALL stay current while the list is shown. When the operating times do not allow a sure answer, the ferry SHALL show no status.

#### Scenario: Open now
- **WHEN** a ferry runs "Apr-Oct: Mo-Su 07:00-19:00" and it is Wednesday, 16 June 2027, 14:00
- **THEN** the ferry's line shows "Open until 19:00", and expanded, "Apr-Oct: Mo-Su 07:00-19:00" below it

#### Scenario: Opens later today
- **WHEN** the same ferry is shown on Wednesday, 16 June 2027, 06:15
- **THEN** it shows "Closed until 07:00"

#### Scenario: Opens tomorrow
- **WHEN** the same ferry is shown on Wednesday, 16 June 2027, 20:00
- **THEN** it shows "Closed until tomorrow 07:00"

#### Scenario: Closed for the season
- **WHEN** the same ferry is shown on Wednesday, 2 December 2026
- **THEN** it shows "Closed until 1 April 07:00"

#### Scenario: Open past midnight
- **WHEN** a ferry runs "Mo-Th 06:00-24:00; Fr-Su 06:00-01:00" and it is Saturday, 19 June 2027, 00:30
- **THEN** it shows "Open until 01:00"

#### Scenario: Open around the clock
- **WHEN** a ferry runs `24/7; "Not in ice conditions"`
- **THEN** it shows "Open", and expanded, the tagged times with the comment

#### Scenario: Public holiday
- **WHEN** a ferry runs "Mo-Sa 06:00-19:00; PH,Su 09:00-19:00" and it is Christmas Day, Friday, 25 December 2026, 08:00
- **THEN** it shows "Closed until 09:00"

#### Scenario: Holiday in only some countries
- **WHEN** the same ferry is shown on Good Friday, 26 March 2027, 08:00, a public holiday in Nordrhein-Westfalen but not in the Netherlands or Belgium
- **THEN** it shows no status, only the tagged times

#### Scenario: School holidays
- **WHEN** a ferry runs "Sa,Su,PH 09:30-19:40; Mar-Sep SH 09:30-19:40" and it is Wednesday, 16 June 2027, 10:00
- **THEN** it shows no status, only the tagged times

#### Scenario: Unreadable times
- **WHEN** a ferry's operating times are tagged as "April-October"
- **THEN** it shows no status, only the tagged times

#### Scenario: Phone in another time zone
- **WHEN** a ferry runs "Apr-Oct: Mo-Su 07:00-19:00" and the phone is set to London time, where it is 18:30 on Wednesday, 16 June 2027 (19:30 in the Netherlands)
- **THEN** it shows "Closed until tomorrow 07:00"

#### Scenario: Closing while shown
- **WHEN** the ferry list is shown at 18:59 for a ferry that closes at 19:00
- **THEN** its status changes to closed within a minute after 19:00, without any action

#### Scenario: Offline status
- **WHEN** the device has no internet access
- **THEN** the status, including public holidays, is shown as online
