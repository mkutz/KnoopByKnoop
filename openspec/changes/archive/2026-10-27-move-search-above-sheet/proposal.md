# Proposal

## Why

On iOS, the search field sits at the top of the map, above the network switch, while the waypoint sheet takes the bottom of the screen. Many iOS apps keep search at the bottom, within reach of the thumb. Search belongs to the map, not to the route, so it should stay on the map rather than move into the waypoint sheet. Placed right above the sheet, it is at the bottom and still on the map.

## What Changes

- On iOS in portrait, the search field moves from the top of the map to the bottom of the map, floating right above the waypoint sheet's top edge and following it when the sheet is dragged to another height. When the sheet is pulled up all the way, the field is hidden, as the map can then not be seen.
- While the user types, the field rises above the keyboard and the hits are shown above the field, over the map. Choosing a hit, or clearing the search, dismisses the keyboard and puts the field back above the sheet.
- On iOS in landscape, the field floats at the bottom of the map, next to the side panel.
- The network switch and the info button stay at the top of the map and get the space the field leaves. The attribution moves above the field.
- On iOS 26 and later, the views floating over the map (the search field and its hits, the network switch with the info button, the loading notes, the attribution) use Liquid Glass instead of a blurred material. Earlier iOS versions keep the material. The sheet and the system controls in it get Liquid Glass from iOS itself.

Out of scope: Android, which keeps the field at the top of the map, as in Google Maps, and gets no Material counterpart of Liquid Glass; replacing the system sheet with a custom panel; changes to what is searched, how hits are ranked or how the map jumps to a hit; Liquid Glass on the map's knots, routes or marker.

## Capabilities

### New Capabilities

### Modified Capabilities
- `place-search`: where the search field and its hits are shown, which differs between Android and iOS

## Impact

- `apps/ios`: `PlannerView` tracks the sheet's height and places `PlaceSearchView` above it (or at the bottom of the map in landscape), with the hits above the field; the attribution and the map's notes move; a modifier that uses `glassEffect` on iOS 26 and later and the material before
- Builds need the iOS 26 SDK or later (Xcode 26+), which local builds and CI's `macos-latest` are expected to have; the deployment target stays iOS 17
- No changes to `modules/core` or `apps/android`
