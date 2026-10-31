#!/usr/bin/env bash
# End-to-end checks for Baaja on a real phone, driven like a person would: launch, tap, read the screen.
# The screen is read through Android's accessibility dump (uiautomator), so it checks what a user sees.
#
#   e2e/baaja_e2e.sh [package]        default package: com.baaja.music.debug
#
# Needs adb connected to one phone with the app installed. Plays one YouTube song for a few seconds.
# Optional: LOCAL_SONG="<title in On this device>" also plays that file from the phone.
set -uo pipefail

PKG="${1:-com.baaja.music.debug}"
ADB="${ADB:-adb}"
OUT="$(mktemp -d)"
PASS=0
FAIL=0

dump() {
  "$ADB" shell uiautomator dump /sdcard/baaja_e2e.xml >/dev/null 2>&1
  "$ADB" pull /sdcard/baaja_e2e.xml "$OUT/screen.xml" >/dev/null 2>&1
  "$ADB" shell rm -f /sdcard/baaja_e2e.xml
}

# Centre "x y" of the first (or, with $2 = last, the lowest) node whose text or content-desc matches the
# regex $1, from the last dump. Prints nothing if there is no match or no dump.
centre_of() {
  [[ -f "$OUT/screen.xml" ]] || return 0
  python3 - "$OUT/screen.xml" "$1" "${2:-first}" <<'PY'
import re, sys
xml, pattern, which = open(sys.argv[1], encoding="utf-8").read(), re.compile(sys.argv[2]), sys.argv[3]
hits = []
for node in re.finditer(r'<node [^>]*>', xml):
    n = node.group(0)
    text = (re.search(r' text="([^"]*)"', n) or [None, ""])[1]
    desc = (re.search(r' content-desc="([^"]*)"', n) or [None, ""])[1]
    if pattern.search(text) or pattern.search(desc):
        x1, y1, x2, y2 = map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups())
        hits.append(((x1 + x2) // 2, (y1 + y2) // 2))
if hits:
    x, y = hits[-1] if which == "last" else hits[0]
    print(x, y)
PY
}

on_screen() { dump; [[ -n "$(centre_of "$1")" ]]; }
not_on_screen() { dump; [[ -z "$(centre_of "$1")" ]]; }

# Waits up to $2 seconds (default 10) for the regex $1 to be on screen.
wait_for() {
  local i
  for ((i = 0; i < ${2:-10}; i++)); do
    on_screen "$1" && return 0
    sleep 1
  done
  return 1
}

tap() {
  dump
  local xy
  xy="$(centre_of "$1" "${2:-first}")"
  [[ -n "$xy" ]] && "$ADB" shell input tap $xy
}

# Scrolls the page down until the regex $1 is on screen, up to $2 swipes (default 4).
scroll_to() {
  local i
  for ((i = 0; i <= ${2:-4}; i++)); do
    on_screen "$1" && return 0
    "$ADB" shell input swipe 540 1600 540 900 300
    sleep 1
  done
  return 1
}

check() {
  local name="$1"
  shift
  if "$@"; then
    PASS=$((PASS + 1))
    echo "PASS  $name"
  else
    FAIL=$((FAIL + 1))
    "$ADB" exec-out screencap -p >"$OUT/fail_$FAIL.png"
    echo "FAIL  $name   (screenshot: $OUT/fail_$FAIL.png)"
  fi
}

app_alive() { [[ -n "$("$ADB" shell pidof "$PKG")" ]]; }
healthy() { app_alive && not_on_screen 'Report this crash on GitHub|Baaja Crash Report'; }
scroll_home() { "$ADB" shell input swipe 540 1700 540 600 300; "$ADB" shell input swipe 540 1700 540 600 300; sleep 2; healthy; }
open_trending() { tap 'Songs, artists' && wait_for '^Trending searches$' 15; }
open_player() { tap 'Despacito' && wait_for '^NOW PLAYING$|^Now playing$' 10; }
labelled_share_like() { on_screen '^Share$' && on_screen '^Like$|^Liked$'; }
artwork_keeps_player() {
  "$ADB" shell input tap 540 800 # the artwork used to open the removed Immersive player
  sleep 2
  on_screen '^(OPUS|AAC) · ' && not_on_screen 'Exit immersive|Tap the record'
}
liked_tile_hint() { on_screen '^Tap ♡ on any song$|^[0-9]+ songs?$'; }
play_local() { tap "$LOCAL_SONG" && sleep 3 && tap "$LOCAL_SONG" last && wait_for '^[A-Z0-9-]+ · ([0-9]+ kbps|[0-9]+-bit · [0-9.]+ kHz|[0-9.]+ kHz)$' 15; }

# ---- journeys ----

"$ADB" shell am force-stop "$PKG"
"$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1

check "Home opens with Speed dial" wait_for '^Speed dial$' 20
check "Home has Quick picks" scroll_to '^Quick picks$'
check "Home scrolls without crashing" scroll_home

tap '^Search$'
check "Search tab opens on Explore" wait_for '^Explore$' 10
check "Empty search shows trending searches" open_trending
"$ADB" shell input keyevent KEYCODE_BACK # closes the keyboard and the search box, staying in the app
tap '^Home$'

# A shared song link opened while Baaja is closed (1.3.2 dropped it and restored the old queue).
"$ADB" shell am force-stop "$PKG"
"$ADB" shell am start -a android.intent.action.VIEW -d "https://music.youtube.com/watch?v=kJQP7kiw5Fk" "$PKG" >/dev/null
check "A song link opened from a closed app plays that song" wait_for '^Despacito$' 25
check "Mini player has a labelled Next button" wait_for '^Next$' 10
check "Player opens from the mini player" open_player
check "Player shows the stream quality" wait_for '^(OPUS|AAC) · [0-9]+ kbps$' 20
check "Share and Like are labelled buttons" labelled_share_like
check "Tapping the artwork keeps the normal player (no Immersive)" artwork_keeps_player
"$ADB" shell input keyevent KEYCODE_MEDIA_PAUSE
"$ADB" shell input keyevent KEYCODE_BACK

tap '^Library$'
check "Library opens" wait_for '^Liked songs$' 10
check "Liked tile shows a hint or a count" liked_tile_hint

if [[ -n "${LOCAL_SONG:-}" ]]; then
  tap '^On this device$'
  check "On this device lists the file" wait_for "$LOCAL_SONG" 15
  check "A file on the phone plays with its quality shown" play_local
  "$ADB" shell input keyevent KEYCODE_MEDIA_PAUSE
fi

check "App still running, no crash screen" healthy

echo
echo "End-to-end: $PASS passed, $FAIL failed"
[[ $FAIL -eq 0 ]]
