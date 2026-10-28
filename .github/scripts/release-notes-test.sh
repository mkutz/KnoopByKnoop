#!/bin/sh
# Tests for release-notes.sh. Run from the repository root.
set -eu

script=.github/scripts/release-notes.sh
changelog=$(mktemp)
trap 'rm -f "$changelog"' EXIT

cat > "$changelog" <<'CHANGELOG'
# Changelog

## Unreleased

- Something not yet released

## 0.2.0

- Navigate a route knot by knot
- Release pipelines

## 0.1.0

- Project setup
CHANGELOG

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

# Release notes
expected='- Navigate a route knot by knot
- Release pipelines'
actual=$(sh "$script" 0.2.0 "$changelog") || fail "the section for 0.2.0 is found"
[ "$actual" = "$expected" ] || fail "only the entries of 0.2.0, got: $actual"

actual=$(sh "$script" 0.1.0 "$changelog") || fail "the last section is found"
[ "$actual" = "- Project setup" ] || fail "the last section ends at the end of the file, got: $actual"

# Missing changelog section
if sh "$script" 0.3.0 "$changelog" 2> /dev/null; then fail "a missing section fails"; fi
if sh "$script" 0.2 "$changelog" 2> /dev/null; then fail "a version prefix is not a section"; fi

printf '# Changelog\n\n## 0.4.0\n\n## 0.3.0\n\n- Old\n' > "$changelog"
if sh "$script" 0.4.0 "$changelog" 2> /dev/null; then fail "an empty section fails"; fi

echo "release-notes.sh: all tests passed"
