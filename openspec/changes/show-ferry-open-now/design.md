# Design

## Context

See `proposal.md` for why. Since `show-ferry-schedules`, the graph carries each ferry's `opening_hours` as tagged, and `RouteFerry` (core) builds the texts that `RouteFerries.kt` and `RouteFerries.swift` show. That change left interpreting the value out because it needs a real implementation of the syntax.

The values in the published graphs (189 distinct ones, cycling and hiking, data of 2026-10-25) use much of it:
- month and date ranges, also across the year end (`Dec 22-Jan 01 off`), single dates, years (`2024 Apr 09-2024 Apr 26 off`)
- Easter with offsets (`easter +40 days`, `easter -2 days-Oct 31`), nth weekdays (`Mar Su[-1]-Oct Su[-1]`, `Tu[1] off`)
- weekday lists and ranges, `PH` (very common), `SH`, several time spans, `24:00`, spans past midnight (`Fr-Su 06:00-01:00`)
- `24/7`, `off`/`closed`, `unknown`, comments, the `:` after a range, additional rules after `,`
- `sunrise`/`sunset`, and a few values that are not valid at all (`April-October`, `Mo-We; Fr 08:00-18:30`)

The core must stay free of platform APIs and has no date library.

## Goals / Non-Goals

**Goals:**
- A status only when it is certainly right according to the tags; otherwise none.
- Read the syntax the real ferry values use, measured by the pipeline.

**Non-Goals:**
- The complete `opening_hours` specification (e.g. `||` fallback rules, week numbers, variable times).
- School holidays and sunrise/sunset times.
- Formatting the full schedule.

## Decisions

### A parser of our own in the core
`OpeningHours.parse(text)` returns the rules or `null` when the value cannot be read. It covers the syntax listed in the context, nothing more: anything else makes the whole value unreadable, so a half-understood value never gives a status.
*Alternative:* an existing library. The ones known (opening_hours.js, the Java OpeningHoursParser) do not run in Kotlin Multiplatform code for iOS.

### Rules as the specification evaluates them
Rules apply in order. A normal rule (after `;` or first) whose day selectors match a day replaces the earlier rules' times for that day; an additional rule (after `,` that follows a complete rule) adds to them; `off`/`closed` removes its times, or the whole day without times. A rule without day selectors applies every day, a rule without times is open all day. A span past midnight belongs to the day it starts on and continues into the next morning, so evaluating a moment also looks at the day before.

### Three answers, and trying both where the data cannot tell
Each day selector matches, does not match, or cannot tell: `SH` always cannot tell, `PH` cannot tell on a day that is a public holiday in some but not all of the three regions. For a moment, the evaluator tries every combination of the selectors that cannot tell (at most a handful on one day) and gives a status only when all combinations agree. So "Christmas" works everywhere, and Easter Sunday works for the usual `PH,Su` rules, while Good Friday under a `PH` rule gives no status. A matching rule with `unknown`, `sunrise` or `sunset` gives no status for that day.
*Alternative:* treat any `PH` or `SH` in a value as unknown. That would drop the many ferries with `PH` on the 360 days that are no holiday anywhere.

### Public holidays from the date
`PublicHolidays` computes the holidays of the Netherlands, Belgium and Nordrhein-Westfalen for a year: fixed dates plus Easter offsets (Easter by the Gregorian computus, Ascension +39, Whit Monday +50, Corpus Christi +60), King's Day on 26 April when 27 April is a Sunday. The ferry's country is not known (all extracts are read together and ferries cross borders), so a day counts as a holiday when it is one in all three, as none when in none, and as cannot tell otherwise.

### The ferry's local time, given by the apps
The core's status takes a local date and minute in the ferry's region; the apps convert the current instant to `Europe/Amsterdam` (the Netherlands, Belgium and Nordrhein-Westfalen share it) with `java.time` and `Foundation`. Weekdays and date ranges are computed from the date in the core.
*Alternative:* kotlinx-datetime in the core. A dependency for a time zone conversion the platforms already do.

### Next change and its text
From the moment, the evaluator walks forward day by day, up to a year, to the next moment the status changes; if that change is not certain in all combinations, only "Open"/"Closed" is shown. Texts, built in `RouteFerry`:
- "Open until 19:00" for a closing within 24 hours, "Open until 31 October at 19:00" later, "Open" without a closing within a year
- "Closed, opens at 07:00" today, "Closed, opens tomorrow at 07:00", "Closed, opens on 1 April at 07:00", "Closed" without an opening within a year

### Current while shown
Android recomputes the status in the ferry list at every full minute while it is composed (a `LaunchedEffect` loop), iOS with a `TimelineView(.everyMinute)`. Only while the planner shows the list, so there is no cost during navigation with the screen off.

### Coverage in the pipeline log
The ferry log line of `pipeline` `Main.kt` also counts the ferries whose times `OpeningHours.parse` reads. It is measured first: if far fewer than most ferries with times are readable, the parser's scope is revisited before the apps show anything.

## Risks / Trade-offs

- [OpenStreetMap times are out of date or ignore weather and low water] → The status says "according to OpenStreetMap" in the list heading, and the tagged times, including comments, stay visible.
- [A parser bug shows a wrong status] → Unit tests with the real values from the graphs and the spec's scenarios; anything unrecognised gives no status rather than a guess.
- [Walking a year forward every minute] → At most 366 days of a handful of rules per ferry, a few ferries per route, once a minute and only while the list is shown; a test bounds the time for the longest real value.
- [Holidays change] (e.g. a new national holiday) → The table is in one place in the core; a wrong table gives no status at worst on days with a difference between countries, but could give a wrong one on a new common holiday.
