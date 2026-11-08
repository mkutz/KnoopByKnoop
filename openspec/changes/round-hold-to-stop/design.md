# Design

## Context

- `HoldAction` in `PlannerActions.kt` is the stop button over the lock screen: a pill with the icon and "Hold to stop", outlined in red, filled from left to right while held over `HOLD_MILLIS` (1 s), timed by the gesture and not by the animation, with a small fill on a tap. TalkBack reads its label and its action stops.
- `StopIconAction` is the round Stop in the app: a filled red icon button of 52 dp with a tooltip on long press.
- `NavigationHeader` in `PlannerScreen.kt` picks one of them by `Navigating.overLockScreen`.

## Decisions

### A round button with a ring
- The button is a 52 dp circle with a red outline and the red stop icon on the surface colour, the same size as `StopIconAction`. While held, a red arc around it grows clockwise from the top to a full circle; the gesture handling and the timing stay as they are.
- **Why a ring around and not a fill inside:** the icon stays readable while the ring grows, and a ring is how Android shows progress elsewhere.
- **Alternative:** filling the circle from the bottom. The red fill would hide the red icon halfway.

### The hint on a tap
- A tap that ends before the hold time shows "Hold to stop" in a plain tooltip above the button for about 2 s, the same kind of tooltip as the labels of the round buttons.
- **Alternative:** a snackbar. It sits over the map's bottom and would cover the strip.

### Screen readers
- The semantics stay: label "Hold to stop", role button, the action stops at once, as a screen reader user can't hold.

## Risks / Trade-offs

- **[A smaller target]** → 52 dp is the size of the Stop in the app, well above the 48 dp minimum; a wrong touch still needs a second's hold.
- **[The tooltip over the lock screen]** → it is shown by the app's own window, which is shown over the lock screen; check on the emulator.
