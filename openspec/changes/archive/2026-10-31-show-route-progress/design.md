# Design

## Context

See `proposal.md` for why. Today:
- `Navigation` (core) knows the route's knots, their courses (if installed), the next knot and whether the rider is off route. It does not know which knots are stops.
- `FerryWatch` (core, `warn-about-closed-ferries`) holds the route distance of each knot (`offsets`, from the connection lengths), the rider's pace (route progress since the start, assumed 16 or 4.5 km/h until 2 km) and the rounding to five minutes. It renews at the start and at each knot passed.
- The apps get a new `Navigation` with every fix and redraw the panel; with the app on screen, fixes come every second. The Android notification and the iOS Live Activity are updated in `apply` with the number to follow.
- `RouteDescription.Complete` carries the knots with their roles (start, stop, end, passed) and the distance text; the knot list of `Navigation` is the same list (positions match, see `warn-about-closed-ferries`).

## Goals / Non-Goals

**Goals:**
- One model for distances along the route and the pace, shared by the ferries, the table, the strip, the notification and the planning time.
- Texts and numbers built in the core, the apps only draw.

**Non-Goals:**
- More location fixes or Live Activity updates than today.
- A map overlay of the progress.

## Decisions

### `RouteProgress` in the core
An immutable `RouteProgress` built at navigation start from the knots, the graph, the geometry and the stop positions: the offset of each knot along the route (connection lengths, as now in `FerryWatch`), the courses per leg, the stop positions and the network's assumed pace. `FerryWatch` uses it for its offsets and pace instead of its own copies, so the ferry lines and the table always agree.
*Alternative:* a second calculation in the apps. Two places to keep in step, and the iOS app would need the leg lengths.

### The rider's place on the route
On route, the place is the offset of the knot before plus the fraction of the current leg's course up to the point nearest the fix, times the leg's length; without courses, the next knot's offset minus the straight distance to it, at most the leg's length (as `FerryWatch` does at the start). Off route, the place stays where it was last on route, and the distances count from the next knot plus the straight distance to it, without a time for the next knot, see "Off route ahead".

### The pace stays per knot
The pace is renewed only when a knot is passed (route progress since the start over the time since), as for the ferries; the distances follow each position. A pace recomputed every second would jump with every traffic light.

### Stops passed in by the apps
`Navigation.start` gets the positions of the stops (the knots with `KnotRole.STOP`), which the apps take from `RouteDescription.Complete.knots`, built from the same knot list. The start and the end are the first and last positions.
*Alternative:* the waypoints. A waypoint can appear twice on a route (round trips, back and forth), so positions are unambiguous where knots are not.

### What the apps get
`Navigation.progress(fix)` returns a `ProgressView` for the panel:
- the strip: the length, each marked knot with its fraction along the route and its label kind (start, stop, end, ferry, other), the rider's fraction, the ridden fraction and whether the rider is off route, and the text for screen readers ("4.0 of 12.4 km ridden, next knot 12")
- for each stop, ferry and the end still ahead, its distance ("800 m", "5.2 km", as `formatDistance`) and arrival ("18:40") on its mark
- the distance to the next knot for the header (straight while off route)

The apps choose which marks show their badge and which their distance and arrival: badges first by priority (end, stop, start, ferry) where 30 pt/dp of room are left, then distance and arrival for the shown stops, ferries and the end where the wider label (about 60 pt/dp) does not overlap a stronger one. A first version had a table of next knot, next stop and end below the strip; on the iPhone it read small and repeated the header, so the maintainer chose labels on the strip instead.

`Navigation.progressLine` is the notification's text ("800 m to 45 · end around 19:05"), computed when a knot is passed and at the start, so it changes only then and causes no extra notification or Live Activity updates.

### The strip
Android: a `Canvas` in the panel's fixed header, 40 dp high: a line for the route, filled up to the rider; ticks for passed-by knots; small badges for the start, the stops and the end, as in the badge row; ⛴ for ferries; the rider as a dot, in the off-route color while off route. Labels that would overlap a stronger one (end > stop > start > ferry) are left out; ticks are always drawn. iOS: the same with a SwiftUI `Canvas` and the badge views overlaid. The badge row stays while planning.

### Notification and Live Activity
Android: the line becomes the notification's content text below the title. iOS: `NavigationActivityAttributes.ContentState` gets `progress: String?`, shown below the text on the lock screen and in the expanded Dynamic Island; the compact Dynamic Island keeps the numbers only.

### Planning time
`RouteDescription.Complete` gets `duration` ("about 47 min") at the network's assumed pace, from the route's length; the header shows "about 47 min" in small type below "12.4 km" (first next to it, which wrapped beside the actions on the emulator).

### Rounding
One function for durations (minutes under an hour, at least one; five minutes above) and the existing rounding of arrivals to five minutes, used by the table, the notification and the planning time.

## Risks / Trade-offs

- [The pace is far off in the first 2 km] → The assumed pace applies there, as for the ferries; the table updates as soon as the own pace is known.
- [Breaks make the times pessimistic] → Intended, as for the ferries: a ride includes breaks.
- [Crowded strip on long routes with many stops] → Labels give way by priority, ticks stay; the table names the next stop and the end in full.
- [Updating the panel every second costs battery] → Only while the app is on screen, when fixes come every second anyway; the computation is a projection onto one leg's course.
- [The notification's distance goes stale between knots] → It says the distance as of the last knot; refreshing it would need fixes the screen-off budget does not allow.

### Ferries on the strip and their popover
`ProgressMark` gets the ferry's route position and its `FerryArrivalState` (from `FerryWatch`, as of the last knot passed) for ferries ahead. The apps rank the badges end, ferries with a warning, stops, start, other ferries, and the distances and arrivals end, warned ferries, stops, other ferries. A warned ferry's badge gets a red ring and its arrival is red; an open ferry's arrival is green.
Tapping a ferry badge opens a popover anchored to it: Android a `Popup` with a card, iOS `.popover` kept as a popover on the iPhone (`presentationCompactAdaptation(.popover)`). It shows the same details as the expanded ferry row: place, name, the arrival line (`Navigation.ferryArrival`) or, once passed, the status for now, the times, fee, the OpenStreetMap note and the website. The strip stays one element for screen readers ("… ridden, next knot …"), except the ferries, which are buttons named by `FerryArrival.description` ("Ferry 76 → 02, at 21:10 closed until 1 May 10:00"). While navigating, the ferry list below the strip is not shown.

### Ferry website links on iOS
Found during the iPhone checks (from `polish-planner-panel`): all ferries were one row of the sheet's `List`, and a `List` row sends taps anywhere in it to its first `Link`, so tapping a ferry without a website opened the website of the one before. Each ferry is its own `List` row now, and the link uses `.buttonStyle(.borderless)`, so it only reacts to taps on itself.
