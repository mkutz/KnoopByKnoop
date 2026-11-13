## Context

`PlanningSession` is immutable, and every edit on both platforms goes through one `update(session)` that saves a `RouteSnapshot` (`route.json`) and shows the session. Not every `update` is an edit: the courses coming in re-attach the route (`reattached`) and also call `update`. The note after a tap already keeps the session before the tap (`TapNote.before`) for its Undo, and its role choices replace the session without a new note.

## Goals / Non-Goals

**Goals:** many steps back for every user edit while planning, kept across restarts, one button in the panel.

**Non-Goals:** redo; undoing navigation-only changes (ferry detours, start on the route nearby); keeping the edits across a network switch; Undo for search, place cards or settings.

## Decisions

- **Snapshots, not inverse operations.** The history is a list of `RouteSnapshot`s of the route *before* each edit, newest last, at most 50 (older ones dropped). Undo restores the newest with `RouteSnapshot.restore(graph)` on the current graph and drops it. Snapshots are small (knot ids, route points with their way) and already serialise; restoring on the current graph keeps the courses that came in since, and leaves out knots that are gone, telling the user as on a restart. Alternative: inverse operations per edit kind; more code per edit and wrong after the courses changed the session.
- **Core `RouteHistory`.** An immutable class with `record(before, after)` (an edit that changed no waypoint, such as tapping the end again, is not kept), `canUndo`, `undo(graph): Pair<RestoredRoute, RouteHistory>?`, `toJson()` and `parse(text)`, tested on the JVM. The apps hold one and save it beside the route.
- **Edits are recorded where the user acts, not in `update`.** A separate `edit(session)` records the current session and then calls `update`; the courses' `update` stays as it is. The note's choices call `update` without recording, so the tap's record covers them and Undo after a choice goes back to before the tap.
- **Stored in `route-history.json`** beside `route.json`, written with it, in the same atomic way. A missing or unreadable file means no history. Clear empties `route.json` as today but records first, so it can be undone. A network switch deletes both.
- **The note's Undo goes.** Undo in the panel does the same, and the note closes on any undo. The note's previews stay.
- **Placement.** The action row gets Undo as the last icon button (`ic_undo` on Android, `arrow.counterclockwise` on iOS, since Back to start already shows `arrow.uturn.backward` in the same row); the empty panel's FlowRow gets it after its other buttons. Shown only while `canUndo` and not navigating.

## Risks / Trade-offs

- Undo after a data update may bring back a route with a knot left out. → Same message as on a restart.
- 50 snapshots of routes with many route points could grow the file. → Route points store their way; 50 × a few kB stays well below a megabyte.
- The note loses a button riders know. → Undo sits in the panel right below the note.
