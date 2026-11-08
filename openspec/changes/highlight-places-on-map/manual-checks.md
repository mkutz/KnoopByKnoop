# Manual checks

## How it was checked

Checked on 2026-11-08 with the published network data and OpenFreeMap's tiles (version 20261101):

- **Android:** the emulator (`Medium_Phone_API_37.0`) with the debug build. Camera and route were set through the app's files, positions with `adb emu geo fix`, dark mode with `cmd uimode night`.
- **iOS:** the simulator (iPhone 18 Pro, iOS 27) with the debug build. Camera and route through the app's files, dark mode with `simctl ui appearance`. The simulator takes no taps here, so the switch's stored value was set with `defaults write`.
- **Afterwards:** both devices got their own route and camera back, and the places setting is on.

## Scenarios

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Places in a town | ✅ | ✅ | Troisdorf and Siegburg at z15: food magenta, supermarkets purple, bike shops brown, stations slate, water teal ([Android](screenshots/android-town-z15.png), [iOS](screenshots/ios-z15.png)). `PlaceKindTest` |
| Names close up | ✅ | ✅ | z16.3: "Eiscafé Riviera", "Aldi Süd", "Siegburg/Bonn" ([Android](screenshots/android-z16.png), [iOS](screenshots/ios-z16.png)) |
| Zoomed out | ✅ | — | z13.5: none ([Android](screenshots/android-z13.png)) |
| Shown once | ✅ | ✅ | Kaufland is purple with places on, the base map's grey cart with them off. `PlaceKindTest`, `BaseMapPlacesTest` for the dark style's layers |
| Other places unchanged | ✅ | ✅ | Clothes shops, the Tagesklinik, churches and tram stops keep the base map's icons. `PlaceKindTest` |
| Knot next to a café | open | open | Code in place (invisible badge copies); no knot with a café close by was found in Troisdorf or Siegburg |
| Under the route | ✅ | ✅ | Cafés under the way to the route and the route in Siegburg ([Android](screenshots/android-navigating.png)) |
| Distinct from knots | unit test | unit test | `PlaceKindTest`: against the three sign colours, the app green, the route's orange and each other |
| Places on the dark map | ✅ | ✅ | [Android](screenshots/android-dark-z15.png), [iOS](screenshots/ios-dark-z15.png) |
| Places while navigating | ✅ | — | Siegburg centre while navigating from 02 ([Android](screenshots/android-navigating.png)). iOS uses the same layers; not started there |
| Places on a hiking map | ✅ | — | Siegburg with hiking selected ([Android](screenshots/android-hiking.png)) |
| Without the base map | open | open | The places are added only when the style has the `openmaptiles` source, which the offline style lacks; not seen, as the tile cache kept the base map |
| Turning the places off | ✅ | ✅ | Android: the switch in the info sheet ([sheet](screenshots/android-info-sheet.png), [off](screenshots/android-switched-off.png)); iOS: stored value off ([off](screenshots/ios-off.png)), the toggle itself not tapped |
| Choice kept for places | ✅ | ✅ | Android: off after a restart ([screenshot](screenshots/android-off-after-restart.png)); iOS: read from `@AppStorage` at launch |
| Switch by default | ✅ | ✅ | On without a stored value on both |

## For the maintainer

1. **Plan in a town centre** (Siegburg, Troisdorf): do the icons help, or do they crowd out the street names at z14–15? If too busy, food and drink could start at z15.
2. **The info sheet on the iPhone:** the "Places on the map" toggle turns them off and on, also after a restart.
3. **Near a knot in a town:** a café right next to a knot is left out, and street names under a knot are hidden from z14.
4. **Navigate through a town:** the places are below the route and your arrow.
5. **Dark mode:** icons and names readable.
