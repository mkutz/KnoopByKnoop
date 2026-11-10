# Proposal

## Why

On the maintainer's ride of 2026-11-08 the last leg ran from knot 02 to the end flag at home, about 2.5 km and 10 minutes. The voice said "On to the end" at 02 and then nothing until "The end ahead" (log of the Pixel, 17:15 to 17:25). The planned way was good: it followed the course from 02 towards 06 and left the network at Cecilienstraße. But the guidance did not use that. As `add-route-points` specified, a way along paths to a route point gets only the distance and direction, "with no directions at each turn".

The maintainer wants the guidance to say what the rider sees: carry on along the network towards 06, turn into Cecilienstraße where the way leaves it, and then turn by turn to the flag; and the same in reverse from the start flag onto the network.

## What Changes

- While the way to a route point follows a course of the network, the notices name the knot that course leads to: "Next 02, then towards 06", and after 02 "Towards 06, then the end".
- Where the way leaves the network, a turn notice with a reminder before it, like at a knot: "In 150 m turn right into Cecilienstraße", "Turn right now into Cecilienstraße".
- From there to the route point, turn notices as on the guided way back to the route, found on the device from the path data.
- The same from a route point onto the network, e.g. from the start flag at home: turn by turn along the way to where it joins the network, and there "Turn left onto the route towards 02".
- The planned route does not change.
- **Found on the maintainer's starts of 2026-11-10** (iPhone and Pixel, a route of a start and an end flag): right after starting beside the start flag, the guided way led 1.3 km east instead of along the planned way south (1.14 km), and a turn at the very start of a guided way was not spoken. So:
  - a way found from a position less precise than 20 m is searched again with the next position of 20 m or better, to a route point as off route; the first position at the start was about 30 m off, and the way found from there stuck (decided with the maintainer: one logic for both);
  - a way starting more than 45° to the side of the riding direction is told at once, e.g. "Turn left now".

## Out of Scope

- **Straight lines**, e.g. on hiking routes: they cannot be followed, and keep the distance and direction.
- **Where the way leaves the network:** planning stays as it is.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: new requirements "Guidance to a route point off the network", "Guidance from a route point onto the network" and "Way searched again from a precise position"; "Turn notices on the guided way" tells a turn at the start of a way.

## Impact

- **Core:** `Navigation` (the part of the way along the course, the turn-off point, the notices), `WayGuide` (a way search towards the route point from the turn-off on), the texts in all four languages.
- **Apps:** none expected; both speak and show the core's notices and the guided way.
