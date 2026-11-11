# Manual checks

## How it was checked

On 2026-11-11 with the debug build and the published data of 2026-11-08, replaying the fixes of the recorded ride `siegburg-going-on` (01 → 02 → 28) with `adb emu geo fix`:

- **Android 12:** the API 31 emulator (`KbK_API_31`), phone in English, app set to German.
- **Android 16:** the API 37 emulator.

## Scenarios

| Scenario | Android 12 | Android 16 | Notes |
|---|---|---|---|
| Navigation on Android 12 | ✅ | — | The notification "02 · 1,6 km › 28" with "Beenden", the notice "Nächster Knotenpunkt 02, dann 28" spoken in DE ([shade](screenshots/android-12-german.png)). Before the fix: "1.6 km", "Stop", "Next 2, then 28" in English |
| Phone's language changed while navigating on Android 12 | ✅ | — | The phone switched to French in the settings during the ride: the system's texts French, the notification still "Beenden" and no language change logged ([shade](screenshots/android-12-phone-french.png)). Navigation started again with the phone in French speaks German. The emulator delivered no more positions to the app after the phone's language changed, so no notice came in between; navigation started again got positions |
| Words and line outside the app, Later notices (unchanged) | — | ✅ | Per-app language set to German with `cmd locale` while navigating: "language: DE, spoken in DE" logged once, the texts in German |

The planner's texts on Android 16 stay as before (task 1.1): English, then German after the per-app change.
