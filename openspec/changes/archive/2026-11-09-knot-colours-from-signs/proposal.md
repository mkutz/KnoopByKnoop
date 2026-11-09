# Proposal

## Why

Riders look for the knot numbers on the signposts, and the signposts look different in each country: white on red in North Rhine-Westphalia, white on blue in Belgium, and green with white in the Netherlands. The app's own badges (green, orange and dark by route role) don't match any of them. The maintainer is now confident about these three styles from their own photos (Troisdorf, Lünen, Midden-Limburg, Noord- en Midden-Limburg, Haspengouw), so the colours can be tried after the 2026-11-03 attempt was dropped for lack of data. Mockups: https://claude.ai/artifact/XVeganeauBp3Pe2WGy1qUT (board "On the map: knot roles").

## What Changes

- **Sign region per knot:** the pipeline gives each cycling knot the region whose signs it follows: the Netherlands, Belgium or North Rhine-Westphalia, from OSM boundaries, offline at build time. Other knots and all hiking knots get none. Graph files carry the region in a section old apps skip.
- **Plate colours:** the badges take the plate colour of the knot's region: red #C8102E for North Rhine-Westphalia, dark green #12663A for the Netherlands, blue #1A5FB4 for Belgium. Knots without a region take the app's green #2E9E5B.
- **Roles by fill, not by colour:**
  - Filled plate for a waypoint picked by the user: a white number, and in Germany and Belgium a white ring inside the plate, as on their signs.
  - White with one coloured ring and number for a knot the route passes, as on the Dutch arrow signs.
  - Grey ring and number for a knot not on the route.
  - A small dark corner symbol tells start ▶, stop (flag), end ■ and round trip (circular arrow) apart. **BREAKING** for the look: today's green start, orange stops and passed knots, and dark end go away.
- **Red stops meaning a problem**, wherever it would sit next to red German knots:
  - The off-route and wrong-way headers turn dark (light in dark mode) instead of red.
  - The rider's dot on the strip off route becomes a ring with ✕.
  - A ferry the rider may miss gets a dark clock badge on its mark instead of a red ring.
  - The map's way-to-route and guided-way lines, the unreachable leg and the search-hit marker change from red to dark.
  - Red text in popovers and ferry details stays, since it reads as text.

## Out of Scope

- The wordless header layout, the four header states and the always shown end arrival: `sign-like-header`. This change only recolours today's header.
- Dutch "at the knot filled, follow white" in the header: `sign-like-header`.
- Turn arrows at knots: `turn-at-knot`.
- The lock screen, Live Activity, Dynamic Island and notification: `sign-like-lock-screen`. They keep their current look until then.
- Regional styles within a country, e.g. Dutch-style signs near the Belgian border, and other German states. Their knots keep the app's green or the country's colour as stated above.
- Hiking sign colours.
- Improving OpenStreetMap's tagging of sign colours.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `network-data`: a new requirement that cycling knots carry their sign region in graph files, with old apps and old files unaffected.
- `map-planning`: "Route knots stand out" now names the plate colour and the fill-by-role scheme. "Light and dark appearance" changes its off-route scenario from red to the dark header.
- `knot-navigation`: "Ferry warning" swaps the red ring and red arrival for a clock badge. "Progress strip" says how the off-route dot looks. "Riding the wrong way" loses its red in the panel.

## Impact

- **Pipeline:** reads the admin boundaries of the Netherlands, Belgium and North Rhine-Westphalia from the extracts and does point-in-polygon per knot. The build fails if a boundary is incomplete.
- **Core:**
  - `Knot` gets an optional sign region.
  - `GraphFile` gets an appended section, still format version 2.
  - `MapFeatures` adds the region to route knot features.
  - A shared plate table maps each region to its colours and ring.
- **Android:** `KnotBadges.kt`, the map layers and image names in `NetworkMap.kt`, `ProgressPanel.kt`, `RouteFerries.kt`, and the header in `PlannerScreen.kt`.
- **iOS:** `KnotBadges.swift`, `NetworkMapView.swift`, `ProgressPanel.swift`, `RouteFerries.swift`, and the header in `PlannerView.swift`.
- **Data:** the next weekly network data update writes the regions. Until then, every knot shows the app's green.
