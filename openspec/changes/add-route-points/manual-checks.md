# Manual checks

## Scenarios

Checked 2026-11-06 on a build of committed code plus these changes only, with the published data of 3 November. Android: emulator `Medium_Phone_API_37.0`, Android 17. iOS: iPhone 18 Pro simulator, iOS 27, driven by a throwaway UI test. Around Siegburg and Sankt Augustin, in English unless noted.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Knots go first | ✅ | ✅ | A tap near knot 09 made it a stop, without an offer. Core: `NetworkGeometryTest` |
| A route point goes first | ✅ | ✅ | A tap on the start flag offered nothing, 44 km unchanged |
| Offer and dismiss | ✅ | ✅ | Android: Back closed the menu. iOS: the menu shows below the tapped place since the maintainer found the dialog pointing at the sheet; a tap beside it closed it, 4.6 km unchanged |
| Offer without a route | ✅ core | ✅ core | `NetworkGeometryTest`; with a route, all three choices showed on both |
| Zoomed out | ✅ | – | Zoomed out, a tap offered nothing |
| All four languages of the offer | ✅ | ✅ | "Hier starten / Hier Zwischenstopp / Hier enden", "Hier starten / Hier tussenstop / Hier eindigen", "Partir d'ici / Ajouter une étape ici / Arriver ici" |
| First waypoint, Before an existing start | ✅ | ✅ | Start here on 02 → 80: 24 km, 02 a stop. iOS: the old start became a stop, 4.6 → 7.4 km |
| End before the start | ✅ | – | Hiking: "End set, tap a knot to set the start", dark end flag in the list |
| A route point away from the network, A way is found | ✅ | ✅ | Sankt Augustin to the course at Hangelar along the B 56, white dots on the route |
| No path data | ✅ | ✅ | Android offline: "Ways for this route couldn't be loaded"; iOS after deleting the ways: a straight line and "The way to a chosen place is only a straight line so far, and so is its length" |
| While the path data loads | – | ✅ | iOS: straight first (5.0 km), replaced by a way once the squares arrived (5.1 km). This found a bug on both platforms: the planner's router kept squares it had found missing; it now forgets them when new ones are installed |
| A hiking route | ✅ | – | A straight dashed line and the note |
| A stop on the route's own course, A stop as a detour | ✅ | ✅ | Out and back along the way, dots both ways |
| The line on the map | ✅ | ✅ | Ways along paths dotted white, straight lines dashed white, over the orange route |
| On the map, In the list | ✅ | ✅ | Flags green, orange and dark; the rows say Start, Stop, End; screen readers "Remove Start" (Android), "Remove, Start" (iOS) |
| After a restart | ✅ | ✅ | Android restarted offline: 24 km, the same way. iOS: the route restored from the stored file |
| Mocked ride (4.4, 5.4) | ✅ | ✅ | Start in the old town, stop on a detour, 02, end on the Sieg course, 4.6 km. Both logged: "On to the stop, then 02", "The stop ahead, then 02", "Next 02, then the end", "02 ahead, then the end", "On to the end", "The end ahead", "You've arrived"; spoken the same with "2" |
| Header, strip, notification, lock screen | ✅ | ✅ | "Next ⚑ 264 m then 02"; "⚑ is the end"; notification and iOS lock screen "On to the stop, then 02 · 2.8 km to the stop" with a flag |
| Dynamic Island | – | ⏳ | The simulator asked to allow Live Activities; the prompt was left alone, so the island didn't show |

Covered by tests only: "Connection gone", "Older route", "Starting away from the point", "Joining further along", "A point the way does not reach", "Off route with the end next", "A round trip from a route point", "A straight line" while riding, "Turning round at a detour", a point within 30 m of a knot, and GPX with route points.

## For the maintainer

| Check | Pixel | iPhone | Notes |
|---|---|---|---|
| Plan from home: Start here, a stop off the network, back to the start | ⏳ | ⏳ | Does the way from home look right, and is the menu or dialog in the way while planning? |
| Ride it and listen to the notices at the stop and the end | ⏳ | ⏳ | "On to the stop", "The end ahead" |
| Dynamic Island with a route point next | – | ⏳ | A flag where the number would be |
| GPX export of a route with route points | ⏳ | ⏳ | "Start", "Stop", "End" in the file |
