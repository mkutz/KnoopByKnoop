## Why

Below the panel's actions, one or two lines of small text say whether the ways and the map for the route are loading, loaded or failed. They appear a moment after the route is complete, which makes the sheet jump, and they are a lot of words for a status the rider takes in at a glance. The maintainer likes the information but wants it shown with symbols.

## What Changes

- The two lines go. Instead, the end of the action row (Back to start, Export GPX, Clear, Undo) shows a map symbol for the base map and, on cycling routes, a network symbol for the ways.
- Each symbol shows its status: a small spinner while loading, a checkmark when loaded, a warning mark when it couldn't be loaded.
- The symbols take no extra line, so the sheet no longer changes height when they appear or change.
- A screen reader reads each symbol as the sentence shown today, e.g. "Ways for this route are loaded". A tap on a symbol shows that sentence for a moment.

## Capabilities

### New Capabilities

### Modified Capabilities
- `network-sync`: "Path data ready for the ride" and "Base map ready for the ride" show their status as symbols in the action row instead of lines of text.
- `map-planning`: "Clear actions in the planner panel" holds the symbols at the row's end; Back to start shows its icon only where the row does not fit.

## Impact

- Android: `PlannerScreen` (`RouteActions`, `PathDataNote`, `BaseMapNote`), new drawables for the map, the network and the status marks if Material symbols don't cover them.
- iOS: `PlannerView` (`routeActions`, `pathDataNote`, `baseMapNote`) with SF Symbols.
- Strings stay; they become the symbols' accessibility labels and tap text.
