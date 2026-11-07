# Design

## Context

- **The base map:**
  - The light map is OpenFreeMap's `liberty` style, loaded from its URL.
  - The dark map is `map-styles/liberty-dark.json`, generated from it by `:pipeline:darkStyle` and shipped with the apps.
  - Both use the OpenMapTiles vector source `openmaptiles`.
- **Places in the tiles:** the source layer `poi` has a `class` (such as `cafe`, `grocery`, `bicycle`, `railway`), a `subclass` with the OSM tag value (such as `supermarket`, `station`, `drinking_water`), a `rank` and the names. The tiles end at zoom 14 and have all places there; lower zooms have a thinned-out subset.
- **liberty draws them** in `poi_r1` (from z15), `poi_r7` (z16), `poi_r20` (z17) and `poi_transit`, with the sprite's grey Maki icons. Checked on 2026-11-07: the sprite has `restaurant`, `cafe`, `fast_food`, `bar`, `beer`, `ice_cream`, `grocery`, `bicycle`, `railway`, `drinking_water` and `toilets`.
- **The apps already change the base map at runtime:** both copy the base map's `bridge_*` layers above the route (`show-route-on-bridges`), and add their own layers in `addLayers` (Android) and the map coordinator (iOS) after each style load, light or dark.
- **The info sheet** has the appearance and language choices, stored in `SharedPreferences("settings")` on Android and `@AppStorage` on iOS.

## Goals / Non-Goals

**Goals:**
- One table of kinds and their tile matches, shared by both apps and tested in the core.
- Works on the light and the dark style without changing `:pipeline:darkStyle`.
- No cost while navigating beyond a few more symbols in the tiles that are already loaded.

**Non-Goals:**
- Our own place data or anything beyond what the tiles carry; see the proposal.

## Decisions

### A kind table in the core

`PlaceKind` (enum) in the core, one entry per kind, with:
- the `subclass` values it matches, such as `restaurant`, `cafe`, `fast_food`, `bar`, `pub`, `biergarten`, `ice_cream` for food and drink
- its colour for light and dark
- a stable `key` for the icon image name (`place-food`, `place-supermarket`, …)

From it, the core builds the MapLibre filter and icon expressions as JSON, like `MapFeatures` builds the knot layers:
- `placesFilter`: the `poi` features of any kind
- `placesIcon`: a `match` from `subclass` to the image name
- `baseMapPoiFilter(original)`: the original filter combined with "not one of our kinds"

**Why match on `subclass`:** it carries the OSM value, so "supermarket" matches supermarkets and not every `grocery` (which also covers delis and wine shops). Railway stations are `subclass` `station` or `halt` with `class` `railway`, so the match includes the class there, as `station` alone also appears for bus stations.

**Bicycle repair stations** (`amenity=bicycle_repair_station`) may not be in the OpenMapTiles mapping. Task 1.1 checks a real tile; if they're absent, the kind covers only shops, and the spec's "where the base map has them" already allows that.

### Our own layers on the base source, added at runtime

After each style load, if the switch is on, each app:
1. sets the filter of `poi_r1`, `poi_r7`, `poi_r20` and `poi_transit` to `baseMapPoiFilter` of their own filter, so our kinds aren't drawn twice
2. adds one symbol layer `places` on `openmaptiles`/`poi`, from z14, with `placesFilter` and `placesIcon`
3. adds names from z16 in the same layer, below the icon, in the base map's text font, `text-optional` so a name never hides an icon

With the switch off, the original filters are restored and `places` is removed, without reloading the style.

**Order:** `places` goes directly below our lowest layer (`connections`), above all base map layers. MapLibre places symbols of higher layers first, so the knots, route knots and rider arrow win any collision, and the route line is drawn over the icons.

**Alternative:** add the layers in `:pipeline:darkStyle` and ship a light style too. That would also change the light map's source from the URL to a copy shipped with the app, and the switch would need a style reload, which is about 5 s on some phones ("appearance switch ~5 s map style reload" in the tester feedback). Runtime layers avoid both.

### Icons drawn by the apps

Each icon is a filled disc in the kind's colour with a white glyph and a thin white edge, the size of a far-away knot badge, drawn by the apps' badge code and added to the style as `place-<key>`:
- **Android:** Material Symbols vector drawables `restaurant`, `shopping_cart`, `pedal_bike`, `train`, `water_drop`, bundled as drawables
- **iOS:** SF Symbols `fork.knife`, `cart.fill`, `bicycle`, `tram.fill`, `drop.fill`

Water and toilets share one kind and one icon (the drop). Toilets get their own glyph only if the mockup shows the drop misleads.

**Why not the sprite's Maki icons:** they're grey line icons meant for the base map's own look, can't be put on a coloured disc in a style layer, and would look like the places we're taking out.

### Colours away from the signs

The discs must not read as a knot (NRW red `#C8102E`, NL green `#12663A`, BE blue `#1A5FB4`, app green `#2E9E5B`), nor as a problem (dark) or the route. Starting palette, settled in the mockup (task 1.2) before code:
- food and drink: orange `#D9600F`
- supermarket: purple `#7A3E9D`
- bicycle: brown `#8A5A2B`
- railway: slate `#3D4A5C`
- water and toilets: cyan `#0B8FA8`

The white glyph needs at least 3:1 contrast on each. On the dark map, the same colours with a dark edge.

### The switch

"Places on the map", a toggle in the info sheet below the appearance choice, stored as `places` (`true` by default) next to `appearance`. Changing it updates the layers on the running style.

## Risks / Trade-offs

- **[Busy town centres]** Dozens of cafés at z15 could crowd out the base map's street names. → Places below our layers but above the base labels; at z14–15 only icons. Checked in a city centre (Utrecht Oudegracht) on the emulator; if it's too busy, food and drink start at z15.
- **[OpenFreeMap changes liberty's POI layer ids]** The filters would then not apply, and places show twice. → The apps change only the layers they find; a test with the shipped dark style checks the four ids exist. The dark style is regenerated by hand, so a change shows up there.
- **[Tile data differs from expectations]** Classes or subclasses could be named differently. → Task 1.1 reads a real tile before the table is final.
- **[Station names in two layers]** `poi_transit` also draws bus stops and airports. → Its filter only loses `railway` stations; bus stops stay.

## Migration Plan

App-only, no data change. The switch is on after the update; turning it off gives the old map.
