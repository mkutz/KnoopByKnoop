# Spike: background location with "while in use"

Both platforms keep navigating with the screen off with location access "while in use" only. iOS needs the `audio` background mode in addition to `location` to speak while locked.

## Android (task 1.1)

Emulator `Medium_Phone_API_37.0`, 2026-10-28. A throwaway foreground service of type `location`, started from the visible app, with only `ACCESS_FINE_LOCATION` granted (`ACCESS_BACKGROUND_LOCATION` not granted). Platform `LocationManager` (GPS provider, every 3 s), as the emulator's simulated fixes arrive there; the background rules do not depend on the provider. Fixes simulated with `adb emu geo fix` every 3 s.

- Screen off from 13:03:10 to 13:13:21; from 13:09:14 the device was forced into deep Doze (`dumpsys deviceidle force-idle`) and the battery unplugged (`dumpsys battery unplug`).
- 211 fixes, a steady 23 to 24 per minute through the whole run, deep Doze included.
- All 42 `TextToSpeech.speak` calls returned `SUCCESS`, and the speech engine opened an audio stream for each of them, also in deep Doze. The emulator plays no sound, so the speech itself is checked on a phone in the manual checks.

Result: "while in use" is enough on Android.

## iOS (task 1.2)

Simulator (iPhone 18 Pro, iOS 27.0) first, then the maintainer's iPhone 18 Pro, 2026-10-28. A throwaway `CLLocationManager` with "when in use" access, `allowsBackgroundLocationUpdates`, a `CLBackgroundActivitySession`, best accuracy and no distance filter; `AVSpeechSynthesizer` with an audio session of category `.playback`, mode `.voicePrompt`, option `.duckOthers`, deactivated after each utterance.

- **Simulator, `location` background mode only:** fixes kept coming every 3 s in the background, and speaking there gave no error. The simulator cannot lock its screen.
- **iPhone, `location` background mode only:** the app spoke while unlocked, not while locked or in the background. The blue location indicator was shown. Spotify was lowered while the app spoke in the foreground.
- **iPhone, `location` and `audio` background modes, location updates not paused automatically (`pausesLocationUpdatesAutomatically = false`, activity type `.otherNavigation`), speaking every 30 s:** locked from 13:38:58 to 13:52:15 (13 minutes), about one fix per second with an accuracy of 3 to 6 m, and all 27 announcements in the background started and finished, over a Bluetooth headset, without any audio session error. In an earlier, shorter run, announcements also played while Spotify was playing (`isOtherAudioPlaying`).

Result: "when in use" is enough on iOS, with the `audio` background mode for speaking while locked. The maintainer was standing still in both iPhone runs; without turning off the automatic pause, iOS may stop updates when the rider stops.

## Consequences for the design

- iOS: `UIBackgroundModes` gets `location` and `audio`. App Review asks why an app uses background audio; spoken turn-by-turn guidance is a standard reason.
- iOS: `pausesLocationUpdatesAutomatically = false` and activity type `.otherNavigation`, so a rider waiting at a ferry or a traffic light keeps getting notices afterwards. The precision table controls how often fixes come, not the automatic pause.
- Nothing changes for Android.
