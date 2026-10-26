# Tasks

## 1. Network graph model

- [x] 1.1 Add an immutable in-memory network graph (knots plus routes between knots with their length in meters) to `modules/core`
- [x] 1.2 Add a test fixture graph small enough to reason about by hand

## 2. Routing

- [x] 2.1 Implement shortest-path search between two knots
- [x] 2.2 Report unreachable legs explicitly
- [x] 2.3 Tests: "Start and end only", "Route through a stop", "Unreachable waypoint"

## 3. Editable waypoint route

- [x] 3.1 Add the waypoint route model with insert at position, insert into best leg, move and remove
- [x] 3.2 Recompute only the legs next to a change
- [x] 3.3 Tests: every scenario of "Insert a stop", "Reorder and remove stops", "Round trips" and "Route summary"

## 4. Wrap-up

- [x] 4.1 `./gradlew check` passes, including `iosSimulatorArm64Test` in CI
