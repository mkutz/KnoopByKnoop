# Design

## Context

- **A tap today** (`PlanningSession.tap`): the first knot is the start, the second the end, and later ones go through `Route.insertStop`, which takes the leg where the stop adds the least distance.
- **The panel:**
  - Android: `WaypointSheet` in a `BottomSheetScaffold`, whose `peekHeight` is computed in `PlannerScreen`, with `SummaryWithActions` in one row and the knot sequence below it.
  - iOS: `WaypointSheet` in `PlannerView`, with `presentationDetents`.
- **Existing controls:** the info sheet has a `SingleChoiceSegmentedButtonRow` on Android and a segmented `Picker` on iOS, stored in the `settings` preferences and in `AppStorage`.
- **Why not guess (survey 2026-11-03):** on the installed networks, 150 random routes each, a rule that the way to the knot runs through the end caught 53% (cycling) and 57% (hiking) of the knots next to the end, and 35 to 39% of those two knots past it. A broader rule caught about 76%, but moved the destination for 13 to 16% of the knots next to the middle of the route.

## Goals / Non-Goals

**Goals:**
- Looking around from the end takes one tap per knot, and A to B over C works as today.
- What a tap will do can always be seen.

**Non-Goals:**
- Telling what the user means from where they tap.

## Decisions

**A segmented control, with words for what happens to the route.** "Add knots" with "Between" and "At end". The first version used the roles' names, "New knots" with "Stop" and "End", and the maintainer found them hard to understand when trying it: they expect you to know the roles first. "Between" is accurate, as a stop always goes between two waypoints. It is the same kind of control as in the info sheet, and shows the state at a glance, which a toggle does not.

**An icon with an arrow on each choice,** showing where the knot goes: `●↓●` and `●●↓`, 32 by 16, in the color of the word. On iOS, the segmented `Picker` shows only the word of a `Label`, so the choices are a small control of their own. On Android, the icon is part of the segmented button's label, as it overlaps the word in the icon slot.
- Alternative, the role colors (green start, orange stop, dark end): tried in mock-ups, but both choices end up with the same colors in the same order, as the old end becomes a stop, and the dark end almost disappears on the dark panel.
- Alternative, green for the new knot: green means start in this app.
- Alternative, a ring or a plus as the new knot: these work too, but the arrow says where the knot goes.
- Alternative, a cursor in the knot sequence: on Between, the position is only known after the tap, as the stop goes in the leg where it adds the least distance.

**In its own row at the top of the panel,** directly below the knot sequence, and counted in the smallest height. It must be reachable while looking at the map, so it cannot sit in the list or the More actions menu. This costs one row of map while planning.
- Alternative, on the map next to the zoom buttons: it would save the panel row, but it would add to a crowded map, and the map's controls are not about the route.

**The mode is passed in.** `enum class NewKnots { STOP, END }` in the core, and `PlanningSession.tap(knot, newKnots)` without a default, so neither app can forget it. The apps own the choice, as with the appearance, because the session is restored from the waypoints and the choice belongs to the user, not the route. `tap` stays pure.

**`Route.withNewEnd(knot)`:** the knot is appended, or inserted before the last waypoint when the first equals the last. Legs are reused as in `withWaypoints`. `PlanningSession.tap` calls it when `newKnots` is `END` and a route exists; the two cases before it (no start, no end) do not depend on the mode. A route with an unreachable leg gets the new end like any other, and reports it.

**One choice for both networks,** as with the appearance. Looking around is a way of planning, not a property of a network.

**Between as the default.** Existing users see one new row, and nothing else changes.

## Risks / Trade-offs

- **Left on At end by accident:** a later A to B over C session turns stops into ends. This is visible in the switch, in the list and on the map, and a wrong tap is fixed with the handle or the ×. The remembered choice was picked over resetting it, on the assumption that someone who looks around usually plans that way each time. That is not confirmed, so it is to be judged when trying it.
- **Less map while planning:** one row in the smallest panel. Measured on the phones in the checks.
- **Long labels:** German and French labels need more room, and the icons take room as well. At 360 dp and 130% text the label goes above the choices, as the scenario says, which adds height at large text.
- **A knot on the course beside the route on At end** rides on to the end first and back, as the user asked for; the line shows it.
