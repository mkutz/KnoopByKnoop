# Design

## Context

- The core builds many texts itself: the notices and their spoken form (`Navigation`, `spoken(ref)` reading numbers digit by digit), durations ("about 15 min"), distances (`MapFeatures.formatDistance`, "5.2 km"), dates and ferry states (`MapFeatures.clock`, month names, "closed until 1 May 10:00"), off-route guidance with compass directions, the progress line outside the app, and screen reader texts. `PlaceSearch` already takes the device's language for Photon.
- Android writes its texts as literals in Compose and in `NavigationService`; `Speaker.kt` sets `Locale.UK`.
- iOS writes its texts as literals in SwiftUI, which a String Catalog can localize without code changes; `Navigator.swift` uses the `en-GB` voice; the widget has its own texts; `NSLocationWhenInUseUsageDescription` is set in `project.yml`.

## Goals / Non-Goals

**Goals:**
- One place per platform for its own texts, and one in the core for the core's texts, each in four languages.
- The core's texts tested in each language.
- English output unchanged, so the existing specs and tests stay valid.

**Non-Goals:**
- Plural rules beyond what the texts need (the apps show numbers mostly with units).

## Decisions

**Core: a `Language` and texts per language.** `enum class Language { EN, DE, NL, FR }` with `of(code: String)` falling back to `EN`. The core's phrases live in one `Texts` interface with an implementation per language (`EnglishTexts`, …), as functions where they take values (`follow(next, follow)`, `closedUntil(moment)`). Number and date formatting goes there too (decimal separator, month names, "1. Mai"/"1 mei"/"1er mai"). Entry points that build texts take the language: `Navigation.start`, `PlanningSession` descriptions, `RouteFerry`/`FerryWatch` texts, `MapFeatures` formatting. Defaulting to `EN` keeps current callers and tests unchanged.
- Alternative: return structured values and let each app format them. Would duplicate every phrase and number format per platform, and lose the core tests of the texts.

**Spoken numbers.** `spoken(ref)` becomes per language: knot numbers are read as numbers ("fünfundvierzig", "vijfenveertig", "quarante-cinq"); leading zeros as in English today ("zero two" → "null zwei"/"nul twee"/"zéro deux"). The text-to-speech engine reads plain numbers in its language, so the core passes digits and only spells out leading zeros.

**Android: string resources.** All Compose literals move to `res/values/strings.xml` with `values-de`, `values-nl`, `values-fr`; `res/xml/locales_config.xml` and `android:localeConfig` enable the per-app language setting. The view model passes `Language.of(the app's locale)` to the core and recomputes texts when the configuration changes. `Speaker.kt` uses the app's locale and falls back to `Locale.UK` where `setLanguage` reports it missing.

**iOS: String Catalogs.** `Localizable.xcstrings` for the app and the widget, `InfoPlist.xcstrings` for the location reason; `project.yml` lists them as resources and sets `knownRegions` to `en, de, nl, fr`. Texts built in Swift with interpolation use `String(localized:)`. The voice is `AVSpeechSynthesisVoice(language:)` for the app's preferred localization, falling back to `en-GB` where it is `nil`.

**Translations.** Written in this change for all four languages, kept short like the English. The maintainer reviews German; Dutch and French are reviewed by native speakers among the testers before the release, noted in `manual-checks.md`.

## Risks / Trade-offs

- [Translations that sound off to native riders] → Review by native speakers before the release; texts kept short and close to what signposts and other route planners use ("knooppunt", "Knotenpunkt", "point-nœud").
- [Longer texts in German, Dutch or French break layouts] → The headers already move parts to a second line where they do not fit (`strip-at-large-text`); checked at the default and the largest text size per language.
- [Missing voices on some phones] → Fall back to English, as specified.
- [Many files touched at once] → The core first, with tests, then each app; English output stays byte for byte the same, which the existing tests check.
