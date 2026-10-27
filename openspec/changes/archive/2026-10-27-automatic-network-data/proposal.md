# Proposal

## Why

Before planning anything, users have to open a regions screen, pick regions and wait for them to download. That is friction for no benefit: all published networks together are about 6 MB (cycling about 2.7 MB, hiking about 3.5 MB, geometry included). Worse, every region is a graph of its own, so a route from Zeeland into Flanders or from Limburg into Nordrhein-Westfalen cannot be planned at all, although the networks are signposted continuously across the borders.

## What Changes

- The pipeline builds **one graph and one geometry file per network type** from all extracts together. Knots and connections that appear in several extracts at a border become one, so routes cross borders like any other route.
- The apps **download both networks automatically** in the background on first start, cycling first, and keep them up to date without the user doing anything.
- The apps **ship the latest graphs** of both networks (not their geometry, about 1.35 MB of app size), so planning works offline right from the first start.
- **The regions screen is removed.** Until the geometry has arrived, routes are drawn as straight lines with a note that the courses are loading; without network access the download starts by itself once it is back.
- The data date, the update state and the OpenStreetMap attribution move to a small **info sheet** on the map.
- **BREAKING** for the published data: the per-region files are replaced by `cycling.*` and `hiking.*`. No app has been released, so no installed app depends on the old files.

Out of scope: adding countries beyond the Netherlands, Belgium and Nordrhein-Westfalen; splitting the data again (e.g. per country chosen from the location) — the design only keeps room for it; bundling the geometry; navigation; a setting to download only on Wi-Fi.

## Capabilities

### New Capabilities
- `network-sync`: getting the networks onto the device automatically and keeping them current, including the bundled graphs, the note while courses are loading and the info sheet

### Modified Capabilities
- `network-data`: graph and geometry files per network type instead of per region, routes across region borders, an index without regions
- `map-planning`: the map shows the installed network instead of an installed region
- `region-management`: retired; its download, verification and update behavior moves to `network-sync`, choosing and removing regions is dropped

## Impact

- `pipeline`: `build` reads several extracts into one extraction and deduplicates relations found in more than one extract; file names without region
- `.github/workflows/update-network-data.yml`: one build job reading all extracts instead of a matrix per region; size budgets for the merged files
- `modules/core`: `RegionCatalog` and `RegionStatus` give way to a catalog of networks; installing, verifying and update detection stay; route snapshots no longer store a region
- `pipeline` and both app builds: a build step fetches the latest graphs from the `network-data` release, verifies them and bundles them into the apps
- `apps/android`, `apps/ios`: regions screen and its navigation removed; bundled graphs installed and downloads started on app start; courses-loading note and info sheet added
- The old per-region release assets are deleted from the `network-data` release after the first merged publication
