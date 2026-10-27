# Proposal

## Why

Starting the app shows a loading screen for several seconds: on the Android emulator, a normal start with everything cached still showed it after 3 s, and a fresh install in airplane mode showed knots only after 6–8 s. The maintainer saw the same on an iPhone. The app reads the graph, reads and decodes the geometry (580,000 points for cycling), builds or reads the map layers, and only then shows the map. Until then it looks as if the app is not loading at all, although planning only needs the graph.

## What Changes

- The map is shown **right away** on start, with its base map (or the offline style).
- The **knots** of the selected knot network follow as soon as the graph is read, with connections as straight lines; planning works from then on.
- The **courses** replace the straight lines once the geometry is read, without interrupting planning or moving the map.
- Android hands the large map layers to MapLibre as **files** instead of strings, as iOS already does.
- Startup targets for release builds become part of the spec, so they can be checked.

Out of scope: making the routing itself faster, smaller graph or geometry files, startup of debug builds (they are measured, but have no target).

## Capabilities

### New Capabilities

### Modified Capabilities
- `map-planning`: how fast the map, the knots and the courses appear on start, and that they appear one after the other

## Impact

- `apps/android`: `AppViewModel` loads in two steps (graph, then geometry); `NetworkMap` shows the map before a session exists and reads GeoJSON layers from files
- `apps/ios`: `AppModel` and `PlannerView` load in the same two steps; the map is shown before a session exists
- `modules/core`: no behavior change; possibly a way to plan on a graph before its geometry is known, if the apps need it
- Builds on `automatic-network-data` (bundled graphs, courses-loading note); implement after it is archived
