# Design

## Context

- The iPhone's sheet has the detents `planningDetent` (or `navigationDetent`), `.medium` and `.large`. `planningDetent` is a height measured from the summary, the knots and the switch (`summaryHeight`); at the largest text size it is taller than half the screen.
- `openSearch()` keeps the detent in `detentBeforeSearch` and selects `.large`; `closeSearch()` restores it. An `onChange(of: keyboardShown)` sets `planningDetent` when the keyboard shows outside the search.
- Seen in the simulator (iPhone 18 Pro, Accessibility XXXL), right after the start: the first tap on the magnifier dropped the sheet to a lower height with its content scrolled up ("15 km" cut off), without opening the search; the second tap opened it, with the field's top under the grabber.

## Decisions

### Find the cause first
- Reproduce with the throwaway XCUITest runner at the largest size, with timing notes on detent changes, `openSearch` and `closeSearch`, to see which detent the sheet gets and what sets it. Likely suspects: `planningDetent` changing while the summary is measured, so the selected detent is not among the offered ones and the sheet falls back; or the summary's measured height including the scrolled content.
- Fix at the cause; the decision and its reason go here once known.

### Room below the grabber
- The panel's search content gets top padding below the grabber, as the waypoint content has, at every text size.

## Risks / Trade-offs

- **[A fix in the detent logic changes the sheet at other sizes]** → Check the default size too: smallest, medium, large, opening and closing the search, and navigating.
