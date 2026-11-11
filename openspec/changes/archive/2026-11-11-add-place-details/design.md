# Design

## Context

- **Highlighted places** come from the base map's `poi` source layer (OpenFreeMap, OpenMapTiles schema). `PlaceKind` matches `class`/`subclass`. `placeAt()` (Android `NetworkMap.kt`) and `place(at:in:)` (iOS `NetworkMapView.swift`) read only the geometry, `name`, `class` and `subclass` into a `MapPlace`. The card is `PlaceOfferMenu` in `PlannerScreen.kt` / `PlannerView.swift`.
- **The tiles' feature id is the OpenStreetMap object** (checked 2026-11-10 on a Troisdorf tile and the OSM API):
  - a node is `id * 10 + 1`, e.g. Troisdorf station node 2949341746 is feature `29493417461`
  - a way is `id * 10 + 2`, e.g. way 80405542 is `804055422`
- **Campsites and lodging in the tiles** (checked on four z14 tiles around Renesse):
  - campsites are `class` `campsite`, with `subclass` `camp_site` or `caravan_site`
  - lodging is `class` `lodging`, with `subclass` `hotel`, `guest_house`, `chalet`, and per OpenMapTiles also `motel` and `hostel`
- **Path data is the model to follow:**
  - `PathFile` is a directory of squares (1/16°) with offsets and sha256s, followed by zlib-deflated square blobs. It has a magic and a format version.
  - The index has an optional top-level entry (`PathIndexEntry`) with the directory's offset, size and sha.
  - `PathStore`/`PathDownloader` read the directory and squares by HTTP range, merge adjacent ranges, verify each square, and evict by LRU, never the current route's squares.
  - `PathSquares.near` picks the squares within 5 km of a course.
  - The apps trigger the download 2 s after the route stops changing. It is gated to cycling by `PathSquares.courseToLoad`.
- **The pipeline** reads PBFs with osmpbf. `OsmNode` already carries tags. Ferry details (`NetworkExtraction.ferryTagsOf`) are the only POI-style tags read so far. The weekly workflow builds `cycling.paths`, checks its size budget, and `pipeline index` writes the entry.
- **Opening hours** exist for ferries: `OpeningHours.statusAt` answers only what's certain (public holidays, seasons, past midnight; school holidays and holidays of only some covered countries give no answer), and both apps word and refresh the status in their ferry lists (`RouteFerries.kt`, `RouteFerries.swift`).
- **Opening a website** already exists for ferries: `LocalUriHandler` on Android, `Link` on iOS, adding `https://` when the scheme is missing. There is no phone handling anywhere yet.

## Goals / Non-Goals

**Goals:**
- One file format and one store for squares of data, shared by path data and place details, so the next details (opening hours, cuisine, tents) are a format version, not a new mechanism.
- A tapped icon finds its details by the tile's feature id. Matching by name or position is never needed.
- No change to what path data does or requests.

**Non-Goals:**
- Relations (multipolygon places). Their tile id encoding is unchecked and they are rare for these kinds; they get no details.
- Drawing places from the places file.

## Decisions

### The feature id as the key

The places file keys each entry by the tile's feature id (`osm id * 10 + 1` for nodes, `+ 2` for ways). The pipeline computes it the same way. The apps read the tapped feature's id into `MapPlace.featureId: Long?`:
- **Android:** `Feature.id()` is a String, parsed to a Long.
- **iOS:** `MLNFeature.identifier`, an NSNumber.

A feature without an id, or with one that doesn't parse, gets no details.

**Alternative:** match by name and position. This is fragile: places without names, or several cafés in one building. The id is exact and costs nothing.

### Which places the pipeline keeps

`PlaceKind` gets the OSM tags each subclass comes from. For example:
- FOOD: `amenity` = `restaurant`, `cafe` …
- SUPERMARKET: `shop=supermarket`
- BICYCLE: `shop=bicycle`, `amenity=bicycle_repair_station`
- RAILWAY: `railway` = `station`, `halt`
- WATER: `amenity` = `drinking_water`, `toilets`
- CAMPSITE: `tourism=camp_site`
- LODGING: `tourism` = `hotel`, `motel`, `hostel`, `guest_house`

The pipeline and the app use the same table, so "highlighted" means the same thing on both sides. A test checks that every `subclass` matched on the map has its OSM tag.

**Details:**
- `website`, else `contact:website`
- `phone`, else `contact:phone`, else `contact:mobile`
- `opening_hours`, as tagged

Values are kept as tagged, trimmed, so the file stays faithful to OSM. For websites and phone numbers the apps take the first of `;`-separated values; in opening hours `;` separates rules, so they are passed to the parser whole. Places with none of the three are left out.

### The square a place is in

The app looks up the square of the tapped icon's position. For nodes, the pipeline uses the node's square.

For ways, OpenMapTiles puts the icon somewhere inside the outline, which can fall into a neighbouring square. So the pipeline writes the entry into every square the way's bounding box touches. Buildings are small, so this is almost always one square, sometimes two.

Ways need their nodes' coordinates. The pipeline therefore reads in two passes:
1. ways: keep place ways with their node ids
2. nodes: node places, plus the coordinates of the kept ways' nodes

This is a separate, small pass. It doesn't touch `PathExtraction`.

### Squares of 1/16°, the same grid as path data

The places use `PathSquare` and `PathSquares.near` unchanged. A tapped place's square is about 7 × 4.4 km, which is the rough area the info sheet text promises.

**Alternative:** coarser squares for sparse data. They would mean fewer requests, but more data per tap and a second grid. Adjacent squares are merged into one range request anyway.

### A shared square file and store

The square container is pulled out of `PathFile`/`PathStore`:
- **`SquareFile`:** magic, format version, directory (data date, attribution, entries with offset/size/rawSize/sha256), deflated blobs.
- **`SquareStore`:** files, directory cache, verify, LRU limit, never evict the current squares.
- **`SquareDownloader`:** index entry, directory by range, merged ranges, retry on a changed file.

They are parameterised by:
- the magic (`KBKP` for paths, `KBKL` for places)
- the store directory
- the byte limit (50 MB paths, 10 MB places)
- which index entry to use

`PathFile` keeps its segment encoding inside the square blobs. The new `PlaceFile` encodes per square:
- a varint count
- per entry: the feature id (varint), a flags byte (has website, has phone, has opening hours), and the strings

The opening hours are carried as tagged text and parsed in the app, as for ferries, so a better parser later needs no new file.

The path file's bytes don't change, so its format version stays 2. `PathStoreTest` must pass unchanged before the places side is added (task order).

**Alternative:** copy `PathStore` into a `PlaceStore`. It's quicker, but two copies of range merging, verification and eviction would drift apart.

### Index entry

`NetworkIndex.places: SquareIndexEntry?` is an optional top-level field like `paths`. `PathIndexEntry` becomes a type alias of `SquareIndexEntry`, or the same class under a new name, with identical JSON. Older apps ignore the unknown key (`ignoreUnknownKeys` is already needed for `paths`; checked in the task).

### Size budget

Measured on the extracts of 2026-11-08 (task 3.3), the whole covered area gives a 3.3 MB file:
- 99,738 places in 3,375 squares
- food 68,590; supermarkets 12,262; lodging 8,353; bicycle 4,117; campsites 3,589; water and toilets 1,771; railway 834
- 62,177 opening hours, of which 59,356 (95%) parse with `OpeningHours`
- 75 s with `-Xmx8g`

The budget is 10 MB, checked in the workflow's "Check size budgets" step like `cycling.paths`. That leaves room for about three times the data, e.g. for opening hours, cuisine and tents in later formats.

A failed places build must not stop the networks or paths:
- The "Build place details" step continues on error and removes a half-written file.
- A file over its budget is removed in "Check size budgets" instead of failing the job.
- Without a new file, "Keep published place details" downloads the published one, because the release step removes every asset a run doesn't have. So the last place details stay published, as the spec asks.
- `verify` reads every square of the place details when the index lists them, and fails on a broken one, like path data. Without place details it only logs that none are published.

The file is called `places.details`.

### When the apps download

Next to `loadPathData()` (Android `AppViewModel`, iOS `AppModel`) comes `loadPlaceDetails()`:
- **Around the route:** the same 2 s settle after the route stops changing, for both network types. It shares the course with the path data, but has its own `deletedFor` and cancellation.
- **On a tap while planning:** if the tapped place's square isn't installed, it downloads that one square. When it arrives, the open card's contacts are filled in. The card's state holds the `MapPlace` and observes the store's version.
- **While navigating:** nothing is requested on a tap. The navigation's own route squares were loaded at planning or at the start of navigation, like path data (`Starting before the download is done`).

### The card

Below the name, before the route point choices:
- **Website row:** a globe icon and the host (`www.campingdehoeve.nl`, without scheme and trailing slash).
  - Android: `uriHandler.openUri` with `https://` added when no scheme.
  - iOS: `openURL`.
- **Phone row:** a phone icon and the first number as tagged.
  - Android: `Intent(ACTION_DIAL, "tel:<digits and +>")`. No permission is needed, and it never calls by itself.
  - iOS: `tel:` via `openURL`. iOS asks before calling.
- **Screen readers:** both rows are buttons with labels like "Website, www.…, opens the browser" and "Phone, +31 …, opens the dialler".

- **Opening row:** a clock icon, the status in the ferries' wording ("Open until 22:00", "Closed until tomorrow 08:00") from `OpeningHours.statusAt` at the covered area's local time, and the tagged hours below in smaller text. The status line is left out when the parser gives no answer.
  - The wording is the core's ferry wording (`RouteFerry.statusText` with the language texts), so the ferry list and the card share it; `PlaceDetails.openingStatusAt` gives a place's status.
  - The card refreshes the status every minute while shown, like the ferry list.

The tel URI is built in the core (`PlaceDetails.dialUri`), and the display host in the core (`PlaceDetails.displayWebsite`), so both are tested once.

### Icons and colours

- **Campsites:** Material Symbols `camping`, SF Symbol `tent.fill`.
- **Lodging:** Material Symbols `hotel`, SF Symbol `bed.double.fill`.
- **Card rows:** Material Symbols `language`, `call` and `schedule` on Android; SF Symbols `globe`, `phone` and `clock` on iOS.

The colours are picked so the existing `Distinct from knots` test passes: olive `0xFF5E6E1A` for campsites (orange is the route's colour) and indigo `0xFF4B3FB5` for lodging, far enough from Belgium's blue `0xFF1A5FB4` and the supermarkets' purple. The info sheet's list of the highlighted kinds names the two new ones too. The two new kinds' names are added to `strings.xml` (en/de/fr/nl) and `Localizable.xcstrings`.

## Risks / Trade-offs

- **[Way icons in a square the bounding box misses]** → Only with outlines bigger than a square, which isn't the case for these kinds. The scenario "Place drawn from a building" checks a real building.
- **[OpenFreeMap changes its id encoding]** → The card shows no contacts. That is graceful, not wrong. A jvm test against a stored real tile, like `BaseMapPlacesTest`, catches it when the tile fixture is renewed.
- **[Base map and places file from different weeks]** → A place added in OSM after the places build shows no contacts until the next week. Acceptable.
- **[Pulling the store out of path data breaks path data]** → It is done first, as a pure refactor with `PathStoreTest` and `RealRideTest` green before any places code.
- **[Phone numbers in odd formats]** (`+31 (0)113 …`, `0113-123456`) → The dialler gets digits and a leading `+` only; `(0)` after a country code is dropped. Tests cover the formats seen in the Zeeland sample.
- **[Restaurant hours the ferry parser doesn't know]** (e.g. `week` selectors, `sunrise`) → No status, only the tagged hours, as the spec allows. The pipeline's statistics print how many places' hours parse, to judge whether parser work is worth its own change.
- **[Privacy wording]** → The info sheet text states the tapped place's rough area. The privacy policy is checked for "around planned routes" wording and extended if needed (task).

## Migration Plan

- **Pipeline first:** the weekly run publishes `places` in the index. Released apps ignore it.
- **Apps:** a version without the places file still works, since the cards just show no contacts.
- **Rollback:** drop the workflow step. The index loses `places` and the apps fall back to cards without contacts.
