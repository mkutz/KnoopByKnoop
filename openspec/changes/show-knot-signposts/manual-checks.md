# Manual checks

## Scenarios

Android checked 2026-11-02 on the emulator (`Medium_Phone_API_37.0`) with the debug build and the published data of that day (extract date 2026-10-31), at Siegburg 02. iOS checked the same way in the simulator (iPhone 18 Pro, iOS 27), with navigation started by a temporary launch flag (removed again). The route, camera and position of both were restored.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Badges at both signposts | ✅ | ✅ simulator | 02 by the river and near the L 16 ([Android](screenshots/android-badges-tap-route.png), [iOS](screenshots/ios-badges-and-rings.png)) |
| Tapping the other signpost | ✅ | ✅ | Tap on the north-west badge: `route.json` has 02 (node 59469043), "Start at 02" (left). iOS checked by the maintainer on the iPhone, and again on the Pixel: both badges are highlighted. The core lookup is covered by `PlanningTest` "tapping the other signpost" |
| Route through a knot with two signposts | ✅ | ✅ simulator | Route 28 → 02: both badges have the end's style, and the panel shows 02 once (middle) |
| Next knot with two signposts | ✅ | ✅ simulator | Both rings ([Android](screenshots/android-next-knot-rings.png), iOS right) |
| Route starting at the other signpost | ✅ | ✅ simulator | With the published data: 02 → 28 starts at the north-west badge ([Android](screenshots/android-routes-at-signposts.png), first; [iOS](screenshots/ios-routes-at-signposts.png), left), and 02 → 01 at the river badge (Android, second). `NetworkGeometryTest` |
| Route ending at the other signpost | unit test | unit test | `NetworkGeometryTest` |
| Route through a knot between its signposts | ✅ | ✅ simulator | 01 → 27, passing 02: the line runs along the path between the badges, and both are highlighted as passed (Android, third; iOS, right). 28 → 02 → 01 with 02 as a stop: the same (Android, fourth) |
| Knots with equal numbers on both banks | unit test | unit test | `PlanningTest` |
| Along the ways to the knot's position | ✅ pipeline | ✅ pipeline | Köln extract, course 02 → 28: before, a straight 195 m from the river badge to the L 16 signpost; now along the ways in steps of 22, 37, 75 and 109 m, passing that signpost within 5 m. `NetworkExtractionTest`. Seen on the emulator with a graph and geometry built from that extract: the route 28 → 02 follows the path near the river, under the L 16, not over the bridge ([screenshot](screenshots/android-course-under-the-bridge.png)). The other screenshots show the published courses, which are still straight |
| No way between the nodes | unit test | unit test | `NetworkExtractionTest`, also with a way more than 3 times the straight distance |
| Junction tagged on several nodes | unit test | unit test | The course still ends at the knot's position |

Startup on the emulator, mark "knots": 6.9 s with the old build, 5.0 s and 4.0 s with the new one.

### Steps for the maintainer

1. ✅ At Siegburg 02 on the Pixel and the iPhone: a badge 02 by the river and one near the L 16.
2. ✅ iPhone: with no route, tap the badge near the L 16: 02 becomes the start, shown once in the list. Checked by the maintainer on the iPhone and the Pixel, 2026-11-02.
3. Navigate a route through 02: both badges are highlighted, and both have the ring while 02 is next.
4. Plan 02 → 28 and 02 → 01: each line starts at the badge it leaves from. Plan 01 → 27: the line runs through both badges.
5. ✅ After the next data publication: the route line between the two badges follows the paths, not a straight line across the L 16. Checked by the maintainer on the Pixel and the iPhone, 2026-11-02, with the data of 2026-11-01.

### Ride of 2026-11-02

Step 3 ✅ on the Pixel and the iPhone: both badges 02 with the ring, highlighted. Step 4: 02 → 28 and 02 → 01 each start at their badge in planning; 01 → 27 stays open. Waits for `pass-merged-knots`.
