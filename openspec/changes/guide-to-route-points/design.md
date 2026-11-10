# Design

## Context

- **Route points:** a route point is a `Knot` with a `RoutePoint`: its connection (`fromId`, `toId`), the place on the course, and the `way` from the point to that place, along paths or `straight`. The route's line to the point is the course up to the place, then the way reversed.
- **Navigation:** the leg to a route point is one leg; its notices name the point ("On to the end"), and on a way along paths the rider only gets the distance and direction (`add-route-points`, "A straight line" and "Following a way along paths").
- **Guided way:** `WayGuide` searches a `PathWay` with `PathTurn`s (direction, street, kind) on the device and `GuidedWay` tells each turn, with a reminder, while off route.
- **The ride of 2026-11-08:** the last leg 02 → home, about 2.5 km, silent for ten minutes.

## Goals / Non-Goals

**Goals:**
- Name the network's direction while the way to a route point follows it.
- Tell the turn-off and each turn after it.
- Reuse the guided way's turns and texts.

**Non-Goals:**
- Changing the planned route or where it leaves the network.

## Decisions

### The knot ahead on the course
- The leg to a route point runs along the course of the point's connection towards the point's place. The knot the course leads to is the connection's knot on the far side of the place, in the riding direction (`toId` when coming from `fromId`, and the other way round). The notices name it: "then towards 06", "Towards 06, then the end".
- On a leg that starts at a route point and joins the network, nothing changes.

### The turn-off as a turn
- The turn-off is the place where the way leaves the course. Its turn direction comes from the course before it and the way's first stretch after it, measured like the turn at a knot (`turn-at-knot`). The street is the one of the way's first segment, from the path data.
- Its notices use the distances of a guided way's turn: from 100 m ahead, and "now" within 40 m.
- **Street name:** only the turn-off names its street ("into Cecilienstraße"), as it is the one turn the rider cannot see coming from the signs. The turns after it are told as on the guided way back to the route, without the street; the header shows the street (the maintainer, 2026-11-08).

### The header off the network
- Between the turn-off and the route point, and from a route point to the network, the header shows the light tile of the way to the route after starting elsewhere: the arrow and distance, the next turn and its street. Not the dark one, as the rider is on route (the maintainer, 2026-11-08).

### Turns after the turn-off from a guided way
- 300 m before the turn-off, `WayGuide` searches the way from the turn-off to the route point; it names the turn-off's street. From 40 m before the turn-off it follows that way with `GuidedWay`, or searches one from the rider if there is none, as it does off route. So the turns, the reminders, leaving the way and searching again work as on the way back to the route; the guided way tells each turn once, as off route.
- **Why search again:** the planned way is only points; searching from the turn-off gives the turns and streets, from the same path data and so nearly the same way.
- **Alternative:** keeping the `PathWay` of the planning in the route point. It would need storing turns in the saved route, and a route restored after an app update would have none.

### From a route point onto the network
- Starting at a route point, or passing a stop off the network, the way goes from the point to its place on a course. `WayGuide` searches a way from the rider to that place and follows it with `GuidedWay`, as above.
- At the place, the turn onto the course is told like the turn-off, in reverse: its direction from the way's last stretch and the course after the place, and the knot the course leads to in the riding direction ("onto the route towards 02").
- After the join, navigation goes on as on the route.

### Missing the turn-off
- More than the off-route distance from both the course and the way counts as off route, as today; the guided way then leads to the route point from where the rider is.

### Searching along the planned way (found 2026-11-10)
- **What happened:** the maintainer started a route of a start and an end flag 15–40 m from the start flag, on the iPhone and on the Pixel. The Pixel's log has the first guided way 1 s after starting: 1,291 m with 5 turns. A replay on a local cut-out of the published data gives the planned way (1,139–1,143 m, south via Josefstraße) without a riding direction, and 1,287–1,301 m along Lindenstraße and the railway with a direction north. So the first search already had a riding direction, from the first two positions while the position settled, and the way ahead won, being less than 200 m longer (`path-routing` "A way ahead about as short as the way back").
- **No direction from settling:** the riding direction for a way search needs positions at least 5 s apart; the ridden track's direction for the map is unchanged. This fixes both starts: the Pixel's first search, 1 s after starting, has no direction and finds the planned way; on the iPhone, after walking west, a direction west also finds it (1,145 m).
- **One logic with off route:** the way to and from a route point is searched exactly as off route, riding direction included. A rider who really rides off in another direction next to the planned way gets a way ahead, as off route does (the maintainer, 2026-11-10).
- *Alternatives:* searching in the planned way's direction near it (dropped: a special case that only matters when riding off on purpose); keeping the planned way's points and following them without a search (rejected for the reason above: no turns or streets, nothing after an app update).

### A turn at the start of a way (found 2026-11-10)
- A new way whose first stretch leaves more than 45° to the side of the riding direction, and not against it, gets "Turn left now" or "Turn right now" at once, like a turn within 40 m; against the direction stays "Turn back now".

## Risks / Trade-offs

- **[The search after the turn-off finds a slightly different way]** → It searches from the rider to the point, so it may leave out a bend of the planned way; the rider still reaches the point.
- **[No path data for the way's squares]** → The turn-off is still told, from the course and the planned way's first points; then the distance and direction, as before.
- **[Text changes in four languages]** → New phrases next to the existing turn texts.

## Migration Plan

App update only.
