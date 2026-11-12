## Why

The maintainer asked for design improvements to the map's controls, mostly for the iPhone (2026-11-12):
- the map buttons should be grouped and have a glass effect;
- the ⓘ has become a settings menu and undersells it;
- Navigate is long and could be an icon, like Stop;
- the overflow menu might go.

The mockups at https://claude.ai/artifact/NPjcSYcmLEZyvwu3CFSjne were liked, with two fixes found while looking at them:
- the Android Navigate button covered a map button, so the map buttons have to float with it;
- the gear should stand apart from the Cycling/Hiking switch on Android too.

## What Changes

- The map buttons stand in groups: search alone, the position button with the nearest knots button, and zoom in with zoom out. Each group shares one background: Liquid Glass on iOS, a card on Android. In portrait the column stands on the panel's top edge and moves with it; on Android it stands directly above Navigate. Where it doesn't fit, the zoom group, then the position group are left out.
- The ⓘ button becomes a round gear button at the top right, apart from the network switch, announced as "Settings". It still opens the info sheet.
- **BREAKING (UI):**
  - Navigate becomes a round icon button with the navigation arrow and no label, like Stop. On iOS it sits in the panel's header. On Android it's a floating action button at the panel's top right that moves with the panel. In landscape it's at the end of the side panel's header on both.
  - The "More actions" menu goes. Back to start (with its label), Export GPX and Clear sit in one row in the panel below the route's knots.
- While the note after a tap shows, it stands on the panel's edge, and the column (and Navigate on Android) stands on top of it. Where the column doesn't fit, the zoom group is left out while the note shows.
- Everything over the map shares one glass look (maintainer, 2026-11-12): the switch, the gear, the button groups, the note, the place card, Navigate on Android, and the notes like "Loading the courses". On iOS it's Liquid Glass. On Android it's a slightly translucent surface with a soft border and shadow: the map is drawn on its own surface, so Compose can't blur what's behind it.
- Cycling/Hiking stays a segmented control. A bottom navigation was considered and rejected: it's a filter on the same map, not a section of the app.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`:
  - New, "Map buttons in groups" and "Glass on the map".
  - "Zoom controls": one control.
  - "Show my position" and "Nearest knots": grouped above the zoom.
  - "Clear actions in the planner panel": Navigate as an icon, no More actions menu.
- `network-sync`:
  - "Info sheet": opened by the gear button, announced as "Settings".

## Impact

- Android: `NetworkMap.kt` (button column into groups, placed by `PlannerScreen` on the sheet's top edge), `PlannerScreen.kt` (`NavigateButton` as a FAB outside the sheet, `MoreActions` replaced by an action row, `InfoButton` as a gear apart from the switch), strings, the gear drawable.
- iOS: `NetworkMapView.swift` (UIKit button stacks into groups with glass, positioned from the sheet height), `PlannerView.swift` (Navigate as a round `glassProminent` button, the action row, the gear button).
- Specs: `openspec/checked-by-hand.md` gets the new layout scenarios; device checks on both platforms and in landscape.

## Out of scope

- The navigation-mode layout beyond Stop and the recenter button.
- New actions or a new settings screen; the info sheet's content stays.
- The colours of the green actions in light mode (an earlier note).
