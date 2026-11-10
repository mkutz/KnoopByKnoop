# Tasks

## 1. Shared geometry

- [x] 1.1 `Geo.kt` with `Polyline` (`meters`, `length`, `pointAt`, `stretch`, `nearest`), `METERS_PER_DEGREE`, `boxAround`, `bearing`, `signedAngle`, `angleBetween`; `distanceMeters` public. Tests in `GeoTest`: ends clamped, zero-length segments, reversed stretches, nearest before the start and past the end, a position beside a corner, bearings in all four quadrants and across north, signed angles across ±180°.
- [x] 1.2 Before replacing anything: run the replay (`openspec/changes/archive/…-move-navigation-loop-to-core/checks/Replay.kt`, or its copy if that change is not archived yet) on the current build and keep its output, and write the published geometry files through `read` and `toBytes` and keep the bytes.

## 2. Use it

Each task replaces the copies of one helper and keeps `./gradlew :modules:core:jvmTest :pipeline:test` green; a moved numeric expectation is explained in its commit.

- [x] 2.1 Point along and stretch: `Navigation.pointAlong`, `NetworkTurns.pointOn` and `stretchOf`, `PathRouter.pointAtMeters`, `pointAlong` and `stretchOf`, `RouteProgress` `pointAt`, `TrickyStretches.pointAlongCourse` and `runningCumulative`, `MapFocus.stretchOfLine`, `RoutePoint.between`, `cutAt` and `lengthOf`, `NetworkGeometry.lengthOf`, `PathRouter.lengthOf`.
- [x] 2.2 Nearest place: `Navigation.distanceToLine`, `PathRouter.nearestOnLine` and `nearestOn`, `RouteProgress.fractionAlong`, `RoutePoint.nearestOn`, the windowed loop in `GuidedWay`.
- [x] 2.3 Meters per degree, boxes, bearings and angles: the 8 definitions, the 5 boxes, `Navigation.bearing` and `angleBetween`, `PathRouter.heading` and `signedAngle`, `RiddenTrack.bearing`, the conversion in `WayGuide`.
- [x] 2.4 `MinHeap<T>` for `Router` and `PathRouter`; `TurnNotice` for `GuidedWay`, `NetworkTurns` and `WayGuide`; `MINUTES_PER_DAY` and minute arithmetic on `LocalMoment` for `FerryWatch`, `MapFeatures` and `OpeningHours`.
- [x] 2.5 `NetworkGeometry.readPoints`, used by `read` in place of its four loops, and `writePoints` in `toBytes` in place of its two inline copies; the pipeline on `distanceMeters`, after a test comparing it with `haversineMeters` on sample points.

## 3. Proof

- [ ] 3.1 The replay again: the same notices and way searches as in 1.2, lengths within a meter; the geometry files written again give the same bytes as in 1.2. Both apps build (`./gradlew check`, `xcodebuild` as in the Build workflow), the core passes on the iOS simulator. `manual-checks.md` with the results. No CHANGELOG entry: nothing changes for riders.
