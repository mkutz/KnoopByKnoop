# Design

## Context

- Android, `ProgressPanel.kt`: `LABEL_WIDTH = 64.dp`, `LABEL_HEIGHT = 40.dp`; the strip's `BoxWithConstraints` is `STRIP_HEIGHT + LABEL_HEIGHT` high. The labels use `bodyMedium` (sp), so they grow with the font scale. `shown(badges, lineWidth, LABEL_WIDTH)` leaves out labels that would overlap, by rank.
- iOS, `ProgressPanel.swift`: `labelWidth = 70`, `labelHeight = 40`, with `.callout` text, which follows Dynamic Type; the same `shown` ranking.
- Android, `KnotBadges.kt`: `KnotBadge` is 28 or 34 dp (times `scale`), its number 10 or 12 sp. iOS badges are drawn with fixed point sizes and are not affected.
- The navigation panel's height is measured since `navigation-sheet-height`, so a higher strip makes the sheet higher.

## Goals / Non-Goals

**Goals:**
- Label room that grows exactly as much as the label text.
- Unchanged look at the default text size.

**Non-Goals:**
- Measuring each label's text: the room for the widest label ("280 km" over "04:30") is enough, as now.

## Decisions

**Scale the label room by the text scale.** Android multiplies `LABEL_WIDTH` and `LABEL_HEIGHT` by `LocalDensity.current.fontScale`; iOS uses `@ScaledMetric(relativeTo: .callout)` for `labelWidth` and `labelHeight`. The overlap ranking already works on `LABEL_WIDTH`, so passing the scaled width leaves out more labels at large sizes, by the existing rank.
- Alternative: measure the labels' text. More precise, but the fixed room for the widest label has worked so far, and scaling it keeps the overlap rule simple.

**Badge numbers in dp on Android.** `KnotBadge` converts its font size from dp: `with(LocalDensity.current) { size.dp.toSp() }`, so the font scale does not apply, as for the map's bitmaps and the iOS badges.
- Alternative: grow the badges with the text. Would push the header and the badge rows apart and differ from the map; the numbers are symbols the rider matches with the signposts.

**Headers: one line where it fits, two where not.** Android: the number to follow and the distance are in a `FlowRow`, so the distance wraps below the badges; the planning header measures whether the summary, with its distance and time not wrapping, fits next to the actions, and puts the actions below it if not (a small custom `Layout`). Only the route summary of a complete route takes part: the instructions without a route ("Tap a knot to set the start") keep wrapping next to the actions, as now. iOS: `ViewThatFits` with the one-line layout first and the two-line one second, for the same parts; the `minimumScaleFactor` stays for the last bit.
- Alternative: let Stop and the actions shrink to icons. Would make the most important button smaller just when the rider needs large text.

## Risks / Trade-offs

- [At the largest text sizes, fewer labels fit on the strip] → The ranking keeps the end and warned ferries; the others come back as the rider gets closer, or at a smaller text size.
- [A badge number no longer grows for riders who need large text] → It stays at the size of the badges on the map, which riders already read; the labels and the header text do grow.
