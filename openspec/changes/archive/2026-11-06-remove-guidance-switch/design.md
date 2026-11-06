# Design

## Context

- **The switch** is a stored boolean `guidance`: `KEY_GUIDANCE` in Android's settings and `"guidance"` in iOS's `UserDefaults`. It feeds `PathSquares.courseToLoad(enabled, network, course)`, the gate before every path download. Turning it off cancels downloads and calls `PathStore.clear()`.
- **Downloads** start 2 seconds after the route last changed, in `AppViewModel.loadPathData` (a `snapshotFlow` over the switch, network access, session and layers) and `AppModel.loadPathData` on iOS.
- **The info sheet** shows the switch, its explanation and the path data's date in one block, above "Knot network data", where the cycling and hiking networks are listed, each with its date and a retry button.
- **`InstalledSquare`** already stores each square's compressed `size`. The directory is cached on its own, about 290 kB for the data of 3 November.
- **Release state:** the switch is only in Unreleased. 0.9.0 has no path data at all, so nothing needs migrating for users.

## Goals / Non-Goals

**Goals:**
- Path data downloads for every complete cycling route, with no setting.
- One place to see and delete the downloaded ways.

**Non-Goals:**
- Any change to what is downloaded, when, or how much is kept.

## Decisions

### The gate keeps its other conditions
- `courseToLoad` loses `enabled`. It still returns `null` for hiking and without a complete course.
- It gets a new parameter: the course the ways were deleted for, see below.

### Deleting holds off until the route changes or navigation starts
- The delete action cancels a running download, calls `PathStore.clear()`, and remembers the current route's course as "deleted for".
- `courseToLoad` returns `null` while the course equals that one, so the panel shows nothing about path data, as on a hiking route.
- A changed route has another course, so the hold ends by itself. Starting navigation clears it, and the download of "Starting before the download is done" runs.
- The hold is not stored: after a restart, the planned route's squares download again. That's acceptable, and simpler than another setting.
- **Alternative:** downloading the current route's squares again at once. Then deleting looks broken, as the size comes back seconds later.
- **Alternative:** keeping the current route's squares when deleting. Then delete doesn't mean "all", and the size never reaches zero while a route is planned.

### The entry in the info sheet
- The section "Knot network data" becomes "Downloaded data", in all four languages, with three rows: cycling, hiking, and "Ways for guidance".
- The ways' row shows "Data of …" and the size in MB with one decimal, or "None downloaded". A delete button, as a text button where the networks show "Retry", only while squares are installed.
- The explanation follows the rows as body text on Android and as the section footer on iOS. It keeps the switch's wording without "For this": "Guides you back to the route along suitable paths, also without a connection. The app downloads the ways around planned cycling routes from GitHub, so GitHub can see the rough area of a route, but never where you are."
- **Size:** the sum of `InstalledSquare.size` plus the cached directory, from a new `PathStore.installedBytes()`. It is read when the sheet opens and after a download or delete.
- **Screen readers:** the button is labelled "Delete downloaded ways", while it shows "Delete".
- **No confirmation dialog:** the ways download again on their own, so a mistake costs a few MB.

### The old setting is left alone
The apps stop reading `guidance`. Deleting the key on the maintainer's phones isn't worth code.

### Renaming the guided way requirement
OpenSpec can't drop a scenario from a modified requirement, so "Guided way while off route" is replaced by "Guided way back to the route". The code comments that cite the old name change with it.

## Risks / Trade-offs

- **[GitHub sees the rough area of every cycling route, for every user]** → Squares are about 7 × 4 km, chosen from the route alone. That's less than the base map tiles reveal, and the policy says so.
- **[Mobile data]** → 1 to 6 MB per measured route, once per area, kept up to 50 MB.
- **[GitHub rate limits or bandwidth as downloads go up]** → Release assets are served by GitHub's CDN. One square is one range request. A rider planning the same area again downloads nothing.
- **[Store declarations]** → The Play data safety form and App Store labels were filled in before path data existed. The maintainer checks them before the release (follow-up, not code).

## Migration Plan

None for users, as the switch was never released. On the maintainer's phones, the stored `guidance` value is ignored, and path data starts downloading for the planned route.
