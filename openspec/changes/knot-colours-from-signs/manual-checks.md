# Manual checks

## How it was checked

Checked on 2026-11-07:

- **Data:** built locally with `pipeline build` from the Geofabrik extracts of the Netherlands, Belgium and North Rhine-Westphalia found on this machine. The regions were published on 2026-11-07 by "Update Network Data" run 42407900788 (`cycling.graph` 558,357 bytes). Afterwards the maintainer saw the knots around Siegburg red on their iPhone with the published data.
- **Android:** on the emulator (`Medium_Phone_API_37.0`) with the debug build. The new `cycling.graph` and `cycling.geometry` were copied into the app's storage, the emulator was offline so the published files would not replace them, and positions were simulated with `adb emu geo fix`.
- **iOS:** in the simulator (iPhone 18 Pro, iOS 27) with the debug build. The same files were copied into the app's container, with a far-future data date so the published files would not replace them.
- **Afterwards:** routes, cameras and data were restored on both, and the emulator is back online.

## Scenarios

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Knot in Limburg | ✅ pipeline | ✅ pipeline | Roermond 95 (51.208, 5.950): Netherlands. `NetworkExtractionTest` "knots in each sign region" |
| Knot in Flanders | ✅ pipeline | ✅ pipeline | Haspengouw 150 (50.862, 5.208): Belgium |
| Knot in North Rhine-Westphalia | ✅ pipeline | ✅ pipeline | Troisdorf 25, Siegburg 02 and 60 near Lünen: NRW |
| Knot in another German state | unit test | unit test | `NetworkExtractionTest`. Real data: 119 cycling knots without a region, in the extracts' border buffers |
| Knot near a border | unit test | unit test | `NetworkExtractionTest`, the same elements read twice as from two extracts |
| Hiking knots | ✅ pipeline | ✅ pipeline | All 50,400 hiking knots without a region. `NetworkExtractionTest` |
| Incomplete border | ✅ pipeline | ✅ pipeline | The first real run failed on the Dutch national relation, which holds the Caribbean islands, see the design. `NetworkExtractionTest` |
| Apps without sign regions | unit test | unit test | `GraphFileTest`: the bytes before the new section are unchanged |
| Graph files without sign regions | unit test | unit test | `GraphFileTest` |
| Passed knots | ✅ | ✅ | [Android](screenshots/android-route-nrw.png), [iOS](screenshots/ios-route-nrw.png) |
| Start, stop and end | ✅ | ✅ | ▶, flag and ■ at the corners, on the map and in the waypoint list |
| Round trip on the map | ✅ | — | One 25 marker with a circular arrow ([Android, dark](screenshots/android-round-trip-dark.png)) |
| Route in North Rhine-Westphalia | ✅ | ✅ | Troisdorf 25 → 32 → 29: red plates with a white ring, the passed 25 white with a red ring |
| Route in the Netherlands | ✅ | ✅ | 16 near the border: a plain dark green disc |
| Route in Belgium | ✅ | ✅ | 17 near the border: blue with a white ring |
| Route across the border | ✅ | ✅ | 16 → 17 ([Android](screenshots/android-route-across-the-border.png), [iOS, dark](screenshots/ios-route-across-the-border-dark.png)) |
| Knots not on the route | ✅ | ✅ | 24, 06, 28 grey around Troisdorf |
| Knot without a region | ✅ | — | Hiking 73 → 74 near Aachen in the app's green ([Android](screenshots/android-hiking-without-region.png)) |
| Data from before sign regions | unit test | unit test | `PlanningTest`: plate `none` |
| Off route on the map | ✅ | open | The guided way is dark with a white casing, light on the dark map ([light](screenshots/android-off-route-light.png), [dark](screenshots/android-off-route-dark.png)) |
| Knots on the dark map | ✅ | ✅ | Grey knots on dark discs, plates unchanged |
| Off route in light mode | ✅ | open | Dark header with white text, the badge keeps its plate |
| Off route in dark mode | ✅ | open | Light header with dark text |
| Off route on the strip | ✅ | open | A ring with ✕ in place of the dot. Badges on the strip stay round ([navigating](screenshots/android-navigating-strip.png)) |
| Wrong direction in the panel | open | open | Same code path as off route, not seen on a device yet |
| Warning in the ferry list | open | open | Clock badge and bold arrival. `FerryArrivalClockTest` covers the arrival's look per state |
| Closed at the renewed estimate | open | open | As above |

On iOS, navigation can't be started in the simulator without tapping Navigate, so the navigation scenarios wait for a throwaway UI test or the maintainer.

## For the maintainer

After the next data publication with regions:

1. **At Troisdorf, Siegburg and a Dutch knot:** compare the badges with the signs. Red with a white ring and white number in NRW, a plain dark green disc in the Netherlands.
2. **Leave the route once:** the top of the panel turns dark, the dot on the strip becomes a ring with ✕, and the way back is dark on the map.
3. **Ride back along the route for 150 m:** the wrong-direction header is dark, not red.
4. **If a ferry route is at hand late in the day:** its badge on the strip shows a clock, and its arrival is bold.
5. **On the iPhone:** the same, including the Live Activity, which keeps today's look.
