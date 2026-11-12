# Design

## Context

- **Core:** `MapFeatures.describe` gives `RouteDescription.StartOnly(start, routePoint, endFirst)` with fewer than two waypoints. It has only the number, not the knot.
- **Android:** `WaypointSheet` in `PlannerScreen.kt` draws the header. For non-complete states it shows one `Text` (`tap_to_set_start`, `start_at_set_end`, `start_point_set_end`, `end_point_set_start`, `no_knots_in_view`), plus a `ProminentAction` "Show the nearest knots" with no knots in view. `RouteActions` (Back to start, Export, Clear) sits below the badges. The place a knot lies in comes from `knot.place?.nameIn(language)`, as in the waypoint list.
- **iOS:** `WaypointSheet.summary` in `PlannerView.swift` mirrors it, and `mainAction` holds the nearest knots button. `routeActions` sits below `RouteKnots`.
- **Sheet heights:** both platforms measure the header for the smallest detent, so a taller empty state needs no fixed numbers.
- **Mockup A:** https://claude.ai/artifact/23wUTABxwB9fbwSpyXdN3G

## Goals / Non-Goals

**Goals:**
- Show what a route needs (start, end), and fill it as the user taps, in the badges they already know.
- Offer the other ways to begin (search, nearest knots) in the panel.
- Same structure on both platforms and in landscape.

**Non-Goals:**
- Changing the complete-route header, the navigation panel or the map's buttons.
- New ways to plan (round trips, saved routes), or a general undo.

## Decisions

- **The core carries the waypoint.** `StartOnly` gains `waypoint: Knot`, keeping `start`, `routePoint` and `endFirst`. The apps already draw a `Knot` as a badge (network colour, flag for a route point) and read its place. Alternative: look the waypoint up in the session in each app. That duplicates the role logic of `endFirst` per platform.
- **One `EmptyRoute` view per platform** for every non-complete state: title, the slot row, labels, hint and the buttons. It replaces the `Text` and the prominent nearest knots button in the header. With no waypoints at all, `StartOnly` is absent, so the view takes the description as nullable plus `noKnotsInView`.
- **Slots.** 40 dp/pt circles like the route row's badges. Empty is a 2 dp dashed outline in a muted green with "?". The next slot is in the action green, with a soft halo pulsing every 1.8 s. The halo is not drawn when the platform reduces motion: `Settings.Global.ANIMATOR_DURATION_SCALE == 0` on Android, `accessibilityReduceMotion` on iOS. Between the slots is a dotted line filling the width.
- **Labels and screen readers.** "Start"/"End", plus " · <place>" when the slot holds a knot with a place. Each slot with its label is one accessibility element: "Start, 48, De Rips" or "End, not set yet".
- **Buttons.** Tonal buttons with icon and label, wrapping in a `FlowRow` (Android) / `ViewThatFits` falling back to a `VStack` (iOS) at large text. "Search a place" calls the same open-search callback as the map's button; it's hidden while the search is open. "Nearest knots" calls `showNearestKnots` and is filled (the prominent green) with no knots in view. With a waypoint, Clear (the existing action) replaces it; `RouteActions` is then not shown separately, so Clear appears once.
- **Strings (four languages):** "Plan a ride", "Tap a numbered knot on the map for the start.", "Now tap a knot for the end.", "Now tap a knot for the start.", "Search a place", "Nearest knots", "not set yet". The old `tap_to_set_start`, `start_at_set_end`, `start_point_set_end` and `end_point_set_start` go. "No knots in view" stays as the hint in that state.
- **Landscape:** the same `EmptyRoute` at the top of the side panel.

## Risks / Trade-offs

- **Taller smallest sheet.** The empty state is about one row taller than today, so it covers a bit more map while nothing is planned. This is accepted for the clearer start. It is measured, so it stays as small as the content.
- **Two search entries.** The panel's search duplicates the map button on purpose: it's the visible way in for new users. The map button stays for when the panel is pulled down.
- **Pulse as distraction.** A soft halo, not a bounce, and no pulse with reduced motion.
