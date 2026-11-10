# Tasks

## 1. Record the rides first

- [x] 1.1 `NavigationReplayTest` with the rides of the design, their expected texts written by the current code and committed; a coverage report of `Navigation.kt` (Kover or the IDE's) read, and rides added for branches of `update()` none of them reach. `./gradlew :modules:core:jvmTest` passes.

## 2. State and setup

- [x] 2.1 `RideSetup` and `RideState`, `Navigation(setup, state)` with its public properties reading through, `with { }` in place of `copy(...)` with the `joinedRoute` rule in it, `start()` and `inLanguage()` building the two. The recorded rides and `NavigationTest` unchanged.

## 3. Steps of `update()`

- [x] 3.1 `backOnRoute` and `headingFor` in place of the repeated changes. Rides unchanged.
- [ ] 3.2 `update()` as the steps of the design, each a function of the navigation before it, `confirming` part of a step's result. Rides unchanged.

## 4. Proof

- [ ] 4.1 The real-data replay as after `share-line-geometry` (908 lines, identical); `./gradlew check`, the core on the iOS simulator and the iOS app build pass; the coverage report read again. `manual-checks.md` with the results. No CHANGELOG entry.
