#!/bin/sh
# Prints the epoch seconds of the next Saturday 12:00 in Amsterdam strictly after now, the moment
# the store screenshots are taken at, see `store-screenshots` "A fixed moment".
# Usage: coming-saturday.sh [now as epoch seconds]
set -eu

python3 - "${1:-}" <<'PYTHON'
import sys
from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo

amsterdam = ZoneInfo("Europe/Amsterdam")
now = (
    datetime.fromtimestamp(int(sys.argv[1]), timezone.utc)
    if sys.argv[1]
    else datetime.now(timezone.utc)
).astimezone(amsterdam)
saturday = now.replace(hour=12, minute=0, second=0, microsecond=0) + timedelta(
    days=(5 - now.weekday()) % 7
)
if saturday <= now:
    saturday += timedelta(days=7)
print(int(saturday.timestamp()))
PYTHON
