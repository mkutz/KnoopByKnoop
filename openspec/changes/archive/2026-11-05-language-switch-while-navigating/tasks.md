# Tasks

## 1. Core

- [x] 1.1 `Navigation.inLanguage` and `FerryWatch.inLanguage`. Tests in `NavigationTest` and `FerryWarningTest`:
  - "Words and line outside the app": English to German, close to a knot.
  - "Off route": the line and `guidance` in German.
  - "Later notices": the next notice's speech in German.
  - "No voice for the new language": text in French, speech in English.
  - "Ferry ahead": the popover in German, and no second warning.
  - "Nothing else changes": next knot, place and pace unchanged, and the next fix gives no notice from the change.

## 2. Android

- [x] 2.1 `Speaker` can change its language; `NavigationService.onConfigurationChanged` applies it. On the emulator with a mocked ride, `cmd locale set-app-locales` from English to German and on to Dutch:
  - screenshots of the notification and the panel, on route and off route ("Words and line outside the app", "Off route");
  - logcat "spoken in DE" at the next knot ("Later notices");
  - nothing spoken and the strip's place unchanged at the change ("Nothing else changes").

## 3. iOS

- [x] 3.1 No code. In `manual-checks.md`, the maintainer's step: change the app's language in the iPhone's settings while navigating, and see what the app does.

## 4. Wrap-up

- [x] 4.1 `manual-checks.md` with the scenarios; `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased.

## Workflow follow-up

- The maintainer checks it on the Pixel and the iPhone before `/opsx:archive language-switch-while-navigating`, after `add-app-languages` and `add-language-setting`.
