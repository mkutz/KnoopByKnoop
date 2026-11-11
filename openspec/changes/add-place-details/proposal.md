# Proposal

## Why

Where to sleep is the biggest open question on a tour, and the map doesn't show it: campsites and hotels are drawn small and grey like any other shop, and tapping them gives no way to reach them. For a café or a campsite, a website or a phone number is often all a rider needs to check prices, ask if there's room for a tent, or see the menu. OpenStreetMap has them for most overnight places (website on 89% of campsites and 70% of hotels in a Zeeland sample, 2026-11-10), but the base map's tiles carry only a place's kind and name.

This is also the first step towards multi-day tours (version 2.0): place data of our own, published by the pipeline, that later changes can extend with opening hours, cuisine or tents and search along a route.

## What Changes

- **Two new kinds of highlighted places,** with their own icon and colour like the five existing ones:
  - **Campsites** (`tourism=camp_site`).
  - **Hotels and guest houses:** hotels, motels, hostels and guest houses.
- **The place card shows the website and the phone number** OpenStreetMap has for the tapped place, for every highlighted kind, with actions to open the website in the browser and to call the number. Details missing in OpenStreetMap are left out, not guessed, as with ferries.
- **The place card shows whether the place is open now** and when that changes, like the ferries ("Open until 22:00", "Closed until tomorrow 08:00"), with the opening hours as tagged below. Where the hours don't allow a sure answer, only the tagged hours are shown.
- **The pipeline publishes a places file** with the website, phone number and opening hours of every highlighted place in the covered area, keyed by the OpenStreetMap object, built weekly from the same extracts and split into squares like the path data.
- **The apps download the places squares** within 5 km of a complete route, for cycling and hiking, and the square of a tapped place while planning, with network access. While navigating, the card uses only the squares that are already there, so nothing that follows the rider's position is requested. Squares around a route stay usable offline on the ride.

## Out of Scope

- **Prices** of overnight places or price ranges of restaurants. OpenStreetMap has practically none; the website is the way to them.
- **Cuisine, tents allowed, stars** and other details. They can be added to the places file in later changes.
- **Whether a place is open when the rider gets there,** as the ferries ahead show while navigating. The card tells the status for now.
- **Booking, sending messages or e-mail,** and any check that a website or number still works.
- **Campsites for motorhomes only** (`tourism=caravan_site`) and **holiday homes** (`tourism=chalet`). Chalets are many single holiday homes in parks, rented by the week, and would crowd the map.
- **Drawing places from our own data,** places beyond the base map, searching places by kind or along the route, a list of places on the route. The icons still come from the base map's tiles.
- **Multi-day tours,** night stops and stages.
- **Places details while offline without a route,** e.g. tapping a place far from the route without network access.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`: "Places useful on a ride" gets campsites and hotels and guest houses; new requirements for the website and phone number on the place card with their actions, and for its opening status and hours.
- `network-data`: the weekly publication includes the places file with its contacts, in squares, in the index and with a format version.
- `network-sync`: the places squares are downloaded around a route and for a tapped place while planning, kept, and never requested by the rider's position.

## Impact

- **Pipeline:** reading the places' tags from the extracts, writing the places file in squares, its entry in `index.json`, a size budget; the weekly workflow publishes it next to the path file.
- **Core:** the two new `PlaceKind` entries, the places file format and reader, the opening status of a place with the ferries' opening hours, which squares to request (shared with the path data's squares where possible), looking a place up by its OpenStreetMap object, and the card's texts in all app languages.
- **Apps:** reading the tapped feature's id, the card's website and phone rows with their actions and its opening status, kept current while it is shown, the new icons, and the places downloads next to the path data's.
- **Outside the code:** the privacy policy and the store declarations already cover downloads of data around a route from the release; the tapped square while planning is a rough area of a place the user looks at, not their position. To be checked once against the policy's wording.
