# Tasks

## 1. The scenario check

- [x] 1.1 `tools/scenario-check` (Kotlin JVM, ktfmt via Spotless like the other modules) reading the specs, the test sources and `openspec/checked-by-hand.md`, with `ScenarioCheckTest` covering `spec-checks` "Test named after the scenario", "Test marked with the scenario", "Short scenario name", "Scenario name in two requirements", "Scenario without a test", "Scenario checked on the devices", "Entry without a way of checking", "Scenario renamed", "Scenario tested later" and "Change with a scenario still untested" on specs, tests and lists written by the tests. `./gradlew :tools:scenario-check:test` passes.
- [x] 1.2 The task `checkScenarios` with `-Pchange=<name>`, and the root `check` depending on it. Run once on the repository: it fails listing 363 scenarios: the 342 measured, and 21 of one or two words that only an exact name or a marker covers.

## 2. Sorting the scenarios without a test

- [x] 2.1 For each capability, the scenarios without a test sorted into: covered under another name (marker added to the test after reading it), checked on the devices or by a workflow (entry in `checked-by-hand.md` with how), testable logic without a test (collected). The counts per group reported to the maintainer before 2.2 if the third group has more than about 20.
- [x] 2.2 Tests for the testable logic of 2.1, in the test class of the code they check, named after the scenario. `./gradlew check` passes, including `checkScenarios`.

## 3. Specs workflow and documentation

- [x] 3.1 `.github/workflows/specs.yml` with `openspec validate --all` (pinned 1.17.0) and `checkScenarios`; actionlint passes (Build's workflows job). `spec-checks` "Commit changing only a spec" and "Valid specs" listed in `checked-by-hand.md` as checked by a run on GitHub.
- [x] 3.2 `CLAUDE.md`, `CONTRIBUTING.md` where it speaks of tests, and the tasks rule in `openspec/config.yaml`: how a test names or marks its scenario, the list, and `checkScenarios -Pchange=<name>` before archiving.

## 4. Proof

- [ ] 4.1 Locally: a scenario added to a spec without a test fails `./gradlew check` naming it, and passes once listed; `openspec validate --all` passes. After the push: the Specs workflow passes on main, and a pull request changing only a spec into an invalid one fails it (closed without merging). `manual-checks.md` with the results. No CHANGELOG entry.

## Workflow follow-up

- Archive, which adds `spec-checks` to `openspec/specs`; its scenarios are then checked by the check itself.
