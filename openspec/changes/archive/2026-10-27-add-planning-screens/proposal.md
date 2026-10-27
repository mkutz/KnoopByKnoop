# Proposal

## Why

The core can plan routes over knooppunt networks and the weekly pipeline publishes the networks, but neither app can show or use them yet. This change delivers the first usable version of both apps: get a region onto the phone and plan a route on a map, the way existing planners make hard (see `route-planning`: free editing of start, end and stops).

## What Changes

- **Regions:** both apps list the regions of the published index, download a region's networks (cycling and hiking) with their geometry in the background, verify them before installing, offer updates, and can remove a region again.
- **Map:** the selected network of an installed region is shown on a map: knots with their numbers and the connections between them along their actual course (see `network-data`), over an online base map.
- **Planning on the map:** tapping knots sets start and end, further taps add stops where they add the least distance. The route is shown on the map, with its knot sequence (e.g. `12 → 7 → 45`) and total distance.
- **Waypoint list:** the waypoints can be reordered and removed in a list next to the map.
- **Keep the current route:** the route being planned is restored after the app restarts.
- Shared logic for all of this (planning session, region state, nearest knot lookup, map data, route persistence) lives in the core, so both apps behave the same and the behavior is tested once.

Out of scope:

- Navigation while riding, sharing routes and QR codes, saving several named routes
- An offline base map: without network access the knot network and planning keep working, only the base map is missing
- Routes across regions (e.g. from the Netherlands into Belgium)
- Searching knots by number

## Capabilities

### New Capabilities
- `region-management`: listing, downloading, verifying, updating and removing regions on the device
- `map-planning`: showing a network on the map and planning a route on it, including the waypoint list and keeping the current route

### Modified Capabilities

## Impact

- `modules/core`: planning session, region state, nearest knot lookup, map data (GeoJSON) and route persistence, with tests
- `apps/android`: MapLibre Android, `DownloadManager` for downloads, Compose screens for regions and planning
- `apps/ios`: MapLibre Native via Swift Package Manager, background `URLSession` downloads, SwiftUI screens
- Network access to the GitHub release `network-data` and to an OpenStreetMap vector tile service; the apps show the required map attribution
