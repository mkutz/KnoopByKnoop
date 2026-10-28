# Design

## Context

`PlannerView.searchOverlay` places the search field and the attribution in a bottom-aligned overlay of the map. It uses the field's focus (`searchFocused`) for three things:

- the bottom padding: 8 points while focused, assuming the keyboard's safe area lifts the overlay; otherwise the measured `sheetHeight`,
- hiding: the overlay is never hidden while focused,
- the sheet: when the field gets the focus in portrait, the sheet is set to its smallest detent, which the keyboard then covers.

With a hardware keyboard the field gets the focus, but no keyboard safe area comes up, so the overlay sits 8 points above the bottom, behind the sheet. Reproduced in the iPhone 18 Pro simulator (iOS 27.0) with the Mac keyboard connected: the hits for "Sieg" were listed above the sheet, with the field behind it.

## Goals / Non-Goals

**Goals:**
- The field stays visible while typing, with or without an on-screen keyboard.
- With an on-screen keyboard, the behavior stays as it is.

**Non-Goals:**
- Landscape: the overlay's padding there is 8 points with or without focus, and the panel is not a sheet, so nothing changes.
- The small shortcut bar some iOS versions show for hardware keyboards: it is treated as no keyboard (see Risks).

## Decisions

### Use whether a keyboard covers the screen, not the focus
A `keyboardShown` value, derived in `PlannerView` from its geometry on every layout, replaces `searchFocused` in the overlay's padding, in the hiding rule and as the trigger for lowering the sheet. The focus still drives the field itself (dismissing the keyboard after a hit or clearing). Without a keyboard, the overlay keeps its place above the sheet, so the hits keep growing upwards over the map as they do now.

### Tell from the keyboard's safe area
The `GeometryReader` of `PlannerView` already includes the keyboard's safe area in `safeAreaInsets.bottom` (the existing comment on `landscape` relies on that). A keyboard counts as shown when that inset is more than 100 points; without a keyboard, it is the home indicator's 34 points, or 0 on iPhones with a home button. (Planned as "more than 100 points over the inset without a keyboard"; during apply, a fixed 100 turned out to be enough, as that inset is never more than 34.) This needs no extra state for keyboard frames and follows the same layout pass that moves the overlay. Alternative: observing `UIResponder.keyboardWillShowNotification` and `keyboardWillHideNotification` and keeping the keyboard's height in a state. It is the classic way, but it is UIKit plumbing in a SwiftUI view, and its frames can disagree with the safe area SwiftUI actually applies, e.g. for floating keyboards.

### Lower the sheet when the keyboard comes up
`.onChange(of: keyboardShown)` sets the smallest detent in portrait, instead of `.onChange(of: searchFocused)`. With a hardware keyboard, the sheet keeps its height, and the field stays above it at whatever height the sheet has, up to medium. At large, the overlay is hidden as before, so the field cannot get the focus there.

## Risks / Trade-offs

- [The keyboard comes up a moment after the focus, so the sheet is lowered a little later than now] → the keyboard's animation and the detent change then run together; checked in the simulator.
- [Hardware keyboards can show a small shortcut bar at the bottom, under the 100-point threshold] → it is then treated as no keyboard: the field stays above the sheet, which the bar does not cover.
- [The threshold is a fixed number] → on-screen keyboards on iPhones are well over 200 points high, the home indicator about 34, so 100 leaves room on both sides.
