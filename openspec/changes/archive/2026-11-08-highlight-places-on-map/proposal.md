# Proposal

## Why

On a ride, the questions are often where to eat, where to buy food, where the next bike shop is, and where a train can take you home. The base map already has these places from OpenStreetMap, but draws them small and grey among every other shop, so they're hard to find while planning and impossible to spot at a glance while riding.

## What Changes

- **Places that matter on a ride stand out:** the map draws five kinds of places with their own clear icon on a coloured disc, distinct from the knots' sign colours:
  - **Food and drink:** restaurants, cafés, fast food, bars and pubs, beer gardens, ice cream.
  - **Supermarkets.**
  - **Bicycle shops**, including repair, and bicycle repair stations where the map data has them.
  - **Railway stations** and halts.
  - **Water and toilets:** drinking water and public toilets.
- **The same places in the base map's own grey style are left out,** so each place shows once.
- **From zoom 14,** the first zoom where the map data has all of them, and with their names from zoom 16.
- **The knots, the route and the rider stay on top.** A place gives way to a knot's badge where they would overlap.
- **While planning and while navigating,** on the light and the dark map, for cycling and hiking.
- **A switch in the info sheet** turns the places off, for riders who want a plain map. It's on by default, and the choice is kept.
- **No new data:** the places come with the base map's tiles, so they're there wherever the base map is. Nothing is downloaded or sent beyond what the map loads today.

## Out of Scope

- **Places without the base map,** e.g. offline on first start. They come with the map tiles.
- **Our own place data** from the pipeline, places along the route at every zoom, a list of places on the strip or in the header ("next supermarket in 14 km").
- **Opening hours** and whether a place is open at the expected arrival.
- **Tapping a place** for details, or to make it a route point. Taps keep going to knots and route points as now.
- **Searching for places by kind.** The place search stays as it is.
- **Choosing which kinds to show.** There's one switch for all of them.
- **Other kinds,** such as cash machines, bike rental, campsites or pharmacies. They stay in the base map's grey style.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`: a new requirement for showing places useful on a ride, and the switch to turn them off.

## Impact

- **Core:** a table of the place kinds with their OpenMapTiles classes and subclasses, shared by both apps and tested once.
- **Apps:** symbol layers on the base map's `poi` source layer, added at runtime to the light and the dark style, the icons drawn by the apps' badge code, the base map's own POI layers filtered to leave the five kinds out, and the switch in the info sheet.
- **Pipeline:** none. `:pipeline:darkStyle` stays as it is, as the layers are added at runtime.
- **Outside the code:** none. No new service, so the privacy policy and the store declarations stay as they are.
