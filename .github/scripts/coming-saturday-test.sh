#!/bin/sh
# Tests for coming-saturday.sh. Run from the repository root.
set -eu

script=.github/scripts/coming-saturday.sh

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

# Epoch seconds of a UTC time, e.g. epoch 2026-11-18T19:00:00.
epoch() { python3 -c 'import sys; from datetime import datetime, timezone; print(int(datetime.fromisoformat(sys.argv[1]).replace(tzinfo=timezone.utc).timestamp()))' "$1"; }

# Scenario: Coming Saturday
# Wednesday 20:00 in Amsterdam (CET, UTC+1).
actual=$(sh "$script" "$(epoch 2026-11-18T19:00:00)")
[ "$actual" = "$(epoch 2026-11-21T11:00:00)" ] || fail "Wednesday gives Saturday 12:00, got $actual"

# Scenario: Run on a Saturday afternoon
actual=$(sh "$script" "$(epoch 2026-11-21T14:00:00)")
[ "$actual" = "$(epoch 2026-11-28T11:00:00)" ] || fail "Saturday 15:00 gives next Saturday, got $actual"

actual=$(sh "$script" "$(epoch 2026-11-21T10:00:00)")
[ "$actual" = "$(epoch 2026-11-21T11:00:00)" ] || fail "Saturday 11:00 gives the same day, got $actual"

# Scenario: Summer time
actual=$(sh "$script" "$(epoch 2027-06-09T12:00:00)")
[ "$actual" = "$(epoch 2027-06-12T10:00:00)" ] || fail "in summer, 12:00 is 10:00 UTC, got $actual"

now=$(date +%s)
actual=$(sh "$script")
[ "$actual" -gt "$now" ] && [ $((actual - now)) -le $((7 * 24 * 3600)) ] || fail "at most seven days ahead, got $actual"

echo "coming-saturday.sh: all tests passed"
