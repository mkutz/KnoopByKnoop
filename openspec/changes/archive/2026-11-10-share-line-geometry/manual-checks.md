# Manual checks

## How it was checked

Checked on 2026-11-10, after every step of task 2 and at the end:

- **Tests:** the core's tests on the JVM and the iOS simulator (814, none failing) including the new `GeoTest`, the pipeline's tests, `./gradlew check` with the Android build, and the iOS app build (`xcodebuild` as in the Build workflow). No numeric expectation in an existing test had to change.
- **Replay of a real ride** (`checks/Replay2.kt`, the 06 → 08 ride near Titz of `move-navigation-loop-to-core` on the published data, now also printing the progress line, the header, the rider's place on the strip and the guided way's line at every fix): 908 lines before the change ([before](checks/replay-before.txt)), and after it ([after](checks/replay-after.txt)) the same notices, way searches and lengths, progress lines and headers; 2 lines differ, the rider on the strip by 0.001 of the route, since `RouteProgress` measures the fraction of a leg by great-circle instead of flat length (task 2.2). Every other step left the replay as it was.
- **Geometry files** (`checks/RoundTrip.kt`): the published cycling and hiking geometry, read and written again, gives the same bytes before the change and after `readPoints` and `writePoints` (3,295,907 and 2,607,497 bytes).
- **Pipeline lengths:** its haversine and the core's `distanceMeters` agreed within 4·10⁻¹² m on all 31,927 cycling connections before the haversine went.
- **Kept on purpose:** `PathRouter.firstPointBeyond` (was `pointAlong`), which takes a point of the way itself rather than one between two, for the heading at a junction; `RoutePoint.cutAt` and `between`, which keep a given place and drop doubled points, now on `Polyline`; `GuidedWay.nearest`, which only looks around the rider's last place, now on the shared `nearestOnLine`.
- **Found while doing it:** the copy that extrapolated past the ends (`TrickyStretches.pointAlongCourse`) was never called within 60 m of an end, so it was not a bug.
