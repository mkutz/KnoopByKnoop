# Design

## Context

- Android: the texts are string resources; `MainActivity` is a `ComponentActivity` and recomputes the core's texts when `LocalConfiguration`'s locale changes (`AppViewModel.useLanguage`); `NavigationService` and `Speaker` read the language from their context's configuration. The per-app language comes from `res/xml/locales_config.xml`.
- iOS: the texts are in String Catalogs, looked up by SwiftUI from `Text`/`Label` literals and by `String(localized:)` in code; `AppLanguage.current` reads `Bundle.main.preferredLocalizations`. The appearance choice is stored with `@AppStorage("appearance")` and applied on the windows.

## Goals / Non-Goals

**Goals:**
- One choice in the app, applied without a restart on both platforms.
- On Android, the same setting as the system's per-app language, not a second one.

**Non-Goals:**
- Changing what the system draws (permission prompts) before the next start.

## Decisions

**Android: AppCompat's application locales.** `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("de"))`, or `getEmptyLocaleList()` for System. On Android 13 and later it sets the framework's per-app language, which the system settings show; on 12 and older AppCompat stores it (`AppLocalesMetadataHolderService` with `autoStoreLocales`) and applies it to `AppCompatActivity`s, so `MainActivity` becomes one. Either way the activity is recreated in the new language, and the existing `useLanguage` recomputes the core's texts. The info sheet reads the current choice from `AppCompatDelegate.getApplicationLocales()`.
- Alternative: a stored choice applied with `attachBaseContext` by hand. Would be a second setting next to the system's on Android 13+.

**iOS: the app's own lookup.** iOS has no API to set the per-app language at runtime. The choice is stored with `@AppStorage("language")` (`system`, `en`, `de`, `nl`, `fr`) and applied in three places: `.environment(\.locale, …)` at the root and on each sheet, so SwiftUI looks up its `Text` literals in that language; `AppLanguage` returns a `Bundle` for the chosen `.lproj` that the texts built in code use (`String(localized:bundle:)`), and `AppLanguage.current` returns the chosen language for the core and the voice; and `UserDefaults` `AppleLanguages` is set to it (removed for System), so the location prompt and the widget follow from the next start. Whether SwiftUI's `locale` is enough for all `Text` literals is checked in the simulator first; where it is not, those texts move to the bundle lookup too.
- Alternative: open the system's per-app language page with `UIApplication.openSettingsURLString`. Simple, but leaves the app, which is what this change is for.

**The choice in the info sheet.** A row below Appearance with the five choices; on Android a dropdown (`ExposedDropdownMenuBox`) as five do not fit a segmented row, on iOS a `Picker` with the menu style.

## Risks / Trade-offs

- [AppCompat on Android adds a dependency and changes the activity's base class] → It is the platform's documented way for per-app languages below Android 13; the Compose code does not change.
- [iOS texts that miss the chosen language] → Checked in the simulator in all four languages; the widget and the location prompt follow at the next start, as stated.
