# Design

## Context

`PlannerView.searchOverlay` lays out a `VStack` in a `GeometryReader` over the map: a `Spacer`, the attribution, then `PlaceSearchView` (the hits above the field). It has a top padding of `notesTop` (64 points, to stay below the network switch) and a bottom padding of 8 points in landscape. The hits get `maxResultsHeight = min(320, max(area height - notesTop - bottom - 90, 80))`. In landscape with the on-screen keyboard, the area is about 190 points high (iPhone 18 Pro), so the formula gives the 80-point minimum. Top padding, attribution, 80 points of hits and the field come to more than the area, so the `VStack` overflows downwards and the field ends up under the keyboard. `keyboardShown` (from `keep-ios-search-above-sheet`) already tells whether an on-screen keyboard covers the screen.

## Goals / Non-Goals

**Goals:**
- The field is never pushed under the keyboard, in any orientation.
- In landscape while typing, the hits get all the height above the field.

**Non-Goals:**
- Changing the portrait layout, where `keyboardShown` lowers the sheet and the room is enough.
- Hiding the zoom buttons: they are on the right edge of the map, and the hits already cover them while typing.

## Decisions

### Hide the switch and the attribution while typing in landscape
`typingInLandscape = landscape && keyboardShown`. While it is true, `content` leaves out the `HStack` with the network picker and the info button, and `searchOverlay` leaves out the attribution and uses the top safe area plus 8 points as its top padding instead of `notesTop`. Both come back as soon as the keyboard is gone, which happens when a hit is chosen or the search is cleared (both remove the focus) or the user dismisses the keyboard. The maintainer chose this over hiding only the switch (about half a row more for the attribution) and over hiding nothing (about one row).

### The hits get the room that is left, without a fixed minimum
The fixed 80-point minimum is what makes the column overflow. The hits' maximum height becomes the room above the field: the area's height minus the top padding, the bottom padding, the field's height and the spacing, in every orientation, still at most 320 points. When less room is left than one hit needs, the hits list still gets it and scrolls, but the field keeps its place. The field's height is measured with `onGeometryChange` instead of the guessed 90 points, which covered the field and the attribution.

### No notes while typing in landscape
The loading and courses notes sit at `notesTop` below the switch. While typing in landscape, the hits cover them as they cover the map. They are left as they are.

## Risks / Trade-offs

- [The OpenStreetMap attribution is not visible while typing in landscape] → the hits and the keyboard cover nearly all of the map then; the attribution comes back with the map as soon as the keyboard goes away, and the info sheet always shows it. The `map-planning` "Attribution" scenario names this exception.
- [Switch and attribution disappear and reappear with the keyboard] → they are animated with the keyboard, like the field; checked on the iPhone.
- [Measuring the field's height adds a layout pass] → one `onGeometryChange` on a small view; the sheet's height is measured the same way.
