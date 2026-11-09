# Design

## Context

- `AppModel` is one `ObservableObject` with about 25 `@Published` properties, among them `searchQuery` and `searchResult`. `PlannerView`, `WaypointSheet`, `MessageAlert`, `InfoSheet` and `PlaceSearchView` observe it. A change of any property renders all of them; `PlannerView` then updates `NetworkMapView`, whose `updateUIView` touches the map's sources and buttons.
- Since `search-in-panel`, the `WaypointSheet` stays in the hierarchy under the open search, so it renders with each character too.
- The debug build's `DebugTiming` log notes main-thread stalls over 100 ms; per keystroke on the iPhone: 110 to 125 ms.

## Decisions

### A model for the search alone
- `PlaceSearchModel: ObservableObject` holds `query`, `result` and the running request, with `search`, `searchAgain` and `clear`. `AppModel` owns one instance as a plain `let`, not `@Published`, so changes inside it don't reach `AppModel`'s observers.
- `PlaceSearchView` observes the search model; picking a hit calls `AppModel.choose(hit)`, which sets the marker and `showingHit` as now, and tells the search model the hit's name for the field.
- The search model needs the knot locator, the graph's area and the map's centre for its requests; it gets them through a closure from `AppModel`, read when a request starts.
- **Alternative:** `@Observable` (Observation framework) for `AppModel`, which tracks properties per view. It would fix this and more, but it changes the whole model and every view; a separate model is a contained change.

### Measuring
- The `DebugTiming` log's stall threshold is lowered to 50 ms, to see stalls well below the target. The target was 50 ms per keystroke at first; after the maintainer's check (a single stall of 52 ms, typing felt smooth) it is 100 ms, so it isn't optimized past what can be felt, while the 110 to 125 ms from before still count as too slow.

## Risks / Trade-offs

- **[Two models to keep in step]** → Only the hit's name crosses from `AppModel` to the search model, when a hit is chosen.
- **[Other @Published changes still render everything]** → Out of scope; the search is the only one that changes on every keystroke.
