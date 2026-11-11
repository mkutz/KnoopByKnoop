# Design

## Context

- **The base map:** OpenFreeMap's "liberty" style (`tiles.openfreemap.org/styles/liberty`), shown by MapLibre Native on Android 13.7.1 and on iOS. The dark map is a variant of it shipped with the app (`map-styles/liberty-dark.json`). It uses the same sources:
  - the vector tiles `openmaptiles` (TileJSON `tiles.openfreemap.org/planet`, zoom 0–14, versioned tile URLs that change with OpenFreeMap's weekly update);
  - the shaded relief `ne2_shaded` (raster, up to zoom 6);
  - the fonts and icons at the same URLs.
- **What is offline today:** MapLibre caches tiles it has shown (its ambient cache), which is all that works offline now. A fresh install, or an area not looked at, shows nothing.
- **Tile sizes:** a zoom-14 tile in Siegburg is about 175 KB, a zoom-12 tile 72 KB, a zoom-10 tile 220 KB.
- **OpenFreeMap's terms** (openfreemap.org/tos) allow offline downloads in apps of "at most 3,000 tiles or 50 MB", "made with at most 4 parallel requests", "sent with a unique User-Agent identifying your app".
- **The pattern to copy:** path data and place details already download around the planned course.
  - Both are driven by the same flow in `AppViewModel.loadPathData` / `loadPlaceDetails`: 2 seconds after the route last changed, restarted when the connection comes back, with a "deleted for" course after the user deletes.
  - The core decides what to load (`PathSquares.courseToLoad`, `PlaceSquares.courseToLoad`). iOS mirrors this in `AppModel`.
- **MapLibre offline regions:**
  - Android: `OfflineManager.createOfflineRegion` with an `OfflineGeometryRegionDefinition` (a GeoJSON geometry, a style URL, a zoom range and a pixel ratio), observed with `OfflineRegionObserver`, removed with `OfflineRegion.delete`.
  - iOS: `MLNOfflineStorage.shared.addPack(for: MLNShapeOfflineRegion)`, with notifications for progress and errors.
  - Resources shared between regions are stored once.
  - `setOfflineMapboxTileCountLimit` (default 6,000) limits all regions together.

## Goals / Non-Goals

**Goals:**
- The map around the planned route works without a connection, light and dark, for cycling and hiking.
- One core function decides the area, so both apps request the same tiles.
- OpenFreeMap's offline terms are kept, for every route.

**Non-Goals:**
- Regions, countries or several routes.
- Own tile hosting.
- Changing the ambient cache.

## Decisions

1. **The area: two regions per route, from `BaseMapArea.of(course)` in the core.**
   - **Detail:** a polygon 500 m around the course, at zoom 10–14. Zoom 14 is the vector tiles' last level; MapLibre draws closer zooms from it.
   - **Overview:** the route's bounds plus 5 km, at zoom 5–9.
   - The function returns the polygons, simplified to a few hundred points, as GeoJSON-ready rings, plus a tile estimate per zoom from a tile cover of the polygon.
   - A 50 km route through towns comes to roughly 150 tiles and 15–20 MB; the overview is a few dozen tiles.

   *Alternative:* one region over the route's bounds at all zooms. A long diagonal route would cover far too many tiles.

2. **Limits.**
   - **Tile count:** before downloading, the core's estimate decides the zooms. If detail plus overview exceed 3,000 tiles, zoom 14 is left out of the detail, then zoom 13, until it fits.
   - **Size:** while downloading, the observer watches the region's completed size. Above 50 MB, the detail region's download is stopped and the region kept as it is, so a long route keeps zoom 14 where it got it ("A long route").
   - **Global count:** the app sets MapLibre's global tile count limit to 3,000. Only the current route's map is kept, so it is also the per-route limit.
   - **Parallel requests:** Android gives MapLibre an OkHttp client (`HttpRequestUtil.setOkHttpClient`) whose dispatcher allows 4 requests per host. iOS sets `httpMaximumConnectionsPerHost = 4` on `MLNNetworkConfiguration`'s session configuration.
   - **User-Agent:** the same two clients send `knoopbyknoop-android` / `knoopbyknoop-ios` with the repository URL, as the place search does, for every base map request.

3. **When.**
   - The same flow as path data, keyed on the planned course and the base map's "deleted for" course: 2 seconds after the route last changed, restarted when the connection comes back, and on navigation start for a route whose map is incomplete.
   - Only the route decides the area, never the rider's position.
   - A new route's regions are created with the route's course in their metadata. Once they are complete, regions whose metadata belongs to another course are deleted.

4. **Light and dark.** The regions are made with the light style's URL. The dark style, loaded from the app's assets, uses the same tile, font and icon URLs, so it finds them in the offline database. The check on the emulator covers dark offline explicitly.

5. **The panel and the info sheet.**
   - The state reuses `PathDataStatus` (`LOADING`, `READY`, `UNAVAILABLE`). The panel line comes from the apps' strings in four languages, as the ways' line does.
   - The info sheet entry reads the regions' completed sizes. Delete removes the regions and sets the "deleted for" course, as the place details do.

## Risks / Trade-offs

- **OpenFreeMap's weekly update:** tile URLs are versioned, so after an update online use loads new tiles while the stored ones stay usable offline through the stored TileJSON. A route planned weeks ago shows a slightly older map offline, which is acceptable.
- **The terms are per download, not per device.** Keeping only the current route's map keeps both within the limit.
- **Storage:** up to 50 MB more, removed with the next route or by the delete button.
- **Download size on mobile data:** 15–50 MB per route is noticeable on a data plan. It happens at planning time like the other downloads; a Wi-Fi-only setting is left for later, if testers ask.
- **iOS and Android offline APIs differ in detail** (notifications vs observers, pack state on restart). Each app keeps its own small wrapper around the same core decisions.
