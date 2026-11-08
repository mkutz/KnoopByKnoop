# Manual checks

## Scenarios

Checked 2026-11-06 with geometry built locally from that day's extracts (not published), installed into the apps' storage. Android: emulator `Medium_Phone_API_37.0`, Android 17, and the maintainer's Pixel 7. iOS: iPhone 18 Pro simulator, iOS 27, and the maintainer's iPhone. At the Sieg bridges near Siegburg.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Route under a bridge | ✅ | ✅ | 28 → 29: south of 28 the A 560 is drawn semi-transparent across the route |
| Route over a bridge | ✅ | ✅ | 28 → 29 over the Sieg: the route on top of the bridge |
| Over and under the same bridge | ✅ | ✅ | The maintainer's reference routes 02 → 28 → 29 → 28 → 27 and 32 → … → 01 on the Pixel; core: `BridgeLinesTest` |
| Under, then onto the same bridge | ✅ | ✅ | The maintainer on the Pixel: seen at a glance with the bridge edges; edges toned down once at their request |
| Connections and bridges, Connection on a bridge | ✅ | ✅ | The 28–29 connection over the Sieg on top of the bridge without edges, the one under the A 560 below it |
| Same transparency everywhere | ✅ core | ✅ core | `BridgeLinesTest`: the pieces of a leg differ only in `bridge` |
| Dark map, Edges on the dark map | ✅ | ✅ | Copies in the dark style's colours; edges light grey, faint where they overlap a yellow road |
| Offline map | ✅ | – | Android offline: plain map, lines as before, no bridge layers |
| Geometry without bridge stretches | ✅ core | ✅ core | `BridgeLinesTest`, `BridgeStretchesTest` |
| Knots and position stay on top | ✅ | ✅ | Badges 28 and 29 and the position above the bridges |
| Line ahead while navigating | ✅ | ⏳ | Android emulator, walking 28 → 29: the line ahead on top of the bridge with edges; iOS waits for the ride check below |
| Ridden track | ✅ | – | Android emulator: the blue track visible on the bridge after walking across it |
| Tunnel | – | – | Not looked at separately; nothing in the change touches tunnels |

Covered by tests only: "Course over a bridge", "Course under a bridge", "Several ways of one bridge", "Bridge tagged no", "Stretch kept through simplification", "Signpost course over a bridge", "Released apps", and the bundled dark style's bridge layers (`BridgeLayersTest`).

## For the maintainer

| Check | Pixel | iPhone | Notes |
|---|---|---|---|
| Over and under the bridge between 28 and 29 | ✅ | ✅ | Checked while planning on 2026-11-06 |
| Riding it: the line ahead and the ridden track on the bridge | ⏳ | ⏳ | |
| The dark map outside | ⏳ | ⏳ | Are the edges visible enough? |
| Battery while navigating | ⏳ | ⏳ | About 20 more line layers on tiles already loaded |

## Archived

Archived on 8 November 2026 at the maintainer's choice. Not ridden: the line ahead and the ridden track on a bridge, the dark map, and the battery.
