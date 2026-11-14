# Design

## Context

`PlaceSearchField` keeps a local `TextFieldValue` (`field`) for the cursor and selection.

- `onValueChange` sets `field` and calls `viewModel.search(text)`, which stores `state.searchQuery`.
- A `LaunchedEffect(state.searchQuery)` copies the query back into `field` whenever `field.text` differs. This is how Clear, a chosen hit (the query becomes the hit's name) and reopening with text left from before reach the field.

The effect runs on a later frame. With keystrokes faster than frames:
1. The field holds "Blu".
2. The effect for the stored "Bl" runs, sees "Blu" ≠ "Bl" and resets the field to "Bl".
3. The "u" is lost.

## Goals / Non-Goals

**Goals:** typed text always survives; Clear, hits and reopening still set the field.

**Non-Goals:** changing when or how often the app searches.

## Decisions

### Ignore the echo of the field's own text

The field remembers the texts it sent to the view model since the last outside change.
- When the stored query is one of them, it is an echo and is ignored.
- Otherwise it came from outside (Clear, a hit, reopening), and it replaces the field's text with the cursor at the end.

As a pure function in `PlaceSearchField.kt`:

```kotlin
/** The field's value after the stored query became [query], or null to keep [field]. */
internal fun fieldAfterQuery(field: TextFieldValue, query: String, sent: Set<String>): TextFieldValue?
```

The set is cleared whenever an outside change is applied, so it stays small.

**Why a set and not just the last text sent:** a late echo can be any earlier keystroke ("Bl" after "Blu" and "Blue" were sent), not only the last one.

**Rejected:**
- Dropping the effect and reading `state.searchQuery` directly as the field's text: the field would lose its cursor and selection on every keystroke, and the same late echo would overwrite it.
- Moving the field's state into the view model: a bigger change for the same effect.

### Clear and picking a hit

- **Clear** empties the field directly in its `onClick`, besides clearing the stored query, so it never depends on the echo rule.
- **A picked hit** sets the query to the hit's name. That is not among the sent texts, unless the user typed exactly that name, so it replaces the field. Picking a hit closes the search anyway.

## Risks / Trade-offs

- **A hit whose name equals something typed earlier** wouldn't replace the field. → It is rare, and the field then already shows what the user typed. Picking a hit closes the search anyway.
