# Tasks

## 1. Pipeline and format

- [x] 1.1 `NetworkExtraction` keeps the ferry tags of ferry ways and ferry relations and assigns them per connection; tests for "Ferry with times and fee", "Details on the ferry relation" and "Ferry without details"
- [x] 1.2 `GraphFile` format version 2 with the ferry section; reads versions 1 and 2; tests for round trip and "Older graph files"
- [x] 1.3 The build logs ferry connections with and without operating times and fees; the bundle check accepts versions 1 and 2

## 2. Core

- [x] 2.1 `NetworkGraph.ferries` and `RouteDescription.Complete.ferries` with the positions in the knot list; tests for "Route over a ferry", "Several ferries", "Route without ferries"
- [x] 2.2 A fee and times text per ferry (charge, free, fee, unknown); tests including "Ferry without known times"

## 3. Apps

- [x] 3.1 Android: ferry badge between knots in the route summary and the "Ferries on this route" list with link
- [x] 3.2 iOS: the same
- [x] 3.3 Both apps build; `./gradlew check` passes

## 4. Data and checks

- [x] 4.1 Run `update-network-data`; record the logged ferry coverage in `manual-checks.md`
- [x] 4.2 Manual checks in `manual-checks.md` with a real route over a ferry, on the Android emulator and by the maintainer on iOS: "Route over a ferry", "Ferry without known times", "Several ferries", "Route without ferries", "Offline"
- [x] 4.3 CI passes

## 5. Found during the manual checks

- [x] 5.1 Bundled graphs in a newer format replace installed ones of the same data date: `GraphFile.formatVersion` and the format versions in `NetworkCatalog.bundledToInstall`; tests for "Bundled graph in a newer format" and for not installing the same data and format again; both apps pass the versions; checked on the Android emulator
- [x] 5.2 CI passes
