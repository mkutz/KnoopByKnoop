# Tasks

## 1. Reading the times (core)

- [x] 1.1 `OpeningHours.parse`: the syntax listed in the design's context, `null` for anything else; tests with the real values from the published graphs (every distinct value either parses or is knowingly unreadable, e.g. "April-October" for "Unreadable times")
- [x] 1.2 Coverage: the pipeline's ferry log line counts the ferries with readable times; run it on the bundled graphs and record the figure in `manual-checks.md` before going on

## 2. Evaluating the times (core)

- [x] 2.1 Dates without a library: weekdays, Easter, nth weekday of a month, date ranges across the year end; tests
- [x] 2.2 `PublicHolidays` for the Netherlands, Belgium and Nordrhein-Westfalen, with "all", "none" and "cannot tell"; tests for Christmas Day and Good Friday ("Public holiday", "Holiday in only some countries")
- [x] 2.3 Evaluation of a moment: rule order, additional rules, `off`, spans past midnight, trying both answers for selectors that cannot tell, `unknown`/`sunrise`/`sunset`; tests for "Open now", "Opens later today", "Open past midnight", "Open around the clock", "School holidays"
- [x] 2.4 Next change within a year and the status texts in `RouteFerry`; tests for "Opens tomorrow", "Closed for the season", and a time bound for the longest real value

## 3. Apps

- [x] 3.1 Android `RouteFerries.kt`: the status above the tagged times, for the current time in `Europe/Amsterdam`, recomputed every full minute while shown; JVM unit tests of the ferry clock (`FerryClockTest`) for "Phone in another time zone" and "Closing while shown", as the app has no Compose test setup
- [x] 3.2 iOS `RouteFerries.swift`: the same with `TimelineView(.everyMinute)` and `Europe/Amsterdam`
- [x] 3.3 "Offline status": covered by the core computing everything from the graph; check on the Android emulator in airplane mode

## 4. Checks

- [x] 4.1 `./gradlew check`, iOS build and core tests on the iOS simulator in CI
- [x] 4.2 `manual-checks.md` with screenshots: Android emulator for the route over the Broekhuizen ferry and a seasonal ferry; iOS steps for the maintainer
