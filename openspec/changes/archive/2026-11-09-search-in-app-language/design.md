# Design

## Context

- `PlaceSearch.requests(query, …, language)` in the core maps a language code to Photon's `lang`: `de`, `en` and `fr` as they are, anything else to `default`, the names on the spot. A test covers `default`.
- iOS passes `Locale.current.language.languageCode`, the system's language; the app's own language is `AppLanguage.code` (info sheet choice, per-app setting, system).
- Android passes `Locale.getDefault().language`; its texts use the language from the app's resources configuration, kept in `AppViewModel.language`.

## Decisions

- **The app's language, from where the texts take it:** `AppLanguage.code` on iOS, the configuration's language as in `AppViewModel` on Android, so hits and texts always agree, also right after the language is changed.
- **Dutch stays `default`:** Photon answers `lang=nl` with an error, and searching "Keulen" finds a butcher, not Köln, in any language: it doesn't know Dutch names at all. Local names are closer to Dutch than English ones for the places a Dutch rider looks for in the Netherlands and Belgium.
- **Alternative:** Nominatim names places in any language (`accept-language`), but allows one request per second and no search while typing; not for this.

## Risks / Trade-offs

- **[Dutch users get "Köln", "Bruxelles - Brussel"]** → Named as on the spot; accepted until a service offers Dutch.
