# Proposal

## Why

Siegburg 02 is signposted at two places 195 m apart. The data has merged them into one knot since the start, and since `pass-merged-knots` navigation counts either place as passing it. The map, though, shows 02 only at the signpost by the river.

A rider standing at the other signpost, near the L 16, sees 02 on the sign but not on the map. The course from there to the badge is a straight line across the L 16 that no one rides.

The maintainer compared it with fietsknoop, which shows a badge at each signpost but treats them as two knots, which is what made navigation stall on 2026-11-01. This change takes fietsknoop's map and keeps our single knot.

## What Changes

- **A badge at every signpost:** the map shows a knot's badge at every place it is signposted, not only at its main position.
  - The badges behave as one knot: tapping any of them selects that knot, and route roles (passed, start, stop, end) are shown on all of them.
  - While navigating, the next-knot ring is drawn at all of them.
- **One knot everywhere else:** routing, navigation, announcements, the waypoint list, the strip and the coming knots still have the knot once.
- **Courses follow the paths:** a connection that reaches a merged knot at one of its other signposts continues to the knot's main position along the network's ways between them, instead of in a straight line. Where no such way exists, it stays a straight line.

## Out of Scope

- Telling which signpost a route uses, e.g. in announcements ("02 at the L 16").
- Knots with equal numbers on both banks of a river with a ferry between them: they stay two knots, as now.
- A setting to show only one badge.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`: a new requirement "Knots signposted at several places", covering badges, taps, route roles and the next-knot ring.
- `network-data`: "Course of every connection" now has courses reach a merged knot's position along the ways between its signposts.

## Impact

- **Core:**
  - `MapFeatures.knots` and `routeKnotsLayer` emit one point per position of a knot.
  - Taps use `KnotLocator.nearestAtAnyNode`, which already exists since `pass-merged-knots`.
  - Tests are in `MapFeaturesTest` and `KnotLocatorTest`.
- **Pipeline:** `NetworkExtraction.connectionOf` builds the extension along the ways. Tests are in `NetworkExtractionTest`. The new courses reach the phones with the next data publication; the file formats don't change.
- **Android:** the next-knot ring in `NetworkMap.kt` (one point per position) and the tap in `AppViewModel`.
- **iOS:** the same in `NetworkMapView.swift` and `AppModel`.
- **Archive order:** this change uses the knot positions from `pass-merged-knots`, so it is archived after it.
