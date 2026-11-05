# Proposal

## Why

Away from the route, the app only gives a straight line, a distance and a compass direction. The maintainer wants real guidance from wherever they are, before the first knot and after a wrong turn, along ways suited to bicycles, without an external routing service. A test on the Köln extract on 2026-11-04 showed that this is plausible: about 1 MB of path data around a 15 km route, routes in 1 to 32 ms in Python on a Mac, and lengths close to BRouter's. Nothing has been ridden or measured on a phone yet.

## What Changes

- **Path data:** the weekly pipeline also publishes the rideable ways of the covered area in one file, split into small squares. It holds the cycling network's surroundings only.
- **Download around the route:** the app fetches the squares within 5 km of a complete cycling route while planning, and shows when they are in. So guidance works offline on the ride, and nothing is requested because of where the rider is.
- **Routing on the phone:** a new router in the core finds a way that prefers cycle paths and quiet streets. It uses no network.
- **Guided way:** while off route on a cycling route, the app guides the rider to where today's dashed line ends: the course ahead, or the first knot before the route has begun. The way is drawn on the map and recalculated when the rider leaves it.
- **Turn notices:** the header, the lock screen, the notification and the voice give the next turn and the distance along the way, in place of the straight distance and compass direction.
- **Fallback:** with the feature off, without path data, e.g. more than 5 km from the route, without a way, on hiking routes, or more than 20 km from the target, off route works as it does today.
- **Optional, off by default:** which squares the app asks GitHub for reveals the rough area of a planned route, but never where the rider is. So the feature only runs after the user turns it on in the info sheet, where this is explained. While it's off, the app requests no path data and "Your location and your routes stay on your device" holds as today. The privacy policy and the store declarations describe what changes for those who turn it on.

## Out of Scope

- **Pictures of junctions and wording for doubtful junctions:** see `add-junction-pictures`.
- **Hiking:** walking has other preferences and more paths. It can follow later on the same format.
- **Turn guidance on the route itself:** on route, the rider follows the signposts, as now.
- **Changing the planned route:** the route stays fixed while navigating, and the guided way only leads back to it.
- **Planning with the path data:** route points away from the network stay in `add-route-points`, which could use this data once this change is settled. That plan isn't changed here.
- **Downloading the whole area up front, choosing a profile, or avoiding hills.**
- **Spoken street names:** names appear on screen only.

## Capabilities

### New Capabilities

- `path-routing`: which ways the on-device router uses and prefers, and what happens when there is no way.

### Modified Capabilities

- `knot-navigation`: "Off route", "Way to the route on the map", "Direction to the route" and "Direction outside the app" change while a guided way exists; new requirements for the guided way and its turn notices.
- `network-data`: the weekly publication includes the path data.
- `network-sync`: the user turns the feature on and off in the info sheet, and the app fetches, verifies, keeps and reports the path data only while it's on.

## Impact

- **Depends on:** `improve-off-route` must be archived first, as this change builds on its versions of "Off route" and "Way to the route on the map".
- **Pipeline:** a new path extraction and file writer, `update-network-data.yml` with a new size budget and more time and memory, and an optional `paths` entry in `index.json`.
- **Core:** a path file reader, the router, the guided way in `Navigation`, new wording in `Texts` and the speech texts in all app languages, and an `expect` function to decompress squares.
- **Apps:** range requests and a store for squares, the line on the map, the header, the notification and the Live Activity on both platforms.
- **Outside the code:** `PRIVACY.md`, the Play data safety form and the App Store privacy labels.
