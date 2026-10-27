# Manual checks

Checked 2026-10-27 on the Android emulator (`Medium_Phone_API_37.0`) and the iOS simulator (iPhone 18 Pro), each with a route the maintainer planned during the manual checks of `add-planning-screens`.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Zooming out | ✅ | ⏳ | Android: the − button zooms out by one level. iOS: buttons shown, not tapped (taps cannot be scripted in this setup) |
| Passed knots | ✅ | ✅ | White badges with an orange ring, above the network's knots |
| Start, stop and end | ✅ | ✅ | Start green and larger, stops filled orange, end dark and larger |
| Direction arrows | ✅ | ✅ | White arrows on the route line. The base map's sprite has its own `arrow` image for one-way streets, so the app's image is named `route-arrow` |
| Route summary | ✅ | ✅ | Distance and the route's badges in a wrapping row, without arrows |
| Listing waypoints | ✅ | ✅ | Badge and role per waypoint |
| Round trip on the map | – | ✅ | One split badge, green and dark, at the round trip's start and end; the sheet's row shows start and end separately ([screenshot](screenshots/ios-round-trip.png)). Android draws the same badge from the same core markers |
| Back to start | ✅ shown | ✅ shown | Behavior covered by the core tests ("Back to start") |
| Zoomed out | ✅ | ⏳ | Android: route as a line without markers; tapping it zooms to the route. iOS: not tapped |
| Tapping the last waypoint again | – | – | Core behavior, covered by the core tests |

Screenshots: [Android route](screenshots/android-route.png), [Android sheet](screenshots/android-sheet.png), [Android zoomed out](screenshots/android-zoomed-out.png), [iOS route](screenshots/ios-route.png).
