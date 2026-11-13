## Context

The voice speaks every notice as one string with one voice (`Notice.speech`, `Speaker.speak(text)` on Android, `AVSpeechUtterance` with `AppLanguage.voice` on iOS). The only names spoken today are ferry names in the ferry warnings (`FerryWatch`, `Texts.ferryTitle`). Route points are spoken as "the start", "the stop" or "the end" (`Texts.name(spot)`), and knot numbers in the app's language.

The pipeline already reads the administrative boundaries of the Dutch provinces, the ten Belgian provinces, Brussels and North Rhine-Westphalia for the borders between knots (`regionOfBorder`, `Border<Region>`). Each Belgian province is either Flemish (`BE-V…`) or Walloon (`BE-W…`) by its ISO 3166-2 code.

## Goals / Non-Goals

**Goals:**
- Ferry names spoken with a voice for their language where the phone has one.
- The same mechanism usable for any later spoken name (places, regions).
- No change for riders whose phone lacks the voice, or with older data.

**Non-Goals:**
- SSML, lexicons, installing voices.
- Changing any text on screen or outside the app.

## Decisions

### The name's language is decided in the pipeline, from the region
The pipeline knows the boundaries; the app only has a knot's sign region (cycling only, no Flemish/Walloon split) and its place (only for half of the knots). So the pipeline gives each named ferry a language from the region the middle of its ferry way lies in: Dutch provinces and `BE-V…` give Dutch, `BE-W…` French, `DE-NW` German, `BE-BRU` and anywhere else none.
- *Alternative:* guess in the app from the knot's place names (e.g. `name` equals `name:fr`). Rejected: unreliable, and many knots have no place.
- *Alternative:* `language:*` or `name:xx` tags on the ferry. Rarely mapped.
- The middle of the ferry way, not the knots': a border ferry belongs to the bank region, either is fine, but the middle is stable. With several ferry ways, the longest one counts.

### Format: a new section at the end of graph format 2
Like the sign regions, places and borders before: after the crossings, the count of ferries with a name language, then per ferry its connection index and the language as a byte (the `Language` ordinal is not used; a fixed code: 1 Dutch, 2 French, 3 German, so reordering the enum can't change the data). Readers from before stop after the crossings. `Ferry` gets `nameLanguage: Language?`, defaulting to `null`.

### Spoken notices become parts
`Notice` gets `spoken: List<SpokenPart>`, with `SpokenPart(text: String, language: Language?)`, where `null` means the notice's speech language. `Notice.speech` stays as the parts joined with spaces, so tests, logs and the recorded rides keep reading it. Only the ferry warning builds more than one part for now: `Texts.ferryWarning` and `ferryTitle` gain a variant that returns parts, with the name as its own part when its language differs from the speech language. Neighbouring parts with the same language merge, so a Dutch name in a Dutch notice stays one utterance with no pause.

The core decides nothing about voices: whether the phone has a voice for a part's language is the app's call, and a part without a usable voice simply uses the notice's voice. Parts in that case are joined back into one utterance by the app, so there are no needless pauses.

### Android: one queued utterance per part
`Speaker.speak(parts)`: for each run of parts that share a voice, set the voice (`setLanguage` with the language's locale, or the notice's locale) and queue with `QUEUE_ADD`, each with its own utterance id. The language is captured when `speak` is called, so changing it for the next part doesn't affect the one queued. *Risk:* some engines apply `setLanguage` late; if the emulator or the Pixel show the name in the wrong voice, pass the `Voice` per request via `setVoice` before each `speak` instead. The availability check per language is `isLanguageAvailable(locale) >= LANG_AVAILABLE`, cached per language. The audio focus is held while `speaking` is not empty, which already spans all ids of a notice queued at once; `whenSilent` waits for all of them.

### iOS: one utterance per part
`Navigator.speak(parts)` builds one `AVSpeechUtterance` per run, with `AVSpeechSynthesisVoice(language:)` for the part's language or `AppLanguage.voice`. `pendingNotices` counts utterances, not notices, so the audio is given back only after the last part. The voice codes come from `AppLanguage.voiceCode`.

### Locales for the name languages
Dutch `nl-NL`, French `fr-FR`, German `de-DE`. Belgian variants (`nl-BE`, `fr-BE`) are rarer on phones, and the difference for names is small; the code takes the Belgian voice first where the ferry is in Belgium only if that turns out to matter on rides (not now).

## Risks / Trade-offs

- A short pause between the parts → acceptable; the name stands out, which helps.
- A phone with the language but a poor voice → still better than a foreign voice; nothing to configure.
- Old data until the next publication → names spoken as before; no app update needed for new data beyond this one.
- The voice for the name may sound different from the notice's (another speaker) → expected, like in car navigation.

## Migration Plan

Ship the app and the pipeline together; the data is published by the weekly run (or a manual run). Apps released before read the new files as before.

## Open Questions

None.
