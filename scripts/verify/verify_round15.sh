#!/bin/bash
# 第十五轮验证：related 点击链路（idea_11 已落卡）+ 记录 Tab 轮询
set -u
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
PROJ="C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon"
OUT="$PROJ/emulator-screens"
APK="$PROJ/avenlo-android/app/build/outputs/apk/debug/app-debug.apk"

wait_device() {
  for i in $(seq 1 12); do
    ST=$("$ADB" get-state 2>/dev/null)
    if [ "$ST" = "device" ]; then return 0; fi
    sleep 5
  done
  return 1
}

shot() {
  "$ADB" shell "screencap -p /sdcard/s.png"
  "$ADB" pull /sdcard/s.png "$OUT/$1.png" > /dev/null 2>&1
  echo "saved $1"
}

dump_to_tmp() {
  "$ADB" shell "rm -f /sdcard/ui.xml; uiautomator dump /sdcard/ui.xml" > /dev/null 2>&1
  "$ADB" pull /sdcard/ui.xml "$PROJ/ui_dump_tmp.xml" > /dev/null 2>&1
}

dump_first() {
  dump_to_tmp
  grep -o 'text="[^"]*"' "$PROJ/ui_dump_tmp.xml" | grep -v 'text=""' | head -12
}

find_tap() {  # $1=python 匹配表达式
  python << PYEOF
import re
with open(r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
$1
PYEOF
}

wait_device || { echo "FATAL: device not ready"; exit 1; }

echo "=== step1: 装新 APK + 冷启动 ==="
"$ADB" install -r "$APK" 2>&1 | tail -1
"$ADB" shell "am force-stop com.hotfix.avenlo"; sleep 2
"$ADB" shell "am start -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
sleep 6

echo "=== step2: 找「关于旅行的灵感」卡（需滚动，今天 3 条在最上） ==="
# 旅行卡是 6 小时前（今天分组）。先 dump 找
dump_to_tmp
TAP=$(find_tap "
for m in re.finditer(r'text=\"关于旅行的灵感\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
if [ -z "$TAP" ]; then
  echo "(not visible, scroll down)"
  "$ADB" shell "input swipe 540 1800 540 700 300"
  sleep 1
  dump_to_tmp
  TAP=$(find_tap "
for m in re.finditer(r'text=\"关于旅行的灵感\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
fi
echo "travel card at: [$TAP]"
if [ -n "$TAP" ]; then
  "$ADB" shell "input tap $TAP"
  sleep 3
  echo "--- detail (idea_01) ---"
  dump_first
  shot "21_detail_idea01"

  echo "=== step3: 点 related「清晨的露水」（idea_11） ==="
  dump_to_tmp
  REL=$(find_tap "
for m in re.finditer(r'text=\"清晨的露水\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
  echo "related idea_11 at: [$REL]"
  if [ -n "$REL" ]; then
    "$ADB" shell "input tap $REL"
    sleep 3
    echo "--- detail (idea_11, 应显示真实卡非空态) ---"
    dump_first
    shot "22_detail_idea11"
  fi
fi

rm -f "$PROJ/ui_dump_tmp.xml"
echo "=== done ==="
