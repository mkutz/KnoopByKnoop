# Design

## Context

See the proposal. The apps use `Navigation`'s public surface: `knots`, `nextIndex`, `offRoute`, `arrived`, `lastFix`, `progressLine`, `language`, `display`, `header`, `progress`, `update`, `start`, `inLanguage`, `precision` and a few more; the change keeps it.

## Goals / Non-Goals

**Goals:**
- Adding a per-fix field touches one data class.
- Each step of `update()` readable on its own: what it reads (the navigation before) and what it changes.
- The same output for every fix, shown by tests that compare whole rides, not single assertions.

**Non-Goals:**
- Faster updates; one data class copy per step is cheap next to the geometry of a fix.

## Decisions

### `RideSetup` and `RideState`

```kotlin
private class RideSetup(knots, courses, signpostCourses, drawnCourses, locator, routeKnotIds,
                        route, localTime, legTurns, tricky)

private data class RideState(nextIndex, offRoute, arrived, remindedIndex, offRouteKnot, passedKnot,
                             confirming, lastPreciseFix, lastFix, ferryWatch, pace, place,
                             progressLine, linePlace, guided, directionWatch, joinedRoute, joining,
                             passedAtMillis, language, speechLanguage, rejoin)

class Navigation private constructor(private val setup: RideSetup, private val state: RideState)
```

Public properties read through: `val nextIndex get() = state.nextIndex`, and so on.

### One way to the next navigation

```kotlin
private fun with(change: RideState.() -> RideState): Navigation =
  state.change().let { Navigation(setup, it.copy(joinedRoute = it.joinedRoute || !it.offRoute)) }
```

replaces `copy(...)`; `copy(offRoute = true)` becomes `with { copy(offRoute = true) }`. The `joinedRoute` rule gets a comment pointing to `knot-navigation` "Header on the way to the route".

*Alternative:* keep `copy(...)` and only move the rule out. Rejected: the 22 parameters re-listed positionally are the part that breaks when a field is added.

### `update()` as steps

```kotlin
fun update(fix, visible): NavigationUpdate {
  // the early returns as today
  var step = Step(forgetPassedKnot(fix), listOf())
  step = when (val passed = step.navigation.passedRouteIndex(fix)) {
    null -> if (joiningFirstLeg(fix)) step.then { joinFirstLeg(fix) } else step.then { followCourse(fix) }
    else -> step.then { passKnot(passed, fix) }
  }
  ...reminder, place, guidance, end of the fix
}
```

Each step is a member of the navigation it starts from, so inside it `this` is always the state before the step, and every read of the state before the fix that today goes through `this.` (e.g. `offRoute` when telling "back on route") gets an explicit parameter `before: Navigation` where it differs. The `confirming` flag, set by the off-route check and applied at the end, becomes part of the step's result instead of a `var` beside it.

`backOnRoute(newNext: Int = nextIndex, place: Double? = null)` and `headingFor(newNext, place, pace, fix)` replace the repeated changes; each existing variant maps onto them with a parameter, written out in the commit that moves it.

### Proving nothing changed: whole rides as tests

1. Before touching `Navigation`, a test `NavigationReplayTest` records whole rides on the test network of `NavigationTest` into a compact text (per fix: notices, next index, off route, arrived, place rounded to 0.1 m, progress line, precision), for rides that pass every branch of `update()`: along the route to the end, a detour off and back, the wrong way and turning around, starting away from the route and joining a leg before the first knot, a coarse fix, a position delivered again, a straight line to a route point. The expected texts are written by the current code and committed with it.
2. Every later step keeps those texts identical; so do the 146 tests of `NavigationTest`.
3. The real-data replay of `share-line-geometry` (`checks/Replay2.kt`) gives the same 908 lines as after that change.

## Risks / Trade-offs

- [The recorded rides miss a branch] → They are chosen per branch of `update()` above, and a line coverage report of `Navigation.kt` from the JVM tests is read before and after; branches no ride reaches get a ride.
- [A large diff in the most important file] → One commit per step of the plan, each with the recorded rides identical.
