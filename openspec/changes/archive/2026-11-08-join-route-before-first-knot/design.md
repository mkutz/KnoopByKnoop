# Design

## Context

- `Navigation.start` begins on the leg whose course is nearest within 100 m (`OFF_COURSE_METERS`), the earliest of legs about as near; farther away it sets the next knot to the first one and `offRoute`.
- `Navigation.update`, off route, ends `offRoute` when the fix is within 30 m (`ON_COURSE_METERS`) of the courses around the current leg. With the next knot at index 0, "around the current leg" is the first leg's course. So reaching it before the first knot says "back on route" while the next knot stays the first one, and no line ahead exists (`courseAhead` is the leg before the next knot). That is what the maintainer saw.
- Joining on the way to the start only happens at a knot of the route (`passedRouteIndex`), see "Joining the route on the way to the start".

## Goals / Non-Goals

**Goals:**
- Following any leg's course before the first knot joins the route on that leg.
- A short crossing of a course does not.
- No "back on route" without a next knot ahead.

**Non-Goals:**
- Changing where navigation begins at the start.
- Choosing a different first knot, e.g. the nearest one, when starting away.

## Decisions

### Join after following a leg's course for 50 m
- **Rule:** before the first knot (next index 0, off route), the leg is remembered when a precise fix is within 30 m of its course. Once later precise fixes stay within 30 m of the same leg and the rider has moved 50 m along it, in either direction, the route is joined: the next knot becomes the leg's end, the place on the route is set as in `start`, and the "back on route" notice names that knot.
- **Leg choice:** the same as `start`: the nearest course, the earliest of legs about as near. One function serves both.
- **Crossing:** a road crossing a course at a right angle stays within 30 m for less than 50 m of movement along it, so it does not join.
- **Alternative:** joining at the first fix within 30 m. Simpler, but a rider crossing the route on the way to its start would skip all legs before the crossing.

### No "back on route" before joining, except close to the first knot
- Before the first knot and more than 200 m (`REMINDER_METERS`) from it, the existing "within 30 m of the course ends off route" branch no longer applies; only joining ends off route.
- Within 200 m of the first knot, the old behaviour stays: the course there is the way to the first knot, and the existing scenario "Distance to the first knot" (back on route, 24 next, "80 m") still holds. Found when its tests failed during implementation.

### Wrong way after joining
- Joining sets up the same state as starting on the course, so the existing wrong-way watch notices riding against the route.

## Risks / Trade-offs

- **[A course along a parallel street]** → Riding 50 m within 30 m of a course on a parallel street joins the route there. That is the same tolerance as starting on the course.
- **[A rider who wants the start, e.g. a round trip from a station]** → Riding along the route towards its start joins it on the way, with the start knot ahead; riding along it away from the start joins it and the wrong-way watch speaks up. Starting at the first knot still works as before.

## Migration Plan

App update only; nothing stored changes.
