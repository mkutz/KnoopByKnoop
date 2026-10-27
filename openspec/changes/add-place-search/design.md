# Design

## Context

See proposal.md for the motivation. The planner has a top bar over the map with the network switch and the Info button. Both apps already track internet access (`online`). The map code can move the camera and draws GeoJSON layers. The covered area is the extent of the installed graph, which the core knows.

Photon (`https://photon.komoot.io/api/`) answers with GeoJSON features. Their properties include `name`, `street`, `housenumber`, `postcode`, `city`, `district`, `country`, `osm_key`/`osm_value` (e.g. `building`/`train_station`) and `type`. It supports `q`, `limit`, `lang` (`default`, `de`, `en`, `fr`), a location bias `lat`/`lon` and `bbox`. It often returns the same place more than once: for "Utrecht Centraal", the station hall and the building are separate features a few hundred meters apart.

## Goals / Non-Goals

**Goals:**
- Search feels immediate: suggestions after a short pause in typing.
- Considerate use of the public Photon instance.

**Non-Goals:**
- A fallback geocoder. Photon is used directly; a self-hosted instance can replace it later by changing one URL.

## Decisions

### Core: requests and hits
`PlaceSearch` in the core builds the request URL and parses the response:
- `q`, `limit=10`, `lang` from the device language if Photon supports it, otherwise `default`
- `bbox` of the covered area, from the installed graph's extent
- `lat`/`lon` of the map center as bias

`PlaceHit` holds the name, a description line and the coordinates. The description is the address (street with house number, town) for addresses and businesses, the type and town for stations and other places (e.g. "Train station, Utrecht"), or the region for towns.

Hits with the same name and the same town that lie within 500 m of each other are merged into the first. That covers Photon's duplicate buildings and halls; separate places of the same name stay apart. All of this is pure Kotlin and tested in `commonTest` with recorded Photon responses.

*Alternative:* call Photon only from the apps. Then the rules for description and merging would be written twice and tested on neither platform.

### Apps: typing, requests, marker
- **Requests:** sent 300 ms after the last keystroke and from 3 characters on. A newer query cancels the running request. Requests carry a `User-Agent` naming the app and its repository, as Photon asks.
- **Results:** shown in a list that replaces the waypoint sheet's content while the search field has focus.
- **Tapping a hit:** closes the list, moves the camera to the hit at zoom 14 (knots are shown from 10, and at 14 single streets and knots are easy to tap), and shows a marker. The marker is a GeoJSON source and layer of its own, drawn under the knots so it never hides one.
- **Clearing the search:** removes the marker.
- **Offline:** the field is disabled with the placeholder "Search needs an internet connection", driven by the existing `online` state.
- **Errors:** timeouts or HTTP errors show "Search is not available right now".

### Attribution
The info sheet gets a line "Search by Photon (komoot), data © OpenStreetMap contributors".

## Risks / Trade-offs

- **[Risk] The public Photon instance can be slow, rate-limit or disappear.** → Requests are debounced and cancelled, and a failure only affects search. The URL is one constant, so it can be switched to a self-hosted instance.
- **[Trade-off] Queries are sent to a third party.** → They contain what the user typed and the map center, nothing else. This is stated in the info sheet line.
- **[Risk] Language support is limited** (no Dutch). → Names come from OSM in their local language anyway; `lang` only affects things like country names.
