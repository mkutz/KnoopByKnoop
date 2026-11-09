# Tasks

## 1. Core

- [x] 1.1 `RouteProgress`: the cheapest point of the leg's course after `place`, with the cost and the 100 m margin over the nearest point of the design. `Navigation.wayToRoute` keeps its target while off route and replaces it only for 100 m more. Tests in `NavigationTest`: "Walking on to where the route goes", "Cutting a loop short", "Steady target", and the existing "Line to the route", "Route behind the rider" and "Line to the knot" unchanged. `./gradlew :modules:core:jvmTest` passes.

## 2. Apps

- [ ] 2.1 Mocked rides on the emulator and in the simulator, built from the installed courses: leave a leg that turns and walk on in its direction; the line and the guided way lead ahead, and stay put from fix to fix. Screenshots in `screenshots/`.

## 3. Release

- [ ] 3.1 Changelog line, and the maintainer's walk home → gym again, leaving the route at the Sieg as on 2026-11-09.

## Notes

- 1.1, 2026-11-09: the three new tests fail with the code from before the change and pass with it. At first two of them passed with the old code too: in "walking on" the southward part was nearer than the river, and in "cutting a loop short" the place on the route had already moved to the loop's end while off route was being confirmed. Both now start where the old rule leads back.
