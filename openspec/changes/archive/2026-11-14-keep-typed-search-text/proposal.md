# Proposal

## Why

On Android, typing quickly into the place search field loses characters. Over `adb shell input text`, "Blue Lotus Nieuw-Lekkerland" became "Blue Lotus NLekkerlan", and "Blue Lotus" became "BlueLotu". A fast typist or a pasted word can lose characters the same way, and then the search looks for the wrong text.

This was found on 2026-11-14 while scripting the store screenshots. `automate-store-screenshots` is paused for this fix, because its place screenshot should come from the search.

## What Changes

- The Android search field keeps every character typed, however fast. It copies the stored query back into the field only when the query changed from somewhere else: Clear, choosing a hit, or reopening the search with text left from before. It no longer copies back the echo of its own typing, which arrives a frame late.
- iOS doesn't change: `PlaceSearchModel.search` stores the query synchronously, so there is no late echo.

## Out of Scope

- **Debouncing or rate-limiting searches.** How often the app asks Photon while typing stays as it is.
- **Other text fields,** e.g. the info sheet's.
- **Resuming `automate-store-screenshots`.** It continues after this change is archived.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `place-search`: a new requirement that text typed into the search field is kept as typed, however fast.

## Impact

- **Android:**
  - `apps/android/src/main/kotlin/com/knoopbyknoop/android/PlaceSearchField.kt`
  - a small pure function deciding when the stored query replaces the field's text, with its unit test in `apps/android/src/test`
- **iOS, core:** none.
