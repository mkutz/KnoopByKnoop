# Proposal

## Why

Friends of the maintainer asked to export tours as GPX. With a GPX file, a route planned in Knoop by Knoop can be followed on a bike computer (Garmin, Wahoo), opened in other apps (Komoot, OsmAnd), archived, or sent to someone without the app.

## What Changes

- The core writes a planned route as GPX 1.1: the route's line along the knot network as a track, and every knot passed as a waypoint named with its number, start, stops and end marked.
- The planner panel gets an "Export GPX" icon button next to Back to start and Clear. It opens the system share sheet with a `.gpx` file, to save it to files or send it to another app.
- Export is offered for a complete route whose line along the network is known, also while offline; not while navigating.

Out of scope:
- Importing GPX files.
- Other formats (KML, FIT, TCX) and direct uploads to Garmin Connect, Komoot or Strava.
- Exporting the ridden track while navigating.
- Ferry times or other notes in the file.

## Capabilities

### New Capabilities

### Modified Capabilities
- `map-planning`: new requirement "Export the route as GPX"; "Clear actions in the planner panel" gets the Export GPX icon button

## Impact

- `modules/core`: a GPX writer from a route, its knots and its course, with tests
- `apps/android`: the icon button in the planner header, a `FileProvider` for the cache directory, the share intent
- `apps/ios`: the icon button in the planner header, a temporary `.gpx` file shared with `UIActivityViewController`
