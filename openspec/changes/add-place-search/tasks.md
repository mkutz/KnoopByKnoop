# Tasks

## 1. Core

- [ ] 1.1 `PlaceSearch.requestUrl(query, center, coveredArea, language)` with `limit`, `lang`, `bbox` and bias; tests for the URL, including an unsupported language
- [ ] 1.2 `PlaceSearch.parse(json)` into `PlaceHit`s with name, description and coordinates; tests with recorded Photon responses for "Station", "Address", "Business" and "No hits"
- [ ] 1.3 Merging duplicates (same name and town within 500 m); test with the recorded "Utrecht Centraal" response giving one hit
- [ ] 1.4 The covered area from the installed graph's extent; test that a hit outside it is not returned ("Place outside the covered area")

## 2. Android

- [ ] 2.1 Search field in the top bar; hit list in place of the waypoint sheet while searching; 300 ms debounce, 3 characters minimum, cancelled requests, `User-Agent`
- [ ] 2.2 Tapping a hit: camera to zoom 14 at the hit, marker layer under the knots; clearing removes it; the route stays
- [ ] 2.3 Disabled field without internet access; "not available right now" on errors
- [ ] 2.4 Photon attribution in the info sheet; `./gradlew :apps:android:assembleDebug` passes

## 3. iOS

- [ ] 3.1 Search field, hit list, debounce, cancellation and `User-Agent` as on Android
- [ ] 3.2 Tapping a hit, marker and clearing as on Android
- [ ] 3.3 Disabled field offline, error message, attribution; the iOS app builds

## 4. Checks

- [ ] 4.1 Manual checks in `manual-checks.md` on the Android emulator, iOS by the maintainer: "Station", "Address", "Business", "No hits", "Same name in several towns", "Place outside the covered area", "Tapping a hit", "Removing the marker", "Airplane mode", "Internet access is back", "Search service unavailable"
- [ ] 4.2 `./gradlew check` and the iOS app build pass locally and in CI
