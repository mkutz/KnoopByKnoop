# Proposal

## Why

At the gym on 2026-11-12, the maintainer pressed Navigate and nothing visible happened for about 10 seconds. Navigate first finds the rider's position, to decide whether to ask "ride to the start or start on the route nearby". It asks for the best accuracy the phone can give: `kCLLocationAccuracyBest` on iOS, `PRIORITY_HIGH_ACCURACY` on Android. Indoors that often never arrives, so the app waits for the whole 10-second limit and shows nothing meanwhile. This has been the case since 0.14.0.

## What Changes

- Navigate uses a position the phone already has when it is at most 60 seconds old and accurate to 100 m or better, so it can decide at once.
- Otherwise it asks for a position of about 100 m accuracy instead of the best. That is enough to tell "near the route" from "at the start", and phones answer it much sooner.
- Right after the tap, Navigate shows a spinner in place of its arrow, keeps its size, and ignores further taps until navigation starts or the question shows. Screen readers hear "Finding your position".
- A rough position doesn't cause a needless question: the app only asks when the course is more than 200 m along from the start and less than half as far as the start, so the rider must be several hundred metres from the start, well beyond 100 m of inaccuracy. A scenario pins this down.
- The 10-second limit stays: without a position, navigation leads to the start as today.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: "Start on the route nearby" covers how the position is found, the feedback while waiting, and the allowance for its accuracy.

## Out of scope

- The position used once navigation runs: following the rider keeps the best accuracy.
- "Show my position" and "Nearest knots" on the map keep their own way of finding a position.
- The ferry check after the start choice, which uses the same start position.
- A setting for the limit.

## Impact

- iOS: `PositionFinder` gets an accuracy and a maximum age, `AppModel.startNavigation` publishes that it is finding the position, `NavigateIconAction` shows the spinner.
- Android: `AppViewModel.startNavigation` uses `getCurrentLocation` with `CurrentLocationRequest` (balanced power accuracy, max update age 60 s) and a `findingStart` state; `NavigateFab` and `NavigateButton` show the spinner.
- Core: no change; a test for the rough position at the start.
