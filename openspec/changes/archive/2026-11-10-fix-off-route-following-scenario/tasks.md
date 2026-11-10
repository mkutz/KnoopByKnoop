# Tasks

## 1. Specs

- [x] 1.1 The THEN of "Off route while following" in the "Following the rider" delta of `add-dynamic-navigation-zoom` worded as in this change. `openspec validate --all` passes, and `./gradlew checkScenarios -Pchange=fix-off-route-following-scenario` passes (the scenario keeps its name and its line in `openspec/checked-by-hand.md`). For `-Pchange=add-dynamic-navigation-zoom` the scenario passes too; that change still lacks a test or a line for 5 scenarios of its own ("Recentering near a knot", "No automatic zoom after looking around", "Next knot far ahead", "Zooming in at a knot", "Labels while zoomed in"), which it needs before its archive, not part of this change.

## Workflow follow-up

- Archive, which writes the new wording into `openspec/specs/knot-navigation/spec.md`; `./gradlew checkScenarios` passes after it.
