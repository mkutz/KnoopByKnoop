## Context

`PathDataNote` and `BaseMapNote` (Android `PlannerScreen`, iOS `PlannerView`) are text lines below `RouteActions`, shown when `pathData` or `baseMap` is not `null`. `pathData` is `null` on hiking routes. Both arrive a little after the route is complete, so the panel grows by one or two lines, and the sheet's height jumps. The action row already holds Back to start (with its label), Export GPX, Clear and, since `add-undo-button`, Undo, in one row that must fit 360 dp.

## Goals / Non-Goals

**Goals:** the same three states for both downloads, at a glance, without the sheet changing height; the same words for screen readers.

**Non-Goals:** a percentage or progress amount; changing what is downloaded or when; the info sheet's sizes.

## Decisions

- **Placement: trailing end of the action row**, pushed right by a spacer, map first, then ways. The row is laid out whenever a route is complete, even with no action, so the symbols never add a line.
- **One status chip per download:** the subject symbol (Android: a map and a network drawable; iOS: `map` and `point.3.connected.trianglepath.dotted`) in the secondary text colour, with a small status mark at its bottom-right corner: a circular progress indicator while loading, a green checkmark when ready, an amber exclamation mark when unavailable. Each chip is 32 dp/pt wide, so both take about 72 dp with spacing.
- **Fitting 360 dp:** Back to start (about 150 dp), up to four 40 dp icon buttons and the two chips do not fit in 328 dp. When the row is too narrow, Back to start shows its icon only, still named "Back to start" to screen readers and on a long press; this is measured on the emulator at 360 dp. Alternative: wrap the chips onto a second line, which brings the jump back.
- **Words:** each chip's accessibility label is today's sentence, and it is announced again when the status changes (a live region / `.accessibilityValue`). A tap shows the sentence as a tooltip (Android `PlainTooltip`) or a popover (iOS), so sighted riders can still read it.
- **Status changes without motion:** the progress ring is replaced by the checkmark with a short fade, none with reduce motion.

## Risks / Trade-offs

- Back to start losing its label on narrow phones makes it less obvious. → Only when it doesn't fit; the icon and long press stay.
- A small mark at the corner might be too small to read in sunlight. → Mark at least 12 dp with a contrasting ring, checked on the phones outdoors.
- Riders may not know what the network symbol stands for. → Tap shows the sentence; screen readers read it.
