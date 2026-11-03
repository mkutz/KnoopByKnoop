# Design

## Context

- **The to-me button** (`map-planning` "Show my position"): round, below the zoom buttons, while planning. `locate` asks for access, finds a position within 15 s (`AppViewModel`, `AppModel`), then the map centers and marks it with a dot. `onLocate` is `null` while navigating, which hides it.
- **The camera:** the apps report `CameraPlace` (center and zoom) when the map stops moving (`mapMoved`). Knots show from `KNOT_MIN_ZOOM`, 10.
- **`KnotLocator`:** a grid of about 1 km cells, for lookups within a radius; `nearest` needs a radius. `loaded.locator` belongs to the selected network.
- **The panel:** `RouteDescription.Empty` shows "Tap a knot to set the start".

## Goals / Non-Goals

**Goals:**
- From an empty map to tappable knots in one tap, on both platforms, from the core's knowledge of the network.

**Non-Goals:**
- Asking for location access, or finding the network's area.

## Decisions

**"In view" is decided in the core, when the map stops moving.** `CameraPlace` stays as it is, and the apps also pass a `GeoBounds?` to `mapMoved`: the map's area, or `null` below `KNOT_MIN_ZOOM`, where no knots show. `KnotLocator.noKnotsIn(area)` looks at all of the knots' positions, and only runs when the map stops moving. Both apps keep the result as `noKnotsInView` in their state.
- Android's map ends above the panel, so its visible region is the area. iOS's map lies below the panel, so its area leaves out the bottom `edgePadding`, the same room as for a route.

**`KnotLocator.nearestPosition(latitude, longitude)`,** a scan of all positions, only on a tap. A knot's nearest position counts, and the map shows that position. Null only for an empty network, where `noKnotsIn` is false, so the button is hidden.

**Which position.** On a tap:
- With access already allowed: the same position finder as the to-me button, with its spinner on the button, and its dot marking the position. If it fails, the middle of the map.
- Otherwise: the middle of the map, without asking. Taking a permission prompt out of this flow keeps the tap predictable; the to-me button remains the way to allow it.

**Where the map goes.** The apps fit the rider and the knot with the same room as for a route, no closer than the position button's zoom, 14. If the fitted zoom would be below `KNOT_MIN_ZOOM`, the map centers on the knot at `KNOT_MIN_ZOOM` instead, so knots are always tappable after a tap. Without a rider's position, it centers on the knot at that zoom too. The position's dot is set without the position button's own centering.

**The panel and the button use one action.** `showNearestKnots()` on `AppViewModel` and `AppModel`. Panel text and button appear only for `RouteDescription.Empty`, so an existing route's panel is unaffected.

**New strings** in all four languages:

| | No knots in view | Show the nearest knots |
|---|---|---|
| de | Keine Knotenpunkte zu sehen | Nächste Knotenpunkte zeigen |
| nl | Geen knooppunten in beeld | Dichtstbijzijnde knooppunten tonen |
| fr | Aucun point-nœud en vue | Afficher les points-nœuds les plus proches |

The icon is a signpost, on Android as an own vector and on iOS as the symbol `signpost.right`: the network's knots are signposted, and it reads as different from the to-me button.

**The fit stays clear of the controls.** Found by the maintainer on the iPhone, 2026-11-03: the knot lay behind the search field, and the button stayed, correctly, as `knotArea` leaves out the panel. `cameraThatFitsCoordinateBounds` honors the padding for the zoom, but keeps the middle of the bounds in the middle of the whole map, and on iOS the map lies below the panel. So the camera is moved to have it in the middle of the padded area, with a margin for a badge and its ring. Android's top padding now includes the controls. The same fit on iOS for the off-route line gets the same move (`improve-off-route` "Both in view").

## Risks / Trade-offs

- **Knots behind the top controls count as in view:** the button can be missing with the only knot behind the search field or the network switch. Likewise on iOS with the panel pulled up, as the area only leaves out the panel at its smallest.
- **Button appearing and disappearing** while the user pans, at the moment the map stops. Expected, and it only moves into free space below the to-me button, so nothing else shifts.
- **A position up to 15 s away:** with access allowed and a slow fix, the tap only shows the spinner for a while, as the to-me button does.
