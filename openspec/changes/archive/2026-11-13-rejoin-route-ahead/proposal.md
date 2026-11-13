# Proposal

## Why

On a walk from home to the gym on 2026-11-09, the maintainer left the route at the Sieg in Siegburg and walked south-west. The guidance sent them north-east, back to the river path where they had left the route, although walking on west would have met the route at the station just as soon, without going back: "The right turn away from the route seems unnecessary." A minute later it switched to the way west. On the ride of 2026-11-08 the same happened at a loop in Siegburg the maintainer cut short: the guidance first pointed back into the loop, and only later forward.

Both are as specified: "Way to the route on the map" leads to the nearest point of the leg's course ahead of where the rider left it, however much of the route lies between that point and the next knot.

## What Changes

- Off route, the line back to the route, and the guided way along paths, lead to the point of the leg's course ahead of where the rider left it where the way back plus the route from there to the next knot is shortest, instead of the nearest point.
- The nearest point stays the target unless another one saves at least 100 m, so a rider beside a straight course is still led straight back.
- Once the line leads ahead, it keeps doing so until that saves less than 50 m, so it doesn't jump between the nearest point and one ahead from fix to fix.

## Out of Scope

- **Rejoining beyond the next knot:** that would skip a knot of the route; "Skipping ahead" covers riding past later knots.
- **The guided way's look on the map:** the white line in dark mode and the line drawn while on the route are a change of their own.
- **When off route is announced:** unchanged, see `confirm-off-route`.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: "Way to the route on the map" leads to where the way back is shortest, not to the nearest point.

## Impact

- **Core:** `RouteProgress` gets the shortest-rejoin search on the leg's course; `Navigation.wayToRoute` uses it and keeps the target with the margin. `WayGuide` and the apps take the target as before.
- **Apps:** none in code; checked with mocked rides.
