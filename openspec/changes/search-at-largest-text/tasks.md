# Tasks

## 1. iOS

- [x] 1.1 Reproduce at the largest text size in the simulator with notes on detent changes, `openSearch` and `closeSearch`; record the cause in design.md.
- [x] 1.2 Fix it: the first tap opens the search, the field shows completely below the grabber, and Cancel brings the sheet back with "15 km" complete ("Opening at the largest text size on iOS", "Closing at the largest text size on iOS"). Check the default size again: the sheet's heights, opening and closing the search, navigating. Screenshots of both sizes.

## 2. Android and release

- [x] 2.1 On the emulator with the largest font size: "Opening at the largest text size on Android".
- [ ] 2.2 Changelog line, and a check on the maintainer's iPhone at the largest text size.

## Notes

- 1.2, simulator, 2026-11-09, Accessibility XXXL: after a fresh start, one tap on the magnifier opens the search; field, text and Cancel complete below the grabber; Cancel brings back "15 km" and "about 58 min" complete; pulled lower, the sheet now loses its bottom instead of its top. Default size: the sheet's heights, opening, typing and picking, landscape and navigating look as before.
- 2.1, emulator, 2026-11-09, font size 2.0: one tap on the magnifier opens the search, field and Cancel complete; back brings the sheet back with the summary complete. Font size reset to 1.0.
