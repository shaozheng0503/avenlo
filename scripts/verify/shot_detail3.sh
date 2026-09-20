#!/bin/bash
# 详情页专项：坐标提取与 tap 分离（stderr 污染问题修复）
set -u
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
PROJ="C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon"
OUT="$PROJ/emulator-screens"

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
  grep -o 'text="[^"]*"' "$PROJ/ui_dump_tmp.xml" | grep -v 'text=""' | head -10
}

cold_start_home() {
  "$ADB" shell "am force-stop com.hotfix.avenlo"
  sleep 2
  "$ADB" shell "am start -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
  sleep 6
}

wait_device || { echo "FATAL: device not ready"; exit 1; }

echo "=== 1. 冷启动回首页 ==="
cold_start_home

echo "=== 2. 提取卡片坐标（仅 stdout） ==="
dump_to_tmp
TAP=$(python << 'PYEOF'
import re
with open(r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
nodes = re.findall(r'text="([^"]+)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
seen_today = False
for t, x1, y1, x2, y2 in nodes:
    y = (int(y1)+int(y2))//2
    if '今天' in t and '条' in t and y < 600:
        seen_today = True
        continue
    if seen_today and len(t) >= 6 and y < 2100:
        print(f'{(int(x1)+int(x2))//2} {y}')
        break
PYEOF
)
echo "tap coords: [$TAP]"
if [ -n "$TAP" ]; then
  "$ADB" shell "input tap $TAP"
  sleep 3
  shot "05_detail"
  echo "--- detail text ---"
  dump_first

  echo "=== 3. 延展思路 Tab ==="
  EXT=$(python << 'PYEOF'
import re
with open(r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
for m in re.finditer(r'text="[^"]*延展[^"]*"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
PYEOF
)
  echo "extend tab at: [$EXT]"
  if [ -n "$EXT" ]; then
    "$ADB" shell "input tap $EXT"
    sleep 2
    shot "05b_detail_extend"
    dump_first
  fi
  "$ADB" shell "input keyevent 4"
  sleep 2
else
  echo "WARN: no card found"
fi

rm -f "$PROJ/ui_dump_tmp.xml"
echo "=== done ==="
