# Proposal

## Why

The maintainer's notes on the test ride of 2026-11-01 (Bonn towards Troisdorf, Pixel 7): "Generally, ETAs are not updated according to my actual speed."

The pace behind the arrivals is the average since navigation started, breaks included, and it is only renewed at a knot. Knots can be kilometres apart, so the arrivals lag. On a long ride, a headwind hardly moves the average, and a long break keeps the arrivals too late for the rest of the ride.

## What Changes

- **Your recent pace:** once the rider has covered 2 km, the pace is the one over the last 5 km of the route, with standing still left out. Until then, the assumed 16 km/h cycling or 4.5 km/h hiking stay.
- **More often:** the pace is renewed with every precise position on the route, not only at knots. The strip, the coming knots and a ferry's popover and red ring follow it.
- **Standing still:** the arrivals move later while the rider stands, as the clock runs on, and the break does not count against their pace afterwards.
- **Detours:** time off route does not count, since the rider's place on the route stands still then.
- **No more location fixes:** it uses the fixes navigation already gets.

## Out of Scope

- **Spoken ferry warnings:** they are still only given at knots, so a short slowdown does not set one off. They use the new pace.
- **The lock screen and Live Activity:** the arrival is still renewed at each knot, with the new pace. Making them more readable is its own ride note.
- **A setting for the pace, or showing the speed.**
- **The other ride notes:** out-and-back legs with a fork, the lock-screen notification and the language switch during navigation.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: "Ferry arrival estimate" defines the pace that the strip and the coming knots use, so it changes how the pace is measured and how often the estimates are renewed.

## Impact

- **Core:** `Navigation.Pace` and `Navigation.update`; `FerryWatch` renews its estimates without warning between knots. Tests are in `RouteProgressTest` and `FerryWarningTest`.
- **Android and iOS:** no code. Both already ask the core for the progress at each position.
