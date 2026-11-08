# Proposal

## Why

On the maintainer's ride of 2026-11-08 the iPhone said "off route" twice while they rode on the route with the screen off, and "back on route" right after: at 14:16:26 ("Off route, next 28, 2.3 km to the west", back 2 s later) and at 15:28:30 ("Off route, next 21, 3.4 km to the west", back in the same second). The app's log of that ride shows both right after stretches of coarse sampling, and something worse around them: the requested precision switched between coarse and fine hundreds to thousands of times within a second, about 20 times during the ride, 1,781 times at 15:28:04 just before the second false alert.

Each switch changes the location manager's accuracy and distance filter. iOS answers a changed request by delivering the last known location again, so the app processes the same old fixes over and over, flipping between a coarse one, which asks for a fine fix to confirm, and a precise-looking one, which allows coarse sampling again. One of these old or coarse fixes far from the course is enough for an "off route".

## What Changes

- A fix that is not newer than the last one processed is ignored, so locations delivered again do not count twice and cannot flip the sampling.
- Before saying "off route" because of the distance to the course, navigation confirms it with a precise fix: accurate to 20 m or better and still more than 100 m from the course. Until then it samples finely and says nothing. Passing a knot that is not on the route still counts at once.
- The guided way back is only searched once off route is confirmed.

## Out of Scope

- **Off route in towns at less than 100 m**, e.g. a shortcut through a loop: the threshold stays.
- **Android's sampling.** It shares the core's confirmation; its own location client is not known to flap, which the Pixel's logs check.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: "Off route" gets the confirmation, with scenarios for it and for fixes delivered again.

## Impact

- **Core:** `Navigation.update` (ignoring fixes not newer than `lastFix`, confirming off route with a precise fix).
- **iOS and Android:** nothing beyond the core; the iOS log shows whether the flapping stopped.
