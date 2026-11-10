# Design

## Context

- 788 scenarios in 9 capabilities. Tests refer to them three ways today: a test name starting with the scenario's name (e.g. `Cycle path beside a main road is preferred when 20 percent longer`), a `// Scenario:` comment above the tests (27, e.g. `GpxTest`, `NavigationSessionTest`, `VerifyTest`), or a `// Requirement:` comment over a group of tests (89), which says nothing about single scenarios. By the first two, 446 scenarios have a test; per capability from `route-planning` 22/22 to `release-delivery` 0/9 and `place-search` 7/36.
- Tests live in `modules/core/src/{commonTest,jvmTest}`, `pipeline/src/test`, `apps/android/src/test` and `.github/scripts/*-test.sh`; iOS has no test target.
- Build ignores `**.md`, and the release workflow relies on that: it accepts later commits that change only Markdown after the last green Build.
- `openspec validate --all` passes; `--strict` fails on warnings only.

## Goals / Non-Goals

**Goals:**
- A missing test for a new or changed scenario fails locally in `./gradlew check` and on GitHub, with the scenario named.
- The scenarios not tested are visible in one place, each with how it is checked instead.

**Non-Goals:**
- Proving a test really checks its scenario; the check ties names, review judges content.
- Running the scenario check on the iOS app's code, which has no tests.

## Decisions

### The check as a small Kotlin module

`tools/scenario-check`, a Kotlin JVM application with its own tests, and a task `checkScenarios` that runs it over the repository; the root `check` depends on it. Its inputs are the specs, the test sources and the list, so Gradle skips it when none changed.

*Alternatives:* a shell script beside the release scripts: parsing Markdown and Kotlin in `sh` is brittle, and it would not run in `./gradlew check`. A Python script: a new language in the repository for one tool. A test in the pipeline module: the pipeline is about OpenStreetMap, not the repository.

### What counts as a test

- Test names: Kotlin's backticked names in test source sets, and, for the shell scripts, `# Scenario:` comments only (their checks have no names).
- Matching ignores case and anything but letters and digits. A test counts when its name is the scenario's name or starts with it followed by a word; for names of one or two words ("Ready", "Nothing new") only the exact name or a marker counts, so unrelated tests do not cover them by chance.
- A marker is a comment `Scenario: <name>` in a test file, as `GpxTest` already does; it covers that scenario name wherever it occurs.

*Alternative:* qualify every marker with the capability (`// Scenario: network-sync "Updated app"`). It would tell apart the 37 names used more than once, but needs every existing marker and every test name changed; left for when a duplicate actually hides a gap.

### `openspec/checked-by-hand.md`

```markdown
# Scenarios checked otherwise

## network-sync

- Readable info button — on the devices
- Storage limit — on the devices, by filling the limit with a throwaway build
```

One section per capability, one line per scenario name, `—` before how it is checked. The check fails on a line without a way, on a scenario not in that capability, and on a scenario that has a test. The file is beside the specs, not in them, so the specs stay what the apps do. The scenarios of `spec-checks` that need a run on GitHub are listed there too.

### A change's delta

`./gradlew checkScenarios -Pchange=<name>` checks the scenarios under `ADDED` and `MODIFIED` in `openspec/changes/<name>/specs/`, against the same tests and list. The apply workflow runs it before marking the test tasks done; CI checks only `openspec/specs`, as active changes are proposals whose tests may not exist yet.

### The Specs workflow

`.github/workflows/specs.yml` on every push to main and pull request, without a paths filter, on Ubuntu: Java and Gradle for `checkScenarios`, Node for `npx -y @fission-ai/openspec@1.17.0 validate --all`, the version used here. A separate workflow, not a job in Build, so Build's Markdown skip and the release gate stay as they are, and a spec-only commit costs one short Ubuntu run, not the macOS job.

### Sorting the scenarios without a test

Once, in this change, each of the 342 is either:
1. covered by an existing test under another name → `// Scenario:` marker on that test;
2. decided on the devices (layout, screen readers, platform behavior) or by a workflow run → entry in the list with how;
3. logic a test can check but none does → a new test.

## Risks / Trade-offs

- [Group 3 is large] → After sorting, if more than about 20 scenarios need new tests, the maintainer decides before they are written whether all of them belong in this change; the rest would be listed as `not tested yet`, visible, to shrink later.
- [A name-only check lets a test claim a scenario it does not check] → Markers are added by reading the test against the scenario; the check makes gaps visible, it does not replace review.
- [The list becomes a place to park untested logic] → Each entry says how it is checked, and an entry for a scenario with a test fails the build, so the list cannot hide tested and untested alike.
- [Two Gradle runs of the check on a code push (Build and Specs)] → It takes seconds and is cached; accepted for one workflow that always runs.

## Migration Plan

The check goes in together with the markers, the list and the new tests, so main stays green. Rollback: remove the `check` dependency and the workflow.
