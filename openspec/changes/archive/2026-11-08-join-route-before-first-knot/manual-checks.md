# Manual checks

## Scenarios

Checked 2026-11-08. Android: emulator `Medium_Phone_API_37.0`, Android 17, with locally built data. Core: `NavigationTest`.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Reaching the course and following it | ✅ | ✅ core | Emulator: 06 → 02 from 250 m east of the course; after ~100 m along it "02 1.9 km" and the line ahead |
| Crossing the course | ✅ core | ✅ core | |
| Reaching the course of the first leg near the start | ✅ core | ✅ core | |
| On the course close to the first knot | ✅ core | ✅ core | The existing "Distance to the first knot" still holds |
| Following the course against the route | ✅ core | ✅ core | The wrong-way notice after joining |
| Without courses | ✅ core | ✅ core | |

## For the maintainer

| Check | Pixel | iPhone | Notes |
|---|---|---|---|
| Start at home away from the route, ride onto the course and along it | ⏳ | ⏳ | The header switches from the first knot to the one ahead, the line ahead appears, "Back on route" is said once |

## Archived

Archived on 8 November 2026 at the maintainer's choice. Built after the morning ride of 8 November that showed the problem; the maintainer judged it working after the afternoon ride.
