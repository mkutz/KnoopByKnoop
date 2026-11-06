#!/bin/sh
# Fits release notes, read on stdin, into Google Play's limit, see `release-delivery` "Release notes
# from the changelog": the entries in their order, as many whole entries as fit, then a pointer to
# the full changelog when some were left out. An entry too long on its own is cut with "…".
# Counts characters, as Play does, so in Python: awk on macOS and Ubuntu counts bytes.
#
# Usage: release-notes.sh <version> | play-release-notes.sh [limit]
set -eu

exec python3 -c '
import sys

limit = int(sys.argv[1])
more = "More: github.com/mkutz/KnoopByKnoop/blob/main/CHANGELOG.md"
entries = []
for line in sys.stdin.read().splitlines():
    if line.startswith("- "):
        entries.append(line)
    elif line.strip() and entries:
        entries[-1] += "\n" + line

if len("\n".join(entries)) <= limit:
    print("\n".join(entries))
    sys.exit()

budget = limit - len(more) - 1
kept = []
for entry in entries:
    if len("\n".join(kept + [entry])) > budget:
        break
    kept.append(entry)
if not kept:
    kept = [entries[0][: budget - 1] + "…"]
print("\n".join(kept + [more]))
' "${1:-500}"
