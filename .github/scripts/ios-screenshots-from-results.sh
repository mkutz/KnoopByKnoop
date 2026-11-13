#!/bin/sh
# Copies the screenshots the UI test KnoopByKnoopScreenshots attached out of its result bundle,
# as <out-dir>/<n>-<name>.png, see `store-screenshots`.
# Usage: ios-screenshots-from-results.sh <result.xcresult> <out-dir>
set -eu

results=$1
out=$2
exported=$(mktemp -d)
trap 'rm -rf "$exported"' EXIT

xcrun xcresulttool export attachments --path "$results" --output-path "$exported"
mkdir -p "$out"
python3 - "$exported" "$out" << 'PYTHON'
import json, re, shutil, sys
exported, out = sys.argv[1:]
count = 0
for test in json.load(open(f"{exported}/manifest.json")):
    for attachment in test.get("attachments", []):
        name = attachment.get("suggestedHumanReadableName", "")
        match = re.match(r"(\d-[a-z]+)", name)
        if match:
            shutil.copy(f"{exported}/{attachment['exportedFileName']}", f"{out}/{match.group(1)}.png")
            count += 1
if count != 5:
    sys.exit(f"{count} screenshots in the results, expected 5")
PYTHON
