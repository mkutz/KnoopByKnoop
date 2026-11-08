# Proposal

## Why

On the maintainer's ride of 2026-11-08 the route passed knot 83 twice: out (27, 83, 15) and back (…, 14, 83, 30). The log shows "Next 15, then the stop" at 14:41:18 after passing 83, and "Next 30, then 29" 38 seconds later: half the route skipped. The rider had not left 83's surroundings. Navigation looks for the first route knot the rider enters, from the next knot on; entering 83's circle again, by jitter or riding around at the junction, matched the second 83 on the route and skipped ahead to it.

## What Changes

- After passing a knot, entering the same knot's circle again only counts as reaching a later visit of it once the rider has been more than 200 m away from it since passing.
- Skipping ahead to any other later knot stays as it is.

## Out of Scope

- Routes that visit a knot twice in a row without going away, which planning does not create.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: "Skipping ahead" gets a scenario for a knot visited twice.

## Impact

- **Core:** `Navigation.passedRouteIndex` and the state it needs (the knot passed last and whether the rider was away from it).
