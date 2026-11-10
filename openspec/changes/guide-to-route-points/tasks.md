# Tasks

## 1. Core

- [x] 1.1 `Navigation`: the knot ahead on the course for a leg to a route point with a way along paths, and the notices naming it ("then towards 06", "Towards 06, then the end"), in all four languages. Test "Towards the next knot".
- [x] 1.2 The turn-off: its place, direction and street; a reminder and a turn notice. Tests "Leaving the network", "Without path data".
- [x] 1.3 `WayGuide`: from the turn-off on, a way search to the route point and its turns, as on the guided way. Tests "Turn by turn to the flag", "Missing the turn-off", "A straight line".
- [x] 1.4 From a route point onto the network: the way search from the point to its place on the course, its turns, and the join notice with the knot ahead. Test "From the start flag onto the network".

- [x] 1.5 `WayGuide`: a way found from a position less precise than 20 m is searched again with the next position of 20 m or better. Tests "Starting beside the start flag" and "Precise from the start".
- [ ] 1.6 `GuidedWay`: a way starting more than 45° to the side of the riding direction tells that turn at once. Test "Turn at the start of the way".
- [ ] 1.7 Ride scripts can place route points as planning does (`point <latitude> <longitude>` with its way from `RoutePointWays`), and a recorded real-data ride starts beside a route point off the network, away from the maintainer's home area: the guided way follows the planned way. `./gradlew check` passes, `checkScenarios -Pchange=guide-to-route-points` too.

## 2. Apps

- [x] 2.1 Both apps build. On the emulator, a mocked ride home → 28 → 02 → home like the maintainer's: the notices towards 06, the turn into Cecilienstraße, and the turns to the flag, spoken and in the header. Same in the simulator.

- [ ] 2.2 Both apps build; on the emulator, starting beside a start flag off the network with a first position 30 m off at 30 m accuracy: the guided way follows the planned way once a precise position comes.

## 3. Release

- [ ] 3.1 Changelog line, and a manual check: start at home, ride onto the network, and ride home from 02; listen.
