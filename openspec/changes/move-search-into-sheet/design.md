# Design

## Context

`add-place-search` put the iOS search field (`PlaceSearchView`) into the `VStack` over the map in `PlannerView.content`, above the network picker and the info button, with the hits in a card right below the field. In portrait, the waypoint panel is a sheet with the detents `.height(170)`, `.medium` and `.large` (`detent` state), presented from the root of `PlannerView`; in landscape, the same `panel` is shown next to the map. The panel already switches its content between `InfoSheet`, `WaypointSheet` and a progress view. `AppModel` holds `searchQuery`, `searchResult` and `marker`, and is not touched.

## Goals / Non-Goals

**Goals:**
- The field at the bottom within reach of the thumb in portrait, like Apple Maps.
- The hits in the panel, never under the keyboard.
- The map less covered at the top.

**Non-Goals:**
- Android: Google Maps keeps search at the top, and Android's layout stays as it is.
- Any change to the search itself (`PlaceSearch`, `AppModel.search`).

## Decisions

### The field at the top of the panel, in both orientations
The field becomes the first row of `panel`, above `WaypointSheet`, in portrait and landscape alike, so there is one layout to build and check. It is hidden while the panel shows the info, which has its own Done button and is not about the map. Alternative: the field at the bottom of the sheet, above the home indicator. That puts it under the keyboard once it is focused and needs the sheet to move anyway, so it gains nothing over the top of the sheet.

### Hits in place of the waypoints
`PlaceSearchView` is split into `PlaceSearchField` and `PlaceSearchResults`. While `searchResult` is not `.none` (searching, hits, nothing found, unavailable), the panel shows `PlaceSearchResults` below the field instead of `WaypointSheet`. The hits become a `List` of plain rows, filling the panel, instead of a card with a maximum height. When a hit is chosen or the search is cleared, `searchResult` goes back to `.none` (as `AppModel.choose` and `clearSearch` already do), and the waypoints come back.

### The sheet follows the field
When the field gets the focus in portrait, `detent` is set to `.large`, so the hits have the room above the keyboard. When the search ends (a hit chosen, the search cleared), `detent` goes back to the smallest height, so the map shows the hit and its marker. Focus is passed from the field to `PlannerView` with a binding to a `@FocusState`, which also lets choosing a hit dismiss the keyboard. Dragging the sheet down while searching keeps the search; the hits are still there when it is pulled up again.

### The smallest height grows by the field
The smallest detent becomes `.height(220)` instead of 170, so the field and the summary line stay visible. The attribution's bottom padding over the sheet (132) and the notes' top padding (120, below the field and the picker) are adjusted: the attribution to the new sheet height, the notes to just below the network picker.

## Risks / Trade-offs

- [The taller smallest sheet covers more of the map in portrait] → 50 points, offset by the field leaving the top of the map; checked on the iPhone.
- [Changing `detent` while the keyboard appears can animate awkwardly] → set the detent when the field gets the focus, before the first character; checked on the iPhone.
- [The two apps now differ in layout] → the spec says so per platform; the behavior of search is the same.
