# Proposal

## Why

Close to a knot, the header shows the number to follow on a sign plate (`sign-like-header`). Its arrow is still a plain →, though the rider has to find which arm of the signpost carries that number. Knowing "right to 12" before reaching the junction tells them which way to look and which way to go. The maintainer listed the direction at the knot as new information they want ("that knot's number and the direction"), and the plate's arrow was reserved for it in the mockups (https://claude.ai/artifact/XVeganeauBp3Pe2WGy1qUT).

## What Changes

- **Turn at the next knot:** the core works out the turn the route makes at the next knot. It compares the route's direction over the 40 m before the knot with its direction over the 40 m after it, along the route's courses and across any knots within those 40 m. It classifies the turn like the guided way's turns: straight, slight left/right, left/right, sharp left/right, or back.
- **On the sign plate:** the plate around the number to follow shows that turn as its arrow (↑, ↖, ←, ↙, ↗, →, ↘ or a U-turn), in the header, the Live Activity and the Dynamic Island.
- **In the Android title:** the turn character goes before the number to follow, e.g. "45 → ↱12".
- **Without a turn:** without courses, at the start knot before the rider has moved, or when the route leaves the knot the way it came in from a different signpost, the arrow stays the plain → of today.

## Out of Scope

- **Spoken notices:** the reminder keeps saying "45 ahead, then 12". A spoken direction can follow in its own change, once the shown arrow has proven right on rides.
- **The guided way's turns,** which already have their own arrows.
- **Lanes or junction pictures:** `add-junction-pictures`.
- **The arrow between the next knot and the plate,** which stays → as "then".

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: a new requirement, "Turn at the next knot", for the plate's arrow wherever the plate is shown and in the Android title.

## Impact

- **Core:**
  - `RouteProgress` / `NetworkGeometry`: the route's direction before and after a knot along the courses.
  - `HeaderRow`: a `turn` on the close row.
  - `notificationTitle`: the turn character.
  - Reuses `TurnDirection` and its angle limits from `PathRouter`.
- **Android:** the plate's arrow in the header and the large icon draws the turn.
- **iOS:** the same in the header, the Live Activity and the island.
- **Depends on** `sign-like-header` (the plate) and `sign-like-lock-screen` (the outside views).
