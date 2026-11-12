# Manual checks

## Done on the emulator (2026-11-12)

The check route was 12 → 15 over the ferry Mondorf–Graurheindorf, with the emulator 69 m from the course near 13. The ferry closes at 19:00 on winter weekdays, so the emulator's clock was set to 21:00 (`cmd alarm set-time`). Afterwards the automatic time, the screen timeout and the earlier route 14 → 15 were put back.

| Scenario | Seen |
|---|---|
| Starting nearby | ✅ "Start on the route nearby · 69 m" puts a temporary start flag on the course by 14. Then the ferry question comes for the route from there (`screenshots/android-question-nearby.png`) |
| Started nearby and riding around | ✅ "Ride around it · 13 km longer, about 49 min": navigation from the temporary start, 14 km over the bridge in Bonn (`screenshots/android-around.png`) |
| Riding around it | ✅ `route.json` still holds 12 → 15 while navigating |
| Plan restored after stopping, Map after the ride | ✅ After Stop, the planner shows 12 → 13 → 14 → ferry → 15, 4.9 km, with no dashed line and no temporary start (`screenshots/android-restored.png`) |
| Navigating the plan again | ✅ Navigate asks "Where do you want to start?" again, and after starting nearby it asks about the ferry again |
| Question dismissed | ✅ Starting nearby, then Back on the ferry question: the plan is back from 12 |
| App ended while navigating | ✅ Started nearby, rode around, force-stopped and opened again: the plan 12 → 15 (`screenshots/android-after-restart.png`) |

## Done in the simulator (2026-11-12, 07:09)

The ferry was open at that time, so only starting nearby was checked. Same route and place, driven by a throwaway UI test.

| Scenario | Seen |
|---|---|
| Starting nearby | ✅ Navigation from the temporary start flag by 14, over the ferry (`screenshots/ios-navigating-nearby.png`) |
| The plan after the ride | ✅ After Stop, the planner shows 12, 13, 14, ⛴, 15 again, and `route.json` holds 12 → 15 (`screenshots/ios-restored.png`) |

## On the iPhone (maintainer, 2026-11-12)

- Starting nearby, Stop, Navigate again and closing the app while navigating all worked ("Yes").
- On a long route to Den Helder, one question named all the closed ferries but showed only the first detour, and Horsterveer twice. That led to one question per ferry (option 1, confirmed by the maintainer) and one ferry per set of crossings (7bcab0e).
- With 7bcab0e, the maintainer saw the two questions on the iPhone ("Saw the two questions on my iPhone 👍").

## On the Pixel (maintainer, 2026-11-12)

- With aac562f's build, the per-ferry questions, riding around, keeping and the restored plan all work ("Works on the Pixel too").

## Steps for the maintainer

On a phone, in the evening after the ferry's last crossing (Mondorf–Graurheindorf closes at 19:00 on winter weekdays):

1. Plan a route that starts some way before the ferry, e.g. 12 → 15, and press Navigate near the course close to the ferry.
2. "Start on the route nearby", then "Ride around it". Navigation starts at a flag where you are and goes around.
3. Stop. The plan is back as you made it: from its own start, over the ferry.
4. Navigate again: both questions come again.
5. Start nearby and ride around once more, close the app while navigating, and open it again. The plan is as you made it.
6. Optional, on a ride: arrive at the end after starting nearby. The plan is back from its own start ("Plan restored after arriving").
