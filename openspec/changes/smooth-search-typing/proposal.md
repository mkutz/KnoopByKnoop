# Proposal

## Why

The debug timing log on the maintainer's iPhone on 2026-11-09 shows that every keystroke in the place search stalls the main thread for about 110 to 125 ms (see `search-in-panel`'s manual checks). Typing feels sticky. The search's text and its results are published on `AppModel`, which the planner, the map, the waypoint list kept under the search, and the message alert all observe, so each character renders them all again.

## What Changes

- The search's text, results and running request move into their own observable model, which only the search view observes.
- `AppModel` keeps what the rest of the app needs from the search: the marker of a picked place and the note while the map shows it.
- Typing renders only the search view; the planner, the map and the waypoint list stay as they are.

## Out of Scope

- **Android:** no lag has been measured there.
- **What the search finds** and how it looks: unchanged.
- **The map's drawing after a pick:** handled in `search-in-panel`.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `place-search`: a new requirement that typing in the search shows each character without delay.

## Impact

- **iOS:** a new `PlaceSearchModel` (text, results, request), `AppModel` (search state moved out, `choose` and the marker stay), `PlaceSearchView` and `PlannerView` (which model they observe).
- **Core and Android:** none.
