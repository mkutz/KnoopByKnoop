# Proposal

## Why

The navigation header is made of sentences ("At 45 follow 12", "Next 83 … then 30", "Abseits der Route, nächster 02 … Nordosten"). Its height and layout vary with the language, and German already needed fixes for wrapping. While riding, the rider needs to see at a glance what to look for on the signs, how far it is, and, off route, which way to go. Numbers, badges, arrows and distances do that in every language. The maintainer chose the layout from mockups on 2026-11-07 (https://claude.ai/artifact/XVeganeauBp3Pe2WGy1qUT, boards "In the app" and "Warnings without amber").

## What Changes

**Wordless header:** the header shows only badges, arrows, distances and street names. Its height no longer depends on the language. Screen readers and spoken notices keep their words.

**One hero row per state:**
- **Close to a knot** (from the reminder): the next knot's badge, an arrow, and the badge to follow there, largest, with the distance to the knot.
- **Between knots:** ↑ and the next knot's badge with the distance, both large, then › and the knot after it, small.
- **Next knot is the end:** the end's badge with its ■ corner symbol and the distance, with no knot after it.
- **Off route, no guided way:** the dark header from `knot-colours-from-signs`, a to-the-route symbol (a small rider arrow joined by a dashed line to the orange route line, as on the map, the arrow pointing away from the route), an arrow towards the place where the rider gets back onto the route, and the straight distance to it. The next knot's badge is shown small only while the rider is on a path of the knot network, since there are no signposts elsewhere. **BREAKING:** the distance and arrow now point to the place where the rider gets back onto the route, not to the next knot. The compass word goes.
- **Off route on a guided way:** the dark header, the turn's arrow, the distance to it, the street name when known, and the distance left along the way.
- **On the way to the route** (after starting away from it, until first on it): like off route, with the same symbol but its arrow pointing towards the route, on a neutral tile, not the dark header. It is not a mistake.
- **Riding the wrong way:** the dark header, a U-turn symbol, the next knot's badge and the distance along the route to it.

**Badges like the signs in the header:**
- The header shows each knot as its signs do. In the Netherlands, the knot the rider is at or arriving at is filled, and the knots to follow are white, as on the Dutch arrow signs. In North Rhine-Westphalia and Belgium all are filled.
- A knot the rider picked themselves keeps its corner symbol. Elsewhere, badges keep the role scheme from `knot-colours-from-signs`.

**Strip labels:** the next knot and the number to follow get their badges on the progress strip too, so every number in the header can be found on the strip.

**Stop:** becomes an icon button of fixed size, announced as "Stop". Its label no longer changes the header's width per language.

## Out of Scope

- The turn direction at a knot. The arrow between the two badges is a plain → until `turn-at-knot`.
- The lock screen, Live Activity, Dynamic Island and Android notification: `sign-like-lock-screen`.
- The progress strip's distances and arrivals. The end arrival already ranks first among them, stop arrivals already give way, and the ferry clock badge comes with `knot-colours-from-signs`.
- Spoken notices and their wording, including the off-route guidance notices, which keep naming the next knot.
- Plate colours and map badges: `knot-colours-from-signs`. This change requires it.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`:
  - "Number to follow": the wordless hero row, the header's sign-like badges and the end.
  - "Direction to the route": the arrow and distance to the place where the rider gets back onto the route, and no compass word.
  - "Guided way in the header and outside the app": the header without words.
  - "Riding the wrong way": the panel scenario.
  - "Progress strip": the next knot and the number to follow labeled with badges. This builds on the version from `knot-colours-from-signs`.
  - "Number to follow at large text sizes": the same height in every language.
  - A new requirement for the header on the way to the route.
- `map-planning`: "Clear actions in the planner panel", for Stop as a fixed-size icon button while navigating.

## Impact

- **Core:**
  - `RouteProgress` / `Navigation`: the header state (close, far, end, off route, guided, on the way, wrong way), the place where the rider gets back onto the route with its distance and bearing, and whether the rider is on a network path.
  - `Texts`: the screen reader texts stay. The header strings the screen no longer shows are kept for speech and the lock screen until `sign-like-lock-screen`.
- **Android:** the header in `PlannerScreen.kt` and `Navigating.kt`.
- **iOS:** the header in `PlannerView.swift`.
- **Depends on** `knot-colours-from-signs` (plates, dark off-route header, corner symbols).
