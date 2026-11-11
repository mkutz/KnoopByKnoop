# Proposal

## Why

The lists beside the knots say little about where they are:
- **Planning:** the waypoint list shows a knot's number and its role (start, stop, end).
- **Navigating:** the coming knots list shows the distance and the arrival.

Whether 06 is in Lohmar or out in the fields, or whether the route crosses into Belgium on the way, the rider has to find on the map. The maintainer asked for meaningful texts there (2026-11-11).

## What Changes

- **A place name per knot.** It is the name of the village, town or city the knot lies in, as on that place's signpost: OpenStreetMap's `name`.
  - Hamlets, suburbs, quarters and districts don't count.
  - A knot out in the open has no name.
  - Where the place has a different name in the app's language, it follows in brackets, e.g. "Liège (Lüttich)" in German.
- **Borders between knots.** Where the route crosses a border between two knots, the coming knots list shows a line for it, named in the app's language. The borders are:
  - country borders: the Netherlands, Belgium, Germany
  - Dutch provinces, Belgian provinces (and Brussels), German states
- **Where they show:**
  - the place name after the role in the waypoint list
  - the place name beside each knot in the coming knots list while navigating, also for the screen reader
  - the border lines between the knots in the coming knots list
- **Where the data comes from:** the pipeline computes both from the extracts at build time, so they are offline with the networks. The graph file gains sections that apps released before skip.

Out of scope:
- **Entering or leaving a town between knots.** Towns are mostly mapped as points, and municipality borders often run through fields, so the line would come in odd places.
- **Hamlets, suburbs and districts.**
- **Names in the header, in the voice, on the lock screen, or in the planner's row of badges,** which is full already.
- **Names for route points.** They keep their role and, for a place, its name.
- **Borders while planning,** e.g. in the route summary. A follow-up if wanted.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `network-data`: new requirements "Place of each knot" and "Borders between knots".
- `map-planning`: "Waypoint list with badges" shows the place name.
- `knot-navigation`: "Coming knots" shows the place names and the borders.

## Impact

- **`pipeline`:**
  - Reads `place=village|town|city` nodes and areas with their names.
  - Reads the boundaries of countries, Dutch and Belgian provinces, Brussels and German states. Today it reads only the three borders of the sign regions.
  - Assigns a place to each knot and the border crossings to each connection's course.
  - Writes both into the graph file as new sections.
- **`modules/core`:**
  - `GraphFile` reads and writes the sections. Older files have no names or borders.
  - `Knot` gets its place; `Connection` or the route gets its crossings.
  - Texts for "name (name in the app's language)", and the crossings along a route for the coming knots.
- **`apps/android`, `apps/ios`:** the waypoint list row, the coming knots list and its screen reader texts.
- **Data size:** a few hundred KB more per network at most, well within the graph's 5 MB budget.
