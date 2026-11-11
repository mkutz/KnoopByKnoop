# Tasks

## 1. Pipeline: places

- [x] 1.1 Read `place=village|town|city` nodes and areas with their names (`name`, `name:en/de/nl/fr`), and assign each knot its place: inside an area, or within 1 km of a village, 2 km of a town or 4 km of a city, the nearest relative to its reach. Tests in the pipeline:
  - "Knot in a town"
  - "Knot in the open"
  - "Only a hamlet nearby"
  - "Names in other languages"
- [x] 1.2 Measure on the real extracts: the share of cycling and hiking knots with a place, and the places' count. Give the maintainer a sample of 20 random knots with their place to judge against the map. **The maintainer decides the reaches before the format is fixed.**
  - 2026-11-11, published data of 2026-11-08's extracts: cycling 10,732 of 20,584 knots in a place (52 %), 4,877 places; hiking 23,185 of 50,460 (46 %), 2,984 places. The maintainer judged the sample and kept the reaches (1, 2 and 4 km). The second build with borders: cycling 1,772 crossings on 1,155 connections, hiking 1,750 on 1,210; all regions complete.

## 2. Pipeline: borders

- [x] 2.1 Generalise `Border` to regions: the Dutch provinces, the Belgian provinces and Brussels, North Rhine-Westphalia, each with its country and names; fail on an incomplete one. Keep the sign regions working from the same borders. Tests:
  - "Incomplete border"
  - the sign region tests unchanged
- [x] 2.2 The crossings of each connection's course, bisected to 5 m. Tests:
  - "Crossing into Belgium"
  - "Crossing a province border"
  - "Within one province"
  - "Beyond the covered area"

## 3. Core: file and model

- [x] 3.1 `Place`, `Knot.place`, `Crossing`, `Connection.crossings`. Write and read the two new sections in `GraphFile`. Tests in `GraphFileTest`:
  - a round trip with places and crossings
  - "Graph files without places" and "Graph files without borders" (an old file)
  - a reader that stops before the sections, as apps released before do
- [x] 3.2 `Texts.placeName` and the border line's text in four languages. The crossings along a route in riding order, also on a connection ridden backwards. Tests:
  - "Place name in the app's language"
  - "Crossing into another country" and "Crossing a province border" (texts in English and German)
  - a connection ridden backwards
  - back and forth along a border: crossings less than 500 m apart on one connection merge into one, and none shows when the route ends up where it was
- [x] 3.3 The size of the cycling graph stays below 5 MB, with the real data (the "Size budget" check).

## 4. Android

- [x] 4.1 The waypoint list with " · place", and the coming knots list with places, border lines and their TalkBack texts. Strings for "Border" in four languages. Check on the emulator with the new data: "Place in the waypoint list", "Waypoint in the open", "Places in the list", a border crossing on a route from Limburg (NL) into Limburg (BE), and TalkBack's texts. Screenshots in the change folder.

## 5. iOS

- [x] 5.1 The same in `PlannerView.swift` and `ProgressPanel.swift`, with the strings in the String Catalog. The project builds; simulator screenshots of the same routes and VoiceOver's labels.

## 6. Wrap-up

- [ ] 6.1 Publish the new data with the pipeline workflow, and check that the published apps (0.14.0) still read it.
- [x] 6.2 `manual-checks.md` with the device scenarios and the maintainer's steps (does the place name match the town sign, on a ride). Put a CHANGELOG line under Unreleased. `./gradlew check` and `npx @fission-ai/openspec validate --all` pass. Finally run `./gradlew checkScenarios -Pchange=add-place-names-and-borders`.
