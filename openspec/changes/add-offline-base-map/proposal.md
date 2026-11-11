# Proposal

## Why

Without a connection the map shows the knots and the route on an empty background. The base map, OpenFreeMap's tiles, is the only part of the app that still needs the internet on a ride.
- The maintainer met it on 2026-11-10: a fresh build on the Pixel with no data connection showed no streets, villages or rivers until the phone joined a hotspot.
- The rest already works offline: the networks, their courses, the ways along paths, place details and navigation.

The maintainer wants "works without a connection" to hold from start to finish (2026-11-11).

## What Changes

- **The app stores the base map around a planned route.** This works for cycling and hiking routes, as path data does around cycling routes, once the route has stopped changing. Navigating starts the download too if parts are still missing.
  - The area is a corridor along the route's course, detailed close to it and coarser around it, so the map is readable while riding and while looking around the route.
  - The tiles requested depend only on the route, never on the rider's position.
- **OpenFreeMap's terms for offline downloads are kept.**
  - At most 3,000 tiles and 50 MB per route.
  - At most 4 requests at a time.
  - A User-Agent that names the app, which every base map request sends from now on.
  - A route too long for that stores its map without the most detailed level where it doesn't fit, as the next level is still readable zoomed in.
- **Only the current route's base map is kept.** The map of a new route replaces it once that is downloaded. The app's usual cache of viewed tiles stays as it is.
- **The panel shows the state** next to the ways: "Map for this route is loading", "loaded" or "couldn't be loaded".
- **The info sheet gets a base map entry** after the place details, with its size, a delete button and a short explanation: OpenFreeMap can see the rough area of a planned route, never where the rider is.
- **The light and dark map both work offline,** as the dark style uses the same tiles, fonts and icons.

Out of scope:
- Downloading the base map of a whole region or country. It is too big, and OpenFreeMap asks to be contacted for that.
- Hosting base map tiles ourselves, or switching to another provider.
- Keeping the base maps of several routes, or of routes planned earlier.
- A base map on a fresh install that has never been online. Nothing can be downloaded without a connection.
- Satellite or terrain layers.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `network-sync`:
  - New: "Base map around the route", "Base map ready for the ride", "Keeping the base map", "Base map in the info sheet" and "Base map requests".
- `map-planning`:
  - Modified: "Show the network on the map". Its scenario "No network access" says only the base map may be missing; now that holds only away from the planned route.

## Impact

- **`modules/core`:** the route's corridor (the tiles along the course) for the download.
- **`apps/android`:**
  - MapLibre's `OfflineManager`, with an offline region per zoom band for the route.
  - Its HTTP client limited to 4 requests per host and sending the app's User-Agent.
  - The panel line, the info sheet entry, and strings in four languages.
- **`apps/ios`:** the same, with `MLNOfflineStorage`, `MLNShapeOfflineRegion` and the session configuration of `MLNNetworkConfiguration`.
- **Storage:** up to 50 MB for the current route's map, in addition to the path data and place details.
- **Privacy:** OpenFreeMap sees the rough area of a planned route when its map is downloaded. It already sees the areas a user looks at on the map.
