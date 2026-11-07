# Proposal

## Why

On the maintainer's walk through Siegburg on 2026-11-06, the ridden track on the Pixel turned into a tangle around the Markt and a roundabout: spikes out and back across buildings, loops where the route was walked once. The walk was mostly with the display off.

The track takes every fix accurate to 50 m as soon as it is 5 m from the last point. With the display off and far from the next knot, navigation samples rarely and mostly without GPS, so fixes scatter by tens of meters. At walking pace almost every scattered fix is more than 5 m from the last point and becomes a point. Navigation itself was not affected; the track just looks broken.

## What Changes

- A fix becomes a point of the track only when it is farther from the last point than the inaccuracies of both together, and at least 5 m, so scatter does not draw lines while the rider stands or moves slowly.
- A fix that would mean an impossible speed since the last point is left out: faster than 15 m/s on a cycling route, 4 m/s on a hiking route.
- A point the next fix comes straight back from, a spike out and back, is taken out of the track.

## Out of Scope

- **Navigation and the notices,** which already treat fixes on their own.
- **How often the location is sampled.** "Screen off" keeps saying it is not sampled more often.
- **Smoothing the line** beyond leaving points out, e.g. averaging.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: "Ridden track" says scatter and spikes are not part of the track, with scenarios for them.

## Impact

- **Core:** `RiddenTrack.add`, and `RiddenTrack` learns the network for the speed limit.
- **Apps:** one line each where the track is started (`NavigationService`, `Navigator`).
- **Riding direction:** it comes from the same points, so with fewer points it may show the dot a little more often while walking slowly in poor reception; see the design.
