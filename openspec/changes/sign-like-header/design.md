# Design

## Context

- **Core, `NavigationDisplay`:** carries `next`, `follow`, `close`, `offRoute`, `wrongWay` and the waypoint roles. It turns them into sentences with `text` and `offRouteText`, which both apps show as the header and reuse for the notification and Live Activity.
- **Core, `Navigation`:** `wayToRoute(fix)` already gives the end of the line in "Way to the route on the map". `Guidance` carries the bearing and distance to the next knot for the off-route notices. The guided way has its turns and the distance left.
- **Apps:** the header is built in `PlannerScreen.kt` / `Navigating.kt` on Android and in `PlannerView.swift` on iOS, from these texts plus badges.

`knot-colours-from-signs` comes first. It brings `SignRegion`, `KnotPlate`, the corner symbols and the dark off-route header colours.

## Goals / Non-Goals

**Goals:**
- One core model of the header state, so both apps draw the same row and the screen reader text comes from the same place.
- A fixed header height per text size, the same in every language.

**Non-Goals:**
- Changing the texts used outside the app or for speech. `text` and `offRouteText` stay for them until `sign-like-lock-screen`.

## Decisions

### A `HeaderRow` state in the core, beside the existing texts

**Choice:** add a sealed `HeaderRow` to `NavigationUpdate`:
- `Close(next, follow, distance)`
- `Far(next, follow, distance)`
- `End(next, distance)`
- `OffRoute(bearingRelative, distance, nextKnot?)`
- `Guided(turn, turnDistance, street?, distanceLeft)`
- `ToRoute(bearingRelative, distance, nextKnot?)`
- `WrongWay(next, distance)`

Each badge in it is a `HeaderBadge(ref, plate, filled, role?)`. `HeaderRow.spoken(language)` gives the screen reader text. The apps only lay the row out.

**Why:** the state rules (close from the reminder, end, first knot, on the network) are already in the core. Leaving the choice to the apps would duplicate them twice.

**Alternative:** let the apps parse `NavigationDisplay`. That would duplicate logic and give no tests on iOS.

### Sign-like fill in the header

`filled` is computed in the core:
- **Dutch knots:** the next knot is filled; the number to follow, the knot after it (far state) and the small badge off route are white.
- **Other regions and no region:** always filled.

The role still adds the corner symbol. This differs from the map's role-by-fill on purpose: the header shows what the rider will see on the post. The map shows which knots were chosen.

### Off route: the arrow and distance to the place where the rider gets back onto the route

The arrow and distance use `wayToRoute(fix)` (the end of the dashed line) when it exists, else the next knot (no courses).
- **Relative bearing:** the bearing to that point minus the riding direction, from the same course-over-ground the map's arrow uses. When the rider has not moved recently, relative to north.
- **Spoken notices** keep the bearing and distance to the next knot, as the guidance rules in "Off route" are stated for the knot.

**Accepted mismatch:** the header may say 150 m while the voice says 1.4 km to 02. The voice names a signpost, and the header shows the shortest way back. The maintainer asked for "how far to get back on the route".

### "On a knot network path"

The rider counts as on a network path within 30 m of any course in the installed network geometry. This uses the same nearest-course lookup as the off-route detection, but over all connections instead of the route's legs. It is evaluated only while off route or on the way to the route, at the fix rate those states already use, so battery use doesn't change.

### On the way to the route = "not yet on the route since start"

`Navigation` gets `joinedRoute: Boolean`. It starts `false` when navigation begins off route, and becomes `true` the first time the rider is on route by the usual rules (within 30 m of the course, or passing a route knot). Off-route detection is unchanged. Only the header state and its colours depend on the flag.

### Fixed-height row

The row has one height per text size:
- **Badges:** at map size, which doesn't grow with text size ("Knot badges at large text sizes").
- **Distance:** grows with text size. When it no longer fits, it wraps into a second line below, at the same rule as today.
- **Stop:** a fixed-size icon button.

No language-dependent element is left in the row. Street names on a guided way are ellipsized to one line.

### Arrow between the badges

A plain → until `turn-at-knot` gives it a direction. The far state uses ↑ before the next knot and a small › before the knot after it, as in the mockups.

## Risks / Trade-offs

- **[Riders used to the sentences]** The header no longer says "Off route". → The dark header, the symbol and the spoken "Off route" carry it, and screen readers keep the words.
- **[Header and voice give different distances off route]** → Stated above. If it confuses on a ride, the voice can follow the header in its own change.
- **[Course-over-ground noise at low speed]** The relative arrow can swing. → Use the same smoothing and "not moved recently" rule the map's rider arrow uses.
- **[A street name changes the width]** → Ellipsized to one line. It's data, not app language.

## Migration Plan

App-only change. No data or format change. Rollback by reverting the apps' header to `NavigationDisplay.text`, which stays.

## Open Questions

- The exact off-route symbol and the U-turn icon may be refined on the device. That doesn't change the specs.
