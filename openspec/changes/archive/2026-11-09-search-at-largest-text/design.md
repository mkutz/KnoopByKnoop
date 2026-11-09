# Design

## Context

- The iPhone's sheet has the detents `planningDetent` (or `navigationDetent`), `.medium` and `.large`. `planningDetent` is a height measured from the summary, the knots and the switch (`summaryHeight`); at the largest text size it is taller than half the screen.
- `openSearch()` keeps the detent in `detentBeforeSearch` and selects `.large`; `closeSearch()` restores it. An `onChange(of: keyboardShown)` sets `planningDetent` when the keyboard shows outside the search.
- Seen in the simulator (iPhone 18 Pro, Accessibility XXXL), right after the start: the first tap on the magnifier dropped the sheet to a lower height with its content scrolled up ("15 km" cut off), without opening the search; the second tap opened it, with the field's top under the grabber.

## Decisions

### Find the cause first
- Reproduce with the throwaway XCUITest runner at the largest size, with timing notes on detent changes, `openSearch` and `closeSearch`, to see which detent the sheet gets and what sets it. Likely suspects: `planningDetent` changing while the summary is measured, so the selected detent is not among the offered ones and the sheet falls back; or the summary's measured height including the scrolled content.
- **Found (2026-11-09, simulator, with notes on detents and the search):** the tap never reached the magnifier. At this size the summary measures 562 pt, so the planning height (596 pt) is taller than `.medium`, half the screen, while the sheet let the map take touches only `upThrough: .medium`. Above that, the system treats the map as behind a modal sheet: the first tap is swallowed or pulls the sheet down to medium. That was the drop, and every button on the map was dead on the first tap, not only the magnifier.
- **Fix:** touches pass to the map up through the planning height when it is taller than medium, else up through medium as before.
- **Found too:** the field's top and, with the sheet lower than its content, the route's distance were cut off because the panel's content is taller than the sheet at this size and SwiftUI centred it, so it overflowed at the top and the bottom. The panel now pins its content to the top, so only the bottom goes off.

### Room below the grabber
- No extra padding was needed: pinned to the top, the search keeps the 20 pt it already had below the grabber.

## Risks / Trade-offs

- **[A fix in the detent logic changes the sheet at other sizes]** → Check the default size too: smallest, medium, large, opening and closing the search, and navigating.
