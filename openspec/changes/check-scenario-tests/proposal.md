# Proposal

## Why

"Every scenario needs a test" is a rule in `CLAUDE.md`, but nothing checks it: of the 788 scenarios in `openspec/specs`, 446 have a test named after them or marked with `// Scenario:`, and the rest is a mix of scenarios checked on the devices and logic nobody tests, which cannot be told apart. Even today's changes slipped: `network-sync` "Retrying a corrupted download" and "Published data readable again" are tested, but under other names. And `openspec validate` runs nowhere, while Build skips commits that change only Markdown, so a broken spec is not noticed until someone runs the CLI.

## What Changes

- A scenario check that fails when a scenario of `openspec/specs` has no test and is not listed as checked another way. A test counts when its name starts with the scenario's name, or when it carries a `Scenario:` comment with the name (`//` in Kotlin, `#` in the shell test scripts).
- `openspec/checked-by-hand.md`, listing per capability the scenarios checked another way, each with how (e.g. on the devices, by the network data workflow). The check fails on entries for scenarios that no longer exist or have a test, so the list stays true.
- The check runs in `./gradlew check`, so locally and in Build, and can be run for a change's spec delta before archiving.
- A new workflow `Specs` that runs `openspec validate --all` and the scenario check on every push and pull request, including commits that change only specs.
- The scenarios without a test today are sorted once: tests that cover them under another name get a `Scenario:` comment, scenarios checked on the devices or by a workflow go into the list, and testable logic without a test gets a test.
- `CLAUDE.md` and the tasks rule in `openspec/config.yaml` say how a test names its scenario.

Out of scope:
- `openspec validate --strict`: it fails today only on warnings (long requirement texts in six specs); fixing those is a spec edit of its own.
- An iOS test target for the app's own code; iOS scenarios not decided by the core stay checked on the devices.
- Telling apart scenarios of the same name by their test's name: a test named after a scenario counts for every scenario of that name. Markers and list entries can name the requirement where that matters.
- Changing what Build runs or skips; the release workflow relies on Build skipping Markdown.

## Capabilities

### New Capabilities
- `spec-checks`: the checks that keep the specs valid and every scenario tied to a test or to a stated way it is checked.

### Modified Capabilities

## Impact

- New Gradle module `tools:scenario-check` (Kotlin JVM), wired into the root `check`.
- New `.github/workflows/specs.yml`; Node only there, for the pinned OpenSpec CLI.
- `// Scenario:` comments added to existing tests, new tests for untested logic, `openspec/checked-by-hand.md`.
- `CLAUDE.md`, `CONTRIBUTING.md` if it mentions tests, `openspec/config.yaml`.
- No change to the apps; no CHANGELOG entry.
