# Design

## Context

- `PlannerView` puts `PlaceSearchView` and the attribution in `searchOverlay`, above the sheet in portrait (following its height) and above the keyboard while typing; it hides them while the sheet is pulled up. The hits are listed above the field, over the map. Landscape has a side panel instead of the sheet.
- The field draws its own glass with `FloatingBackground` (`glassEffect(.regular)`); the sheet has the system's glass.
- `NetworkMapView` adds the map's buttons as UIKit views: zoom in and out centred on the map, my position and the nearest knots below them, and Recenter while navigating.
- The sheet's detents are the planning detent, `.medium` and `.large`; `detent` is bound to the selection.

## Decisions

### The magnifier button joins the map's buttons
- A round 40 pt button with `magnifyingglass`, styled like the zoom buttons, at the top of the column, above the zoom buttons, hidden while navigating like my position. **Changed during apply:** the mockup had it at the bottom, but at the largest text size the planning sheet leaves too little room for the column, and the lowest buttons go under the sheet; at the top, the search stays reachable, as the floating field was. Its accessibility label is "Search places", in all four languages.
- **Why there:** the column is where the map's tools are; the search was the only one floating elsewhere.

### The button column ends above the sheet
- The column of map buttons is anchored with its bottom a little above the sheet's top edge in portrait, moving with the sheet, instead of being centred on the map, so the extra button and a tall sheet cover none of them. In landscape it stays where it is, beside the side panel.
- **Why:** with the floating field gone, a column centred on the map would still reach under a tall sheet, now one button longer.

### The search opens in the panel
- Tapping the button sets a `searching` state: the sheet goes to `.large` in portrait and its content shows `PlaceSearchView` at the top, focused, with the hits below it instead of the waypoint list. In landscape the side panel does the same.
- The field and hits drop `FloatingBackground` and sit on the panel's background, so there is one surface.
- **Why `.large`:** the keyboard takes the lower half of the screen; the hits need the room above it.
- **Alternative:** `.medium`. The keyboard would cover most of the hits.

### Closing the search
- Picking a hit, Cancel, or a tap on the map ends `searching`; the field's clear button empties it and keeps the search open, as in other iOS apps: focus off, the waypoint list back, and the detent the sheet had before.
- The attribution stays on the map above the sheet, where the overlay had it; it no longer needs to give way while typing in landscape, as nothing is typed over the map.

### Offline
- The button stays enabled; the opened field shows the existing disabled state with its message, so "Search needs internet access" is unchanged.

## Android (added 2026-11-09)

### The same, in Compose
- A `SmallFloatingActionButton` with a magnifier icon at the top of `NetworkMap`'s column, styled like the zoom buttons, only while planning (`onSearch` is `null` while navigating), labelled "Search places".
- `PlannerScreen` keeps a `searching` state (`rememberSaveable`). In portrait, opening expands the `BottomSheetScaffold`'s sheet and its content shows `PlaceSearchField` at the top, focused, with the hits below, instead of the waypoints; closing partially expands it again if it was at its peek, or leaves the height it had. In landscape, the side panel's `Surface` shows the same.
- `PlaceSearchField` drops `mapOverlayColor` for the panel's surface and gets a Cancel button next to the field; the ✕ keeps clearing only.
- `BackHandler(enabled = searching)` closes the search, as Android users expect from the back gesture. **Found during apply:** with the keyboard open, the first back closes only the keyboard, as everywhere on Android; the next closes the search. The scenario says so.
- **Found during apply:** reopening with text left from before put the cursor at the start, so typing on gave "AmsterdamUtrecht Centraal"; the field keeps a `TextFieldValue` with the cursor at the end.
- **Checked during apply:** `BottomSheetScaffold` lays the map out above the sheet's smallest height, so the column of buttons needs no anchoring: at the medium height none of them is covered.
- The map's column of buttons is anchored above the sheet as on iOS if the sheet covers it at its medium height; checked on the emulator first, as Compose's `BottomSheetScaffold` may already lay the content out above the sheet.

### Landscape
- The side panel no longer slides aside while the keyboard is open, and `RouteChip` goes: the field and hits are in the panel, so the map needs no more room. `map-planning` "Landscape layout" is changed to say so.

## Risks / Trade-offs

- **[One more tap to search]** → Accepted by the maintainer for a clear map; the button is where the eye looks for map tools.
- **[The sheet jumping to large]** → It returns to the height it had when the search closes.
- **[Android riders used to the field on the ride]** → The maintainer finds it of little use while navigating; places can be looked up before starting.
- **[Large text sizes]** → The panel's field and hits use the system's dynamic type; check at the largest size.
