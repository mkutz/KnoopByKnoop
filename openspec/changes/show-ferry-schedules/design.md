# Design

## Context

See proposal.md for the motivation.

- **Pipeline:** `NetworkExtraction` knows which routes are ferries: relations with `route=ferry`, and routes with a way tagged `route=ferry`. It uses that only to keep knots on both banks apart. Way tags other than `route` are dropped, and relation tags are dropped after `acceptRelation`.
- **Graph format:** a `Connection` is `fromId`, `toId` and `lengthMeters`. `GraphFile` writes format version 1 and rejects every other version.
- **Display:** route descriptions (`MapFeatures.describe`) list the route's knots with their roles. Both apps render them as badges in the route summary.

## Goals / Non-Goals

**Goals:**
- Ferry details reach the apps through the existing weekly publication, with no new file or download.
- Apps that already read version 1 files keep working until they are updated.

**Non-Goals:**
- Interpreting `opening_hours`. It is shown as OpenStreetMap states it, e.g. "Apr-Oct: Mo-Su 07:00-19:00". Parsing it reliably needs a full implementation of the syntax, and showing it raw is understandable for the typical cases on ferries.

## Decisions

### Collecting ferry tags per connection
`NetworkExtraction` keeps the tags `name`, `operator`, `opening_hours`, `seasonal`, `fee`, `charge` and `website` of ferry ways (`route=ferry`) and of ferry route relations. For a connection that includes a ferry, each detail comes from the first ferry way of the connection that has it, otherwise from the route relation. Several ferry ways in one connection are rare, and taking the first one keeps the result predictable.

*Found during apply:* ferry lines are often mapped as relations of their own (`type=route`, `route=ferry`, not part of a node network) that carry the times and fees, while the node network route only contains the ferry way. The extraction therefore also keeps the ferry tags of such relations by their member ways, and uses them after the ferry way and the node network route itself.

The build logs how many ferry connections have operating times and fees, so the value of the feature can be judged from real data: OpenStreetMap coverage is not known yet.

### Graph format version 2: an optional ferry section
After the connections, version 2 writes the number of ferries, then for each its connection index and the seven details as strings, with an empty string for a missing detail. `GraphFile.read` accepts versions 1 and 2. Version 1 files have no ferries. `FORMAT_VERSION` becomes 2 for writing, and the bundle check accepts both versions.

The ferry details live on the graph as `NetworkGraph.ferries: Map<Connection, Ferry>`, not as a field of `Connection`. `Connection` is a data class that is used as a key in many places (courses, deduplication), and most connections are not ferries.

*Alternative:* a separate ferry file next to graph and geometry. That would mean another download and another index entry for a few kilobytes. Rejected.

*Rollout:* the apps and the pipeline change in the same commit. The next weekly publication writes version 2, which the updated apps read. Apps built before that cannot read the new files and show "needs a newer version of the app", which only affects development builds.

### Ferries in the route description
`RouteDescription.Complete` gets `ferries: List<RouteFerry>`. A `RouteFerry` is the ferry with the positions of the two knots it connects in the knot list. The apps draw a small ferry badge (⛴) between those two knots in the summary. Below it, they show a "Ferries on this route" list with:
- the name, or "Ferry" without a name
- the operating times, or "Operating times not known, check before you go"
- the fee: the `charge`, "free" for `fee=no`, "fee" for `fee=yes`, nothing if unknown
- the website as a link

Without `opening_hours`, a `seasonal` tag other than `no` is mentioned in the note, e.g. "Seasonal (summer). Operating times not known, check before you go". A route that crosses the same ferry twice, e.g. a round trip, lists it at each crossing. The texts are built in the core (`RouteFerry`), so both apps show the same.

## Risks / Trade-offs

- **[Risk] Few ferries have times or fees in OpenStreetMap.** → The list still shows which ferries the route uses, and says when times are unknown. The build's log shows the coverage, and missing data can be added in OpenStreetMap.
- **[Trade-off] Raw `opening_hours` can look technical** (e.g. "PH off"). → Accepted for now. Formatting it nicely is a follow-up once real examples from the log are known.
