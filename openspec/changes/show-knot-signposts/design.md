# Design

## Context

- **Data:**
  - Since `pass-merged-knots` (graph format 2), a merged knot carries its other nodes' positions in `Knot.otherNodes`; `Knot.positions` lists its own position first.
  - Siegburg 02 is node 59469043 by the river, with 81461844 near the L 16, 195 m away. 2,545 of 20,583 cycling knots have other nodes.
- **Map layers:**
  - `MapFeatures.knots(graph)` emits one point per knot (`id`, `ref`), drawn as badges by both apps' `knots` layer.
  - `MapFeatures.routeKnotsLayer(session)` emits one point per route marker (`ref`, `role`), drawn on top as the route's badges.
  - Both apps draw the next-knot ring from `nextKnot.latitude/longitude` (`NetworkMap.updateNavigation`, `NetworkMapView.updateNavigation`).
  - Android caches the knots layer per graph as `cycling-knots-<hash>.geojson` and rebuilds it when the graph changes.
- **Taps:** `KnotLocator.nearest` measures to the knot's own position only; `pass-merged-knots` deliberately left taps unchanged. `nearestAtAnyNode` measures to every position.
- **Courses** (`NetworkExtraction.connectionOf`):
  - The shortest path along the route's ways runs from any node of the start knot to any node of the end knot.
  - The course is then extended with a straight point to each knot's position ("A merged junction is drawn at its representative node, so the course is extended to it"). That's the line across the L 16 at Siegburg 02.
  - `wayNodeIds` holds the nodes of every way of the extracted routes of the network.

## Goals / Non-Goals

**Goals:**
- Badges where the signposts are, with no change to routing or navigation.
- Courses that do not cut across roads between a knot's signposts.

**Non-Goals:**
- Naming the signpost in notices or lists.
- Changing which node is the knot's position.

## Decisions

**One point per position in `MapFeatures`.** `knots` and `routeKnotsLayer` emit a feature for each of `knot.positions`, with the same properties (`id`, `ref`, `role`).
- The apps' layers draw them unchanged, so roles, colours and the round-trip marker apply to every badge.
- The extra points are about 12% more features in the knots layer.
- Both apps cache the layer under the graph's hash, and the content changes with this app version only. So the layer gets a new name, `knot-badges`, for updated apps to rebuild it, and `removeStaleData` removes the old `-knots-` files.

**Taps use `nearestAtAnyNode`** in `AppModel.tap` and `AppViewModel.tap`, so a tap on any badge selects the knot.
- `PlaceSearch` keeps `nearest`; it only asks whether the network is near.
- "Knots with equal numbers" still holds: separate knots have separate positions, and the nearest position wins.

**The next-knot ring at every position.** The apps set the `next-knot` source to one point per `nextKnot.positions`. The off-route line keeps ending where `wayToRoute` says.

**Courses: along the ways, or straight as before.**
- In `connectionOf`, when the path ends (or starts) at a node other than the knot's position, the pipeline adds the shortest path from that node to the position. It uses a neighbor graph of all ways of the network's routes that touch the junction's nodes, and inserts that path's points instead of the single straight point.
- The connection's length stays the length of the route's own path, as today, so routing and the strip don't change.
- If there is no path, or it is longer than 3 times the straight distance (a detour, not the junction), the straight point stays.
- Alternative: end the course at the node it reaches. The route line would then have a gap between legs at a merged knot, and the progress place and off-route distance assume courses meet at the knot.

## Risks / Trade-offs

- **Two badges with the same number close together** may look like two knots. That is what the signposts say, and fietsknoop shows it the same way. The highlights make clear they are one.
- **Courses change with the next data publication:** apps get them only then. The badges work at once, with today's data.
- **Larger knots layer:** about 12% more points. On the emulator, the `Startup` mark "knots" was 6.9 s with the old build and 5.0 s and 4.0 s with the new one, so no measurable regression; these runs vary a lot.
- **Full network not run locally:** the pipeline was run on the Köln extract, 9 s in all. The network-wide neighbor graph is larger for the full run, and the search is bounded to 3 times the distance between a knot's nodes.
