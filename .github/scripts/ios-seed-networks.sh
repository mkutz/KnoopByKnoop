#!/bin/sh
# Puts the published networks and their courses into the iOS app's storage in a simulator, as the
# app installs them after downloading, see `network-sync`. For the store screenshots, see
# `store-screenshots`: on CI simulators the app's background downloads never finish.
# Usage: ios-seed-networks.sh <simulator id>
set -eu

device=$1
base=https://github.com/mkutz/knoopbyknoop/releases/download/network-data
container=$(xcrun simctl get_app_container "$device" com.knoopbyknoop data)
networks="$container/Library/Application Support/networks"
index=$(mktemp)
trap 'rm -f "$index"' EXIT

mkdir -p "$networks"
curl --fail --location --silent --show-error "$base/index.json" -o "$index"
# Graphs and courses together: the courses are read only with the graph they were made for.
files=$(python3 -c '
import json, sys
print(" ".join(f["file"] for f in json.load(open(sys.argv[1]))["files"] if f["kind"] in ("graph", "geometry")))' "$index")
for file in $files; do
  curl --fail --location --silent --show-error "$base/$file" -o "$networks/$file"
done
python3 - "$index" "$networks/installed.json" << 'PYTHON'
import json, sys
index = json.load(open(sys.argv[1]))
files = [f for f in index["files"] if f["kind"] in ("graph", "geometry")]
json.dump({"formatVersion": index["formatVersion"], "generatedAt": "", "files": files}, open(sys.argv[2], "w"), indent=2)
PYTHON
echo "Seeded: $files"
