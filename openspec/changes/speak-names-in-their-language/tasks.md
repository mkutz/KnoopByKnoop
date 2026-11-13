## 1. Data: the language of each ferry's name

- [ ] 1.1 Core: `Ferry.nameLanguage: Language?` (default `null`), and in `GraphFile` a new section after the crossings: the count, then per ferry the connection index and a fixed language code (1 Dutch, 2 French, 3 German). Tests in `GraphFileTest`: a round trip with name languages, and `Graph files without name languages` (bytes written without the section read with no language for any ferry).
- [ ] 1.2 Pipeline: the language of a region (Dutch provinces and `BE-V…` Dutch, `BE-W…` French, `DE-NW` German, `BE-BRU` none) next to `regionOfBorder`, and in `ferryOf` the language of the region the middle of the longest ferry way lies in, only for a named ferry. Tests in `NetworkExtractionTest` (or a new `FerryLanguageTest`) named after the scenarios: `Ferry in Gelderland`, `Ferry in Antwerp`, `Ferry in Wallonia`, `Ferry on the Rhine`, `Ferry on a border river`, `Ferry outside the regions`, `Ferry without a name`.

## 2. Core: spoken notices in parts

- [ ] 2.1 `SpokenPart(text, language: Language?)` and `Notice.spoken: List<SpokenPart>`, with `Notice.speech` as the parts joined, so recorded rides stay as they are. Every notice but the ferry warning has one part with no language.
- [ ] 2.2 The ferry warning in parts: the name as its own part with the ferry's name language where it differs from the speech language; neighbouring parts with the same language merged. Tests in `FerryWarningTest` named after the scenarios: `Dutch ferry name in an English notice`, `French ferry name in a German notice`, `Name in the notice's language`, `Name without a language`, `Texts unchanged` (the warning's text is the same as before).
- [ ] 2.3 Run `./gradlew :modules:core:jvmTest` including `NavigationReplayTest` and `RealRideTest`: the recorded rides must not change.

## 3. Android

- [ ] 3.1 `Speaker.speak(parts)`: per run of parts sharing a voice, set the part's locale (`nl-NL`, `fr-FR`, `de-DE`) where `isLanguageAvailable` says so (cached), else the notice's, and queue it with its own id. `NavigationService` passes `notice.spoken`. Audio focus and `whenSilent` span all parts.
- [ ] 3.2 Check on the emulator with a mocked ride over a Dutch ferry in English and German (install the Dutch voice for the one, remove it for `No voice for the name's language`), with music playing. If the name comes in the wrong voice, set the `Voice` per request instead (see design).

## 4. iOS

- [ ] 4.1 `Navigator.speak(parts)`: one `AVSpeechUtterance` per run with `AVSpeechSynthesisVoice(language:)` for the part's language or `AppLanguage.voice`; `pendingNotices` counts utterances. `voiceCode` reused from `AppLanguage`.
- [ ] 4.2 Check in the simulator with a ferry warning in English over a Dutch ferry, with and without the Dutch voice.

## 5. Checks

- [ ] 5.1 Add `No voice for the name's language` and `Music between the parts` to `openspec/checked-by-hand.md` (on the emulator and the phones), plus a device line for the voices of `Dutch ferry name in an English notice`. Add a CHANGELOG Unreleased line.
- [ ] 5.2 Run `./gradlew check` and `npx @fission-ai/openspec validate --all`, then `./gradlew checkScenarios -Pchange=speak-names-in-their-language`.

## Workflow follow-up

- Publish the data again (manual run) so the phones get name languages, then the maintainer listens on a ride or a mocked ride over a ferry; archive after that.
