# Manual checks

## How it was checked

Checked on 2026-11-10:

- **First run on the repository** (task 1.2): 363 of 788 scenarios without a test or an entry. 342 had no test by name or `// Scenario:` comment; 21 more have names of one or two words, which only an exact test name or a marker covers now.
- **Sorting** (task 2.1, five read-only passes over the specs and the tests, one per group of capabilities, then spot checks of their claims against the tests):

  | | scenarios |
  |---|---|
  | covered by a test under another name: marker added | 82 |
  | checked on the devices or by a workflow run: listed | 276 |
  | logic no test checked: new test | 5 |

  The new tests: `place-search` "German app" (`PlaceSearchTest`), `network-data` "Apps without the positions of merged knots" (`GraphFileTest`), `knot-navigation` "Closed ferry before a stop" and "Open ferry before a stop" (`RouteProgressTest`, the ranking the strips use where marks overlap), and "Header with a screen reader" (`WayGuideTest`, the whole announcement the spec gives).
  `map-planning` "Nothing leaves the device" was a candidate for a test, but which squares are requested is wired in the apps; it is listed as checked on the devices by watching the requests.
  The 276 listed scenarios take 262 lines, as a line covers every scenario of its name in a capability.
- **Shared names:** "Screen off", "Looking around" and "Zoomed out" are each tested in one requirement and only checked on the devices in others; a plain marker would have counted the test for all 6 others. Decided with the maintainer: markers and list entries may name the requirement (`Turn notices on the guided way / Screen off`), added to the spec as "Scenario name in two requirements".
- **Locally** (task 4.1): with a scenario "Knot in Zeeuws-Vlaanderen" added to `network-data` without a test, `./gradlew check` failed with `No test and not listed as checked otherwise: network-data / Sign region of each knot / Knot in Zeeuws-Vlaanderen`; with a line for it in the list, the check passed; `openspec validate --all` passed (22 items). The spec and the list were restored.
- **Builds:** `./gradlew check` (with the Android build and `checkScenarios`) and the core's tests on the iOS simulator pass; the three shell test scripts pass with their markers; actionlint passes on `specs.yml`.
- **On GitHub:** the Specs workflow passed on main for `10c8907` (run 42852229476). Pull request #7, a draft changing only `openspec/specs/knot-navigation/spec.md` by adding a requirement without a scenario, failed it with `requirements.44.scenarios: Requirement must have at least one scenario` (run 42852522649), while Build did not run for it; closed without merging and its branch deleted.
- **Unrelated:** the Build of `cff4f3e` shows as failed because GitHub cancelled its iOS job after 15 minutes without a macOS runner ("capacity constraints"); its other jobs passed.
- **Found while sorting, not changed here:** `knot-navigation` "Off route while following" says the map shows the rider and the next knot, while "Way to the route on the map" and the map focus use the end of the line to the route.
