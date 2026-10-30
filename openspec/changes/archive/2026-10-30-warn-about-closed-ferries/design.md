# Design

## Context

See `proposal.md` for why. The pieces are in place from earlier changes:
- `Navigation` (core) follows the route knot by knot and returns `Notice`s with every fix; `NavigationService.kt` and `Navigator.swift` speak them. It gets the route's knots and the `NetworkGraph` at start, so it can find the ferries (`ferryBetween`) and the leg lengths (`neighborsOf`).
- `RouteFerry.statusAt(LocalMoment)` and `OpeningHours.statusAt` evaluate the operating times for a local moment, with the next change, and give no status when the data cannot tell (`show-ferry-open-now`).
- The apps convert an instant into the ferry's local time (`Europe/Amsterdam`): `FerryClock.kt` on Android, `RouteFerries.ferryMoment` on iOS.
- The ferry list stays in the panel while navigating, below the number to follow, and ticks every minute while shown.

The core stays free of platform APIs, and navigation must not sample the location more often than it does today.

## Goals / Non-Goals

**Goals:**
- Warn early and only when the operating times are certain; no warning beats a wrong one.
- One estimate for the spoken warning and the ferry list.

**Non-Goals:**
- Precise arrival times: the estimate is labeled "around" and rounded.
- Any change to sampling, precision or battery use.

## Decisions

### Ferry warnings are notices of `Navigation`
A new `NoticeKind.FERRY` joins the existing notices, so the apps speak it like any other notice without new plumbing, and the core tests cover it like "Passing a knot". `Navigation` keeps the ferries ahead with their position on the route, which ones it has warned about, and the progress it needs for the pace.
*Alternative:* a separate watcher in the apps. That would duplicate the logic on two platforms.

### Local time handed in by the apps
`Navigation.start` takes a function from epoch milliseconds to `LocalMoment`; the apps pass their existing `Europe/Amsterdam` conversion. The arrival instant (fix time plus estimated duration) is converted only then, so a time change between now and the arrival is handled by the platform.
*Alternative:* the EU summer time rule in the core. Small, but a second time zone implementation next to the platforms', and `show-ferry-open-now` already decided the platforms convert.

### Pace from route progress, not from the track
The pace is the route distance between the knot where navigation started (or first passed) and the knot passed last, divided by the time between those passes. It includes stops at traffic lights and breaks, as the rest of the ride will too, and needs no extra fixes. Until 2 km are covered, the assumed pace applies: 16 km/h cycling, 4.5 km/h hiking, by the graph's network type.
*Alternative:* moving speed from the ridden track. It ignores breaks and so estimates too early, which is the wrong direction for a warning; and the track lives in the apps.

### Distance to a ferry
The route distance from the rider to the ferry is the remaining part of the current leg (the straight distance from the fix to the next knot, capped at the leg's length), plus the lengths of the legs up to the ferry's leg, plus half the ferry's leg: the graph does not tell where on its connection the crossing is. Off route, the estimate is not renewed until the rider is back on it.

### When it is evaluated
At navigation start and whenever a knot is passed (including skipping ahead), for every ferry ahead not yet warned about. Each evaluation walks at most a year of rules per ferry (bounded in `show-ferry-open-now`), a few times per ferry per ride. A ferry counts as passed when the knot after it is passed.

### Warning rules and texts
The comparison uses the exact estimate; the text shows it rounded to five minutes.
- Open at arrival and the next closing within 15 minutes after it: "The ferry Veer Broekhuizen closes at 19:00, shortly after you get there around 18:50".
- Closed at arrival and opening again later on the arrival day: "The ferry Veer Broekhuizen is closed when you get there around 06:15, it opens at 07:00".
- Otherwise closed at arrival, after closing between now and then on the arrival day: "The ferry Veer Broekhuizen closes at 19:00, before you get there around 19:15".
- Otherwise closed at arrival: "The ferry Veer Broekhuizen is closed when you get there around 12:30, it opens on 1 April at 07:00", with "opens tomorrow at" and "opens on D Month at" as in the ferry list, and without the opening when there is none within a year.
- Without a name, "The ferry" alone. No status at arrival, or a closing time that is not certain: no warning.
Spoken and shown text are the same; the times are spoken as the platforms read "19:15". The ferry list shows "You get there around 19:15" below the ferry's title, in the error color when warned about.

### Arrival in the ferry list
`NavigationState` already carries the `Navigation` to the panel. The ferry list asks it for the rounded arrival and the warned state of each ferry by its route position, as computed at the last knot passed; it does not re-estimate every minute.

## Risks / Trade-offs

- [The assumed pace is far off for some riders] → Only for the first 2 km, after which the own pace takes over; the warning says "around".
- [Long breaks make the pace too slow, and the app warns although the rider would make it] → Acceptable: a false warning costs a glance at the times, a missing one a detour. The ferry list shows the times to check.
- [The middle of a long ferry leg is far from the crossing] → The error is at most half a leg, usually a few minutes; ferry legs with long approaches are rare.
- [OpenStreetMap times out of date] → Same as the ferry list: "according to OpenStreetMap" in the list heading, the tagged times stay visible.
- [A warning while another notice is spoken] → Notices queue on both platforms already; the ferry warning comes after the knot notice of the same fix.
