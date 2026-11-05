# Proposal

## Why

The maintainer's note on the test ride of 2026-11-01 (Siegburg towards Bonn, Pixel 7, video 1 at 4:55): "Double route not ideal. The route took the same path there and back with a fork. That was confusing at that fork."

The route in the video is a round trip from 02 to 02, and it uses the stretch along the L 143 by knot 28 in both directions. Both directions are drawn on top of each other with arrows both ways, and every part of the route looks the same. So at the fork the map does not tell which way to go now; only the panel does.

## What Changes

- **Side by side:** the route is drawn to the right of its way in riding direction, so a stretch used in both directions shows as two lines next to each other, each with its own arrows. This holds while planning and while navigating.
- **The leg ahead stands out while navigating:** the course to the next knot is thick and has the arrows. The rest of the route is thin and has none.
- **The ridden track** follows the route's side, so it stays on top of the route, as `show-rider-on-top` wants.

## Out of Scope

- **Avoiding routes that use a stretch twice:** routing does not change.
- **Telling the rider about shared stretches** in notices.
- **The other ride notes:** the language switch during navigation. The badges like the real signs are their own idea.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`: a new requirement "Route used in both directions".
- `knot-navigation`: a new requirement "Leg ahead on the map".

## Impact

- **Core:** `Navigation` gives the course of the leg ahead, from the same courses the map draws, and `MapFeatures` the layer. Tests are in `NavigationTest` and `PlanningTest`.
- **Android:** `NetworkMap.kt`: the offsets of the route, its arrows and the track; a layer for the leg ahead; the route thinner and without arrows while navigating.
- **iOS:** the same in `NetworkMapView.swift`.
- **Archive order:** after `routes-through-signposts` and `show-rider-on-top`, whose courses and track this builds on.
