# Design

## Context

See the proposal for the copies. Their differences that matter for unifying them:

- **Ends:** `PathRouter.pointAtMeters` and `stretchOf` clamp; `Navigation.pointAlong` returns `null` for an empty line; `TrickyStretches.pointAlongCourse` extrapolates past the ends, but its caller skips turns within 60 m of either end, so it never does.
- **Projections for the nearest place:** `Navigation.distanceToLine` projects to meters around the fix; `PathRouter.nearestOnLine` to degrees scaled by the cosine of the latitude, then measures with haversine; `RouteProgress.fractionAlong` projects to flat meters and takes the fraction by flat length; `RoutePoint.nearestOn` uses `nearestOnLine` with the fraction by haversine length.
- **Meters per degree:** 111,195.0 and 6,371,008.8 · π / 180 = 111,195.08; 0.08 m per degree, under a millimeter on the distances used.

## Goals / Non-Goals

**Goals:**
- One implementation of each operation, tested once at its edges.
- Results within a meter of today's everywhere, and the same notices on the replayed ride of `move-navigation-loop-to-core`.

**Non-Goals:**
- Faster geometry; it is not a hot spot today.

## Decisions

### `Polyline`: running meters by haversine, nearest place by a local flat projection

```kotlin
class Polyline(val points: List<GeoPoint>) {
  val meters: List<Double>                      // running, haversine, computed once
  val length: Double
  fun pointAt(at: Double): GeoPoint             // clamped to the ends
  fun stretch(from: Double, to: Double): List<GeoPoint>  // clamped, reversed when from > to
  fun nearest(position: GeoPoint): OnLine       // OnLine(meters, fraction, place, distance)
}
```

The nearest place on each segment is found in meters of a flat projection around the position (as `Navigation.distanceToLine` does), and its distance and the meters along are measured with haversine (as `RoutePoint.nearestOn` does). Over a segment of a few hundred meters the two projections differ by centimeters.

*Alternative:* keep each projection where it is and only share `pointAt`. Rejected: the nearest place is where the copies disagree with each other.

*Empty lines:* `pointAt` and `nearest` need at least one point; the callers that can have none (`Navigation.pointAlong`) check first, as today.

### Bearings: one great-circle bearing, angles derived

`bearing(from, to)` is the great-circle initial bearing, 0–360°; `signedAngle(from, to)` is the turn from one bearing to another, −180–180°; `angleBetween = abs(signedAngle)`. `PathRouter.heading` and `RiddenTrack.bearing` (both flat) become `bearing`; on the distances they are used for (meters to a few hundred meters), flat and great-circle bearings differ by far less than a degree.

### One meters per degree

`METERS_PER_DEGREE = EARTH_RADIUS_METERS * PI / 180` next to `distanceMeters`, and `boxAround(point, meters)` for the five bounding boxes.

### `MinHeap<T>` with `Double` priorities

`Router` uses `Int` meters; they become `Double` without loss. `PathRouter.LabelHeap` keeps its order of equal priorities (first in, first out) only if the heap does; the shared heap does not promise it, so the tests check that the routers' results are the same, not the order of ties. If a tie order turns out to matter, `MinHeap` gets a sequence number as a second key.

### Proving nothing changed

1. The existing tests (about 800 in the core) stay green; where a numeric expectation moves, it moves by less than a meter or the change is explained in the commit.
2. The replay of `move-navigation-loop-to-core` (`checks/Replay.kt`, the 06 → 08 ride on the real data) gives the same notices and way searches before and after, way lengths within a meter.
3. Reading the published geometry files and writing them again gives the same bytes, before and after the change of `readPoints`.
4. The pipeline's graph files keep their connection lengths: `distanceMeters` and the removed haversine are the same formula; a test compares them on sample points before the old one goes.

## Risks / Trade-offs

- [A tiny numeric shift flips a threshold on a ride, e.g. off route at exactly 50 m] → The shifts are centimeters, well below GPS noise; the replay compares notices.
- [Big diff over many files] → One commit per kind of helper, each with all tests green, so a regression bisects to one helper.
