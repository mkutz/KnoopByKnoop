# Contributing

Thanks for your interest in Knoop by Knoop!


## Spec first

This project is spec-driven with [OpenSpec](https://github.com/Fission-AI/OpenSpec).
Every change in behavior starts as a change proposal in `openspec/changes/<change-id>/`:

1. **Propose:** `proposal.md` (why, what, out of scope), spec deltas in `specs/<capability>/spec.md`, and `tasks.md`.
   With Claude Code: `/opsx:propose "your idea"`.
2. **Discuss** the proposal in a pull request or issue before implementing it.
3. **Apply:** implement the tasks; each spec scenario gets a test. With Claude Code: `/opsx:apply`.
4. **Archive:** once merged, move the deltas into `openspec/specs/`. With Claude Code: `/opsx:archive`.

Validate specs with:

```bash
npx @fission-ai/openspec validate --all
```

Pure maintenance (dependency updates, refactorings, build changes) needs no proposal.


## Code

- `./gradlew check` must pass, including formatting. Fix formatting with `./gradlew spotlessApply`.
- Kotlin is formatted with ktfmt (Google style).
- Logic that both apps need belongs in `modules/core` and must not use platform APIs.
- Do not abbreviate names.
