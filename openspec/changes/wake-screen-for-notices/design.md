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

### Every spoken notice alerts
- The maintainer looks down whenever a notice is spoken, to check what they heard (2026-11-08). So every update with notices alerts once, whatever its kinds; several notices of one update give one alert with their texts.
- Notices come a few times per knot, more on a guided way with its turns; each wake lasts until the screen times out.

### iOS: a silent notification, not a Live Activity alert
- **Check of 2026-11-08 (task 1.1):** a Live Activity update with `AlertConfiguration` and a silent sound expanded the Dynamic Island while the phone was unlocked, but did not light up a locked screen. A throwaway queuing it on every scene change also alerted two or three times in a row.
- So `Navigator` posts a local notification per update with notices, while the app is not on screen: the notices' text, no sound, `interruptionLevel` active, replaced by the next one (same identifier), and removed when navigation ends. A delivered notification lights up the locked screen, unless a Focus holds it back.
- **Permission:** notifications need the person's permission. It is asked when navigation starts the first time; without it, nothing alerts and navigation works as before.
- **Check first:** a throwaway build posts one such notification 10 s after locking, once. On the maintainer's iPhone: does the screen light up, is it silent, what does the Watch show?

### Android: the notification alerts once at these moments
- A second channel, "Navigation alerts", with `IMPORTANCE_HIGH`, no sound and no vibration. On an update with notices, the same ongoing notification is posted on it with `setOnlyAlertOnce(false)`; afterwards back on the quiet channel.
- **Screen:** a high-importance notification lights the locked screen where the phone's "wake screen for notifications" is on, the default on Pixels. Where it is off, the alert shows the next time the screen turns on.
- **Alternative:** a wake lock with `ACQUIRE_CAUSES_WAKEUP`, deprecated and ignored by newer Android versions.

## Risks / Trade-offs

- **[The iPhone does not wake for notifications either]** → Checked first; the change is reconsidered if not.
- **[Permission denied]** → No alerts, nothing else changes.
- **[Focus modes, e.g. a cycling Focus]** → They can hold the alert back; the rider decides.
- **[Battery]** → A few screen wakes per knot; the screen goes off again on its own.
- **[Apple Watch shows a notice]** → The title and body are the notice's text, so the Watch shows it too, which is useful.

## Migration Plan

App update only.
