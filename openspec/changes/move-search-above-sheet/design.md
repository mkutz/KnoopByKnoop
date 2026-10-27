# Design

## Context

`add-place-search` put the iOS search field (`PlaceSearchView`) into the `VStack` at the top of the map in `PlannerView.content`, above the network picker and the info button, with the hits in a card right below the field. In portrait, the waypoint panel is a system sheet with the detents `.height(170)`, `.medium` and `.large` (`detent` state), presented from the root of `PlannerView`, with background interaction enabled up through `.medium`; in landscape, the same `panel` is shown next to the map. The attribution sits at the bottom right of the map, 132 points up in portrait to clear the sheet. `AppModel` holds `searchQuery`, `searchResult` and `marker`, and is not touched.

## Goals / Non-Goals

**Goals:**
- The field at the bottom within reach of the thumb, like many iOS apps, but on the map, since search is about the map, not the route.
- The hits never under the keyboard.
- The top of the map less covered.
- The look of iOS 26 and later (Liquid Glass) where the device has it, without dropping iOS 17 to 25.

**Non-Goals:**
- Android: Google Maps keeps search at the top, and Android's layout stays as it is.
- Replacing the system sheet: its dragging, detents and glass come from iOS.
- Any change to the search itself (`PlaceSearch`, `AppModel.search`).

## Decisions

### The field follows the sheet's top edge
The sheet is a separate presentation, so the map view does not know how high it is. The sheet's content reports its height with `onGeometryChange` (available from iOS 16) into a `sheetHeight` state of `PlannerView`; the field is placed in a bottom-aligned overlay of the map with that height (minus the bottom safe area the map ignores) as bottom padding, plus a small gap. The height is reported while the sheet is dragged too, so the field moves with it. Alternatives: a field inside the sheet at its top or bottom, which the maintainer rejected because search belongs to the map; a custom panel with a bar under it, which means rebuilding the sheet.

### Hidden when the sheet is pulled up
At `.large`, the sheet covers the map, which does not take touches then, so a field above the sheet would sit at the top of the screen with no map to search on. The field is hidden whenever the sheet is higher than `.medium` (the sheet height is above half of the screen). Its query and hits are kept and shown again when the sheet is lowered.

### Hits above the field, the field above the keyboard
The hits card moves from below the field to above it, with the field at the bottom, so the hits grow upwards over the map. While the field has the focus, it is placed above the keyboard instead of above the sheet: the overlay respects the keyboard's safe area (SwiftUI's keyboard avoidance), and the sheet's height is ignored while the keyboard is shown, since the keyboard covers the sheet. Choosing a hit or clearing the search removes the focus, which dismisses the keyboard and puts the field back above the sheet. The hits card's maximum height stays at 320 points but never exceeds the room between the field and the network picker.

### Landscape
The panel is next to the map, so the field floats at the bottom of the map area, with the map's bottom safe area as padding. It behaves the same way otherwise.

### Attribution and notes
The attribution moves from 132 points over the bottom to above the field, so they do not overlap. The loading and courses notes move up from 120 to just below the network picker (64 points), as the field no longer sits above the picker.

### Liquid Glass where available
The views floating over the map use a background of their own today: `.regularMaterial` behind the search field, the hits, the network picker with the info button and `LoadingNote`, and a white, 70 % opaque background behind the attribution. A `floatingBackground(in:)` view modifier replaces them: on iOS 26 and later it applies `.glassEffect(.regular, in: shape)`, before that the `.regularMaterial` background as now. The checks use `if #available(iOS 26, *)`, so the deployment target stays iOS 17. The sheet and the system controls get Liquid Glass from iOS when built with the iOS 26 SDK, as long as the sheet sets no background of its own; the portrait sheet sets none. The landscape side panel keeps its `systemBackground`, as it is part of the layout, not floating over the map.

## Risks / Trade-offs

- [The field might lag behind the sheet while it is dragged, if the height is not reported for every frame] → checked on the iPhone; if it lags, the field is hidden while the sheet moves and shown again when it rests.
- [Field and attribution take a strip of the map above the sheet] → about 60 points, offset by the field leaving the top of the map.
- [Glass over a busy map can be hard to read, e.g. the attribution's small text] → `.regular` glass, not `.clear`; checked on the iPhone over a map with many knots.
- [Glass can only be checked on a device with iOS 26 or later] → the fallback is the current material, so it is checked by building, and in an older simulator if one is installed.
- [The two apps now differ in layout] → the spec says so per platform; the behavior of search is the same.
