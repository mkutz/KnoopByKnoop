# Manual checks

iOS only; the simulator can't be tapped by script, so navigation can't be started there. Both targets build in Debug and Release. Checks by the maintainer on the iPhone: ⏳.

| Scenario | iPhone | Notes |
|---|---|---|
| Music playing | ✅ | By the maintainer, 2026-10-29: back to full volume after notices |
| Closing the app while navigating | ❌ → ⏳ | Swipe the app away while navigating: the location indicator and the Live Activity go away right then; the route is there at the next start |
| Stopping in the app | ✅ | By the maintainer, 2026-10-29 |
| No location use after navigation | ⏳ | After a phone restart: open the app without navigating, no indicator; Navigate and Stop, no indicator; Navigate and swipe the app away, no indicator |

## Found and fixed

- **iOS: location indicator after Stop and outside navigation** (found by the maintainer on the iPhone). Not visible while navigating, as the Live Activity covered it; visible after Stop; gone only after restarting the phone. Likely cause: the start-time cleanup from `add-knot-navigation` created a background session on every start and ended it right away, which left one behind. It now only runs after a navigation the app didn't end, and ends its session after a second.
