# Tasks

## 1. Core

- [x] 1.1 `RouteProgress`: the cheapest point of the leg's course after `place`, with the cost and the 100 m margin over the nearest point of the design. `Navigation.wayToRoute` keeps its target while off route and replaces it only for 100 m more. Tests in `NavigationTest`: "Walking on to where the route goes", "Cutting a loop short", "Steady target", and the existing "Line to the route", "Route behind the rider" and "Line to the knot" unchanged. `./gradlew :modules:core:jvmTest` passes.

## 2. Apps

- [x] 2.1 Mocked rides on the emulator and in the simulator, built from the installed courses: leave a leg that turns and walk on in its direction; the line and the guided way lead ahead, and stay put from fix to fix. Screenshots in `screenshots/`.

## 3. Release

- [ ] 3.1 Changelog line, and the maintainer's walk home → gym again, leaving the route at the Sieg as on 2026-11-09.

## Notes

- 1.1, 2026-11-09: the three new tests fail with the code from before the change and pass with it. At first two of them passed with the old code too: in "walking on" the southward part was nearer than the river, and in "cutting a loop short" the place on the route had already moved to the loop's end while off route was being confirmed. Both now start where the old rule leads back.
- 2.1, 2026-11-09, simulator: on the leg 06 → 02, left the course near the Sieg in Siegburg and walked 170 m south, as on the maintainer's walk; the spot was searched in the installed courses as one where the old rule leads back and the new one about 500 m ahead, past the bend. The guided way led west and up to the river path about 80 m ahead of where the route was left, not straight back north, and stayed the same 20 s later ([screenshot](screenshots/ios-off-route-south-of-the-sieg.png)). With path data, though, the screen can't show the target itself: the drawn way ends where it meets the route, and with no path north through the buildings a way to the old target would likely use the same streets. The target is covered by the unit tests; whether it helps is the maintainer's walk (3.1). The emulator was not run for this, as the rule lives in the shared core.
