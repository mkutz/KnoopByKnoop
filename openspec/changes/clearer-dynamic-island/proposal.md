# Proposal

## Why

On the hike on 2026-11-14, the compact and the minimal Dynamic Island cut off the small corner symbol of the end (■) on the badge 71, and of the start and stops likewise. The spec already says the island shows its element "made smaller rather than cut off". In that little room the symbol tells the rider nothing they need, so the maintainer wants it left out there. The distance next to it is small and changes the island's width as it counts down.

## What Changes

- On iOS, the compact and the minimal Dynamic Island show the badge of the start, a stop or the end without its corner symbol.
- The expanded Dynamic Island and the Live Activity on the lock screen keep the corner symbols, as in the app.
- The compact island's distance is larger and bold, with digits of equal width, so the island keeps its width while it counts down. It stays a number: before a turn, the metres left matter more than a progress ring.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `knot-navigation`: "Navigation with the screen off" — the compact and minimal island show badges without the corner symbol, and the compact island a larger distance of steady width.

## Impact

- iOS widget extension: the Live Activity's compact and minimal views and the compact distance, and the shared sign view, which gets an option to leave the corner symbol out.
- No change on Android, which has no Dynamic Island, or in the core.

## Out of scope

- A progress ring instead of the distance, considered and dropped.
- The corner symbols in the app, the expanded island, the lock screen and the Android notification.
- Off-route guidance across water, also from the hike, which becomes its own change.
