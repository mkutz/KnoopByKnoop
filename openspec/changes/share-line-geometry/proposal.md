# Proposal

## Why

The code review of 2026-11-10 found the core's geometry written many times over, each copy with its own edge rules:

- a point some meters along a line: 6 copies (`Navigation.pointAlong`, `NetworkTurns.pointOn`, `PathRouter.pointAtMeters` and `pointAlong`, `RouteProgress` `pointAt`, `TrickyStretches.pointAlongCourse`); some clamp to the ends, one returns `null`, one extrapolates past them
- the stretch between two distances: 4 copies (`NetworkTurns.stretchOf`, `PathRouter.stretchOf`, `MapFocus.stretchOfLine`, `RoutePoint.between`)
- the nearest place on a line: 5 copies with 3 projections (`Navigation.distanceToLine`, `PathRouter.nearestOnLine` and `nearestOn`, `RouteProgress.fractionAlong`, `RoutePoint.nearestOn`), so the progress strip and the route points place the same position slightly differently
- running and total lengths: 5 copies; meters per degree: 8 definitions with two values (111,195.0 and 111,195.08); the box around a point: 5 copies
- bearings: 3 (great-circle 0–360°, flat −180–180°, flat 0–360°) and two angle differences, with hand-made conversions between them (`WayGuide`)
- turn notice distances (100 m, 40 m, rounding to 10 m) in both `GuidedWay` and `NetworkTurns`, two hand-written min-heaps, `MINUTES_PER_DAY` three times
- the pipeline's own haversine, because the core's `distanceMeters` is `internal`, so the lengths in the graph files come from another function than the apps'

None of this is a bug today (the extrapolating copy is only called away from the ends), but every new feature picks one of the copies, and a fix in one leaves the others.

## What Changes

- **`Polyline`** in a new `Geo.kt`: points with their running meters, computed once, with `length`, `pointAt(meters)` and `stretch(from, to)` clamped to the ends, and `nearest(position)` giving meters along, fraction, place and distance in one pass. Every copy above uses it.
- **One set of helpers next to it:** `METERS_PER_DEGREE`, `boxAround(point, meters)`, `bearing(from, to)` (0–360°), `signedAngle(from, to)` and `angleBetween` built on it.
- **Shared constants:** a `TurnNotice` object with the 100 m and 40 m distances and the rounding for `GuidedWay` and `NetworkTurns`; one `MINUTES_PER_DAY` with minute arithmetic on `LocalMoment`.
- **One generic `MinHeap`** for both routers.
- **`distanceMeters` public**, used by the pipeline instead of its haversine.
- **`NetworkGeometry`'s reading and writing symmetric:** one `readPoints` next to the existing `writePoints`, instead of four inline decode loops nested up to 11 levels.

No behavior is meant to change; the specs stay as they are.

## Out of Scope

- **`Navigation`'s state** (about 32 constructor parameters, the hand-written `copy()`, the 185-line `update()`): the next change, `tidy-navigation-state`, which will use `Polyline`.
- **Splitting `PathRouter.kt`** into routing, junction doubts and pictures: after this change has moved the geometry out of it.
- **The graph and geometry file formats:** unchanged, byte for byte.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None: a refactoring, the behavior in the specs stays as it is (`skip_specs: true`).

## Impact

- **Core:** new `Geo.kt` with `Polyline` and the helpers, `MinHeap.kt`, `TurnNotice`; about 15 files call them instead of their own copies; tests in a new `GeoTest`.
- **Pipeline:** `haversineMeters` and `EARTH_RADIUS_METERS` go.
- **Apps:** none, unless an app called a helper that moves (checked by building both).
