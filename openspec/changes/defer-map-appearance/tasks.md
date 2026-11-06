# Tasks

## 1. Android

- [x] 1.1 `NetworkMap` gets whether the info sheet is open and holds back its style reload until it closes; `PlannerScreen` passes it.
- [x] 1.2 On the emulator: "Choosing the appearance in the info sheet" with screenshots, and "Switching while the app is open" (system dark mode with the sheet closed) still switching the map at once.
- [x] 1.3 On the maintainer's Pixel, with their OK: the same four switches with `dumpsys gfxinfo` before and after, compared with the measurement of 2026-11-06 (59 of 868 frames slow, 99th percentile 200 ms).

## 2. Wrap-up

- [x] 2.1 CHANGELOG line, `manual-checks.md` with both measurements, `./gradlew check` and `npx @fission-ai/openspec validate --all` pass.
