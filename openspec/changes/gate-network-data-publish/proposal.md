# Proposal

## Why

Every Monday the workflow `update-network-data` replaces the published network data that every installed app downloads. The only check before `gh release upload --clobber` is a size budget, which is an upper limit. A truncated Geofabrik extract, a tagging change in OpenStreetMap that drops a region, or a pipeline regression that only shows on real data would reach every rider within a week, and the apps would install it as long as the checksums match. The code review of 2026-11-10 ranked this as the gap with the largest blast radius.

## What Changes

- **Read back before publishing:** every graph, geometry and path file is read with the core's own readers, the same checks the apps run when installing (`NetworkInstaller`), and every square of the path data is unpacked and checked against its checksum. A file the apps would reject is not published.
- **Floors:** each network must have at least a fixed number of knots and connections, and the path data at least a fixed number of squares and segments. They are set at about 90 % of the counts of 2026-11-10 (cycling 20,584 knots and 31,927 connections, hiking 50,460 and 74,605, paths 3,711 squares and 10,435,008 segments).
- **No sudden drop:** compared with the currently published graphs, no network may lose more than 5 % of its knots or connections in one week.
- **Known routes:** a few fixed long-distance knot pairs across the Netherlands, Belgium and North Rhine-Westphalia, for cycling and hiking, must still be routable, and not much longer than the straight line between them.
- **Deliberate drops:** a manual run can allow a drop larger than 5 %, for a week where losing knots is expected (e.g. a pipeline change that drops broken routes on purpose). Floors, read-back and known routes still apply.
- **Failure keeps the old data:** any failed check fails the workflow before the upload, so the previous publication stays, as for a failed build.

## Out of Scope

- Checks on the content beyond counts and routes: geometry quality, ferry times, places, sign regions.
- Checking the bundled graphs in the app releases (`pipeline bundle` already checks them with `NetworkInstaller`).
- Notifying the maintainer beyond GitHub's own mail for a failed workflow.
- Tests of the format against frozen bytes from older releases; that is part of the later change for real-data and format tests.
- Comparing the path data with the published path data; it is checked against its floors only, as its directory alone is several megabytes to fetch.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `network-data`: a new requirement "Checked before publication" next to "Weekly publication", with the scenarios for the read-back, the floors, the drop, the known routes and the deliberate drop.

## Impact

- **Pipeline:** a new command `pipeline verify <dir> [<published-base-url>] [--allow-drop]` in a new `Verify.kt`, reusing `NetworkInstaller`, `GraphFile`, `NetworkGeometry`, `PathFile` and `ShortestPathRouter` from the core, with tests on small graphs written by the tests.
- **Workflow:** `update-network-data.yml` runs `verify` in the publish job after the index is written and before the upload, so it checks the files against the index the apps will read; `workflow_dispatch` gets an `allow-drop` input.
- **Apps and core:** no change.
