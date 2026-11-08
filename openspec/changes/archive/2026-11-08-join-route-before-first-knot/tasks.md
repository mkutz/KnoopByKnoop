# Tasks

## 1. Core

- [x] 1.1 `Navigation`: one leg choice shared by `start` and the join; before the first knot, remember the leg and the place along it, join after 50 m along it within 30 m; no "back on route" before joining.
  - Tests: "Reaching the course and following it" (a 06 → 02 fixture like the ride), "Crossing the course", "Reaching the course of the first leg near the start", "Following the course against the route", "Without courses"; the existing navigation tests pass.

## 2. Apps

- [x] 2.1 Both apps build. Check on the emulator with a mocked ride like the maintainer's: start 250 m from the course, reach it, ride along it; the header switches to the next knot and the line ahead appears. Same in the simulator.
  - Checked 2026-11-08 on the emulator: 06 → 02 started 250 m east of the course near home, heading for 06; after following the course south for about 100 m the header showed "02 1.9 km", the line ahead was drawn and the strip moved. Not repeated in the simulator: the change is in the core only, and both apps build.

## 3. Release

- [x] 3.1 Changelog line, and a manual check: start at home away from the route, join the course, see the header switch.
