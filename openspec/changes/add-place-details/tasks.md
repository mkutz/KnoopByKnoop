# Tasks

## 1. Shared square file and store (refactor, no behavior change)

- [x] 1.1 Pull the square container out of `PathFile`/`PathStore` into `SquareFile`, `SquareStore` and `SquareDownloader`, parameterised by magic, store directory, byte limit and index entry, with `PathIndexEntry` serialising to the same JSON. Keep the path file's bytes and format version 2. `PathStoreTest`, `NetworkIndexTest`, the pipeline's `MainTest` and `VerifyTest`, and `RealRideTest` pass unchanged; `./gradlew check` passes.

## 2. Core: kinds, contacts and the places file

- [x] 2.1 `PlaceKind.CAMPSITE` (`campsite`/`camp_site`) and `PlaceKind.LODGING` (`lodging`/`hotel`, `motel`, `hostel`, `guest_house`), colours chosen so `Distinct from knots` passes. In `PlaceKindTest`: "Campsites near Renesse" (both kinds match their subclasses and have their own colour), "Motorhome sites and holiday homes" (`caravan_site` and `chalet` match no kind). Check against a stored z14 tile near Renesse in `BaseMapPlacesTest` that the classes and subclasses are there.
- [x] 2.2 The OSM tags of each kind's subclasses in `PlaceKind`, with a test that every subclass matched on the map has its tag, and "Places that are not highlighted" (a pharmacy and `tourism=chalet` match no kind).
- [x] 2.3 `PlaceDetails`: website, phone and opening hours as tagged; `displayWebsite` (host without scheme and trailing slash), `websiteUri` (adds `https://`), `dialUri` (first of `;`, digits and a leading `+`, drops `(0)` after a country code). Tests: "Website without a scheme", "Several numbers", plus the phone formats seen in the Zeeland sample.
- [x] 2.4 `PlaceFile`: the per-square encoding (feature id, flags, website, phone, opening hours) on `SquareFile` with magic `KBKL` and its own format version, `UnsupportedFormatVersionException` on a mismatch. Round-trip test, and "Unsupported place details format" (an unknown version is not used, path data unaffected).
- [x] 2.5 `NetworkIndex.places`: an optional entry like `paths`. Tests in `NetworkIndexTest`: "An earlier app reads the index with place details" (an index with `places` parses for graphs, geometry and paths, also with a decoder that doesn't know the key) and "An index without place details".
- [x] 2.6 The places store and downloader: 10 MB limit, LRU, the current route's squares kept, checksum per square. A function for the squares to load (both network types, own `deletedFor`, nothing while navigating for a tap) and one for a tapped place's square. Tests in a `PlaceStoreTest` with the fakes of `PathStoreTest`: "Cycling route planned at home", "Hiking route", "Changing the route", "Navigating away from the route", "Without network access", "Café far from the route", "Square already installed", "Tap while navigating", "Corrupted square of place details", "Newer place details published", "Storage limit for place details", "Deleting the place details", "Deleting place details with a route planned".
- [x] 2.7 `MapPlace.featureId` and the lookup of a place's contacts by feature id in the installed square of its position. Tests: "Campsite with website and phone", "Only a phone number", "No contacts", "Details not installed", "Contacts while navigating" (the lookup doesn't depend on navigation).
- [x] 2.8 `PlaceDetails.openingStatusAt`: a place's status at a moment from its tagged hours, worded by the core's ferry wording (`RouteFerry.statusText`, already shared by both apps), so the ferry list stays unchanged. Tests: "Café open now", "Restaurant opening later", "Restaurant closed on its rest day", "Lunch and dinner", "Hours the app can't be sure of", "Unreadable hours", "Place without opening hours", "Opening hours while navigating".

## 3. Pipeline

- [x] 3.1 Place extraction: way pass (place ways with their node ids), node pass (node places and the ways' node coordinates), feature ids `id*10+1` / `id*10+2`, details from `website`/`contact:website`, `phone`/`contact:phone`/`contact:mobile` and `opening_hours`, ways in every square of their bounding box, places without contacts left out. Tests in `PlaceExtractionTest` with `PbfWriter`: "Campsite with contacts", "Contact keys", "Place drawn from a building", "Café with opening hours only", "Place without contacts", "Places that are not highlighted".
- [x] 3.2 `pipeline places <output> <extracts…>` writing `places.details` and printing the count per kind, how many opening hours `OpeningHours` parses, and the size; `pipeline index` writes the `places` entry when the file is there; `Verify` checks the places file's directory and squares. Tests in `MainTest` and `VerifyTest`.
- [x] 3.3 Run it once locally on the NL, BE and NRW extracts, note size and counts in the design, and check a real building (a restaurant mapped as a way) is found by its tile feature id near its icon. Set the size budget from the measurement (30 MB or tighter).
- [x] 3.4 Weekly workflow: a "Build place details" step that may fail without failing the networks and paths, upload, `check places.details <budget>` in "Check size budgets" that only drops the places on failure, publish. "Failed places build" and "Place details size budget" go into `openspec/checked-by-hand.md` (checked by a workflow run), and the published place details are downloaded again when a run has none, as the release step removes assets a run doesn't have.

## 4. Android

- [x] 4.1 The two icons in `PlaceIcons.kt` (Material Symbols `camping`, `hotel`) and their names in `strings.xml` (en, de, fr, nl). Emulator screenshot near Renesse for "Campsites near Renesse", light and dark.
- [x] 4.2 `placeAt()` reads the feature id into `MapPlace`; `loadPlaceDetails()` next to `loadPathData()` in `AppViewModel` for the route and for a tap while planning; the card's state observes the store so contacts appear when the square arrives.
- [x] 4.3 The card: website and phone rows in `PlaceOfferMenu` with `uriHandler.openUri` and `ACTION_DIAL`, and the opening row with the status from the core refreshed every minute and the tagged hours; content descriptions for TalkBack. Emulator checks: "Campsite with website and phone", "Opening the website", "Calling a hotel", "Details arriving after the tap", "Contacts with a screen reader", "Closing while the card is shown", "Opening hours with a screen reader".
- [x] 4.4 Info sheet entry for place details after the ways, with size, date, explanation in four languages and delete button. Emulator checks: "Opening the info sheet with place details", "No place details installed", "Explaining place details", "Place details delete button with a screen reader".

## 5. iOS

- [ ] 5.1 The same as 4.1 to 4.4 in `PlaceIcons.swift` (`tent.fill`, `bed.double.fill`), `NetworkMapView.swift` (`MLNFeature.identifier`), `AppModel.swift`, `PlaceOfferMenu` (`openURL` with the website and `tel:`, the opening row refreshed every minute), the ferry list on the core's status wording, the info sheet and `Localizable.xcstrings`. The project builds; simulator screenshots of the card with contacts and the info sheet entry, through a throwaway UI test, or by the maintainer on the iPhone.

## 6. Wrap-up

- [x] 6.1 Check the privacy policy's wording on downloads around routes and add the tapped place's square while planning if it isn't covered.
- [x] 6.2 `openspec/checked-by-hand.md` lists the device-only scenarios of this change with how they're checked; `manual-checks.md` lists every scenario with how it was checked, plus ride steps for the maintainer: tap a campsite and a café near the route offline on a ride, call one, open one's website, and compare a café's opening status with its door. A CHANGELOG entry goes under Unreleased (create the section if missing).
- [x] 6.3 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass, then `./gradlew checkScenarios -Pchange=add-place-details`.

## Workflow follow-up

- Whether a place is open when the rider gets there (like the ferries ahead), cuisine and tents allowed can follow as their own changes on the places file (a new format version).
- Multi-day tours (version 2.0) can search the places file for campsites near a route.
