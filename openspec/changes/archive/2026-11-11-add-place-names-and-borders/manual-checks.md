# Manual checks

Android on the emulator and iOS in the simulator, 2026-11-11, with graphs built locally from the extracts of 2026-11-08 (places and crossings), put in place of the installed cycling graph.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Place in the waypoint list | ✅ | ✅ | 06 → 02 → 28: "Start · Lohmar", "Stop · Siegburg" ([screenshot](screenshots/android-waypoint-list.png)) |
| Waypoint in the open | ✅ | ✅ | 28 by the Sieg in Menden: "End" only |
| Crossing into another country | ✅ | ✅ | Slenaken (26) → Teuven (428): "590 m, Belgium, Limburg", with a dashed mark ([Android](screenshots/android-coming-border.png), [iOS](screenshots/ios-sim-coming-border.png)) |
| Places in the list | ✅ | ✅ | 428 with "Teuven" |
| Place and border with a screen reader | ✅ | ✅ | TalkBack's text "Knot 428, Teuven, end, 1.2 km, 20:05" and "Border, Belgium, Limburg"; VoiceOver's label "Border, Belgium, Limburg" |
| Knot in a town, Knot in the open, Only a hamlet nearby, Names in other languages, Crossing into Belgium, Crossing a province border, Within one province, Beyond the covered area, Incomplete border, Graph files without places / borders, Place name in the app's language | unit test | unit test | `PlaceNamingTest`, `BorderCrossingTest`, `GraphFileTest`, `PlaceNamesTest` |
| Size budget | ✅ | — | Cycling graph 676 KB with places and crossings (558 KB before) |

Real data (task 1.2):
- **Places:** 52 % of cycling knots (10,732 of 20,584) and 46 % of hiking knots have a place. The maintainer judged a sample of 20 and kept the reaches.
- **Borders:** 1,772 crossings on 1,155 cycling connections and 1,750 on 1,210 hiking connections. All regions were complete.

The maintainer checked it on the iPhone on 2026-11-11 with the published data ("looks good").

### Steps for the maintainer

1. Plan a route through a few villages and look at the waypoint list. Does each name match the town sign at the knot?
2. Navigate a route across a border (Dutch/Belgian Limburg, or into North Rhine-Westphalia). Does the line show at about the right place in the coming knots?
3. Switch the app to German and plan to Liège: "Liège (Lüttich)".
