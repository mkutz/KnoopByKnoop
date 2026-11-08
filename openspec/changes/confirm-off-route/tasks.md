# Tasks

## 1. Core

- [ ] 1.1 `Navigation.update` ignores fixes not newer than `lastFix`; off route by distance needs a fix of 20 m or better, less accurate ones only set `confirming`. Tests: "A coarse position far from the course", "Leaving the route for real", "The same position delivered again", "Passing a knot not on the route"; and a replay of alternating old fixes that does not flip the precision. The existing navigation tests pass.

## 2. Apps

- [ ] 2.1 Both apps build. On the emulator, a mocked ride with a single 40 m fix 200 m off the course: no notice. In the simulator the same, and the log shows no precision bursts.

## 3. Release

- [ ] 3.1 Changelog line, and a manual check: a ride with the screen off on the iPhone, then its log read for false off route alerts and precision bursts.
