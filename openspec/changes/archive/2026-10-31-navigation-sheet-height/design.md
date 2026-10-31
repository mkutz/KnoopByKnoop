# Design

## Context

In portrait, both apps show the panel as a sheet with heights chosen for planning:

- Android: `BottomSheetScaffold(sheetPeekHeight = 150.dp)` in `PlannerScreen.kt`, with the default scaffold state, which is partly expanded or expanded.
- iOS: `.presentationDetents([smallestDetent, .medium, .large], selection: $detent)` with `smallestDetent = .height(170)` in `PlannerView.swift`.

While navigating, the top of `WaypointSheet` is the navigation part: `NavigationHeader` (the number to follow, the distance to the next knot, Stop) and the progress strip with its labels. That part is taller than 150 dp / 170 pt once the labels show distances and arrivals. Nothing in either app changes the sheet when navigation starts or ends.

## Goals / Non-Goals

**Goals:**
- The sheet while navigating is exactly as tall as the navigation part and cannot be dragged, on both platforms.
- The sheet moves to that height when navigation starts and goes back to the planning height when it ends.

**Non-Goals:**
- Changing what the sheet shows above its smallest height while navigating (see proposal, Out of Scope).
- Changing the landscape side panel.

## Decisions

**Measure the navigation part instead of using a fixed height.** The height depends on the font scale, on whether the strip has labels, and on the header's state ("Waiting for your position…", off route, "At 12 follow 45"). Each app measures the navigation part as laid out (Android `onSizeChanged`/`onGloballyPositioned`, iOS `onGeometryChange`) and adds the navigation bar and a small margin. A fixed bigger constant would still cut off text at large font scales, and it would waste map space without labels.
- Alternative: a fixed 220 dp / pt. Simpler, but it breaks with accessibility font sizes.

**The height follows the measurement while navigating.** If the header changes height, for example off route, the smallest height changes with it. This happens rarely and only on real state changes, so the sheet does not jitter. To avoid a jump of a few points on every relayout, the measurement is rounded up to whole dp / pt.

**A fixed sheet while navigating.** While navigating, the sheet holds only the navigation part, so dragging it up showed nothing (Android, where a sheet expands only to its content) or empty space (iOS, with `.medium` and `.large`). So the sheet is fixed while navigating and shows no drag handle; the change that adds the coming knots above the strip brings dragging back with them.
- Alternative: draggable to half the screen on both. Rejected, since it only shows empty space for now.

**Android: peek height, no swiping, plus `partialExpand()`.** `sheetPeekHeight` becomes the measured height plus the navigation bar and a small margin while navigating, and 150 dp otherwise. While navigating, `sheetSwipeEnabled` is false and `sheetDragHandle` is null. A `LaunchedEffect(state.navigating)` calls `bottomSheetState.partialExpand()` on start and on end, so an expanded sheet comes down to the new peek height.

**iOS: a single detent.** While navigating, the detents are `[.height(navigationHeight)]` with the drag indicator hidden, and `detent` is set to that detent on start. On end, `detent` goes back to `smallestDetent` and the planning detents return. The selection is set in the same update as the detent set, so SwiftUI never selects a detent that is not in the set (it would ignore it). `.presentationBackgroundInteraction` is enabled up through the navigation detent while navigating and up through `.medium` otherwise: with `.medium` alone, which is not a detent while navigating, the map was dimmed and ignored touches (found on the iPhone). The place search above the sheet keeps following `sheetHeight`.

**Before the first measurement.** For the first frame after start, the measurement may still be missing. The apps then use the planning height and switch as soon as the measurement arrives, which is within the same frame or the next one.

## Risks / Trade-offs

- [The navigation sheet takes more of the map in portrait than the planning sheet] → It is only as tall as the text that has to be read; the map keeps the rest.
- [iOS: changing the detent set while presenting can animate oddly] → Checked on the iPhone; if needed, change the set first and the selection in the next run loop.
- [Android: `partialExpand()` while the user drags the sheet] → Only called when `navigating` changes, not continuously.
