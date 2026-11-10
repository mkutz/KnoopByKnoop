# Manual checks

## How it was checked

Checked on 2026-11-10, after every step of tasks 2 and 3 and at the end:

- **Recorded rides** (`NavigationReplayTest`, task 1.1): 28 whole rides on the `NavigationTest` network recorded with the code before the change, fix by fix, with everything the apps read (notices with their speech, next knot, off route, arrived, wrong way, display, progress line, header, distances and arrivals ahead, ferry arrival, guidance, way to the route, map focus, courses ahead, precision). Identical after each of the three commits, and so were the 146 tests of `NavigationTest`.
- **Coverage** (JaCoCo through a throwaway Gradle init script, not committed): the rides alone reach every branch of `update()`, before and after (532 branches of `Navigation.kt` covered both times). Left out: no courses while off route on the way to the first knot, which cannot happen, and two guards in `watchingDirection` (a course of fewer than two points, no precise fix before).
- **Replay of a real ride** (`share-line-geometry`'s `checks/Replay2.kt`, the 06 → 08 ride near Titz on the published data): 908 lines, identical to that change's [after](../2026-11-10-share-line-geometry/checks/replay-after.txt).
- **Builds:** `./gradlew check` with the Android build, the core's tests on the iOS simulator, and the iOS app build (`xcodebuild` for the simulator), all passing.
- **Different from the design:** `with { }` takes the state as a parameter (`with { it.copy(...) }`) rather than as the receiver, so the names inside `copy(...)` still mean the navigation's own properties, as before; with the receiver, `offRoute` or `ferryWatch` there would silently have meant the state being changed.
- **Size:** `Navigation.kt` grew from 1564 to 1686 lines, mostly the properties reading through to the setup and the state (two lines each in ktfmt) and the docs of the new steps.
