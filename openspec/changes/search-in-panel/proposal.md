# Proposal

## Why

The maintainer's TestFlight feedback on build 25 (2026-11-09, 08:02 and 08:04) showed two problems with the place search on the iPhone's planning screen. The field floats above the waypoint sheet, and the map's button column, centred on the map, ends up partly under it when the sheet is tall: the button for the nearest knots was half hidden. The field also draws its own glass, which looks different from the sheet's right below it. The maintainer chose to collapse the search into a button while it isn't used, and to put the field into the panel.

## What Changes

- While not in use, the search is a round magnifier button in the map's button column on iOS, styled like the zoom buttons. The column ends just above the sheet and moves with it, so a tall sheet covers none of its buttons.
- Tapping it opens the search field at the top of the waypoint sheet in portrait, or of the side panel in landscape, with the keyboard; the places found show in the panel.
- Picking a place, Cancel or tapping the map collapses it back into the button; clearing the field keeps it open to type again.
- No search field floats over the map any more, so it covers no button, and the search shares the panel's glass.
- Without internet access, opening the search shows the field disabled with its message, as the field does today.

## Out of Scope

- **Android:** its search stays at the top of the map, where it covers nothing.
- **What the search finds**, how hits are listed and what a picked hit does: unchanged.
- **The attribution and the network switch:** they stay on the map.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `place-search`: "Search while typing" changes where the field is on iOS: a button on the map, and the field in the panel while searching.

## Impact

- **iOS:** `PlannerView.swift` (the search overlay above the sheet goes, the panel gets the field and the hits), `PlaceSearchView.swift`, `NetworkMapView.swift` (the magnifier button in the button column), `Localizable.xcstrings` (its label).
- **Core and Android:** none.
