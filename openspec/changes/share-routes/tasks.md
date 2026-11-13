## 1. Core: the route file

- [x] 1.1 `RouteFile.write(route, title)`: the Markdown list from `Gpx.knotsWithRoles`, the anchor's node id, towns where they change, labels in place of towns, positions with five decimals. Tests in a new `RouteFileTest` named after the scenarios: `Route as a file`, `Hiking route as a file`, `Route point as a file`, `Route point from a place as a file`, `Round trip as a file`, `Towns in the file`.
- [x] 1.2 `RouteFile.parse(text)` and `resolve(graph)` as in design.md. Tests on built networks with repeated numbers: `Knot found as a neighbour`, `Round trip back to the start`, `Hand-written file without an id`, `Knot skipped in a hand-written list`, `Knot not found`, `Other lines skipped`, `Not a route file`, `Route point opened` (connected after `reattached`), and a round trip write → parse → resolve.
- [x] 1.3 Waypoint label: an optional `label` kept by the planning session and `RouteSnapshot` (older snapshots read without it) and used by the waypoint list's text. Tests `Own text for a stop` (label kept through a snapshot and an undo) and a snapshot written before this change.
- [x] 1.4 The file name and title: `RouteFile.fileName(route)` (the GPX name with `.knoop`) and the default title from the start's and end's numbers and towns; a check of the listed knots against the route found, for the message. Tests `Share a route`, `Route different from the list`.

## 2. Android

- [x] 2.1 Share in `RouteActions`: the Export GPX button becomes Share with a `DropdownMenu` of "Route for Knoop by Knoop" and "GPX for other apps"; `shareGpx` generalised to share either file. Strings in all four languages. Check on the emulator: the share sheet with the `.knoop` file, also offline; the GPX export as before.
- [x] 2.2 Opening: intent filters in `AndroidManifest.xml` (design.md), `MainActivity` passing the URI from `onCreate`/`onNewIntent` to `AppViewModel.open(text)`: network switch, `history.record`, the route shown whole, the waypoint labels, the messages for left-out knots, a different route, no route and navigating; pending until the graph (and geometry for route points) is read. Check on the emulator by pushing a file with `adb push` and opening it from Files, and from a mail or messenger app if one is installed.

## 3. iOS

- [ ] 3.1 Share in `routeActions` as a `Menu` with the two choices; `ExportedGpx` generalised to any shared file. Strings in all four languages. Check in the simulator: the share sheet with the file.
- [x] 3.2 Opening: the exported type and document type in `project.yml`, `onOpenURL` in `KnoopByKnoopApp` handing the text to `AppModel.open(text)` with the same behaviour as on Android. Check in the simulator by dragging a `.knoop` file in and opening it from Files.

## 4. Checks

- [x] 4.1 Update the `checked-by-hand.md` lines that name Export GPX, and add the scenarios only the devices can check (`Share while offline`, `Home flag shared as it is`, `Open a shared route`, `Opened on the other network`, `Opened while navigating`, `Undo an opened route`, `Opened route on the other network`, and the panel scenarios changed to Share), with how they are checked. CHANGELOG Unreleased line.
- [ ] 4.2 Manual check across the phones: share the route Siegburg 02 → 83 → 73 → 02 with a home flag from the Pixel to the iPhone and back, and open each; open a file written by hand from a paper-map list without an id. Both show the same route.
- [x] 4.3 Run `./gradlew check` and `npx @fission-ai/openspec validate --all`, then `./gradlew checkScenarios -Pchange=share-routes`.
