# Manual checks

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Reminder before a knot, Passing a knot, Off route and back, A ferry closing, Riding on between notices, App on screen | unit test | unit test | `ScreenWakeTest`: the alert's text per update, none without a notice or with the app on screen |
| The screen lights up (the throwaway of 1.2) | — | ✅ iPhone | 2026-11-11: the locked screen lit up; a notification replaced under the same identifier was not shown again, so each notice is a new notification |
| Alerts at each notice, not in between | ✅ log | — | Emulator, 2026-11-11, the recorded ride `siegburg-going-on` with the screen off: an alert posted at "02 ahead, then 28", "Next 28", "Off route, next 28" and "Turn right now", none in between. The emulator has no "wake screen for notifications" and no ambient display, so its screen stayed off; the Pixel has to show it |
| The screen lights up with each notice, silently | ⏳ Pixel | ⏳ iPhone | The maintainer's ride |

### Steps for the maintainer

On both phones, with the screen locked, ride or walk a route through two or three knots, ideally with a wrong turn:
1. **At each spoken notice:** does the screen light up and show the notice, with no sound other than the voice?
2. **In between:** does the screen stay off, apart from turning off again after its timeout?
3. **iPhone:** what does the Watch show, if you wear it? Does a Focus mode hold the notice back?
4. **Pixel:** if the screen doesn't light up, check Settings → Display → Lock screen → "Wake screen for notifications".
5. **With the app open:** no extra notification shows.
