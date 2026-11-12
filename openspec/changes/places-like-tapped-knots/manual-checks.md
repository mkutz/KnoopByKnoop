# Manual checks

## Done on the emulator (2026-11-12)

| Scenario | Seen |
|---|---|
| Spot added at once | ✅ A tap on an empty spot shows a spinner there (`screenshots/android-spot-adding.png`), then the spot is added: here as the new start before 14, as it lies far before it (`screenshots/android-spot-added.png`) |
| Offer without a route | ✅ Earlier round: "Add to route" twice made a start and an end; now a spot is added at once the same way |

The emulator takes about a minute to add a spot on this long route. The Pixel took about a second for the same work.

## Done in the simulator (2026-11-12)

| Scenario | Seen |
|---|---|
| Spot beyond the end, Note for a spot | ✅ On 33 → 41, a spot far beyond the end: added at once as the end, the note "Added as the end" with the flag in the previews (`screenshots/ios-spot-note.png`) |

## Not yet checked

- A highlighted place (restaurant, campsite) with "Add to route" on the phones
- The maintainer's check on both phones
