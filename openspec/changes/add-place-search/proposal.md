# Proposal

## Why

To plan a route, users first have to find the place where it starts or ends on the map by panning and zooming. Most of the time they know where they want to go: a station, a town, an address, a café. Map apps like Google Maps let them type it and jump there.

## What Changes

- A search field on the map. While the user types, a list shows matching places, stations, addresses and businesses.
- Tapping a hit moves the map there, zoomed in far enough that knots are shown and can be tapped, and marks the place with a marker.
- Search uses the Photon geocoder (OpenStreetMap data, photon.komoot.io), with results limited to and ranked around the covered area.
- Without internet access, the search field is disabled and says that search needs an internet connection.

Out of scope: searching offline, searching knot numbers (they are not unique and mean little on their own), using the device location, routing to a searched place directly, a search history.

## Capabilities

### New Capabilities
- `place-search`: finding places by name or address and showing them on the map

### Modified Capabilities

## Impact

- `modules/core`: building Photon requests and reading their responses into search hits, merging duplicates, describing hits
- `apps/android`, `apps/ios`: search field and hit list over the map, debounced requests, the marker, the disabled state without internet access
- Attribution for Photon in the info sheet
- A new external service at runtime: photon.komoot.io, with its fair-use expectations
