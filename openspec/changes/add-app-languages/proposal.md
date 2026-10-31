# Proposal

## Why

All texts of the apps are English, including the spoken notices with a British voice. On a German phone, "At 45 follow 12" and "about 15 min" next to German system texts read oddly, and riders hear knot numbers in English. The knot networks are in the Netherlands, Belgium and Germany, so most riders' phones are set to Dutch, German or French.

## What Changes

- The apps show their texts in the phone's language when it is German, Dutch or French, and in English otherwise. Android's and iOS's per-app language settings choose it too.
- Distances, times and dates are written as usual in that language, e.g. "5,2 km" and "1. Mai" in German.
- The spoken notices are in that language, with a voice for it, and the knot numbers are spoken in it.
- The screen reader texts, the notification and the Live Activity, and the location permission texts are translated too.
- The core gets the texts it builds (notices, durations, ferry states, screen reader texts) in all four languages; the apps get string resources (Android) and a String Catalog (iOS) for theirs.

## Out of Scope

- Store listings, screenshots and the privacy policy in other languages.
- Languages beyond English, German, Dutch and French.
- Place names, which come from OpenStreetMap and the place search as they are.
- A language setting inside the app (the system's per-app language settings cover it).

## Capabilities

### New Capabilities

- `app-languages`: the languages of the apps' texts and spoken notices, and how numbers, times and dates are written in them.

### Modified Capabilities

None: the existing requirements keep their English examples, which stay valid for English.

## Impact

- Core: a language passed to navigation, planning, ferry states and the progress texts; translations of its phrases; tests per language.
- Android: `strings.xml` for `values`, `values-de`, `values-nl`, `values-fr`, a `locales_config.xml`, the texts in Compose via `stringResource`, `Speaker.kt` choosing the voice by language.
- iOS: `Localizable.xcstrings` and `InfoPlist.xcstrings` in `project.yml`, the widget's texts, `Navigator.swift` choosing the voice by language.
