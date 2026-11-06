# Proposal

## Why

An Android tester searched for "Amsterdam" in landscape on 2026-11-05. With the on-screen keyboard open, only a thin strip of the screen is left, and the side panel's distance, Navigate and knot badges still take its right part, where they are of no use while typing. The tester would rather not see them. The panel can't be covered by the keyboard instead, as its top is always above it.

## What Changes

- **Android, landscape:** while the on-screen keyboard is open for the search, the side panel slides aside, and the map with the search field and its hits uses the whole width. A chip with the route's start, end and distance stays on the map, so the route is clearly still there; tapping it closes the keyboard. When the keyboard closes, the panel slides back, with the route unchanged.

## Out of Scope

- **Portrait,** where the sheet is below the keyboard as before.
- **iOS,** where the search moves above the keyboard, see `place-search`.
- **The slow appearance switch** from the same tester, a change of its own.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`: "Landscape layout" gets a scenario for typing in the search.

## Impact

- **Android:** `PlannerScreen`'s landscape layout leaves the panel out while the keyboard is open.
