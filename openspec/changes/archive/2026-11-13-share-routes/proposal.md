## Why

The app plans a route and rides it right away, but a route can't be kept for later or given to someone else. The GPX export goes to other apps, and nothing reads it back as a route of knots. Sharing a route as a file covers both: send it to a friend, or keep it in Files to open on the day of the ride. The maintainer wants this before 1.0, without making the planner heavier. Saving routes by name in the app, planned dates and ferry checks for a date wait for 2.0, together with multi-day tours.

## What Changes

- A route file (`.knoop`) that is plain Markdown and close to the knot lists riders wrote down before smartphones: a `# ` title, an optional `Network: hiking` line, then the knots in riding order as a list. The waypoints are top-level items, and the knots passed between them are indented below.
  ```
  # Siegburg Bonn and back

  - 02 6364748 Siegburg
    - 28
    - 29 Troisdorf
    - 30
  - 83 Bonn
    - 27
  ```
- Only the first knot carries its OpenStreetMap id. Every further number is found among the knots connected to the one before, so numbers that repeat across regions are no problem, and people can write a file by hand.
- A route point is written as its position, e.g. `- 50.78210, 7.21540 Home`. Route points, a home flag included, are shared as they are; the user decides whom to send the file to.
- Text after a number is the knot's town, which the app writes where it changes, or the user's own words. A waypoint's own words show in the waypoint list in place of its town.
- Export GPX in the panel's action row becomes Share, in the same place with the same icon. It offers "Route for Knoop by Knoop" (the `.knoop` file in the system share sheet) and "GPX for other apps" (the export as before).
- Opening a `.knoop` file (from Files, a messenger, mail or AirDrop) opens the app and loads the route in place of the one being planned. Undo brings the earlier route back. The app switches to the file's network if needed, and shows the whole route. Knots that can't be found are left out and named.

## Out of scope

- Saving routes in the app, showing the title in the app, planned dates and times, ferry checks for a date: 2.0, with multi-day tours.
- Riding exactly the knots a hand-written list passes where they differ from the route the app finds: the route is planned between the waypoints, and the user is told it differs. Knots that steer the route without being stops can come later.
- Editing a waypoint's text in the app.
- Importing GPX files from other apps, which needs matching a track to the network: a change of its own, later.
- Links that open a route (they need a website with app links). Only files here.
- Opening a route while navigating: the app says to stop first and doesn't load it.

## Capabilities

### New Capabilities
- `route-sharing`: the route file format, waypoint texts from a file, sharing the planned route as a file, and opening a route file in the app.

### Modified Capabilities
- `map-planning`: "Clear actions in the planner panel" shows Share instead of Export GPX; "Export the route as GPX" is reached through Share; "Undo route edits" counts opening a route file as an edit.

## Impact

- Core: a new `RouteFile` (write and read the format, find knots by id, as neighbours, or by number nearby) next to `Gpx`; a waypoint text in `Knot` or the planning session and in `RouteSnapshot` (optional field, so stored routes stay readable); the waypoint list's label uses it.
- Android: `PlannerScreen` (`RouteActions`: Share with a menu), `GpxExport.kt` generalised to share either file, `AndroidManifest.xml` intent filters for `.knoop` files, `MainActivity`/`AppViewModel` handling the opened file and the network switch.
- iOS: `PlannerView` (`routeActions`: Share as a `Menu`), `GpxExport.swift` generalised, `project.yml` with an exported type `com.knoopbyknoop.route` and the document type, `onOpenURL` in the app.
- Strings for Share, its two choices, and the messages for an unreadable file, left-out knots, a route that differs from the file, and opening while navigating, in English, German, Dutch and French.
