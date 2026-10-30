# Proposal

## Why

Since `show-ferry-open-now`, the planner tells whether each ferry on the route is open now. A rider who starts late in the afternoon still has to work out by themselves whether they reach a ferry before it closes, and finding a closed ferry at the river can mean a detour of many kilometers. While navigating, the app knows where the rider is, how fast they actually go and how far the ferry is, so it can warn them early enough to change plans.

## What Changes

- While navigating, the app estimates when the rider gets to each ferry still ahead: from the route distance to the ferry and the rider's pace so far, or an assumed pace (cycling 16 km/h, hiking 4.5 km/h) until they have covered enough of the route to measure it.
- When a ferry is closed at that time, or closes shortly after it, the app warns the rider once with a spoken notice, e.g. "The ferry Veer Broekhuizen closes at 19:00, before you get there around 19:15." This uses the same evaluation of the operating times as the ferry list, in the ferry's local time.
- The estimate is renewed whenever the rider passes a knot, so a ferry the rider falls behind on is still warned about in time.
- While navigating, the ferry list in the panel also shows when the rider gets to each ferry ahead, in red when it is warned about.
- When the operating times do not tell whether the ferry runs at that time, there is no warning, never a wrong one.

Out of scope:
- A departure date and time and arrival estimates while planning (step 3, only if testers ask for it).
- Planning a detour around a closed ferry, or avoiding closed ferries when routing.
- Warnings about ferries without known operating times.
- The warning in the Android notification and the iOS Live Activity; it is spoken, and shown in the app.
- Waiting and crossing times of the ferry itself, and the exact position of the crossing on its connection.
- An "all clear" notice when a ferry warned about turns out to be reachable after all.

## Capabilities

### New Capabilities

### Modified Capabilities
- `knot-navigation`: a new requirement for warning about ferries that are closed when the rider gets there, and the arrival times shown in the ferry list while navigating

## Impact

- `modules/core`: an arrival estimate from route progress, the ferry warnings as a new notice kind in `Navigation`, and the texts
- `apps/android` `NavigationService.kt`, `RouteFerries.kt` and `apps/ios` `Navigator.swift`, `RouteFerries.swift`: hand the ferry's local time to the core, show the arrival in the ferry list while navigating
- No change to the graph format, the location sampling or the battery budget: the estimate is renewed only when a knot is passed
