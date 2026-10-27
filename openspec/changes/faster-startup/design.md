# Design

## Context

See proposal.md for the motivation. On start, both apps today:

1. On Android, wait for the bundled graphs to be installed (`installBundled`). Only then is the route restored.
2. In `loadNetwork`, read the graph, read and decode the geometry, build or read the cached knots and connections GeoJSON, and restore the route. All of this happens before a session exists.
3. Show a loading view until then. The map composable or view only exists once there is a session.
4. Pass the knots and connections layers to MapLibre. Android passes them as `String`s via `GeoJsonSource.setGeoJson`, which copies tens of megabytes through JNI and parses them on the main thread. iOS passes file URLs, which MapLibre reads on its own.
5. Load the base map style. Offline without a cached style, MapLibre neither loads nor fails, so the offline style is used only after a 3 s timeout. No layer can be shown before a style is loaded.

Measured on the Android emulator with a debug build: on a normal start, the loading view was still shown after 3 s. On a fresh install in airplane mode, the map appeared after 4–5 s and the knots after 6–8 s.

## Goals / Non-Goals

**Goals:**
- Something meaningful on screen right away: the map at the place last shown, then the knots, then the courses.
- No work on the main thread that grows with the size of the network.

**Non-Goals:**
- Faster routing, or a smaller graph or geometry format.
- Targets for debug builds. They are measured, so the gap to release builds is known.

## Decisions

### Two loading steps: graph first, geometry second
`loadNetwork` is split:
1. **Graph:** read the graph, build the `KnotLocator`, take the knots layer and a straight-line connections layer (both cached as files), and restore the route on the graph. The session, the knots and the route are shown at this point, and the route is drawn with straight lines.
2. **Geometry:** read and decode the geometry, and take the connections layer with courses (cached). The layers and the route are redrawn along their course.

The session stays the same object across step 2, so planning continues without interruption. The route is redrawn with the new geometry (`MapFeatures.route(session, geometry)`). The map keeps its camera, because the graph does not change.

Today's single step is kept for one case: when the geometry is already known not to belong to the graph, step 2 is skipped, as now.

*Alternative:* keep one step and only make it faster. The geometry alone takes more time than the targets allow on slower phones, so the order matters more than the speed.

### The map exists before the session
The planner shows the map from the first frame, with empty sources. It moves to the last camera position, saved together with the route snapshot, or otherwise to the center of the covered area at knot zoom. The knots layer is filled in step 1. Without bundled graphs and without an installed network, the note says that the knot network is loading, as today, but over the map.

The last camera position is saved next to `route.json` as `camera.json` (latitude, longitude, zoom). It is written when the app goes to the background. It is not part of `RouteSnapshot`, because a camera without a route is useful too.

### The offline style right away when starting without internet access
If the device has no internet access at start (`ConnectivityManager` / `NWPathMonitor`), the map starts with the offline style, so step 1 can show the knots within the target. The base map is loaded once internet access is back, as today. Online, the base map style is loaded with the 3 s fallback as now.

*Trade-off:* offline, a base map that MapLibre has cached is no longer shown until internet access is back. The spec allows the base map to be missing offline, and knots shown quickly matter more.

### Android: map layers as files
`NetworkStorage.mapLayer` already writes every layer to a file. It returns the `File`, and `NetworkMap` sets `GeoJsonSource.setUri("file://…")`. MapLibre reads and parses the file on its own threads. The route layers stay strings, because they are small and change on every tap.

### Measuring
Both apps log three marks with the time since the process started:
- `startup: map`: the style loaded
- `startup: knots`: step 1 shown
- `startup: courses`: step 2 shown

Android takes the time from `Process.getStartElapsedRealtime()`, iOS from the process start time (`sysctl` `kp_proc.p_starttime`). The logs are written in release builds too. They are three lines per start and contain no personal data.

**Reference devices for "Startup times":** the Android emulator `Medium_Phone_API_37.0` on the maintainer's Mac, and the maintainer's iPhone. Each is measured with a release build, over five starts each, using the slowest of the five.

## Risks / Trade-offs

- **[Risk] The emulator is faster than many real Android phones.** → Accepted for now. A physical reference device can be added to the design later without changing the spec.
- **[Risk] Two steps mean two redraws of the connections.** → The second redraw replaces a source's file, so it costs one parse in MapLibre's background thread and no main-thread work.
- **[Trade-off] Release builds write three log lines per start.** → Cheap, and it makes the targets checkable on any device.
