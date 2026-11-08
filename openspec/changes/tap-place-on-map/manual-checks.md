# Manual checks

## How it was checked

Checked on 2026-11-08 with the published network data and the base map's tiles:

- **Android:** the emulator (`Medium_Phone_API_37.0`) with the debug build, on the route Troisdorf 25 → 32 → 29. Taps with `adb shell input tap`, positions with `adb emu geo fix`. Afterwards the emulator got its own route and camera back.
- **iOS:** the project builds. The simulator here takes no taps, so the iPhone checks are the maintainer's (see below).

## Scenarios

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Name of a café | ✅ | ✅ maintainer | "Eiscafé Riviera" with the food icon above the three choices ([Android](screenshots/android-cafe.png)). `NetworkGeometryTest` |
| Place without a name | ✅ | ✅ maintainer | Unnamed toilets by the station: "Toilets" ([Android](screenshots/android-water.png)). `NetworkGeometryTest` |
| Stop at a place | ✅ | ✅ maintainer | "Add stop here" put the stop at the café, with a way along paths from the course near 25 ([Android](screenshots/android-stop.png)). `NetworkGeometryTest` |
| Place next to a course | unit test | unit test | `NetworkGeometryTest`: no snapping to the course within the tolerance |
| Knot before a place | unit test | unit test | `NetworkGeometryTest` |
| Place far from the network | unit test | unit test | `NetworkGeometryTest`: `PlaceOnly` |
| Place while navigating | ✅ | open | "Bei Ute" while navigating: name only, no flag, the panel unchanged ([Android](screenshots/android-navigating.png)) |
| Closing the card | ✅ | open | A tap beside it closed it, navigation went on ([Android](screenshots/android-navigating-closed.png)) |
| Grey places unchanged | ✅ | open | The church Sankt Maria Königin: choices without a name ([Android](screenshots/android-grey.png)). `NetworkGeometryTest` |

## For the maintainer

On 2026-11-08 the maintainer confirmed the planning features on both phones, the Pixel and the iPhone ("Planning features work on both phones"). What happens while navigating and on a ride waits for the next tour.

On the iPhone on 2026-11-08 the maintainer checked planning (step 1) and asked for the stop to show the place's icon instead of the flag, which is out of scope here and can be its own change.


1. ✅ (2026-11-08) **iPhone, planning:** tap a café, a supermarket and a drinking water point: icon and name (or "Drinking water") above the choices. Make one a stop: the flag is at the place.
2. **iPhone, navigating:** tap a place: the name only, and a tap beside it closes it.
3. **On a ride, both phones:** tap a toilet or a café you pass, and see whether the name helps you decide.
