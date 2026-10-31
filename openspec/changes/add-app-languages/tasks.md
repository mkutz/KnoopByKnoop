# Tasks

## 1. Core

- [x] 1.1 `Language` and `Texts` with English, German, Dutch and French; number and date formatting per language. Verify with a new `LanguageTest` ("Decimal comma", "Date of a ferry's next opening", "English unchanged", "Other language" for `Language.of`), and that all existing core tests pass unchanged
- [x] 1.2 Pass the language through navigation (notices, spoken numbers, guidance, progress line, screen reader texts), planning descriptions and ferry texts. Verify with tests "German notice", "Dutch arrival" (the spoken text), the core part of "German phone", "Dutch phone", "French phone", "Notification in German" and "Screen reader in Dutch", on the JVM and the iOS simulator

## 2. Android

- [x] 2.1 All texts into `strings.xml` with German, Dutch and French; `localeConfig` for the per-app language; the core called with the app's language. Verify on the emulator in English, German, Dutch and French, at the default and the largest font size, with screenshots of planning, navigating, off route, the coming knots, the info sheet and the notification: "German phone", "Dutch phone", "French phone", "Other language", "Per-app language", "Place names stay", "Decimal comma", "English unchanged", "Notification in German", "Screen reader in Dutch" (`uiautomator dump`), "Location prompt in French"
- [x] 2.2 `Speaker.kt` with the app's language and the English fallback. Verify on the emulator with German and Dutch voices installed and with French uninstalled: "German notice", "Dutch arrival", "No voice for the language"

## 3. iOS

- [x] 3.1 String Catalogs for the app, the widget and the location reason, `knownRegions` in `project.yml`, the core called with the app's language, the voice by language. Verify the project builds with `xcodebuild`, install it on the maintainer's iPhone, and write the steps for every scenario in `openspec/changes/add-app-languages/manual-checks.md`

## 4. Wrap-up

- [ ] 4.1 Ask native speakers among the testers to review the Dutch and French texts; record it in `manual-checks.md`
- [x] 4.2 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; CHANGELOG entry under "Unreleased" (create the section, as 0.5.0 was just released)

## Workflow follow-up

- The maintainer checks iOS on the iPhone, in German, before `/opsx:archive add-app-languages`.
