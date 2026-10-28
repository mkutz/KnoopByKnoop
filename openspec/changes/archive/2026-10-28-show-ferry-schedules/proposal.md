# Proposal

## Why

Some ferries on the knooppunt networks only run in summer, on weekends or during the day, and many of them charge a fee. A route that relies on a ferry can be useless on the day of the trip. When the route is planned, the app does not know the day of the trip (people plan in winter for spring), so it cannot warn about a specific date. It can show which ferries the route uses and when they run, so the user can decide.

## What Changes

- The pipeline keeps the details OpenStreetMap has about each ferry connection: name, operator, operating times (`opening_hours`, `seasonal`), fee (`fee`, `charge`) and website.
- The graph file format gets a version 2 that carries these details. The apps read versions 1 and 2.
- An app update installs its bundled graph over an installed one with the same data date when the bundled one is in a newer format, so installed apps get the ferries right away instead of at the next daily sync.
- The route's knot list marks ferries between the knots they connect, and lists each ferry on the route with its operating times and fee, or says that OpenStreetMap has none.

Out of scope: checking a ferry against a trip date, warnings, parsing `opening_hours` into a calendar, data from ferry operators, ferries that are not part of a node network.

## Capabilities

### New Capabilities

### Modified Capabilities
- `network-data`: ferry details are extracted and published with the graph
- `map-planning`: the route's knot list shows the ferries on the route with their details
- `network-sync`: bundled graphs in a newer format replace installed ones of the same data date

## Impact

- `pipeline`: collecting the tags of ferry ways and ferry relations per connection; logging how many ferries have times and fees
- `modules/core`: `GraphFile` format version 2 with an optional ferry section; ferry details on the graph; route descriptions with the ferries per leg
- `modules/core`: `NetworkCatalog.bundledToInstall` also compares the graphs' format versions, read from the first bytes of the files
- `apps/android`, `apps/ios`: a ferry marker between knots in the route summary, and a list of the route's ferries
- Graph files grow slightly (a few hundred ferries with a few short strings each)
