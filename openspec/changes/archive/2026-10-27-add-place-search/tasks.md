# Tasks

## 1. Core

- [x] 1.1 `PlaceSearch.requestUrl(query, center, coveredArea, language)` with `limit`, `lang`, `bbox` and bias; tests for the URL, including an unsupported language
- [x] 1.2 `PlaceSearch.parse(json)` into `PlaceHit`s with name, description and coordinates; tests with recorded Photon responses for "Station", "Address", "Business" and "No hits"
- [x] 1.3 Two requests (prominent and nearby) merged with important places first, see design; tests for "Town far away" and "Same name in several towns". Merging duplicates (same name and town within 500 m); test with the recorded "Utrecht Centraal" response giving one hit
- [x] 1.4 The covered area: the `bbox` from the installed graph's extent, and only hits within 3 km of a knot (the `bbox` alone let "Paris" shops in Lower Saxony through, see design); test that a hit outside it is not returned ("Place outside the covered area")

## 2. Android

- [x] 2.1 Search field in the top bar; hit list in place of the waypoint sheet while searching; 300 ms debounce, 3 characters minimum, cancelled requests, `User-Agent`
- [x] 2.2 Tapping a hit: camera to zoom 14 at the hit, marker layer under the knots; clearing removes it; the route stays
- [x] 2.3 Disabled field without internet access; "not available right now" on errors
- [x] 2.4 Photon attribution in the info sheet; `./gradlew :apps:android:assembleDebug` passes

## 3. iOS

- [x] 3.1 Search field, hit list, debounce, cancellation and `User-Agent` as on Android
- [x] 3.2 Tapping a hit, marker and clearing as on Android
- [x] 3.3 Disabled field offline, error message, attribution; the iOS app builds

## 4. Checks

- [x] 4.1 Manual checks in `manual-checks.md` on the Android emulator, iOS by the maintainer: "Station", "Address", "Business", "No hits", "Same name in several towns", "Place outside the covered area", "Tapping a hit", "Removing the marker", "Airplane mode", "Internet access is back", "Search service unavailable"
- [x] 4.2 `./gradlew check` and the iOS app build pass locally and in CI
