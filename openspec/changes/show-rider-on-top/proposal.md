# Proposal

## Why

The maintainer's notes on the test rides of 2026-11-01: the ridden track is drawn below the route, so where the rider followed the route it is hidden, and the ridden part of the route cannot be told from the part ahead; and the rider's arrow is small for a glance at the handlebar. The track was put below the route on purpose (`improve-navigation-map`), so the route stays visible where both overlap; on top and narrower than the route, it keeps the route visible on both sides.

## What Changes

- The ridden track is drawn on top of the planned route and its direction arrows, narrower than the route and opaque, so the ridden part of the route shows blue with orange edges, and the part ahead stays orange with its arrows.
- The rider's arrow and dot get bigger: the arrow about 44 points (dp) long, the dot 30 across with its white outline, on both platforms.

## Out of Scope

- The other ride notes (out-and-back legs with a fork, which number is most prominent, the line back to the route, back on route on a parallel path, arrivals at the current speed, the lock-screen notification).
- The track's color, and the position shown on request while planning (`my-position`).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: "Ridden track" says where the track is drawn; "Riding direction on the map" gives the arrow's and the dot's size.

## Impact

- Android: `NetworkMap.kt` (layer order, track and dot properties, arrow size).
- iOS: `NetworkMapView.swift` (layer order, track and dot properties), `KnotBadges.swift` (the arrow image).
- Core: none.
