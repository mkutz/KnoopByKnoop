# Proposal

## Why

Navigate pressed before the network's courses have loaded runs the whole ride without courses:
- no "off route"
- distances and directions to the knots' main positions instead of their signposts
- no tricky spots, turns at knots or zoom on them

Nothing tells the rider. The courses load in about 0.7 s on the maintainer's iPhone but took 19 to 25 s on the emulator (2026-11-03 and 2026-11-11), so a slower phone or a fresh start after a data update can hit it. The maintainer has not met it on a ride; it turned up twice in emulator checks.

## What Changes

- A navigation started without courses takes them as soon as they have loaded, without stopping. From then on it navigates along the courses as if it had started with them:
  - off route
  - signposts
  - tricky spots and the zoom on them
  - turns at knots
  - the course drawn ahead
- The knots already passed stay passed. The next knot stays the same, unless the rider is already past it along the course.
- If the rider is away from the course when the courses arrive, the usual off-route handling follows on the next position.
- Both apps hand the courses to a running navigation when they finish loading.

Out of scope:
- Making the courses load faster. The emulator's slow loading and its out-of-memory crash have their own notes.
- Holding back Navigate until the courses are there; the maintainer chose taking them mid-ride (2026-11-11).
- Navigating without courses when none are installed, which stays as it is.
- Courses of another network than the one being navigated.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: a new requirement, "Courses loaded after the start".

## Impact

- `modules/core`: `Navigation` and `NavigationSession` take courses into a running navigation; tests in `NavigationSessionTest`.
- `apps/android`: `AppViewModel` (courses loaded while navigating), `Navigating`, `NavigationService`.
- `apps/ios`: `AppModel`, `Navigator`.
- No pipeline or data changes.
