# Tasks

## 1. Core

- [x] 1.1 `MapFeatures.knots` and `routeKnotsLayer`: one point per position. Tests in `PlanningTest`, where the map layers are tested:
  - "Badges at both signposts": a merged knot gives two points with the same `id` and `ref`.
  - "Route through a knot with two signposts": the route marker of a merged knot gives two points with its role.
  - Separate knots with equal numbers give one point each ("Knots with equal numbers on both banks").

## 2. Pipeline

- [x] 2.1 `NetworkExtraction.connectionOf`: extend to the knot's position along the ways of the junction, straight without a path or beyond 3× the straight distance. Tests in `NetworkExtractionTest`:
  - "Along the ways to the knot's position": two nodes 02 joined by a bent way, so the course follows the way's points.
  - "No way between the nodes": straight as before.
  - The connection's length is unchanged.

  Then run the pipeline on the Köln extract and check Siegburg 02's courses: no straight line across the L 16.

## 3. Android

- [x] 3.1 Tap with `nearestAtAnyNode`, the next-knot ring at every position, a layer version in the cached knots file name. On the emulator with the published data (Siegburg, route 02 → 28):
  - screenshots of both badges 02, also highlighted on the route ("Badges at both signposts", "Route through a knot with two signposts");
  - a tap on the L 16 badge sets 02 ("Tapping the other signpost");
  - navigating towards 02 shows both rings ("Next knot with two signposts");
  - the `Startup` mark "knots" compared with before.

## 4. iOS

- [x] 4.1 The same in `AppModel` and `NetworkMapView`. The project builds, and simulator screenshots show both badges at Siegburg 02, tapping the L 16 badge, and both rings while navigating.

## 5. Wrap-up

- [x] 5.1 `manual-checks.md` with the scenarios and the maintainer's steps; `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased.

## Workflow follow-up

- The new courses reach the phones with the next data publication after the commit (or a manual run of Update Network Data).
- The maintainer checks it at Siegburg 02 on the Pixel and the iPhone, before `/opsx:archive show-knot-signposts`, after `pass-merged-knots`.
