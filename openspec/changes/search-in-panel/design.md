# Design

## Context

- `PlannerView` puts `PlaceSearchView` and the attribution in `searchOverlay`, above the sheet in portrait (following its height) and above the keyboard while typing; it hides them while the sheet is pulled up. The hits are listed above the field, over the map. Landscape has a side panel instead of the sheet.
- The field draws its own glass with `FloatingBackground` (`glassEffect(.regular)`); the sheet has the system's glass.
- `NetworkMapView` adds the map's buttons as UIKit views: zoom in and out centred on the map, my position and the nearest knots below them, and Recenter while navigating.
- The sheet's detents are the planning detent, `.medium` and `.large`; `detent` is bound to the selection.

## Decisions

### The magnifier button joins the map's buttons
- A round 40 pt button with `magnifyingglass`, styled like the zoom buttons, placed below the my-position and nearest-knots buttons, hidden while navigating like my position. Its accessibility label is "Search places", in all four languages.
- **Why there:** the column is where the map's tools are; the search was the only one floating elsewhere.

### The search opens in the panel
- Tapping the button sets a `searching` state: the sheet goes to `.large` in portrait and its content shows `PlaceSearchView` at the top, focused, with the hits below it instead of the waypoint list. In landscape the side panel does the same.
- The field and hits drop `FloatingBackground` and sit on the panel's background, so there is one surface.
- **Why `.large`:** the keyboard takes the lower half of the screen; the hits need the room above it.
- **Alternative:** `.medium`. The keyboard would cover most of the hits.

### Closing the search
- Picking a hit, clearing the field, or a tap on the map ends `searching`: focus off, the waypoint list back, and the detent the sheet had before.
- The attribution stays on the map above the sheet, where the overlay had it; it no longer needs to give way while typing in landscape, as nothing is typed over the map.

### Offline
- The button stays enabled; the opened field shows the existing disabled state with its message, so "Search needs internet access" is unchanged.

## Risks / Trade-offs

- **[One more tap to search]** → Accepted by the maintainer for a clear map; the button is where the eye looks for map tools.
- **[The sheet jumping to large]** → It returns to the height it had when the search closes.
- **[Large text sizes]** → The panel's field and hits use the system's dynamic type; check at the largest size.
