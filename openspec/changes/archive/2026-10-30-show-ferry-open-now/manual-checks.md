# Manual checks

## Coverage

The ferries of the bundled graphs (OpenStreetMap data of 2026-10-25), read with `OpeningHours.parse` and evaluated for Friday, 30 October 2026, 12:00:

| Network | Ferries | With operating times | Readable | With a status |
|---|---|---|---|---|
| Cycling | 213 | 183 | 180 | 177 |
| Hiking | 232 | 156 | 154 | 150 |

Unreadable are only values that are just a comment (`"Zie website"`, `"2026-09-29: Vaartijden voor 2027 nog niet bekend"`, `"(sunrise + 0.5 hour) - (sunset - 0.5 hour)"`). Without a status at that moment are values with sunrise/sunset, school holidays, or `"no data"` for all dates. The pipeline logs the readable count from now on, next to the other ferry figures.

All 184 distinct values were also evaluated for 30 October 2026, 16:30 and 4 July 2027, 09:15 and read through by hand. One finding changed the design: additional rules without dates after a rule with dates are read both ways (see design).

## Scenarios

Android checked 2026-10-30 on the emulator (`Medium_Phone_API_37.0`) with the bundled graphs. The emulator's clock cannot be set (production image), so moments other than now are covered by the core tests (`FerryStatusTest`) and the Android clock tests (`FerryClockTest`).

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Open now | ✅ | ✅ | 56 → 70 at Broekhuizen, Friday 11:45: "Open until 22:00" in green above "Mo-Fr 06:30-22:00, Sa 07:30-22:00, Su 09:00-22:00" ([screenshot](screenshots/android-open-now.png)) |
| Closed for the season | ✅ | ✅ | 02 → 76, Renkumse Veer: "Closed, opens on 1 May at 10:00" in red above the tagged times ([screenshot](screenshots/android-closed-for-the-season.png)) |
| Phone in another time zone | ✅ | ✅ | Emulator set to Tokyo (19:47 on the phone's clock): Broekhuizen still "Open until 22:00", as it was 11:47 in the Netherlands; time zone restored afterwards |
| Offline status | ✅ | ✅ | Airplane mode, app restarted: the same status ([screenshot](screenshots/android-offline.png)) |
| Closing while shown | unit test | ✅ | `FerryClockTest`: the update at the next full minute after 18:59:20 shows the ferry closed |
| Opens later today, Opens tomorrow, Open past midnight, Open around the clock, Public holiday, Holiday in only some countries, School holidays, Unreadable times | unit tests | unit tests | `FerryStatusTest`, run on the JVM and the iOS simulator |

iOS checked 2026-10-30 by the maintainer on the iPhone with the steps below.

Dark mode: the red and green status texts are readable on the dark panel.

### iOS steps for the maintainer

1. Plan 56 → 70 at Broekhuizen (cycling): the ferry shows a green status like "Open until 22:00" above its times. Before 06:30 or after 22:00 (Saturday 07:30, Sunday 09:00) it shows a red "Closed, opens …".
2. Plan 02 → 76 at Renkum (cycling): Renkumse Veer shows "Closed, opens on 1 May at 10:00".
3. Leave the panel open across a full minute where the status changes, e.g. Broekhuizen at 22:00, or check the status text updates at a minute change in general.
4. Settings → General → Date & Time: switch off "Set Automatically" and pick another time zone, e.g. London: the status stays in Dutch time. Switch it back.
5. Airplane mode: the status is still shown.
