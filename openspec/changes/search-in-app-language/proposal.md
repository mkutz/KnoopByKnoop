# Proposal

## Why

The maintainer noticed on the iPhone on 2026-11-09 that the places found are named in English: searching "Köln" or "Nederland" gives "Cologne" and "Netherlands". Searching in any language should stay, but the names should be in the app's language.

The iPhone asks the search for the system's language (`Locale.current`), not the app's language chosen in the info sheet or the per-app setting; on this iPhone that was English. Android asks for `Locale.getDefault()`, which follows the app's language only as far as the system applies it.

## What Changes

- Both apps ask the search for the app's language, the same one their own texts are in.
- The search service, Photon, names places in German, English and French. For Dutch, and so far for any other language, it gives the names used on the spot: "Köln" rather than the Dutch "Keulen", "Bruxelles - Brussel" for Brussels. Getting Dutch names would need another service; out of scope.
- Searching stays possible in German, English, French and the names on the spot, whatever the app's language. Dutch names that differ, such as "Keulen", are not found today either: Photon doesn't know them.

## Out of Scope

- **Dutch names** such as "Keulen", shown or searched: Photon doesn't know Dutch names; local names instead.
- **Names on the map:** the map's labels are not touched.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `place-search`: a new requirement that hits are named in the app's language.

## Impact

- **iOS:** `PlaceSearchModel` passes `AppLanguage.code`.
- **Android:** `AppViewModel`'s search passes the app's language, as for its other texts.
- **Core:** none; `PlaceSearch.requests` already maps a language to Photon's `lang`, Dutch to `default`.
