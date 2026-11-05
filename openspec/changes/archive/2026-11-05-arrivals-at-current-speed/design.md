# Design

## Context

- **`Navigation.Pace`** holds `startOffset`, `startMillis` and `metersPerSecond`. `at(offset, time, assumed)` is called only when a knot is passed: the route covered since the start over the time since the start, or `assumed` until `OWN_PACE_AFTER_METERS` (2 km).
- **Where it is used:**
  - `progress(fix)` for the strip and the coming knots, per position while the app is visible.
  - `routeLine` and `route.line` for the lock screen, at knots.
  - `FerryWatch.atKnot`, for the ferry arrivals, the popover and the warnings.
- **The place:** `Navigation.place`, an offset along the route, is updated on precise fixes on the route (`update`), and stands still off route. Fixes less accurate than 50 m go to `updateCoarse` and change nothing here.
- **Fixes:** with the screen off, precise fixes come about every 30 s (`COARSE`), every 3 s near a knot (`FINE`), and every second when visible (`LIVE`).

## Goals / Non-Goals

**Goals:**
- Arrivals that follow a change of pace within about 15 minutes, on the fixes navigation already has.

**Non-Goals:**
- Predicting from gradient or wind.
- Spoken warnings between knots.

## Decisions

**A window of segments in `Pace`.** `Pace` keeps the last sample (offset, time) and a list of `Segment(meters, seconds)`:
- A precise fix on the route that is at least 100 m further along than the last sample closes a segment. Smaller steps are left to accumulate, so GPS noise and a place that jumps back do not count.
- The pace sums the newest segments until they cover 5 km (`PACE_WINDOW_METERS`), and drops older ones, so about 50 segments at most.
- A single segment longer than the window, e.g. after skipping ahead or with few fixes, counts whole.
- The own pace starts after 2 km covered in all, as now.
- A step faster than 72 km/h is not riding but the place on the route jumping, e.g. on a leg there and back, where the nearest point can be on the other way. It does not count, and measuring starts anew from there.

**Standing still is capped, not detected.** A segment's time counts at most as if ridden at a third of the assumed pace: 5.3 km/h cycling, 1.5 km/h hiking. 100 m with a 30-minute café stop in it counts as 67 s on a bike, so a stop costs about 5% of the pace over the 5 km after it.
- Found on the emulator: the first cap, 0.5 m/s for both, counted a stop as up to 200 s, and the arrival at the end jumped 20 minutes after a 2-minute stop on a short window (`screenshots/android-arrivals-first-cap.png`).
- No separate standing detection, which would need thresholds of its own and GPS jitter handling.
- A cyclist pushing their bike uphill counts as 5.3 km/h at the slowest, which is close enough for an arrival rounded to five minutes.
- Alternative: a rolling time window, e.g. 15 minutes. It needs the break handled separately, and it means less distance when riding slowly.

**Off route.** While off route, no segments are closed. Back on route, the last sample is set to the rider's place and time, so the detour's time is not counted.

**Renewed per fix, warned at knots.** `update` renews the pace on each precise fix on the route. `FerryWatch` gets `renewed(offset, time, nextIndex, metersPerSecond)`, the same calculation as `atKnot` without notices and without adding to `warned`, so the popover and the ring follow while the spoken warning stays at knots.
- Each renewal looks at the ferries ahead only, usually none or a few, so it costs little.
- `progressLine` stays as it is: renewed at knots, with the new pace.

**The arrivals stay rounded to five minutes**, so most renewals change nothing visible.

## Risks / Trade-offs

- **Flicker near a rounding boundary:** an arrival can flip between 18:50 and 18:55. With a 5 km window it changes slowly, so that stays rare. It is checked on the emulator, and hysteresis would be a follow-up.
- **A steep climb or a tailwind** moves the arrivals for the 5 km after it, too. That is the choice the maintainer made: reacting over steadiness.
- **After a break, the first 100 m** count at the cap, a dip of about 5% that the window evens out.
