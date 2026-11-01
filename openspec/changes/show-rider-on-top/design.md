# Design

## Context

- Both apps add the map layers in one place (`NetworkMap.addLayers` on Android, `NetworkMapView.addLayers` on iOS), bottom to top: marker, connections, ridden track, route, unreachable legs, route direction arrows, knots, way to the route, my position, next knot, route knots, rider dot, rider arrow.
- The track is 4 wide at 80 % opacity, the route 8. The rider's dot is radius 9 (Android) and 8 (iOS) with a 3 white outline; the arrow is a 64 px bitmap at 0.9 on Android and a 30 pt image on iOS.

## Goals / Non-Goals

**Goals:**
- The ridden part of the route visible as blue on orange; the same on both platforms.

**Non-Goals:**
- Changing colors or the other layers' order.

## Decisions

**Track above the route and its arrows, below the knots.** The track layer moves to right after the route direction arrows, so the knots, the next-knot ring and the rider stay on top of it. Opacity becomes 1: at 0.8, blue over orange looks brownish. The width stays 4, half the route's, which leaves 2 of orange on each side.
- Alternative: between the route line and its arrows. The arrows would sit on the ridden track, cluttering the part already ridden.
- Alternative: a casing (a white or dark outline) under the track. More layers for little gain; the orange edges already separate them.

**Sizes in points/dp, the same on both platforms.** The dot radius 12 with the 3 outline (30 across); the arrow drawn 44 long: on iOS the image grows to 44 pt, on Android `iconSize` scaled so the bitmap shows 44 dp, as Android MapLibre draws bitmaps at the display's density.
- Alternative: scaling with the zoom. The position matters most at a glance, whatever the zoom.

## Risks / Trade-offs

- [A bigger arrow covers a knot's badge as the rider passes it] → The arrow is drawn on top only for the few seconds at the knot; the badge is the same knot the rider is at, and the panel names it.
- [On a detour that runs right along the route, the track covers the route's center] → The route's edges stay visible, as on any ridden part.
