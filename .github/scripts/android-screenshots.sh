#!/bin/sh
# Takes the five store screenshots of the Android app in every app language over adb, see
# `store-screenshots`: the demo route planned, navigating, its waypoint list, a tapped place and
# dark mode. Run from the repository root with the debug build installed on an emulator at
# 1080 × 1920; `adb` must be on the PATH and reach that emulator (ANDROID_SERIAL).
#
# Usage: android-screenshots.sh <out-dir> [clock as epoch seconds] [locales]
# Writes <out-dir>/<locale>/images/phoneScreenshots/<n>-<name>.png, the fastlane layout.
# The clock, if given, is set before the app first starts.
set -eu

out=$1
clock=${2:-}
locales=${3:-en-US de-DE fr-FR nl-NL}
package=com.knoopbyknoop
activity=$package/.android.MainActivity
route=fastlane/screenshots/demo-route.knoop
resources=apps/android/src/main/res
dump=$(mktemp)

# Gives the emulator back as it was, also after a failure: its screen, clock and status bar.
restore() {
  rm -f "$dump"
  adb shell cmd uimode night no > /dev/null 2>&1 || true
  adb shell am broadcast -a com.android.systemui.demo -e command exit > /dev/null 2>&1 || true
  adb shell wm size reset > /dev/null 2>&1 || true
  if [ -n "$clock" ]; then
    adb shell cmd alarm set-time "$(date +%s)000" > /dev/null 2>&1 || true
    adb shell settings put global auto_time 1 > /dev/null 2>&1 || true
  fi
}
trap restore EXIT

# shellcheck source=/dev/null
. fastlane/screenshots/demo.env

fail() {
  echo "android-screenshots: $1" >&2
  adb logcat -d -s Navigation 2> /dev/null | tail -5 >&2 || true
  adb exec-out screencap -p > "$out/failure.png" 2> /dev/null || true
  exit 1
}

pause() { python3 -c 'import sys, time; time.sleep(float(sys.argv[1]))' "$1"; }

# The app's text for a string resource in the current language, with any `%1$s` part and what
# follows cut off, so it matches the start of a formatted text.
text() {
  python3 - "$resources" "$language" "$1" << 'PYTHON'
import re, sys, xml.etree.ElementTree as tree
resources, language, name = sys.argv[1:]
for folder in (f"values-{language}", "values"):
    try:
        for element in tree.parse(f"{resources}/{folder}/strings.xml").getroot():
            if element.get("name") == name:
                value = "".join(element.itertext()).replace("\\'", "'").replace('\\"', '"')
                print(re.split(r"\s*·?\s*%\d\$", value)[0])
                sys.exit(0)
    except FileNotFoundError:
        pass
sys.exit(f"no string {name}")
PYTHON
}

# Dumps the screen's elements into $dump. A failed dump leaves an older file behind on the device,
# so it is deleted first and the dump's own message is checked.
look() {
  adb shell rm -f /sdcard/ui.xml
  adb shell uiautomator dump /sdcard/ui.xml 2>&1 | grep -q 'dumped to' || return 1
  adb shell cat /sdcard/ui.xml > "$dump"
}

# The centre "x y" of the first element whose text or description starts with $1, if any; with
# $2 "exact", of the first whose description is $1, e.g. a button that has the same word as texts.
find_element() {
  python3 - "$dump" "$1" "${2:-}" << 'PYTHON'
import re, sys
xml, wanted, exact = open(sys.argv[1], encoding="utf-8").read(), sys.argv[2], sys.argv[3] == "exact"
for node in re.finditer(r"<node [^>]*>", xml):
    attributes = dict(re.findall(r'([\w-]+)="([^"]*)"', node.group(0)))
    if "EditText" in attributes.get("class", ""):
        continue
    texts = [attributes.get("text", ""), attributes.get("content-desc", "")]
    if exact:
        found = attributes.get("content-desc", "").replace("&amp;", "&") == wanted
    else:
        found = any(t.replace("&amp;", "&").startswith(wanted) for t in texts if t)
    if found:
        left, top, right, bottom = map(int, re.findall(r"\d+", attributes["bounds"]))
        print((left + right) // 2, (top + bottom) // 2)
        break
PYTHON
}

# Whether an element starting with $1 shows within $2 seconds (30); sets $x and $y to its centre.
# $3 "exact" matches a description exactly, see find_element.
appears() {
  seconds=${2:-30}
  while [ "$seconds" -gt 0 ]; do
    if look; then
      position=$(find_element "$1" "${3:-}")
      if [ -n "$position" ]; then
        x=${position% *}
        y=${position#* }
        return 0
      fi
    fi
    pause 1
    seconds=$((seconds - 1))
  done
  return 1
}

# Waits up to $2 seconds (30) for an element starting with $1 and sets $x and $y to its centre.
# Not called in a subshell, so its failure ends the script.
wait_for() {
  appears "$1" "${2:-30}" "${3:-}" || fail "\"$1\" did not show in $language"
}

# The text of the first text field in $dump.
field_text() {
  python3 - "$dump" << 'PYTHON'
import re, sys
for node in re.finditer(r"<node [^>]*>", open(sys.argv[1], encoding="utf-8").read()):
    attributes = dict(re.findall(r'([\w-]+)="([^"]*)"', node.group(0)))
    if "EditText" in attributes.get("class", ""):
        print(attributes.get("text", "").replace("&amp;", "&"))
        break
PYTHON
}

# Waits up to $2 seconds (300) until no element starts with $1.
wait_gone() {
  seconds=${2:-300}
  while [ "$seconds" -gt 0 ]; do
    if look && [ -z "$(find_element "$1")" ]; then
      return 0
    fi
    pause 2
    seconds=$((seconds - 2))
  done
  fail "\"$1\" still showed in $language"
}

# Whether an element starting with $1 shows now.
shows() { look && [ -n "$(find_element "$1")" ]; }

tap() { adb shell input tap "$1" "$2"; }

# Fails with the question's text when Navigate asks about a closed ferry, see `store-screenshots`
# "Closed ferry".
no_closed_ferry() {
  title=$(text ferry_detour_title)
  if shows "$title"; then
    question=$(python3 -c '
import re, sys
xml = open(sys.argv[1], encoding="utf-8").read()
print(" ".join(t for t in re.findall(r"text=\"([^\"]+)\"", xml)))' "$dump")
    fail "a ferry of the demo route is closed at the screenshots' moment: $question"
  fi
}

# Waits until the screen stops changing (map tiles loaded): three equal frames 3 s apart, at most
# 90 seconds.
settle() {
  previous=""
  same=0
  for _ in $(seq 30); do
    pause 3
    current=$(adb exec-out screencap -p | shasum | cut -d ' ' -f 1)
    if [ "$current" = "$previous" ]; then
      same=$((same + 1))
      [ "$same" -ge 2 ] && return 0
    else
      same=0
    fi
    previous=$current
  done
  echo "android-screenshots: the screen kept changing in $language, taking it anyway" >&2
}

shot() {
  settle
  adb exec-out screencap -p > "$shots/$1.png"
  echo "$language: $1"
}

# Shows the demo route as opened from a file, with the map fitted to it and the panel down.
open_route() {
  adb shell "run-as $package sh -c 'mkdir -p files && cat > files/demo-route.knoop'" < "$route"
  adb shell am start -a android.intent.action.VIEW -t text/markdown \
    -d "file:///data/user/0/$package/files/demo-route.knoop" -n "$activity" > /dev/null
  wait_for "$(text navigate)" 180
  # The map along the route is loaded after the route shows.
  pause 10
}

geo_fix() {
  latitude=${1%,*}
  longitude=${1#*,}
  adb emu geo fix "$longitude" "$latitude" > /dev/null
}

demo_mode() {
  adb shell settings put global sysui_demo_allowed 1
  for command in "enter" "clock -e hhmm 1200" "battery -e level 100 -e plugged false" \
    "network -e wifi show -e level 4 -e fully true" "network -e mobile hide" \
    "notifications -e visible false"; do
    # shellcheck disable=SC2086
    adb shell am broadcast -a com.android.systemui.demo -e command $command > /dev/null
  done
}

adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell wm size 1080x1920
adb shell settings put system screen_off_timeout 1800000
adb shell pm clear "$package" > /dev/null
adb shell pm grant "$package" android.permission.ACCESS_FINE_LOCATION
adb shell pm grant "$package" android.permission.POST_NOTIFICATIONS 2> /dev/null || true
if [ -n "$clock" ]; then
  adb shell settings put global auto_time 0
  adb shell cmd alarm set-timezone Europe/Amsterdam
  adb shell cmd alarm set-time "${clock}000"
fi
demo_mode
geo_fix "${RIDE_POSITIONS%% *}"

for locale in $locales; do
  language=${locale%-*}
  shots="$out/$locale/images/phoneScreenshots"
  mkdir -p "$shots"
  adb shell cmd uimode night no
  adb shell cmd locale set-app-locales "$package" --locales "$locale"
  adb shell am force-stop "$package"

  # 1: the planned route with the panel.
  open_route
  shot 1-route

  # 3: the waypoint list, with the panel pulled up from just above the route's length.
  wait_for "11 km"
  adb shell input swipe "$x" $((y - 120)) "$x" 300 600
  pause 2
  shot 3-waypoints

  # 2: navigating between 03 and 21, started on the route nearby, which needs the courses: after
  # clearing the app's data they download again.
  wait_gone "$(text loading_courses)"
  wait_gone "$(text courses_loading)"
  for position in $RIDE_POSITIONS; do
    geo_fix "$position"
    pause 1
  done
  # Navigate asks for a position at most 60 s old, which the emulator only reports for new fixes,
  # so they keep coming while it looks. The question also needs the courses; without either,
  # navigation starts towards the start, and is stopped to ask again.
  last=${RIDE_POSITIONS##* }
  asked=false
  for _ in 1 2 3 4 5; do
    (
      for _ in $(seq 15); do
        geo_fix "$last"
        pause 1
      done
    ) &
    wait_for "$(text navigate)"
    tap "$x" "$y"
    if appears "$(text start_nearby)" 15; then
      tap "$x" "$y"
      asked=true
      break
    fi
    if appears "$(text stop)" 5 exact; then tap "$x" "$y"; fi
    pause 20
  done
  [ "$asked" = true ] || fail "no question to start nearby in $language"
  pause 3
  no_closed_ferry
  wait_for "$(text stop)" 30 exact
  for position in $RIDE_POSITIONS; do
    geo_fix "$position"
    pause 2
  done
  shot 2-navigate
  wait_for "$(text stop)" 30 exact
  tap "$x" "$y"
  pause 2

  # 5: the planned route in dark mode.
  adb shell cmd uimode night yes
  pause 2
  open_route
  shot 5-dark
  adb shell cmd uimode night no
  pause 2
  # 4: a highlighted place, found by search so the map centres on it, then tapped. Typed at once
  # and searched with Enter, so the public search service gets few requests.
  open_route
  wait_for "$(text search_places)"
  tap "$x" "$y"
  pause 1
  adb shell input text "$(echo "$PLACE_SEARCH" | sed 's/ /%s/g')"
  pause 1
  look || true
  typed=$(field_text)
  [ "$typed" = "$PLACE_SEARCH" ] || fail "typed \"$typed\" instead of \"$PLACE_SEARCH\" in $language"
  adb shell input keyevent KEYCODE_ENTER
  wait_for "$PLACE_NAME" 30
  tap "$x" "$y"
  pause 3
  # The place is in the middle of the map, above the panel.
  look
  map_bottom=$(python3 -c '
import re, sys
xml = open(sys.argv[1], encoding="utf-8").read()
for node in re.finditer(r"<node [^>]*>", xml):
    if "MapLibre" in node.group(0):
        print(re.findall(r"\d+", re.search(r"bounds=\"([^\"]*)\"", node.group(0)).group(1))[3])
        break' "$dump")
  # The place card is a popup, which uiautomator does not see; the review checks it shows.
  tap 540 $((map_bottom / 2))
  pause 2
  shot 4-place
  adb shell input keyevent KEYCODE_BACK
  pause 1

done

