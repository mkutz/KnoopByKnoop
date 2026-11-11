# Design

## Context

- **Extracts:** the pipeline reads the Netherlands, Belgium and North Rhine-Westphalia from Geofabrik in one pass (`update-network-data.yml`).
- **Borders today:** `SignRegions.kt` already turns administrative boundary relations into closed rings and tests points against them (`Border`), for the three sign regions. `NetworkExtraction` assigns each cycling knot its `SignRegion` that way, and fails the build on an incomplete border (`IncompleteBorderException`).
- **The graph file:** `GraphFile` writes version 2 and appends later additions after the existing sections. Readers from before stop after the sections they know, e.g. the positions of merged knots and the sign regions. Budget: under 5 MB for the cycling graph.
- **Places today:** `PlaceExtraction` reads the highlighted places (food, campsites…) into `places.details`, not `place=*` localities.
- **The lists:**
  - The waypoint list rows: Android `PlannerScreen.kt`, iOS `PlannerView.swift`; badge + role.
  - The coming knots list: `ProgressPanel` on both apps, from `RouteProgress.view` / the strip in the core, with a screen reader text per row.

## Goals / Non-Goals

**Goals:**
- One place per knot, decided at build time, with names in four languages.
- The border crossings of each connection, usable in either riding direction.
- Apps released before keep reading the files.

**Non-Goals:**
- Towns between knots, hamlets, districts.
- Borders outside the covered area.

## Decisions

1. **Places from `place=village|town|city`, as points or areas.**
   - An area mapped with `place=*` (closed way or multipolygon) contains the knot: that place.
   - Otherwise, the point within its size's reach:
     - village 1 km
     - town 2 km
     - city 4 km

     Among several, the one nearest relative to its reach wins, so a knot in a village beside a city's edge gets the village.
   - **Names:** `name`, and `name:en`, `name:de`, `name:nl`, `name:fr` where they differ from `name`.
   - The reaches are first guesses. Task 1.2 measures them on the real data before the format is fixed.

   *Alternative:* municipality boundaries (admin level 8). They are complete, but a German municipality spans many villages and fields, so "Lohmar" would stand at knots that are nowhere near the town of Lohmar.

2. **Regions: what the extracts hold whole.**
   - The 12 Dutch provinces (admin level 4), the 10 Belgian provinces (level 6) and Brussels (level 4), and North Rhine-Westphalia (level 4).
   - Each region knows its country: Netherlands, Belgium or Germany.
   - The country borders follow from the regions. The national relations of Germany, and the neighbouring German states, are not whole in the extracts, so reading them would fail the build. A point in no region is "outside", and a crossing to or from outside is not stored.
   - Region names in `name:en/de/nl/fr`, falling back to `name`. The country names are fixed texts in the core's `Texts`, as there are only three.
   - The same `Border` code, generalised from `SignRegion` to a region id.

3. **Crossings per connection, both ways.**
   - Along each connection's course (from its `from` knot), the region of points every 50 m. Where it changes, the crossing is placed at the edge between the two points (bisected to 5 m).
   - A crossing stores its offset in metres from `from`, the region before and the region after. Riding the other way swaps them, and the offset becomes length − offset.
   - On real data a connection crosses 1.5 times on average, as paths run along borders. For the list, crossings on one connection less than 500 m apart merge into one, from the first region to the last. When that brings the route back where it was, there's no line.

4. **File format: two new sections at the end of version 2.**
   - **Places:** a table of places (name, then up to four language names), then one varint per knot in file order, 0 for none and n for the n-th place.
   - **Regions and crossings:**
     - a region table: id, country, four names
     - per connection, a count and its crossings (varint offset, region before, region after)
   - Older readers stop before them, as with the sign regions.

5. **Core model.**
   - `Knot.place: Place?` with `name` and `names: Map<Language, String>`.
   - `Connection.crossings: List<Crossing>`.
   - `Texts.placeName(place)` returns `name`, or `"name (name in the language)"`.
   - `RouteProgress` gains the crossings along the route in riding order, with their place on the route, for the coming knots list. A crossing's line says "Country, Region" when the country changes, and "Region" otherwise.

6. **Apps.**
   - Waypoint list: the role, then " · place" in the same line, cut short with an ellipsis.
   - Coming knots: the place under or beside the badge's distance, in the smaller text style, and border lines styled like ferry lines. The screen reader texts follow the spec.

## Risks / Trade-offs

- **Wrong places from point reaches:** a knot 900 m outside a village would be named after it. The measurement in 1.2 tells how often; the maintainer judges a sample of real knots before the format is fixed.
- **Missing language names:** many small places have no `name:xx`, so no brackets, which is the intended look.
- **Size:** about 6,000 places and 30,000 knots fit in roughly 200 KB uncompressed; crossings are few.
- **Belgian provinces at level 6** and Brussels at level 4 need care in the tags; tests cover a crossing in Flanders and one into Brussels.
