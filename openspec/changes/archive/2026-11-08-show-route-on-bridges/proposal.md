# Proposal

## Why

On the map, the route is drawn over everything. Where it passes under a road, railway or path bridge, the map can't show whether the route goes under it or up onto it. That is exactly where riders misread a junction. A throwaway prototype on the Pixel drew the base map's bridges semi-transparent over the route. At the bridge between knots 28 and 29, the maintainer found the route passing under it "really nice". The ride over the same bridge was wrong, though, because the route was drawn under the bridge it runs on.

Measured on the OpenStreetMap data of 2026-11-04, the cycling networks pass under a mapped bridge about 7 times per 100 km in the Netherlands and Belgium, and 10 times in North Rhine-Westphalia. They run on a bridge over another road about as often.

## What Changes

- The pipeline marks the stretches of each course that run on a bridge (`bridge=*` other than `no`) and publishes them in a section added to the end of the geometry file. Apps released earlier read the file as before.
- The apps draw the base map's bridges over the route and the network lines. The bridges are semi-transparent, with the same transparency everywhere, so the route stays visible below them. The route's and network lines' own bridge stretches are drawn above the bridges, so a route over a bridge lies on it.
- Knots, the rider's position, the way back to the route and the guided way stay above the bridges.
- This applies on the light and the dark map, while planning and while navigating, on Android and iOS.

**Out of scope:**
- **Tunnels.** The base map draws them as it does today. Fading the route itself where something hides it is close to Apple's US 12098930, so the route's own look does not change anywhere.
- **Transparency that depends on the distance from the route.** That is close to US 12281912.
- **The ridden track.** It is the GPS track, has no bridge stretches, and stays below the bridges.
- **The offline map.** It has no bridges, so nothing changes there.
- **Layer numbers** for a bridge crossing another bridge. That case is rare, about 0.1 to 0.2 per 100 km. A route's bridge stretch is simply drawn above all bridges.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `network-data`: the geometry file gains the bridge stretches of each course, readable by older apps.
- `map-planning`: the map draws bridges over the route and network lines, and the route's own bridge stretches above them.

## Impact

- **Pipeline:**
  - `NetworkExtraction.kt` keeps the bridge tag of the route ways and carries it per point along each course.
  - `Simplification.kt` (or its caller) keeps the points where a bridge starts and ends.
  - `Main.kt` writes the new section.
- **Core:**
  - `NetworkGeometry.kt` reads and writes the bridge stretches after the signpost courses.
  - `MapFeatures.kt` splits the route, the line ahead and the network lines into stretches on a bridge and stretches off one.
- **Android:** `NetworkMap.kt` gets the bridge layers copied from the base map style, and the route layers for bridge stretches.
- **iOS:** `NetworkMapView.swift` gets the same.
- **Data:** the geometry files grow slightly and must stay below the 10 MB budget. Until the data workflow has published files with bridge stretches, the apps draw no bridges over the route.
- **No new permissions or services.** Nothing about the rider's position is sent anywhere.
