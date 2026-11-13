# Design

## Context

- **Core:** `Navigation.update` sends fixes less accurate than 50 m (`PRECISE_ACCURACY_METERS`) to `updateCoarse`, which never counts the rider as off route but sets `confirming` when such a fix is more than 100 m from the course, so `precision` asks for FINE. A fix of 50 m or better counts at once: more than 100 m from the course is off route.
- **iOS:** `Navigator.request` changes `desiredAccuracy` and `distanceFilter` whenever the core's precision changes. `didUpdateLocations` hands every delivered location to the core.
- **The ride's log (2026-11-08):** bursts of up to 1,781 precision changes within one second, during coarse sampling; both false alerts right after such stretches, and back on route with the next precise fix.

## Goals / Non-Goals

**Goals:**
- No "off route" from a position the next precise one contradicts.
- No precision flapping from locations delivered again.
- Unchanged behaviour when the rider really leaves the route, apart from the few seconds for a precise fix.

**Non-Goals:**
- A smaller off-route distance in towns.
- Platform-specific sampling changes.

## Decisions

### Ignore fixes not newer than the last one
- `update` returns the navigation unchanged, with no notices and the same precision, for a fix whose time is not after `lastFix.timeMillis`. This stops reprocessing of locations iOS delivers again after a changed request, which is the likely cause of the flapping.
- **Alternative:** debouncing precision changes in the app. It would hide the symptom, but old fixes would still be processed.

### Confirm off route with a fix of 20 m or better
- A fix between 20 and 50 m accuracy more than 100 m from the course sets `confirming` like a coarse one and says nothing; off route needs a fix of 20 m or better (`CONFIRM_ACCURACY_METERS`) more than 100 m from the course.
- **Delay:** with FINE sampling, a precise fix comes within a few seconds; a rider really leaving the route hears it that much later.
- **Knots:** passing a knot not on the route is not about accuracy, the knot's circle is 30 m; it counts at once as before.
- **Alternative:** two consecutive fixes off the course. Two coarse fixes from the same source can agree and both be wrong.

### The guided way after confirmation
- The guided way is searched when off route starts, so after confirmation; the log showed a 24.6 km search started by the false alert at 15:28.

## Risks / Trade-offs

- **[No precise fix comes, e.g. in a tunnel or a deep valley]** → The rider is not told they are off route until one comes. A rider without GPS cannot be guided back well anyway.
- **[A precise fix is itself wrong]** → Rare with GPS; the 100 m distance stays.

## Migration Plan

App update only.
