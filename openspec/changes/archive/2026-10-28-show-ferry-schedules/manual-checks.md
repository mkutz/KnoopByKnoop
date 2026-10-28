# Manual checks

## Ferry coverage

Logged by `update-network-data` run 40551070761 on 2026-10-27 (OpenStreetMap data of 2026-10-25), the first run writing graph format version 2:

| Network | Ferries | With operating times | Only seasonal | With a fee or charge | Without a name |
|---|---|---|---|---|---|
| Cycling | 213 | 183 | 2 | 75 | 5 |
| Hiking | 232 | 156 | 5 | 62 | 49 |

Most cycling ferries have operating times. Some `opening_hours` values are long and technical, e.g. the Biej Ool Euver ferry with seven rules, or carry comments like `"not in bad weather"`; formatting them is the follow-up the design mentions.

## Scenarios

Checked 2026-10-27 on the Android emulator (`Medium_Phone_API_37.0`) with the published version 2 graphs, bundled into a fresh install. iOS was checked by the maintainer on an iPhone on 2026-10-28. Before that, the iPhone showed no ferry for 56 → 70: an installed app keeps its version 1 graph, as the bundled version 2 has the same data date, until its next sync (at most a day).

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Route over a ferry | ✅ | ✅ | 56 → 70 at Broekhuizen: badges 56 ⛴ 70, "Broekhuizen-Arcen", "Mo-Fr 06:30-22:00, Sa 07:30-22:00, Su 09:00-22:00", "Fee: 0.9 €", Website ([screenshot](screenshots/android-route-over-ferry.png)) |
| Ferry without known times | ✅ | ✅ | 24 → 45 in Oostende: "Veer Oostende", "Operating times not known, check before you go" in red ([screenshot](screenshots/android-ferry-without-times.png)) |
| Several ferries | ✅ | ✅ | 08 → 97 at the Kaag: 08 ⛴ 31 ⛴ 97, "Kaag - Buitenkaag" (3.50 EUR) before "Sweilandveer" (2.50 EUR) ([screenshot](screenshots/android-several-ferries.png)) |
| Route without ferries | ✅ | ✅ | 21 → 26 near Kampen: the route goes by land via 12 and 22, which is shorter than the Pieperpont; no ferry badge, no list ([screenshot](screenshots/android-route-without-ferries.png)) |
| Offline | ✅ | ✅ | The Oostende route after a restart in airplane mode: ferry and details as online ([screenshot](screenshots/android-offline.png)) |

## Observations

- Installed apps get the version 2 files at their next sync: the data date did not change, but the checksums did, and updates compare checksums. The emulator's sync was not due yet, so the checks used a fresh install.
- With the ferry list, the route summary gets taller; long routes push the waypoint list out of the sheet. This is `scroll-long-routes`, proposed separately.

## Found and fixed

- **Installed apps got the ferries only with the next daily sync.** On the iPhone, 56 → 70 showed no ferry after installing the new build: the bundled version 2 graph has the same data date as the installed version 1 graph, and bundled graphs only replaced older data. Now a bundled graph in a newer format replaces the installed one (`network-sync` "Bundled graph in a newer format"). Checked 2026-10-28 on the Android emulator in airplane mode: the installed cycling graph, set to version 1, was replaced by the bundled version 2 graph on start, the hiking graph (already version 2) was left alone, a second start did not install it again, and the route 08 ⛴ 31 ⛴ 97 showed its ferries ([screenshot](screenshots/android-bundled-newer-format.png)).
