# Manual checks

## Scenarios

Android checked 2026-10-30 on the emulator (`Medium_Phone_API_37.0`) with the bundled graphs, positions mocked with `adb emu geo fix`. The emulator's clock cannot be set (production image), so the scenarios at fixed moments are covered by the core tests (`FerryWarningTest`, JVM and iOS simulator) and the time zone by the Android unit test (`FerryArrivalClockTest`).

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Closed for the season | ✅ | ⏳ | 76 → 02 at Renkum, Friday 18:07, started south of 76: "The ferry Renkumse Veer is closed when you get there around 18:10, it opens on 1 May at 10:00" spoken after "Off route, next 76" |
| Warning in the ferry list | ✅ | ⏳ | "You get there around 18:10" in red below the ferry's title, above its status and times ([screenshot](screenshots/android-ferry-warning.png)) |
| Screen off | ✅ | ⏳ | Navigation started, screen locked before the first fix: the warning was logged and handed to the speech engine while the screen was asleep |
| Offline | ✅ | ⏳ | Airplane mode: the same warning |
| Phone in another time zone | unit test | ⏳ | `FerryArrivalClockTest`: started at 17:00 London time, 20 km ahead: "around 19:15" and the warning in Dutch time |
| Closed when the rider gets there, Closes shortly after the rider gets there, Not open yet when the rider gets there, Open when the rider gets there, The rider's own pace, Hiking, Falling behind, Warned once, Ferry without a name, Times that cannot be told, Several ferries, Arrival in the ferry list, Ferry passed | unit tests | unit tests | `FerryWarningTest`, run on the JVM and the iOS simulator |

### iOS steps for the maintainer

1. Plan 76 → 02 at Renkum (cycling), across Renkumse Veer, and start navigating. Until May, it says "The ferry Renkumse Veer is closed when you get there around …, it opens on 1 May at 10:00" after the first notice.
2. Open the panel: below "⛴ Renkumse Veer", "You get there around …" in red.
3. Stop, start navigating again and lock the phone right away: the warning is spoken with the screen off.
4. Airplane mode: the same warning.
5. Settings → General → Date & Time: another time zone, e.g. London. Start again: the time in "around …" is Dutch time. Switch it back.
6. Optional, on a ride: a route over a ferry that is open, e.g. Broekhuizen (56 → 70): no warning, and the arrival time in the list in the normal text color; after the knot behind the ferry, the arrival is gone.
