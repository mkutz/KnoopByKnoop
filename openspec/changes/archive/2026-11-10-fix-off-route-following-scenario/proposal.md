# Proposal

## Why

`knot-navigation` says two different things about the map off route: "Following the rider" / "Off route while following" shows the rider and the next knot, while "Way to the route on the map" / "Both in view" shows the rider and the end of the line to the route, which is usually a point of the course, not the knot. The first came with `improve-navigation-map` (2026-10-29), when the line still went to the knot; "Way to the route on the map" replaced that two days later (`guide-back-to-route`), and `fix-ride-display-bugs` (2026-11-05) copied the old scenario along unchanged. Both apps and the core's map focus do what "Way to the route on the map" says. Found while sorting the scenarios in `check-scenario-tests`.

## What Changes

- "Following the rider" / "Off route while following" says the map shows the rider and the end of the line to the route, as described in "Way to the route on the map", instead of the next knot. Nothing else in the requirement changes.
- The open change `add-dynamic-navigation-zoom` modifies "Following the rider" too and carries the old wording; its delta gets the same wording, so archiving it later does not bring the old one back.

The scenario stays rather than going: OpenSpec 1.17 refuses a MODIFIED requirement that leaves out a scenario of the current spec, and offers no way to remove a single scenario. Pointing to "Way to the route on the map" keeps the rule itself in one place.

Out of scope:
- Any change to the apps or the core: they already follow "Way to the route on the map".
- The rest of `add-dynamic-navigation-zoom`'s delta, including its exception for tricky spots in "Way to the route on the map".
- `openspec/checked-by-hand.md`: the scenario keeps its name and stays checked on the devices.

## Capabilities

### New Capabilities

### Modified Capabilities
- `knot-navigation`: "Following the rider" / "Off route while following" agrees with "Way to the route on the map".

## Impact

- `openspec/specs/knot-navigation/spec.md` on archive, `openspec/changes/add-dynamic-navigation-zoom/specs/knot-navigation/spec.md`.
- No code, no tests, no CHANGELOG entry.
