# Design

## Context

- **Back on route** (`Navigation.update`): one distance, `OFF_COURSE_METERS = 100`, decides both leaving and coming back. `distanceToCourse(fix)` measures to the courses of the current leg and its two neighbors.
  - Off route by distance, the rider is back as soon as a fix is within 100 m.
  - Off route by a wrong knot (`offRouteKnot`), they are also back as soon as they are within 100 m, but only once they are more than 100 m from that knot.
  - Passing a knot of the route always brings them back.
- **While off route:**
  - `Navigation.place`, the offset along the route, stays at the last place the rider was on the route; the progress strip shows the dot there.
  - `RouteProgress.placeOf` projects a fix onto the leg's course with `fractionAlong`.
- **The line** (`NetworkMap.updateNavigation` on Android, `NetworkMapView.Coordinator.updateNavigation` on iOS): drawn from the rider to `nextKnot`.
  - While following off route, the camera fits the bounds of the rider and `nextKnot`.
  - Both apps get `nextKnot` from `navigation.nextKnot`.

## Goals / Non-Goals

**Goals:**
- The core decides where the line goes, so both apps draw the same thing.
- Coming back on route needs the rider on the route, not beside it.

**Non-Goals:**
- Guidance (header, notices, lock screen) to the route instead of the next knot.
- Telling parallel paths closer than 30 m from the route.
- How the camera moves while off route, beyond which point it keeps in view.

## Decisions

**Back on route within `ON_COURSE_METERS = 30.0`.** In `update`, the back-on-route branch uses `distanceToCourse <= ON_COURSE_METERS` instead of "not more than `OFF_COURSE_METERS`". Between 30 and 100 m, the rider stays in whatever state they are in.
- 30 m is `PASS_RADIUS_METERS`, the distance at which a knot counts as passed, so "on the route" means the same at a knot and between knots. Precise fixes are within 50 m accuracy, usually under 10 m on a bike in the open.
- The wrong-knot condition (more than 100 m from `offRouteKnot`) stays. Without it, a rider passing a wrong knot right beside the course would be back at once.
- Starting navigation keeps its 100 m. A rider starting 60 m from the course starts on the route, which is what they would expect at the start.
- Alternative: two fixes in a row within 30 m. The maintainer chose the single fix; an outlier only ends off route early, and the next fix puts them off route again.

**`Navigation.wayToRoute(fix): GeoPoint`, the end of the line.** While off route:
- **With a course** for the leg to the next knot (`nextIndex > 0`): the nearest point of that course after `place`.
  - A new `RouteProgress.nearestAhead(fix, nextIndex, place): GeoPoint` cuts the course at the place's fraction of the leg, then projects the fix onto the rest, like `fractionAlong` but returning the point.
  - If the nearest point would be before `place`, the cut makes it the point at `place` ("Route behind the rider").
- **Otherwise:** the next knot's position.
- Only the current leg, not the later ones: back on route is decided on the current leg and its neighbors, and passing the next knot moves on. A line to a later leg would lead past the next knot, which the rider then never passes.
- Alternative: the nearest point of all the route ahead. It could point to a later leg, and navigation would never count the next knot as passed.

**The apps take the point, not the knot.**
- `NetworkMap`/`NetworkMapView` get a `wayToRoute: GeoPoint?` parameter, set from `navigation.wayToRoute(fix)` while off route and `null` otherwise. They draw the line to it and fit the camera to the rider and it.
- The next-knot ring keeps using `nextKnot`.
- The point is computed per fix in the core, where `place` and the courses are; the apps have no courses of their own.

**Off route, the camera stays no closer than following (`NAVIGATION_ZOOM`, 15), below the controls at the top.** Found on the emulator: the way back can be 60 m long, and fitting it alone zoomed in to street level, with the rider under the search field (`screenshots/android-off-route-line-too-close.png`).
- Android takes the camera for the bounds with the top padding below the controls (`compassTop`), then caps its zoom.
- iOS raises the fitted camera's altitude to the one of the navigation zoom; its edge padding already clears the controls.

**"Number to follow" moves with the 30 m.** The scenario "Distance to the first knot" (from `fix-double-knots`, not archived yet) had the rider back on route within 100 m of the course, 80 m from 24. Within 30 m of the course but 80 m from 24 means being on the first leg's course past 24, and the test now rides there. The delta carries the whole requirement, so `fix-double-knots` has to be archived first.

## Risks / Trade-offs

- **The line jumps along the course as the rider moves**, unlike a fixed knot.
  - Expected, as it always points to the closest way back.
  - The camera fit then changes with every fix while off route, as it does now with the rider moving.
- **A parallel path 30–100 m away no longer ends off route**, but it never started it either. A rider who leaves for such a path stays on route, as before.
- **A rider within 30 m of the course but on another way**, e.g. the road under a bridge, counts as back on route. GPS cannot tell this apart.
- **A course that loops back** close to `place` may have its nearest point far along the leg. It is still ahead, and the next knot is at its end.
