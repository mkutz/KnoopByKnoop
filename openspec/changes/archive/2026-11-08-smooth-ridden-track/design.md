# Design

## Context

- **The track today:** `RiddenTrack.add` drops a fix less accurate than 50 m and one within 5 m of the last point; everything else becomes a point. The apps draw the points as one line and take the riding direction from them (`directionAt`).
- **The fixes:** with the screen off and far from the next knot, navigation asks for `Precision.COARSE`, mostly without GPS. Such fixes are often 20–50 m inaccurate and scatter by about that much. With the screen on, `LIVE` gives GPS fixes of a few meters.
- **What the walk showed:** around the Markt in Siegburg, spikes of roughly 80 m out and back, and a knot at a roundabout where the route was walked once. Exact fixes were not recorded, so tests use made-up tracks of the same shape.

## Goals / Non-Goals

**Goals:**
- No lines drawn by scatter while the rider moves slowly.
- No spikes out and back.
- Unchanged track with good fixes, e.g. riding with the screen on.

**Non-Goals:**
- Sampling the location more often or more precisely.
- Map matching the track to paths or to the route.
- Averaging or curve smoothing.

## Decisions

### A fix must move farther than both inaccuracies together
- **Rule:** a fix is added when its distance to the last point is more than `max(5 m, its accuracy + the last point's accuracy)`.
- **Why:** two fixes can be up to their two inaccuracies apart while the rider stands still; only beyond that do their circles not overlap, so the rider has moved. Its own inaccuracy alone was the first idea, but standing with 30 m fixes then still drew lines up to 60 m long. With good fixes of 3–5 m this keeps a point every 6–10 m, which looks the same on the map.
- **Alternative:** a fixed stricter accuracy limit, e.g. 20 m. It would leave gaps exactly where the screen is off, and straight lines across them.

### A speed limit per network
- **Rule:** a fix is dropped when the distance from the last point divided by the time since it is more than 15 m/s on a cycling route or 4 m/s on a hiking route.
- **How:** `RiddenTrack` gets the route's network when navigation starts. The apps pass it where they create the track today.
- **Catches:** quick jumps when fixes come every few seconds. Slow-sampled spikes pass it, which is what the next rule is for.

### Out-and-back spikes are taken out afterwards
- **Rule:** when a fix C is added after points A and B, B is taken out if the track turns sharply at it, by an angle under 75° between B–A and B–C, B lies farther from the line A–C than A's and C's inaccuracies together, and going from A to B and on to C in the time between A and C would mean more than half the network's top speed. C is then added after A by the usual rules, without looking for another spike, so removals never cascade.
- **Speed:** a real out-and-back on the same path, e.g. to a café, has the same shape as a spike when fixes are sparse; only the speed tells them apart. Found when the first version erased such a detour point by point.
- **Why:** a single wrong fix far to the side, followed by a right one, is the typical spike, whether the rider goes on or turns back. Removing B is only possible once C is known.
- **Corners:** a right-angle turn keeps its corner (90° is not sharp enough). A hairpin or dead end of a single point loses its tip.
- **Detour:** a real detour and back takes many fixes; only one point at a time is checked against its neighbors, so a detour stays.
- **First idea:** "C comes back towards A" missed spikes to the side while riding on, the scenario "Spike out and back".

### Riding direction keeps using the points
- `directionAt` still uses the track's points. With fewer points in poor reception, the newest point can be older than the 10 s that count as standing, so the dot may show more often while walking slowly with the screen off. With good fixes, walking adds a point about every 8 s, still within the 10 s.
- When the screen is on, fixes are precise and the points as dense as before, so the arrow behaves as today where it is seen.
- **Alternative:** a separate list of recent raw fixes for the direction. More state for a case nobody looks at.

## Risks / Trade-offs

- **[Thresholds chosen without recorded data]** → They come from the walk's screenshot and typical accuracies. The maintainer checks the next walk and ride with the screen off; the numbers are constants in one place.
- **[A real sharp turn back, e.g. a dead end]** → The rule needs A–C under half of A–B, which a rider turning at a dead end matches only if the dead end is one point long. Then the dead end is missing from the track, which is acceptable.
- **[Cycling at more than 15 m/s downhill]** → 54 km/h. Fixes then are a few seconds apart and the next fix after a dropped one is measured from the same last point, so it is dropped too only while the speed stays above it.

## Migration Plan

App update only. A track exists only during one navigation, so nothing stored changes.
