# Proposal

## Why

Existing knooppunt planners (Fietsknoop, knooppuntnet) build a route as a growing chain of knots: once the end is set, knots can only be appended. Changing the middle of a route means starting over. Planning should instead work like a modern route planner: set a start and an end, then add, move and remove stops in between.

## What Changes

- A route is defined by an ordered list of waypoints (start, stops, end). The knots between consecutive waypoints are computed, not edited by hand.
- Stops can be inserted anywhere, reordered and removed. Only the affected legs are recomputed.
- A route can be a round trip (start equals end).
- The route reports its knot sequence and total distance.

Out of scope: map UI, navigation, route sharing/QR codes, automatic stop-order optimization and loop suggestions. Each gets its own change.

## Capabilities

### New Capabilities
- `route-planning`: planning a route over a node network from ordered waypoints, including editing the waypoints

### Modified Capabilities

## Impact

- `modules/core`: route model and shortest-path routing over a node network graph (in-memory; loading graph files comes with `add-network-graph-pipeline`)
- No platform or dependency changes
