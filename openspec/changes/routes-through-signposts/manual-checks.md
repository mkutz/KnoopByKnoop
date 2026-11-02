# Manual checks

## Scenarios

Checked 2026-11-02 at Siegburg 02 with cycling data built by this change's pipeline from the Geofabrik extract `koeln-regbez` (812 knots, 28 with several signposts), installed in place of the published data. Android on the emulator (`Medium_Phone_API_37.0`), iOS in the simulator (iPhone 18 Pro, iOS 27). The data, route and camera of both were restored.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Two branches of a route | ✅ pipeline | ✅ pipeline | Köln extract: 19 of 1,222 cycling connections and 0 of 704 hiking ones have them; the cycling geometry file grows 3.9%. `NetworkExtractionTest` |
| Only through another node, One node | unit test | unit test | `NetworkExtractionTest` |
| Released apps | ✅ | ✅ | The core of 0.7.0 (5f5af73) read the new Köln file: 1,222 courses for 1,222 connections, format version 1 |
| Geometry size budget | ⏳ | ⏳ | Köln is far below it. To be checked on the data run: it logs the size of the whole network |
| Through the signpost of the next leg | ✅ | ✅ simulator | 06 → 02 → 28 goes straight to the north-west 02, without the 02 by the river ([Android](screenshots/android-routes-through-signposts.png), left; [iOS](screenshots/ios-routes-through-signposts.png), left) |
| The other direction out of the knot | ✅ | ✅ simulator | 06 → 02 → 01 goes through the 02 by the river (Android, middle; iOS, right) |
| No shared signpost | ✅ | unit test | 28 → 02 → 01 joins them along the path (Android, right) |
| A stop at the knot | ✅ | ✅ simulator | The screenshots have 02 as a stop. `NetworkGeometryTest` |
| Back the same way, Same distance | unit test | unit test | `NetworkGeometryTest` |
| Following the line shown | ✅ | unit test | Mocked ride on the emulator along the branch to the north-west 02: "Next 02, then 28", then "Next 28" at the signpost, no "Off route". The branch more than 100 m from the main course: `NavigationTest` |
| The other branch, Away from all courses | unit test | unit test | `NavigationTest` |

### Steps for the maintainer

1. After the data run: plan 06 → 02 → 28 and 06 → 02 → 01 on the Pixel and the iPhone. Each line goes through one 02 only.
2. On a ride from the north-east through 02 towards 28: following the line stays on route, and passing the north-west 02 gives "Next 28".
