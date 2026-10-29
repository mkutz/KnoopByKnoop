# Manual checks

iOS only; the simulator can't be tapped by script, so navigation can't be started there. Both targets build in Debug and Release. Checks by the maintainer on the iPhone: ⏳.

| Scenario | iPhone | Notes |
|---|---|---|
| Music playing | ⏳ | Music on, Navigate: the first notice lowers the music, and it is back to full volume right after |
| Closing the app while navigating | ⏳ | Swipe the app away while navigating: the location indicator and the Live Activity go away right then; the route is there at the next start |
| Stopping in the app | ⏳ | Still ends navigation, the route stays |
