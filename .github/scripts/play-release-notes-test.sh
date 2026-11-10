#!/bin/sh
# Tests for play-release-notes.sh. Run from the repository root.
set -eu

script=.github/scripts/play-release-notes.sh
more="More: github.com/mkutz/KnoopByKnoop/blob/main/CHANGELOG.md"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

chars() { printf '%s' "$1" | python3 -c 'import sys; print(len(sys.stdin.read()))'; }

# Release notes: short notes stay as they are.
notes='- Navigate a route knot by knot
- Release pipelines'
actual=$(printf '%s\n' "$notes" | sh "$script")
[ "$actual" = "$notes" ] || fail "short notes are unchanged, got: $actual"

# Scenario: Long release notes
# Whole entries that fit, then the pointer, within the limit.
long='- First entry with some words in it, about forty
- Second entry with some words in it, about forty
- Third entry with some words in it, about forty-two'
actual=$(printf '%s\n' "$long" | sh "$script" 150)
expected="- First entry with some words in it, about forty
$more"
[ "$actual" = "$expected" ] || fail "whole entries and the pointer, got: $actual"
[ "$(chars "$actual")" -le 150 ] || fail "within the limit, got $(chars "$actual")"

# Umlauts count as one character each.
actual=$(printf '%s\n' '- Änderung für Ölüberführung' | sh "$script" 28)
[ "$actual" = '- Änderung für Ölüberführung' ] || fail "characters, not bytes, got: $actual"

# A single entry too long on its own is cut.
actual=$(printf '%s\n' "- $(printf 'x%.0s' $(seq 1 200))" | sh "$script" 100)
[ "$(chars "$actual")" -le 100 ] || fail "a long entry is cut to the limit, got $(chars "$actual")"
case "$actual" in *"…"*) ;; *) fail "a cut entry ends with …" ;; esac

echo "play-release-notes.sh: all tests passed"
