# Proposal

## Why

On iOS, the search field sits at the top of the map, above the network switch, while the waypoint sheet takes the bottom of the screen. Many iOS apps, Apple Maps first, keep search at the bottom within reach of the thumb, and put the hits in the sheet where the keyboard cannot hide them. The field over the map also covers part of the map that the user wants to see while planning.

## What Changes

- On iOS, the search field moves from the top of the map to the top of the waypoint panel: the sheet in portrait, the side panel in landscape.
- When the user starts typing in portrait, the sheet expands and shows the hits in place of the waypoints. Choosing a hit, or clearing the search, brings the waypoints back and lowers the sheet, so the map shows the hit.
- The sheet's smallest height grows by the height of the field, so the field and the route summary stay visible.
- The network switch and the info button stay on the map, and get the space the field leaves.

Out of scope: Android, which keeps the field over the map, as in Google Maps; changes to what is searched, how hits are ranked or how the map jumps to a hit; showing recent searches in the sheet.

## Capabilities

### New Capabilities

### Modified Capabilities
- `place-search`: where the search field and its hits are shown, which differs between Android and iOS

## Impact

- `apps/ios`: `PlaceSearchView` split into the field and the hit list; `PlannerView` puts both into the panel, switches the panel between hits and waypoints, controls the sheet's detent while searching, and moves the map's notes and attribution to fit the new sheet height
- No changes to `modules/core` or `apps/android`
