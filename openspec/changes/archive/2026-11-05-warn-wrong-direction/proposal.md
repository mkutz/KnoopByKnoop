# Proposal

## Why

On the ride of 2026-11-02 (video `17-23-33`, from 4:55) the maintainer came back onto the route and then rode along it towards the start. The distance to 28 grew from 2.0 to 2.7 km over about three minutes, and the app showed "28 is the end" as if all was well. Only leaving the course gave a warning, at about 8:20.

"Off route" only asks how far the rider is from the course, not which way they ride along it. A rider who turns the wrong way at a junction after a detour hears nothing and loses time, and with the screen off they have no hint at all.

## What Changes

- **Wrong direction is detected:** when a rider on the course of the current leg has ridden about 150 m back along it, the app says so once, by voice and on screen.
- **It shows everywhere the off-route warning shows:** in red in the panel, in the notification and the Live Activity, with the next knot. The panel keeps the distance along the route.
- **It ends by itself:** after turning around, "Back on route, next …"; passing the next knot or leaving the course take over as they do today.
- **No false alarms from the map data:** a jump of the place on the route, e.g. where a course doubles back on itself, does not count, as the rider has not really moved that far.

## Out of Scope

- Planning a new route, or a line to a place to turn around.
- Riding the wrong way before the first knot, or without the network's courses: there is no course or no reliable place on it.
- Riding back on a path beside the course, more than 30 m from it.
- The distance and dashed line towards a knot with two signposts, seen on the same ride (video `16-22-42`, 7:35 to 7:47). That is a separate change.
- The small shift of the strip between the panel's forms, which the maintainer rates low priority.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: a new requirement "Riding the wrong way".

## Impact

- **Core:**
  - `Navigation` keeps the furthest place reached on the leg and a `wrongWay` state; `NavigationDisplay` gets `wrongWay` and its text.
  - `Texts` gets the wording in English, German, Dutch and French.
  - Tests in `NavigationTest`.
- **Android:** the header in `PlannerScreen`, and the notification's title and chip in `NavigationService`.
- **iOS:** the panel's header in `PlannerView`, and `ContentState` and the Live Activity's views. The catalogs get the new words.
- **Battery:** no new sampling while riding normally. While the warning stands, sampling is as fine as off route.
- **Archive order:** the requirement is new, so it does not depend on the order of the other changes, but the code builds on `improve-off-route` and `readable-lock-screen`.
