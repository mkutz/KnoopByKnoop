# Design

## Context

- **The video:** a round trip 02 → 28 → 27 → 83 → … → 02, using the L 143 by 28 in both directions. At the fork, two orange lines lie on top of each other, with arrows both ways.
- **The layers** (`NetworkMap`, `NetworkMapView`), in order: `route` (8 wide), `route-unreachable`, `route-direction` (arrows along the line, from the `route` source), `ridden-track` (4 wide, on top), then the knots. `route` has one line per leg between waypoints, in riding direction.
- **The courses drawn:** `NetworkGeometry.courses(knots)` chooses the signposts over the whole route, from `routes-through-signposts`. `Navigation` keeps the main courses for its place and off-route distance.
- **MapLibre:** `line-offset` moves a line to the right of its direction, in screen units, and follows bends. Symbols placed along a line do not follow it, but `icon-offset` moves them, rotated with the line. Both exist on Android and iOS.

## Goals / Non-Goals

**Goals:**
- At a fork, the map alone tells which way to go now.
- A stretch used twice can be seen while planning.

**Non-Goals:**
- Changing routing, or detecting stretches used twice.

## Decisions

**Offset everywhere, not only on stretches used twice.** All route lines get the same offset to the right. Finding the shared stretches would need comparing courses, which rarely match point by point in both directions, and the line would jump sideways where a shared stretch begins. At these widths, the offset on a stretch used once is about the width of a road on the map.

**Widths and offsets:**

| | Line width | Offset |
|---|---|---|
| Route while planning | 5 | 4 |
| Route while navigating (the rest) | 3 | 4 |
| Leg ahead while navigating | 7 | 4 |
| Ridden track | 3 | 4 |

Tuned on the emulator: at width 6 and offset 3, the two lines touched and read as one thick band. With a gap, they read as two, with the road's line between them.

**The leg ahead is its own source and layers.** `Navigation` gets `courseAhead: List<GeoPoint>?`, the drawn course between the last knot and the next: from `geometry.courses(knots)`, kept when navigation starts, so it is the same line as on the map. It is `null` before the first knot. `MapFeatures.legAhead` makes it a GeoJSON line. The apps add `route-ahead` and `route-ahead-direction` above `route`, and below the track.

The source only changes when `nextIndex` does, so it costs nothing per fix.

**While navigating, the layers change, not the data.** `route` gets the thin width and `route-direction` is hidden; when navigation ends, they go back. So nothing is rebuilt.

**The rider and the knots stay on the course.** Only lines and their arrows are offset. The rider's arrow is on the road, up to 4 dp from the line, and the badges stay where the knots are.

**Starting on a stretch used twice takes its first use.** Found on the emulator: starting the round trip 02, 28, 27, 28, 02 began on the way back, as `Navigation.start` takes the nearest leg, and a rounding difference decided between the two uses of a stretch. Now the earliest leg within 10 m of the nearest wins.

## Risks / Trade-offs

- **Sharp bends:** a line offset around a sharp bend can look kinked. It is checked at Siegburg 02 and at the L 143, and the offset shrinks if it looks wrong.
- **A thin rest of the route** is less visible at a glance while zoomed out. It stays orange, and the track and the next knot's ring are on top.
- **Arrow placement:** `icon-offset` depends on the image's orientation. Checked on both platforms, where the arrows must sit on their own lines.
