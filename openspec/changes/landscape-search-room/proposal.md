# Proposal

## Why

On iOS in landscape, typing in the search leaves about 190 points of the map above the on-screen keyboard. The search column needs more than that: 64 points of space for the network switch, the attribution, at least 80 points for the hits and the field. It overflows, so the field is pushed under the keyboard and the hits get room for about one and a half entries. Seen on the maintainer's iPhone during the checks of `keep-ios-search-above-sheet`. It has behaved this way since `move-search-above-sheet`.

## What Changes

- On iOS in landscape, while the on-screen keyboard is shown, the network switch and the attribution are hidden. The hits can use the height from the top of the screen down to the field, about three entries.
- The field always stays fully visible above the keyboard, and the hits get only the room that is left.
- When the keyboard goes away (a hit is chosen, the search is cleared, or the user dismisses it), the switch and the attribution come back.

Out of scope: portrait, where the sheet is lowered and there is enough room; typing with a hardware keyboard, where there is no on-screen keyboard; Android; the side panel; the zoom buttons, which the hits already cover while typing.

## Capabilities

### New Capabilities

### Modified Capabilities
- `place-search`: the field and the hits while typing in landscape on iOS
- `map-planning`: the network switch and the attribution are hidden while typing in the search in landscape on iOS

## Impact

- `apps/ios`: `PlannerView` hides the network switch and the attribution in landscape while `keyboardShown`, and gives the hits the room between the top safe area and the field instead of a fixed minimum below the switch
- No changes to `modules/core` or `apps/android`
