# Proposal

## Why

Guidance away from the route is off until the rider turns it on, because downloading path data shows GitHub the rough area of a planned route. Since 5 November, squares are chosen from the planned route alone, never from the rider's position. That reveals less than the base map already does: OpenFreeMap receives every map tile at street level, including those around the rider while the map follows them. Measured on 6 November against the published path file (data of 3 November), a planned route costs 0.7 to 6.3 MB:

| Route | Length | Squares | Download |
|---|---|---|---|
| Siegburg town ride | ~22 km | 12 | 0.7 MB |
| Siegburg to Köln | ~30 km | 22 | 1.9 MB |
| Siegburg loop | ~66 km | 31 | 1.2 MB |
| Limburg loop | ~78 km | 28 | 1.0 MB |
| Utrecht loop | ~100 km | 50 | 2.9 MB |
| Randstad loop | ~163 km | 82 | 6.3 MB |

The switch also hides a feature most riders would want, and `add-route-points` needs path data to find the way to a place away from the network without an online service. The switch has not been released yet; it is only in Unreleased.

## What Changes

- **No switch:** path data around a complete cycling route is downloaded for everyone, and the guided way works for everyone. The switch and its setting are removed.
- **A third entry in the info sheet:** next to the cycling and hiking networks, an entry for the ways: their data date and how much space they take, with a button to delete them. A short text says what they are for and that GitHub sees the rough area of planned routes.
- **Deleting:** removes all installed squares. They are downloaded again for the next route, or the current one when it changes or navigation starts.
- **Privacy policy and changelog:** the feature is no longer described as off until turned on.

## Out of Scope

- **An opt-out,** or downloading over Wi-Fi only. The measured sizes don't call for it.
- **Path data for hiking.** Hiking routes still download nothing.
- **Changing which squares are downloaded,** the 5 km distance or the 50 MB limit.
- **`add-route-points` itself.** It is reworked afterwards to use the path data.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `network-sync`: "Turning on guidance away from the route" is removed. "Path data around the route" and "Path data ready for the ride" no longer depend on a switch. "Path data in the info sheet" becomes the entry with its size and the delete button.
- `knot-navigation`: "Guided way while off route" no longer depends on a switch, and its scenario "Guidance turned off" goes.

## Impact

- **Core:** `PathSquares.courseToLoad` loses its `enabled` parameter. `PathStore` reports the installed size and deletes everything.
- **Android:** `AppViewModel` loses `guidance` and `chooseGuidance`, and gets the size and a delete action. `InfoSheet` gets the entry in place of the switch. Strings in all four languages.
- **iOS:** the same in `AppModel` and `InfoSheet.swift`, with the strings in all four languages.
- **Outside the code:** `PRIVACY.md` and the Unreleased entry in `CHANGELOG.md`. The Play data safety form and App Store labels need a look, as GitHub downloads now happen for every cycling route.
- **Phones:** the maintainer's phones have the switch's setting stored. It is ignored after this change.
