# Spec Delta

## MODIFIED Requirements

### Requirement: Ferries on the route
The route's knot list SHALL mark every ferry between the two knots it connects, and SHALL list the ferries of the route, each in one line with its name and its status; where both do not fit, the name SHALL be cut off with "…", so the status stays visible. Tapping a ferry SHALL expand it to its full status, its operating times as tagged, its fee and its website as far as they are known, with a note that they are according to OpenStreetMap; tapping it again SHALL collapse it. A ferry without known operating times SHALL be listed as such, so the user knows to check it.

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
- **THEN** the first one shows its status, times, fee and website and the note "according to OpenStreetMap", the second one stays one line, and tapping the first one again collapses it

#### Scenario: Ferry line too long
- **WHEN** a ferry's name and status do not fit into one line of the panel
- **THEN** the name is cut off with "…" and the status stays visible, and expanded, the whole name and status are shown

### Requirement: Waypoint list with badges
The waypoint list SHALL show each waypoint in one line as a badge with its number and its role, each line no higher than a touch target (48 dp on Android, 44 pt on iOS). While navigating, the waypoint list SHALL NOT be shown, as the route is fixed then and the knot sequence shows it.

#### Scenario: Listing waypoints
- **WHEN** the waypoints are 12, 30 and 45
- **THEN** the list shows the badges 12 (start), 30 (stop) and 45 (end), styled like their markers on the map

#### Scenario: Compact waypoint list
- **WHEN** a route without ferries has four waypoints and the user expands the panel on a phone in portrait
- **THEN** the list shows all four waypoints without scrolling, each in one line

#### Scenario: Waypoint list while navigating
- **WHEN** the rider navigates a route with waypoints 12, 30 and 45
- **THEN** the panel shows the knot sequence and any ferries, but no waypoint list

### Requirement: Ferry open now
The ferry list SHALL show for each ferry with known operating times, except for the ferries ahead while navigating (see `knot-navigation` "Ferry arrival estimate"), whether it is open now according to OpenStreetMap and, where that can be told too, when that changes next, in the local time of the ferry's region. The status SHALL be shown in the ferry's line and the operating times as tagged when it is expanded, and the status SHALL stay current while the list is shown. When the operating times do not allow a sure answer, the ferry SHALL show no status.

#### Scenario: Open now
- **WHEN** a ferry runs "Apr-Oct: Mo-Su 07:00-19:00" and it is Wednesday, 16 June 2027, 14:00
- **THEN** the ferry's line shows "Open until 19:00", and expanded, "Apr-Oct: Mo-Su 07:00-19:00" below it

#### Scenario: Opens later today
- **WHEN** the same ferry is shown on Wednesday, 16 June 2027, 06:15
- **THEN** it shows "Closed, opens at 07:00"

#### Scenario: Opens tomorrow
- **WHEN** the same ferry is shown on Wednesday, 16 June 2027, 20:00
- **THEN** it shows "Closed, opens tomorrow at 07:00"

#### Scenario: Closed for the season
- **WHEN** the same ferry is shown on Wednesday, 2 December 2026
- **THEN** it shows "Closed, opens on 1 April at 07:00"

#### Scenario: Open past midnight
- **WHEN** a ferry runs "Mo-Th 06:00-24:00; Fr-Su 06:00-01:00" and it is Saturday, 19 June 2027, 00:30
- **THEN** it shows "Open until 01:00"

#### Scenario: Open around the clock
- **WHEN** a ferry runs `24/7; "Not in ice conditions"`
- **THEN** it shows "Open", and expanded, the tagged times with the comment

#### Scenario: Public holiday
- **WHEN** a ferry runs "Mo-Sa 06:00-19:00; PH,Su 09:00-19:00" and it is Christmas Day, Friday, 25 December 2026, 08:00
- **THEN** it shows "Closed, opens at 09:00"

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
- **THEN** it shows "Closed, opens tomorrow at 07:00"

#### Scenario: Closing while shown
- **WHEN** the ferry list is shown at 18:59 for a ferry that closes at 19:00
- **THEN** its status changes to closed within a minute after 19:00, without any action

#### Scenario: Offline status
- **WHEN** the device has no internet access
- **THEN** the status, including public holidays, is shown as online
