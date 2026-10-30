# Proposal

## Why

The planner lists the ferries of a route with their operating times exactly as OpenStreetMap tags them, e.g. `Apr-Sep: Mo-Fr 06:30-22:30, Sa 07:30-22:30, PH,Su 08:00-22:30; Oct-Mar: …`. Finding out from that whether the ferry runs right now takes reading and calendar knowledge (is today a public holiday?). This is also the first step towards warning a rider whose ferry will be closed when they get there: the app has to understand the times before it can compare them with an arrival.

## What Changes

- The core reads a ferry's `opening_hours` and tells, for a moment in time, whether the ferry is open, closed, or whether that cannot be told from the data.
- The planner's ferry list shows that status for now next to the tagged times, e.g. "Open until 19:00", "Closed, opens at 10:00", "Closed, opens on 1 April". The tagged times stay visible below it.
- Public holidays of the Netherlands, Belgium and Nordrhein-Westfalen are known without a connection; where the countries differ and it matters, there is no status.
- When the data does not allow a sure answer (school holidays, sunrise and sunset, unreadable values, `unknown`), the ferry gets no status, never a wrong one.
- The pipeline logs how many ferries' times the app can read, like the existing ferry coverage line.

Out of scope:
- Warnings about a ferry that is closed when the rider arrives, and estimating arrival times (the next steps).
- A departure date or time in the planner; the status is always for now.
- Rewriting the tagged times into a friendlier full schedule.
- School holidays, sunrise and sunset, and the conditions in comments such as "not in bad weather".
- Avoiding closed ferries when routing.

## Capabilities

### New Capabilities

### Modified Capabilities
- `map-planning`: the ferry list on the route also shows whether each ferry is open now

## Impact

- `modules/core`: an `opening_hours` reader and evaluator, public holidays computed from the date (Easter included), and the status texts in `RouteFerry`
- `apps/android` `RouteFerries.kt` and `apps/ios` `RouteFerries.swift`: show the status, kept current while the list is visible
- `pipeline` `Main.kt`: one more coverage figure in the ferry log line
- No new graph format: the tagged text is already in the graph
