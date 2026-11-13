## Context

The planned route is kept as a `RouteSnapshot` (JSON, by knot id, with route points carrying their connection and way) for restarts and undo. `RouteSnapshot.restore` drops knots the graph no longer has and reports their numbers, and `PlanningSession.reattached(geometry)` connects route points whose connection is gone to the nearest course. `Gpx.knotsWithRoles` already lists every knot a complete route passes with its role. The GPX export (`GpxExport.kt`, `GpxExport.swift`) writes a file to the cache and offers it in the share sheet through a `FileProvider` on Android and `UIActivityViewController` on iOS. Neither app accepts files from other apps yet. Knots know their town (`Knot.place`, `network-data` "Place of each knot").

## Goals / Non-Goals

**Goals:**
- A file people can read in any Markdown viewer and write by hand from a paper map, close to the knot lists riders used to note down.
- Robust against numbers that repeat across regions and against data updates.
- Opening reuses the restore and reattach paths, so a shared route behaves like one restored after a restart.
- The panel keeps its single action row; Share takes Export GPX's place.

**Non-Goals:**
- No change to how the app stores its own route beyond the waypoint text: `RouteSnapshot` stays JSON with connections and ways.
- No guidance ways in the file: they are found again from the route points.

## Decisions

### Markdown list of knots

```
# Siegburg Bonn and back

- 02 6364748 Siegburg
  - 28
  - 29 Troisdorf
  - 30
- 83 Bonn
  - 27
  - 82
  - 81
  - 80
  - 79
  - 01
- 73
  - …
- 02 Siegburg
```

- Every knot passed, not only the waypoints: knot numbers repeat every 10 to 20 km, so a number alone is only safe next to the knot before it. The full list is also what riders wrote down.
- Waypoints top-level, knots passed indented: renders as a nested list, and the waypoints stand out.
- The id only on the first knot: it anchors the list; every further knot follows from its neighbour. Ids on every line would make hand-writing impossible and add nothing.
- Item grammar: `- ` (top-level) or two or more spaces and `- ` (indented), then either `<number>` (a run without spaces, as refs can be `1a`) or `<latitude>, <longitude>`, then for the first knot only `<node id>` (digits, at least 5, so a number in a text isn't taken for one), then the rest of the line as text. `*` and `+` bullets are accepted too.
- Town written where it changes from the knot before, so the file reads like a signposted list without repeating "Bonn" eight times.
- Optional `Network: hiking`; default cycling. Looking the anchor's id up in both networks would need the hiking graph loaded beside the cycling one, which is too heavy on small phones.
- Route points have no kind in the file: the text is their name. A shared café ends as a named flag, not with the food icon. Adding the kind would need a syntax people don't write; the name carries what matters.

### Reading

`RouteFile.parse(text)` → title, network, items (top-level or indented, number or position, node id, text). `RouteFile.resolve(graph)`:
1. First knot: by id in `graph.knots`; else among knots with its number, those whose `place` matches the text, the only one or none.
2. Each further number: among the knots connected to the previous found knot by one connection (the graph's neighbours) with that number, the one whose place matches the text if several; else the nearest with that number within 5 km of the previous found knot (from `KnotLocator`). After a route point: the nearest within 5 km of the point.
3. Not found: left out, its number reported; the next item is resolved from the previous found knot.
4. Waypoints = the top-level items found; route points become unconnected route points that `reattached(geometry)` connects. Fewer than two → no route.
5. The planner routes between the waypoints as usual. If the knots it passes differ from the indented ones found, a message says so.

### Waypoint text

An optional `label` on the waypoint (`Knot.label`, carried by the planning session and `RouteSnapshot.Waypoint`), set only from a file and only when the text differs from the knot's town. The waypoint list shows it where it now shows the town. On writing, a label is written in place of the town.

### Loading as an edit

The apps load the resolved route through the undo history: `history.record(before, after)` first, so Undo brings back the previous route; a network switch clears the history as switching does today. The map then fits the whole route as after a restart.

### Opening files

- **Android:** an `ACTION_VIEW` intent filter on `MainActivity` for `content` and `file` URIs with the `.knoop` path pattern, plus `application/octet-stream`, `text/plain` and `text/markdown` (messengers and mail often give no extension or a generic type). The app reads the stream and refuses what it can't read with the "no route" message. Handled in `onCreate` and `onNewIntent`. Shared files use the type `text/markdown`.
- **iOS:** an exported type `com.knoopbyknoop.route` conforming to `net.daringfireball.markdown` and `public.utf8-plain-text`, extension `knoop`, a document type in `project.yml` (`LSHandlerRank` Owner), and `onOpenURL` in `KnoopByKnoopApp`, reading under security-scoped access.

### Share in the panel

The Export GPX icon button becomes Share with the same icon: a `DropdownMenu` on Android and a `Menu` on iOS with the two choices. Both need the route's course, so both show when the line is known. One button keeps the row's width at 360 dp. Alternative: two buttons; rejected because two share icons side by side are hard to tell apart and the row is full.

## Risks / Trade-offs

- [Two knots with the same number both connected to the previous knot] → the town in the text decides; without one, the first. Very rare, and the user sees the route before riding.
- [Android intent filters for custom extensions are unreliable] → the generic MIME filters plus parsing; the app may show in "Open with" for other text files, which it then refuses politely.
- [A file opened before the network or its courses are loaded] → keep the opened route pending until the graph is read (and the geometry, for route points), as the restore at startup does.
- [A hand-written list that isn't the shortest way] → the app's route differs, and the user is told; steering knots later if riders need them.
- [A shared file carries the sender's home position] → the user's decision, as the maintainer wants; no prompt.
