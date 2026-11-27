# Charter

Explore the web route planner at https://knoopbyknoop.com/plan/ (Knoop by Knoop): planning by clicking knots, route points, places, search, ferries, undo, hand-off (link, QR, .knoop, GPX), opening routes, languages, light/dark, phone width, keyboard use. Report bugs and observations to the maintainer.

Session focus (s1): planning core on the live site — clicking knots, route points and places, undo, reload, and the hand-off outputs.

Run start: 2026-11-27 18:05 CET. Timebox: 1 hour (default). Interface: the maintainer's Chrome via Claude in Chrome, live site, macOS, dark appearance, desktop width ~1036×1154 CSS px. Authority: read-only use of the public site; no source changes.

# Session log

### Experiment E1
First visit with empty browser storage (localStorage cleared, reload) on /plan/ in English. Watch console errors and the first view.

### Observation O1
No console errors. Whole network area shown, map notice "Zoom in or search a place to see the knots.", panel hint "Click a knot on the map to start your route…". The panel hint says to click a knot while no knot can be seen yet — minor mismatch with the map notice (product judgement, not a defect).

### Experiment E2
Search "Zutphen", choose the first hit "Zutphen, Gelderland, Netherlands". Expect the map to fly there, knots to show, the zoom notice to go, places to show.

### Observation O2
Hits: Zutphen (Gelderland), Zutphen station, "Zutphen — House, Borculo", Zutphenstraat (Eindhoven). A screenshot right after the click showed knots, but still the zoom notice and no place icons; 3 s later the notice was hidden and the places shown. Interpretation: captured during the fly animation; negative result, no defect. The Eindhoven street hit shows the search reaches outside the clicked area, as in the apps.

### Experiment E3
Click knot 75 (Zutphen centre), then 36 across the IJssel; then click a food icon off the network and add it.

### Observation O3
Route 75 → 36, 2.4 km, Dutch green plates, passed knots 74, 10, 01 as ringed badges, courses followed. The café card showed "Vlaams Friethuis Loket 13", "Food and drink", "Open until 20:00" (Fri 18:10 local, hours Mo-Sa 12:00-20:00 — correct), hours and website. "Add to route" made it a stop between 75 and 36 (2.8 km), with a dotted way along paths. Panel badge for the place stop is a plain flag "⚑", while the map draws the place's food disc — small inconsistency (observation).

### Experiment E4
Capture the hand-off outputs by patching URL.createObjectURL / anchor click / clipboard in the page; click "Route file", "GPX", "Copy link".

### Observation O4
File names knoop-by-knoop-cycling-75-36.knoop / .gpx. Route file:
`# 75 Zutphen → 36 Zutphen`, `- 75 45417704 Zutphen`, `- 52.14142, 6.19732 Vlaams Friethuis Loket 13`, passed `74 Zutphen`, `10`, `01`, `- 36`. GPX 1.1 with 40 track points and waypoints; the café waypoint is named "Stop", not its name (core behaviour shared with the apps — observation, not a web defect). Link: https://knoopbyknoop.com/route/#… (239 chars). The browser tool refused to print the full link (it looks like query data), so I read lengths and prefixes only.

### Experiment E5
Switch the page language to NL with the route planned.

### Observation O5
/nl/plan/ shows the same three waypoints, Dutch texts ("Tussenstop", "2,8 km · ongeveer 10 min"), map fitted to the route. As specified.

### Experiment E6
With the route planned, switch to Wandelen (hiking), then back to Fietsen (cycling).

### Observation O6
Potential problem: the route is discarded on the switch and Undo is gone; switching back gives an empty cycling route — one stray click loses a planned route without warning. This is what the spec says ("Switching SHALL start a new route, as in the apps"), so it is a product judgement, not a spec violation.
Bug: each switch zooms the map out to the whole network area (Benelux + NRW, knots hidden, "zoom in" notice). The visitor was looking at Zutphen and must find it again. Expected the map to stay where it was, as in the apps.

### Experiment E7
Phone width. resize_window to 390×844 had no effect (the window stayed 1224 px wide — fidelity gap), so the planner was loaded in a same-origin iframe of 390×844 CSS px.

### Observation O7
No horizontal scroll (scrollWidth 388 = clientWidth). Map first (505 px tall), panel below it. Bug: the map notice "Zoom in of zoek een plaats om de knooppunten te zien." does not wrap (white-space: nowrap) and is wider than the map: cut off at both edges and lying over the zoom-in button. Observation: the site header takes 194 px (brand, nav and language switch on three rows) before the map starts, about a quarter of a phone screen. Not checked: touch gestures, a real phone browser.

### Experiment E8
Keyboard: reload /plan/, blur, press Tab repeatedly.

### Observation O8
Order: skip link, nav, languages, then the network switch; the focus ring is visible (green outline). Observation (accessibility gap, outside the spec): knots exist only on the map canvas, so there is no way to add a knot with the keyboard; a keyboard-only visitor can only open route files. Light appearance not checked: the browser tool can't switch the system appearance (gap).

### Experiment E9
Drop a hand-written route file on the map: a real DragEvent with a DataTransfer holding a File (`# Along the IJssel`, a note paragraph, `- 75 Zutphen`, `- 74`, `- 99`, `- 36`).

### Observation O9
Bug: nothing opens; the page throws `Uncaught TypeError: n.get is not a function`. The drop handler takes `dataTransfer.files` as dynamic and calls `.get(0)` on it, which a FileList doesn't have (it has `item()` and index access). Source reading: PlannerPage.enableDropping, `files?.get(0)` on a dynamic value — the typed `FileList.get` extension isn't used there.

### Experiment E10
The same file through the file chooser (setting input.files and dispatching change).

### Observation O10
Opens: 75 → 74 → 99 (Voorst, found within 5 km) → 36, 13 km, map fitted. Message "The route found differs from the file's list of knots." although the file lists no passed knots at all — a file of waypoints only always "differs" (core rule shared with the apps; product judgement).

### Experiment E11
A route too long for a QR code: a file of 400 waypoints alternating 75 and 74 (3,618 characters) through the file chooser.

### Observation O11
Bug: the route opens (400 waypoints, 217 km) but the whole "Take it along" section is missing: no Copy link, no downloads, no QR note, no "Open a route file". The page logs `Uncaught String`: qrcode-generator throws a string, not an Error; Kotlin's `catch (exception: Throwable)` rethrows non-Error values, so neither the QR fallback text ("too long for a QR code") nor the per-part guard in render() catches it. The places switch after it is missing too. Realistic exposure: links carry passed knots and towns; a long day tour may approach the QR byte limit (~2,950 bytes at level L).

### Experiment E12
After O11, click Undo; then "Back to start".

### Observation O12
Undo restored 75 → 74 → 99 → 36 (13 km) and the hand-off section came back, so the planner recovers after the next edit. "Back to start" made the round trip 75 … → 75 (16 km) and the button then disappeared, as specified.

# Bug reports

### B1 Dropping a route file on the map does nothing (O9)
Steps: open /plan/, drop any .knoop file on the map. Expected: the route opens (spec "Open a route in the planner", scenario "Dropped file"). Actual: nothing changes; console `Uncaught TypeError: n.get is not a function`. The file chooser opens the same file fine (O10). The unit test covers the planner state, not the page's drop handler.

### B2 A route too long for a QR code removes the whole "Take it along" section (O11)
Steps: open a route whose link exceeds the QR code's capacity (here 400 waypoints, 3,618-character file). Expected: link and downloads offered, with the note that the route is too long for a QR code (spec "Route link from the planner"; design risk "Long routes"). Actual: no Copy link, no downloads, no "Open a route file", no places switch; console `Uncaught String`. The QR library throws a string, which Kotlin's `catch (Throwable)` does not catch.

### B3 Switching the network zooms the map out to all countries (O6)
Steps: zoom to a town, switch Cycling → Hiking (or back). Expected: the map stays where it is (the apps keep the camera). Actual: the map fits the whole network area, knots disappear behind the "zoom in" notice, and the visitor has to find the place again.

### B4 The map notice is cut off at phone width (O7)
Steps: open /nl/plan/ 390 px wide. Expected: the notice readable within the map. Actual: one unwrapped line wider than the map, cut off at both sides, lying over the zoom-in button.

# Findings summary

Stopping reason: the main areas of the charter were each touched once and four reportable defects found; the remaining candidates (below) need other tools or a real phone. Elapsed time is unreliable: the environment clock read 18:05 at the start and 18:09 near the end, which can't match the work done — timing not reported.

Explored on the live site in desktop Chrome (dark appearance): first view, search, planning by clicks, a place as route point, place card, undo, round trip, language switch, network switch, hand-off outputs (file names and contents, GPX, link), opening files (drop and chooser), a very long route, phone width (in an iframe), keyboard order.

Confirmed defects: B1 drop broken, B2 long routes lose the hand-off section, B3 network switch loses the map position, B4 notice overflow at phone width.

Product observations (not spec violations): O6 one click on the other network discards the route without Undo; O8 no keyboard way to add knots; O3 the place stop shows a flag in the panel but the place disc on the map; O4 GPX names a place stop "Stop" (core, shared with the apps); O10 a waypoints-only file always "differs" (core, shared); O7 the header takes 194 px on a phone.

Worked as expected: first view and zoom notice (O1–O2), planning and courses (O3), place card with opening status (O3), route file and GPX contents (O4), language switch keeps the route (O5), round trip and undo (O12), file chooser (O10).

Not covered: light appearance, real phone and touch, Firefox and Safari, route links opened from another device, a click far from the network, unreachable legs, hiking route points, places switch off, search failure, slow networks.

# Follow-up (after the session)

The maintainer asked to fix B1–B4 and to keep a route per network (answering O6). Fixed in the change add-web-planner and re-checked in Chrome against a local build with the real data: a dropped file opens (B1); the 400-waypoint route keeps Copy link, downloads and "Open a route file" with the too-long note (B2); switching networks keeps the map on Zutphen (B3); the notice wraps within the map at 390 px (B4); switching back to cycling brings back its route with Undo.
