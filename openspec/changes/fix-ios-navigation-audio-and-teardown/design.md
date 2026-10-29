# Design

## Context

See `proposal.md` for why.
- **Speaking:** `Navigator.speak` sets the audio session to `.playback` with mode `.voicePrompt` and `.duckOthers`, activates it, and speaks with `AVSpeechSynthesizer`. In the delegate's `didFinish` and `didCancel`, `finishedSpeaking()` deactivates the session with `.notifyOthersOnDeactivation`, but only after `guard !speech.isSpeaking`. Inside `didFinish`, `isSpeaking` can still be `true` for the utterance that just finished, so the guard returns and the session stays active. `try?` hides any deactivation error.
- **Closing the app:** the `CLBackgroundActivitySession` started by `begin()` outlives the app. Since `add-knot-navigation`, `endLeftoverSession()` ends it at the next start, and nothing ends it when the app is closed. The `AppDelegate` only handles background downloads.

## Goals / Non-Goals

**Goals:**
- Other audio lowered exactly while a notice is spoken.
- Closing the app while navigating ends navigation right away.

**Non-Goals:**
- Changes on Android.
- Persisting a navigation across app restarts.

## Decisions

### Count pending notices instead of asking the synthesizer
`Navigator` counts the utterances it hands to the synthesizer: +1 in `speak`, −1 in `didFinish` and `didCancel`. When the count reaches 0, the session is deactivated with `.notifyOthersOnDeactivation`, so the music comes back up. The deactivation is wrapped in `do`/`catch` and logged in the `Navigation` category. If it fails because audio is still busy, it is tried once more after half a second. Stopping navigation while a notice is speaking stops the synthesizer (`stopSpeaking(at: .immediate)`), which also ends in `didCancel`.
*Alternative:* re-checking `isSpeaking` after a short delay. It depends on timing, while the count is exact.

### End navigation in `applicationWillTerminate`
The `AppDelegate` calls `Navigator.shared.stop()` in `applicationWillTerminate`. With the location background mode active, iOS calls it when the user swipes the app away. `stop()` already does what's needed: it stops location updates, invalidates the session and ends the Live Activity with `.immediate`. The Live Activity's end is asynchronous, so the termination waits briefly for it, at most 1 s, using a semaphore on a background queue, before returning. The route is saved as before, so it's there at the next start.
The cleanup at start (`endLeftoverSession`) stays for terminations without the callback, e.g. when iOS ends the app for memory.
*Alternative:* `sceneDidDisconnect`. It is also called in other situations, e.g. when the system discards a scene, which shouldn't end navigation.

## Risks / Trade-offs

- [`applicationWillTerminate` isn't called when iOS ends a suspended app] → With background location running, the app isn't suspended while navigating, and the start-time cleanup covers the rest.
- [Ending the Live Activity during termination may not finish] → Bounded wait; iOS also ends Live Activities of apps that no longer exist after a while.
- [Hard to test automatically] → Checked by the maintainer on the iPhone: music back to full volume right after a notice; the indicator and the Live Activity gone right after swiping the app away while navigating.
