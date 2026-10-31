# Proposal

## Why

The apps follow the phone's language or the app's own language set in the system settings (`add-app-languages`). Switching it there takes several steps outside the app, which is awkward for riders who use the apps in another language than their phone, and for testing the translations.

## What Changes

- The info sheet gets a Language choice next to Appearance: System (the default), English, Deutsch, Nederlands, Français, each named in its own language.
- Choosing a language switches the app's texts right away, without restarting, keeps the choice, and the spoken notices follow it.
- On Android, the choice is the same setting as the system's per-app language; on iOS, it is also saved where the system's per-app language is, so the parts the system draws follow at the next start.

## Out of Scope

- More languages.
- The location permission prompt on iOS before the next start, and on Android at all (the system draws it in its language).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `app-languages` (from `add-app-languages`, not yet archived): a new requirement for the language setting in the app.

## Impact

- Android: `androidx.appcompat` for `AppCompatDelegate.setApplicationLocales`, `MainActivity` as `AppCompatActivity`, the `AppLocalesMetadataHolderService` for Android 12 and older, the choice in `InfoSheet.kt`.
- iOS: the chosen language applied to SwiftUI's `locale` and to the texts built in code, saved in `AppleLanguages`; the choice in `InfoSheet.swift`.
- Core: none.
