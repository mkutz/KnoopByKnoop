# Design

## Context

- **Notices:** `Navigation.update` returns `notices` with a `NoticeKind`: NEXT, REMINDER, OFF_ROUTE, BACK_ON_ROUTE, WRONG_WAY, ARRIVED, FERRY, among others. The apps speak them and update what they show outside the app.
- **iOS:** `Navigator` updates the Live Activity with `activity.update(ActivityContent(state:staleDate:))`, silently.
- **ActivityKit:** `Activity.update(_:alertConfiguration:)` updates "and alerts a person about the Live Activity update". `AlertConfiguration(title:body:sound:)` takes a title and body, which Apple's documentation describes as shown on Apple Watch, and an `AlertSound`: the system's default or `named(_:)` a sound file. There is no silent option, and the documentation does not say how the alert looks on the iPhone (checked 2026-11-08). That the locked screen lights up and shows the Live Activity is common knowledge, not documented, and is checked on the maintainer's iPhone first.
- **Android:** `NavigationService` posts an ongoing notification on a channel with `IMPORTANCE_DEFAULT` and `setOnlyAlertOnce(true)`, so updates never alert. `MainActivity` shows over the lock screen while navigating, only when the app is opened.

## Goals / Non-Goals

**Goals:**
- A locked phone shows the panel's information at the moments that need the rider.
- No extra sound beyond the spoken notice.
- No change for the other updates.

**Non-Goals:**
- A setting, sounds or vibration.
- Waking at the "next" notice after passing a knot: it follows right after the reminder, and the rider can ride on without looking.

## Decisions

### Which notices alert, decided in the core
- `NavigationUpdate` gets `alerts: Boolean`, true when one of its notices is a REMINDER, OFF_ROUTE, WRONG_WAY, ARRIVED, or a FERRY notice about closing. Both apps use it; the kinds are decided in one place and tested.
- **Not alerting:** NEXT, BACK_ON_ROUTE, guidance turns on a guided way, which come often while off route.

### iOS: an alerting update with a silent sound
- On an update with `alerts`, `Navigator` passes `AlertConfiguration(title:body:sound: .named("silence.caf"))`. The title and body are the notice's text, for Apple Watch.
- `silence.caf` is a short silent sound in the app bundle, as `AlertSound` has no silent option.
- **Rate:** the alerts are as rare as the notices, a few per knot at most, well within what the system presents.
- **Check first:** before the rest, a throwaway build on the maintainer's iPhone checks that a locked screen wakes, that the silent sound is silent, and how the Watch shows it. If the screen does not wake, the change stops there and is reconsidered.

### Android: the notification alerts once at these moments
- A second channel, "Navigation alerts", with `IMPORTANCE_HIGH`, no sound and no vibration. On an update with `alerts`, the same ongoing notification is posted on it with `setOnlyAlertOnce(false)`; afterwards back on the quiet channel.
- **Screen:** a high-importance notification lights the locked screen where the phone's "wake screen for notifications" is on, the default on Pixels. Where it is off, the alert shows the next time the screen turns on.
- **Alternative:** a wake lock with `ACQUIRE_CAUSES_WAKEUP`, deprecated and ignored by newer Android versions.

## Risks / Trade-offs

- **[The iPhone does not wake for Live Activity alerts]** → Checked first on the maintainer's iPhone; the change is reconsidered if not.
- **[Focus modes, e.g. a cycling Focus]** → They can hold the alert back; the rider decides.
- **[Battery]** → A few screen wakes per knot; the screen goes off again on its own.
- **[Apple Watch shows a notice]** → The title and body are the notice's text, so the Watch shows it too, which is useful.

## Migration Plan

App update only.
