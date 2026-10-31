# Design

## Context

The core already knows everything a GPX file needs: `RouteSummary.Complete.knots` lists the knots passed in riding order (with `ref`, latitude and longitude), the waypoints say which of them are start, stops and end, and `NetworkGeometry.courseOf(route)` gives the line along the network, or `null` while the courses are not loaded. Neither app shares files yet.

## Goals / Non-Goals

**Goals:** a GPX file that bike computers and common apps read without complaints; one writer in the core for both apps; no new dependencies.

**Non-Goals:** import, other formats, uploads to services, the ridden track.

## Decisions

### GPX writer in the core

`Gpx.write(route, knots, waypoints, course, network): String` in `modules/core`, plain string building with XML escaping, no XML library (none exists for Kotlin Multiplatform without dependencies, and the format is small).

- Root `<gpx version="1.1" creator="Knoop by Knoop" xmlns="http://www.topografix.com/GPX/1/1">` with `<metadata><name>`.
- One `<wpt>` per knot passed, in riding order, before the track as GPX 1.1 requires: `<name>` the knot's number, `<type>` `start`, `stop` or `end` for waypoints and `knot` for the others. A knot passed twice is listed twice, so the sequence reads like the knot list.
- One `<trk>` with `<name>` like "Cycling 12 → 34" and one `<trkseg>` with the course's points.
- Coordinates with 6 decimals (about 0.1 m), written without locale, so a German phone does not write commas.
- No timestamps: the file is a plan, and some apps treat timed tracks as rides.
- File name `knoop-by-knoop-<network>-<start>-<end>.gpx`, numbers stripped of characters not allowed in file names.

Alternative: a GPX `<rte>` with the knots as route points. Rejected: devices route between route points themselves and may leave the network.

### Where the action sits

A fifth element did not fit: at 360 dp the distance wrapped to "3.0 / km" and the time was cut off (screenshot `screenshots/android-header-360dp-five.png`). Decided with the maintainer: Back to start and Export GPX move into a menu behind a "More actions" icon button (Material `more_vert`, SF Symbol `ellipsis.circle`), each with icon and label; Clear stays an icon button and Navigate a filled button. The menu button is shown when one of its entries is available: Back to start while the route is not a round trip, Export GPX with a complete route and its course. Everything is hidden while navigating, as before.

### Sharing the file

- Android: write to `cacheDir/export/<name>.gpx`, expose it through an `androidx.core.content.FileProvider` (`<provider>` in the manifest, `res/xml/file_paths.xml` with the `export` cache path), `Intent.ACTION_SEND` with type `application/gpx+xml`, `EXTRA_STREAM` and `FLAG_GRANT_READ_URI_PERMISSION`, via `Intent.createChooser`.
- iOS: write to `FileManager.default.temporaryDirectory/<name>.gpx` and present `UIActivityViewController` from the planner sheet's own presentation (anything presented from the map behind the sheet dismisses the sheet, see `PlannerView`). Offers "Save to Files", AirDrop and apps that open GPX.

## Risks / Trade-offs

- `application/gpx+xml` is not registered with every app; some only show up for `*/*` → fall back to `application/octet-stream` if Garmin Connect or Komoot do not appear in the share sheet during the checks.
- A fifth header element on narrow phones → checked on the emulator at the smallest width, see above.
- Long routes give large files (a 300 km route has some 10 000 points, about 0.5 MB) → acceptable; no simplification.
