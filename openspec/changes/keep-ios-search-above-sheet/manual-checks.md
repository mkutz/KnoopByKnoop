# Manual checks

## Scenarios

Checked 2026-10-28 by the maintainer in the iPhone 18 Pro simulator (iOS 27.0), in portrait, with the Mac keyboard connected (I/O → Keyboard → "Connect Hardware Keyboard") and without it. On the iPhone itself, ⏳ until the maintainer checks it.

| Scenario | Hardware keyboard | On-screen keyboard | iPhone | Notes |
|---|---|---|---|---|
| Searching with a hardware keyboard on iOS | ✅ | – | ⏳ | Typed "Sieg": the field stays right above the sheet, the sheet keeps its height, the hits are shown above the field. Before this change, the field and the attribution were behind the sheet |
| Typing on iOS | – | ✅ | ⏳ | Typed "Utrecht Centraal": the sheet drops to its smallest height, the field sits right above the keyboard with the hits above it |
| Back above the sheet on iOS | ✅ | ✅ | ⏳ | Tapping a hit: the field is right above the sheet again, the on-screen keyboard is dismissed |
| Sheet pulled up on iOS | ⏳ | ⏳ | ⏳ | |
| Searching in landscape on iOS | – | – | – | Unchanged by this change, see Observations |

## Observations

- Without a keyboard, the bottom inset the app uses to tell whether an on-screen keyboard is shown was 34 points in the simulator, well under the 100-point threshold.
- In landscape on the iPhone, with the on-screen keyboard, the hits get room for about one and a half entries, and the field seems to be behind the keyboard's suggestion bar. This was so before this change too (landscape uses neither the sheet's height nor the keyboard check). It becomes its own change, proposed after this one is archived: hide the network switch and the attribution while typing in landscape, so the hits reach the top, and keep the field fully above the keyboard.
