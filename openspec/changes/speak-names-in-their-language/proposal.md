> **Parked** on 2026-11-12: only ferry names are affected, and Belgium's languages make it harder than it's worth for now. Not to be applied unless the maintainer brings it back.

## Why

The voice speaks the app's language, or English where the phone has no voice for it. The names in the notices are Dutch, German or French, though, and an English or German voice mangles names like "Renkumse Veer" until a rider can't match them to the sign (maintainer, 2026-11-12). Mostly the ferry names in the ferry warnings are affected. Those are the only names the voice speaks today: route points are spoken as "the start", "the stop" or "the end", and knot numbers are already spoken in the app's language.

## What Changes

- The published graph carries the language of each ferry's name. The pipeline takes it from where the ferry is: Dutch in the Netherlands and the Flemish provinces, French in the Walloon provinces, German in North Rhine-Westphalia. Brussels and places outside these regions get none. It comes from the same administrative boundaries the borders between knots use, and goes in a new section at the end of graph format 2, so apps released before read the files as before.
- A spoken notice is no longer one string but a list of parts, each tagged with its language. A ferry warning becomes e.g. `[en] "The ferry" · [nl] "Renkumse Veer" · [en] "closes at 19:00, …"`.
- The apps speak each part with a voice for its language: one queued utterance per part on Android and on iOS. A part whose language the phone has no voice for, and a name without a language, is spoken with the notice's voice, as today. Lowering other audio, and stopping after "You've arrived", cover the whole notice, not each part.
- The texts on screen, in the notification and in the Live Activity stay as they are.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `app-languages`: new requirement "Names spoken in their language".
- `network-data`: new requirement "Language of each ferry's name".

## Impact

- Pipeline: `SignRegions.kt` (region → language), `NetworkExtraction.kt` (the ferry's language from where its way lies). Data must be published again before riders hear the difference.
- Core: `GraphFile.kt` (new section), `NetworkGraph.kt` (`Ferry.nameLanguage`), `Navigation.kt` (`Notice` gets spoken parts), `FerryWatch.kt` and `Texts.kt` (the ferry warning in parts).
- Android: `Speaker.kt` speaks a list of parts, choosing a voice per part.
- iOS: `Navigator.swift` and `AppLanguage.swift` speak a list of utterances, a voice per part, and count them for giving back the audio.
- Device checks on both phones: which voices are installed differs per phone.

## Out of scope

- Place names, region names and street names: the voice doesn't speak them today. Once it does (e.g. border crossings in notices), they can use the same parts.
- SSML language tags: support differs between speech engines, so parts are used instead.
- Installing voices for the rider, or telling them that a voice for a name's language is missing.
- The German-speaking community in the province of Liège: it has no boundary in the extracts, so its ferries count as French.
- Pronunciation fixes for single names (a lexicon), and the place in the knots list or the waypoint list for screen readers.
