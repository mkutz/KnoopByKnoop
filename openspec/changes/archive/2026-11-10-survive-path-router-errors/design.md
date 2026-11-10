# Design

## Context

`PathRouter.way` (core) reads squares through a `PathSquareSource`. `PathStore.segments` drops a square whose checksum doesn't match, but `PathFile.decode` can still throw `GraphFormatException` for bytes that match and can't be unpacked or decoded, and reading a file on the phone can fail. Kotlin functions without `@Throws` end the app on iOS when they throw into Swift.

Call sites: `NavigationService.findWay` (Android, guarded), `AppViewModel.findWays` via `RoutePointWays` (Android, not guarded), `Navigator.findWay` and `AppModel.findWays` via `RoutePointWays` (iOS, not guarded).

## Decisions

### Catch in `PathRouter.way`, not in each app

`way` wraps its search in `try`/`catch (exception: Exception)`: forget the squares (some may be half loaded) and return `null`. `RoutePointWays` gets it through `way` without its own guard.

*Alternative:* `@Throws` on `way` and `do`/`catch` in Swift, plus guards in Android's planner. Rejected: four guards in two languages to keep in step, which is the kind of duplication the review asked to reduce.

*Why all exceptions, not only `GraphFormatException`:* a bug in the search itself should also cost the rider the way, not the ride. Running out of memory is an `Error`, not an `Exception`; Android keeps catching it in `NavigationService`.

### The test runs on both platforms

The test sits in `commonTest`, so `iosSimulatorArm64Test` runs it on Kotlin/Native too, where the crash would happen.

## Risks / Trade-offs

- [A bug in the search turns into "no way" silently] → The apps already log every search with its result; a `null` where a way was expected shows there. Logging the exception itself would need a logger in the core, which it doesn't have.
