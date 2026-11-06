# Design

## Context

See `proposal.md` for the motivation. Today, both platforms have a `NAVIGATION_ZOOM` / `navigationZoom` of 15. The map goes there when navigation starts and on recenter. While following, the map only pans, with one `easeCamera` per fix over `FOLLOW_MOVE_MILLIS` (1 s), so the base map's labels don't flicker. Off route, the camera fits the rider and the end of the way back, no closer than 15. Moving or zooming by hand sets `following = false`. The map is always north-up.

The core already knows what the rules need: the next knot and the distance to it, the 200 m reminder (`REMINDER_METERS`), passing a knot within 30 m, the leg's course, and, with `add-offline-paths`, the guided way. `add-junction-pictures` will add doubtful junctions on the guided way.

## Goals / Non-Goals

**Goals:**
- The same zoom on both platforms, decided in the core and tested without a screen.
- No more camera work than today: one move per fix.

**Non-Goals:**
- Turning the map with the riding direction, tilting it, or zooming with speed.
- A setting for the levels.

## Decisions

### The core says what to show, not the zoom level
The core returns a `MapFocus` for each fix: a kind (base, knot, after knot, tricky spot, off route), the points that must be in view, and the closest and farthest view allowed in meters across. The platform turns the meters into a zoom level for its screen size and latitude. Zoom levels depend on screen size and the map library. Meters can be tested in the core and give the same picture on a small and a large phone.

The navigation zoom stays a platform constant of 15, as now. It's the zoom for the base kind.

Alternative: the core returns a zoom level. That's simpler, but it ties the core to the tile scheme and the screen size, and it would be wrong on tablets.

### The rider stays in the middle and the zoom is set once per rule
When a rule starts, the platform works out the zoom where a circle around the rider fits, with the distance to the farthest of the points as its radius, within the limits. Then it keeps that zoom and only pans, as today, until the kind changes. While the rider approaches a knot or a tricky spot, every point gets closer, so they all stay in view without changing the zoom. That keeps the labels as steady as they are now and keeps the rider in the middle, where they expect to be.

Fitting the bounds on every fix would frame the points more tightly, but the zoom would change a little with every fix. Labels would be placed anew each time and would flicker, the problem that "Following the rider" fixed.

Recenter applies the current focus in the same way, not the fixed 15.

### Order of the rules
Tricky spot, then knot, then after knot, then base. This is one `when` in the core, so the order is tested in one place. Off route keeps today's fit of the rider and the end of the way back, except before a doubtful junction on the guided way.

### Tricky stretches are found once per route
For each leg's course, the core walks the line and measures the turn at each point between the direction over the 10 m before it and the 10 m after it. This smooths out OSM's small zigzags of a few vertices. Turns of 60° or more that are within 100 m of each other are grouped into one stretch. This runs once when navigation starts, as the route is fixed while navigating. It's stored as distances along the route, so finding the next spot on a fix is a cheap lookup.

The 60° and 100 m are a first guess. Task 1.2 measures how often they trigger on real routes before any app work, like `add-junction-pictures` did. If they trigger every few hundred meters, the zoom would hardly ever be at the base level, and the maintainer decides how to narrow them.

### Doubtful junctions come from `add-junction-pictures`
On a guided way, the core takes them from the junctions that change finds when it builds the way. Until that change is done, there are none, and the rest still works. The tasks for this part are in their own group, so they can wait.

### After a knot is time-based
The zoom-out lasts 10 s from passing, counted from fix times, not a wall clock, so tests can drive it. A rule that wins ends it early. It doesn't come back after that.

## Risks / Trade-offs

- **[Risk] The map moves too much and distracts.** Three changes per knot: in at 200 m, out on passing, back after 10 s. → Check on a ride. The zoom-out is the easiest rule to drop if it's too much.
- **[Risk] The tricky-spot rule triggers too often** on winding paths in the woods. → Task 1.2 measures it first.
- **[Risk] Labels flicker when zooming.** Each change of zoom places labels anew once. That's acceptable a few times per knot, but not on every fix. → Zoom set once per rule; check on the emulator with a mocked ride.
- **[Trade-off] The rider stays in the middle,** so half the view is behind them. Moving the rider lower would show more ahead, but on a north-up map "ahead" can be any direction. → Kept in the middle; can be revisited with heading-up.
- **[Risk] Limits in meters look different on a tablet.** → Accepted; the limits are about what can be read, which doesn't depend on the screen.

## Open Questions

- Are 10 s after a knot long enough to take in the next knot's position while riding? To check on a ride.
- Should route points from `add-route-points` get the knot zoom too? Left out until that change is done.
