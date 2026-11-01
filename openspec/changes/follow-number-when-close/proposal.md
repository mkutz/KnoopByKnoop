# Proposal

## Why

The maintainer's notes on the test ride of 2026-11-01 (Bonn towards Troisdorf, Pixel 7, video 2 at 0:00): "Even though I'm nowhere near 83, the most prominent knot on screen is 30. I think this should only happen when I'm close enough to 83."

Far from a knot, what matters is where to head next: the next knot and how far it is. The number to follow there only matters when it's time to turn.

## What Changes

- **Far from the next knot:** while the rider is farther than 200 m from it, the header shows "Next 83" and the distance as its most prominent information, with "then 30" smaller below.
- **Close to the next knot:** from the moment the reminder "83 ahead, then 30" is given, at 200 m, the header shows "At 83 follow 30" with 30 most prominent, as now. That includes a knot so close after the last one that the notice on passing already named it.
- **Screen readers** read the far header as "Next 83, 1.2 km, then 30".
- **Unchanged:**
  - the end of the route ("31 is the end");
  - the off-route header;
  - the lock-screen notification and Live Activity.
- **New texts:** "Next" and "then" in English, German, Dutch and French.

## Out of Scope

- **The lock-screen notification and the Live Activity:** they keep "At 83 follow 30". Making them more readable is its own ride note.
- **When the reminder is given:** 200 m straight to the knot, as now.
- **The other ride notes:** out-and-back legs with a fork, arrivals at the current speed, the lock-screen notification and the language switch during navigation.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`:
  - **"Number to follow":** the number to follow is most prominent only once the rider is close; until then, the next knot and its distance are.
  - **"Number to follow at large text sizes":** covers the far header too.

## Impact

- **Core:** `NavigationDisplay` gets whether the rider is close to the next knot, taken from the reminder state in `Navigation`. Tests are in `NavigationTest`.
- **Android:** `NavigationHeader` in `PlannerScreen.kt`, and the strings in `values*/strings.xml`.
- **iOS:** `NavigationHeader` in `PlannerView.swift`, and `Localizable.xcstrings`.
- **Archive order:** the change's delta builds on the "Number to follow" text of `improve-off-route`, so it is archived after `fix-double-knots` and `improve-off-route`.
