# Tasks

## 1. Specs

- [ ] 1.1 The THEN of "Off route while following" in the "Following the rider" delta of `add-dynamic-navigation-zoom` worded as in this change. `openspec validate --all` passes, and `./gradlew checkScenarios -Pchange=fix-off-route-following-scenario` and `-Pchange=add-dynamic-navigation-zoom` pass (the scenario keeps its name and its line in `openspec/checked-by-hand.md`).

## Workflow follow-up

- Archive, which writes the new wording into `openspec/specs/knot-navigation/spec.md`; `./gradlew checkScenarios` passes after it.
