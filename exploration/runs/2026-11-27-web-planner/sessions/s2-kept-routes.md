# Charter

Explore the web route planner at https://knoopbyknoop.com/plan/ (Knoop by Knoop) after the fixes of s1 (cdec436 live). Report bugs and observations to the maintainer.

Session focus (s2), in order of risk:

1. Routes per network on the live site, where kept routes and undo meet: undo right after a switch, a file for the other network then switching back and undoing, a route kept before the change, two tabs at once, storage full or blocked.
2. Light appearance.
3. A kept route after a new weekly publication: knots or route-point connections gone.

Run start: see s1. Session start 2026-11-27T18:56Z. Timebox: 30 minutes. Interface: the maintainer's Chrome via Claude in Chrome, live site, macOS. Authority: read-only use of the public site, browser storage of the planner's origin may be edited; no source changes. Not covered: Safari, Firefox, a real phone (left to the maintainer).

Source evidence before probing (Planner.kt, RouteSnapshot.kt): routes are kept under `knoopbyknoop.route.<network>` and `knoopbyknoop.history.<network>`, the last network under `knoopbyknoop.network`; each tab writes its own copy on every change (last writer wins, no `storage` event handling). A kept waypoint whose knot is gone is dropped and reported; a route point is restored from its stored connection (`fromId`, `toId`) without checking that those knots still exist. Hypotheses: H1 two tabs overwrite each other's route silently; H2 a route point whose connection's knots are gone breaks restoring; H3 undo steps from before a data update restore knots that no longer exist.

# Session log

### Experiment E1
Open /plan/ with the storage s1 left. Read localStorage before anything else.

### Observation O1
The kept cycling route 75 → 74 → 99 → 36 → 75 is restored (16 km), no console errors. Migration ran earlier: `knoopbyknoop.route` is "" and the per-network keys exist, but the legacy `knoopbyknoop.history` is left with its old content. Harmless (migration only runs while the legacy route is non-empty), just never tidied. Observation, not a bug.

### Experiment E2
Switch to Hiking with the cycling route in place; is Undo offered for the empty hiking route?

### Observation O2
Hiking shows the empty-route hint and no Undo button. As expected.

### Experiment E3
On hiking, click the map once.

### Observation O3
A card with only "Add to route" and "Close" opens: the map was already zoomed in far enough, so this was a spot beside the network, not a zoom click. Correct per "Route points in the planner", but the card has no text at all: nothing says what would be added. Product observation.

### Experiment E4
Plan a hiking route (three clicks), then: Cycling → Undo → Hiking → Undo → Cycling, then reload.

### Observation O4
Hiking route L51 → L59 → L67 (L51 was inserted at the start, "where it fits"). Cycling Undo removed the last 75 (16 → 13 km); hiking Undo removed L51; switching back to cycling showed the undone cycling route; after the reload all of it was kept, network CYCLING. Routes per network and their undo histories stay apart. No problem.

### Experiment E5
Two tabs (H1). Tab B opens /plan/ (hiking L59 → L67 shown) and clears the route. Tab A, still showing L59 → L67, presses Undo.

### Observation O5
Tab B's clear is stored, then tab A's Undo writes A's own state: stored route L59 only, history `[[]]`. B's clear and its undo step are gone silently, and tab B still shows an empty route that a reload would replace. Confirms H1: last writer wins, tabs don't hear of each other's changes. Not against the spec (it says nothing about tabs); a risk for a visitor who leaves an old planner tab open: one click there overwrites the newer route. Product observation.

### Experiment E6
Storage full: fill localStorage of the origin to its quota in tab B, then switch, Undo, and add two knots.

### Observation O6
Planning keeps working, no console errors, no message. The stored route lags behind: the panel shows 99 → 20, storage keeps 99 only (the write that failed is skipped). Matches the scenario "Browser storage unavailable" ("only lost on reload"), except that a reload brings back an older state rather than nothing. Fillers removed afterwards.

### Experiment E7
Data update (H2, H3), simulated in storage: cycling route 99 → route point "Gone place" whose connection knots (fromId/toId) don't exist → 75; undo history with one step 99 → 42 (knot id 123, not in the data) → 75. Reload, then Undo.

### Observation O7
H2 rejected: the route point with vanished connection knots is restored and routed (99 → ⚑ Gone place → 75, 12 km), no error. H3 partly confirmed: Undo goes to 99 → 75 (7.9 km) and drops the missing 42 without a word. The same route kept as the current route (E8) does tell it.

### Experiment E8
Kept route 99 → 42 (missing) → 75, empty history, reload.

### Observation O8
The panel says "Removed, no longer on the network: 42" and shows 99 → 75. So restoring on load reports missing knots, undoing into an old step does not. Minor inconsistency; after a real weekly update it would mostly hit visitors undoing far back.

### Experiment E9
Light appearance: tab A's page replaced by a full-size iframe of /plan/ with `color-scheme: light` (Chrome then reports `prefers-color-scheme: light` inside it). Fidelity: same engine and page, only the scheme source differs.

### Observation O9
Inside the iframe `prefers-color-scheme: dark` is false, body background rgb(250, 251, 248), the light OpenFreeMap style loads, badges, route line, QR code and panel are all readable. No problem found in the light appearance at desktop width.

### Observation O10
In both appearances (E8 tab, E9 iframe), the restored route 99 → 75 is not fully in view after the reload: the route line between 99 and 35 runs past the top edge, and 75 sits about 30 px from the bottom. Source: the first fit (`PlannerPage.render`, `routeBounds`) uses the knots of the route, not its courses, so bends between knots can fall outside. Minor.

# Findings summary

Stopped at the end of the three charters, about 30 minutes (18:56Z–19:27Z).

No bug that breaks planning was found. Routes per network, their undo histories and the network shown last are kept and restored correctly across switches and reloads (O2, O4). Full storage degrades as the spec says (O6). A route point whose connection vanished in a data update still restores (O7). The light appearance looks right (O9).

Observations for the maintainer, in order of impact:

1. Two tabs overwrite each other's kept route silently; an old tab left open wins on its next click (O5). Possible remedy: listen to the `storage` event and reload the route, or warn.
2. Undo into a step with knots that left the data drops them silently, while reload reports them (O7, O8).
3. The route fitted on reload leaves curves of the route line outside the view (O10).
4. The card for a spot beside the network has no text, only the buttons (O3).
5. The legacy `knoopbyknoop.history` key stays after migration (O1).

Not covered: Safari, Firefox, a real phone, light appearance at phone width. The browser storage of knoopbyknoop.com in the maintainer's Chrome still holds the test routes of this session (cycling 99 → 75, hiking L59).

Correction to the findings summary: the clock read 19:01Z when the log was written, so the session ran 18:56Z–19:01Z by the system clock, not until 19:27Z. The wall-clock time of the tool calls was longer; that estimate was a guess.

# Follow-up: fixes (after the session)

Correction to O7, O8 and finding 2: the Undo probe in E7 read only `section.route`, while the planner's message is drawn above that section. Re-checked on the live site (same storage as E7, reload, Undo): the panel says "Removed, no longer on the network: 42". Undo does report dropped knots; finding 2 was wrong.

Fixed in `add-web-planner`, checked in Chrome on a local build:

- Finding 1: the page listens to the `storage` event and takes over the route and undo history of the network shown when another tab changes them (`Planner.keptElsewhere`). Two tabs: clearing in one empties the other at once, and its Undo brings back the route from before the clear instead of overwriting it.
- Finding 3: the route's area follows its courses once they are in (`Planner.routeBounds`); a first fit made before the courses is made again when they arrive, unless the visitor has moved the map meanwhile. 99 → 75 near Zutphen is now in view whole; zooming out while loading kept the zoom. Knot 75 near the bottom edge sits exactly at the 48 px fit padding.
- New, found while checking: after "Clear route" the empty panel had no Undo, so a clear could not be taken back, against "Undo SHALL be shown only when there is an edit to take back". Undo now shows below the empty-route hint.
