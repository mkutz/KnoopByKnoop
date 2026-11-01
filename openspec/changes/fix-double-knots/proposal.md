# Proposal

## Why

On a test ride (2026-11-01, Pixel 7) started away from the route, the panel showed "0 m" to the first knot and the rider's dot stayed at the start. After starting off route, the rider counts as back on route within 100 m of the first leg's course while the first knot is still ahead, and along the route nothing lies before the first knot, so the distance to it reads "0 m". (That the first knot, Siegburg 02, was never counted as passed has its own cause, a knot tagged on two nodes, see `pass-merged-knots`.)

Looking into it showed a second way navigation gets stuck: at a double knot, two route knots a few metres apart such as 01 and 03 at the Den Helder ferry, one location fix comes within 30 m of both. The app counts the first as passed, announces the second as the next knot, and then never counts the second as passed: a knot only counts when the rider *enters* its circle, and they were already inside. Until a later knot is passed, the panel shows "0 m" and the dot stays on the knot. A core test reproduces it: on the route 24, 45, 46, 12 with 46 10 m after 45, 46 stays the next knot.

## What Changes

- A fix within 30 m of several consecutive route knots counts all of them as passed. The knot after the last of them becomes the next knot, and one notice names it, not one notice per knot.
- Starting navigation within 30 m of several consecutive route knots begins after the last of them.
- While the first knot of the route is the next knot and the rider is on route, the distance to it is the straight distance, in the app and outside it, not "0 m".

## Out of Scope

- A larger pass radius, or other changes to when a single knot counts as passed.
- Merging double knots into one knot in the pipeline: they are separate signposts with different numbers.
- Where the dot is on the progress strip before the first knot: it stays at the start.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: "Passing a knot" and "Start navigation" get scenarios for double knots; "Number to follow" gets a scenario for the distance to the first knot.

## Impact

- Core: `Navigation.update` and `Navigation.start` (which knots count as passed), `Navigation.progress` and `RouteProgress.view` and `line` (the distance to the first knot). Tests in `NavigationTest` and `RouteProgressTest`.
- Apps: none; they show what the core gives them.
