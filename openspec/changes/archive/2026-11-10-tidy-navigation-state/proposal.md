# Proposal

## Why

`Navigation` decides everything that happens on a ride, and it is the hardest class in the core to change safely (code review of 2026-11-10):

- **32 constructor parameters, mixed:** ten are fixed for the whole ride (knots, courses, the locator, the route's progress, leg turns, tricky stretches, the local time), 22 change with each fix. Adding a field means editing the constructor, the hand-written `copy()` and its positional call to the constructor, in the same order.
- **A rule hidden in `copy()`:** every copy sets `joinedRoute || !offRoute`, so "the rider has been on the route" is decided in a helper nobody reads when changing `update()`.
- **`update()` is about 185 lines** with three top-level branches (passing a knot, joining a leg on the way to the first knot, off and back on route), each with nested `when`s, and it reads the state before the fix through `this.` and after it through `navigation.`, so which one a check sees has to be worked out line by line.
- **Repeated transitions:** "back on route" (off route false, no off-route knot, no guided way, sometimes no direction watch) is written out four times; "now heading for knot N" with its place, pace and progress line twice.

## What Changes

- **The per-fix state becomes a private data class** `RideState` with a generated `copy`; the fixed parts become a private `RideSetup`. `Navigation` holds one of each; its public properties stay as they are.
- **The `joinedRoute` rule moves to the one place that builds a new navigation** from a state, named for what it does.
- **`update()` becomes a sequence of named steps,** each taking the navigation before and returning the one after with its notices: forgetting a passed knot, passing a knot, joining on the way to the first knot, off and back on route, the reminder, following the place, guidance, the end of the fix. The four "back on route" changes and the two "heading for" changes each become one function.
- **No behavior changes:** the same notices, states and precisions for every fix.

## Out of Scope

- **`start()` (about 117 lines)** and `badge(index, sign, onPlate)` with its boolean flags: smaller, and only touched if a step needs it.
- **Splitting `Navigation.kt` into several files:** after this change, if the steps make it obvious where to cut.
- **Any behavior, threshold or text.**

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None: a refactoring, the behavior in the specs stays as it is (`skip_specs: true`).

## Impact

- **Core:** `Navigation.kt` only, plus a replay test in the core's tests (see design).
- **Apps:** none; checked by building both.
