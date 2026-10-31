# Proposal

## Why

An Android tester likes dark mode but finds it hard to find their way on the dark map: "Schwierigkeiten mich zurechtzufinden und zu sehen, wo genau der Weg eigentlich her läuft". The dark map is OpenFreeMap's "dark" style: 47 layers against the light "liberty" style's 111, with almost black water, and without many of the paths, parks and points of interest the light map shows. The tester also wished to switch the map's brightness themselves.

## What Changes

- The dark map becomes a dark variant of the light "liberty" style: the same layers and details, with dark land, blue water, green parks and woods, and paths and roads light enough to follow. It is generated from liberty by a script in the repository and shipped with both apps.
- The info sheet gets an appearance setting: System (default), Light, Dark. It applies to the whole app, as the map, the panel and the controls stay all light or all dark together; it is kept across starts.

Out of scope:
- Changing the light map.
- Map styles beyond light and dark, e.g. a satellite or topographic map.
- An automatic switch by time of day other than the system's own.

## Capabilities

### New Capabilities

### Modified Capabilities
- `map-planning`: "Light and dark appearance" gets the appearance setting and the details of the dark map
- `network-sync`: "Info sheet" offers the appearance setting

## Impact

- `pipeline`: a generator for the dark style from OpenFreeMap's liberty style, with tests; its output `liberty-dark.json` checked in
- `apps/android`: `NetworkMap.kt` loads the shipped dark style, `InfoSheet.kt` the setting, `MainActivity.kt`/`Theme.kt` follow it
- `apps/ios`: `NetworkMapView.swift` loads the shipped dark style, `InfoSheet.swift` the setting, the app's `preferredColorScheme` follows it; `project.yml` bundles the style
