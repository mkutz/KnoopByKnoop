# Design

## Context

- **Core:** `Navigation` holds `language` and `speechLanguage` from `Navigation.start`; its `texts` and `speech`, `FerryWatch`'s, and the stored `progressLine` and ferry `arrivals` were made with them. `display`, `guidance` and `progress(fix)` read `language` when asked.
- **Android:** `NavigationService` reads `Language.of(resources.configuration.locales[0].language)` and `speaker.language` once, when the first fix starts navigation. A language set per app, or a new phone language, reaches a running service as a configuration change; `configChanges` in the manifest covers the activity only. `Speaker` decides its language and voice in its constructor and `onInit`.
- **iOS:** `Navigator` passes `AppLanguage.current` and `AppLanguage.speech` at the start. The choice is in Info, hidden while navigating, and changing the language in the iPhone's settings restarts the app.

## Goals / Non-Goals

**Goals:**
- All of navigation's texts in the new language at once, with nothing else about navigation changed.

**Non-Goals:**
- Changing where the language is chosen.

## Decisions

**`Navigation.inLanguage(language, speechLanguage, fix): Navigation`.** It returns a copy with the new languages, and renews what was stored as text:
- `progressLine`: `guide`'s line off route, `routeLine` on route.
- `FerryWatch.inLanguage`: a copy with the new languages and the same `warned`, with its arrivals renewed by `renewed`, so no warning fires.
- Everything else stays: `nextIndex`, `place`, `pace`, `remindedIndex`, `guided`'s notice values. So no notice is due from the change, and off route the next guidance notice comes when it would have.

**`NavigationService.onConfigurationChanged`.** When `Language.of(newConfig.locales[0].language)` differs from the navigation's, the service:
1. tells `Speaker` the new language, which sets the voice or falls back to English and updates `speaker.language`;
2. applies `navigation.inLanguage(...)` with the last fix;
3. publishes it to `Navigating` and notifies, as after a fix.

If no fix has come yet, there is no navigation to change, and the first fix reads the new language anyway. A language of the phone that the app does not offer maps through `Language.of`, as at the start.

Confirmed on the emulator: a language set per app reaches the running service as a configuration change, so nothing else is needed.

**Android's channel name and Stop action** come from `getString` when the notification is built. The channel's name stays until the service next starts, as the system keeps it; riders don't see it while riding.

## Risks / Trade-offs

- **A voice that is not installed** falls back to English, as at the start of navigation. Texts and voice can then differ, as specified.
- **A notice being spoken during the change** finishes in the old voice.
- **iOS is not changed.** If the iPhone restarts the app with a route being navigated, navigation starts anew with the new language, as after any restart, and the maintainer's steps check this.
