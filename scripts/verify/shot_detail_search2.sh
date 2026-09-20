#!/bin/bash
# 补验证 v2：force-stop 冷启动确保从首页开始 -> 详情 -> 搜索
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

cold_start_home() {  # 强制冷启动回首页
  "$ADB" shell "am force-stop com.hotfix.avenlo"
  sleep 2
  "$ADB" shell "am start -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
  sleep 6
}

wait_device || { echo "FATAL: device not ready"; exit 1; }

echo "=== 1. 冷启动回首页 ==="
cold_start_home
dump_first

echo "=== 2. 点今天分组第一张卡标题进详情 ==="
dump_to_tmp
TARGET=$(python -c "
import re
with open('$PROJ/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
nodes = re.findall(r'text=\"([^\"]+)\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml)
seen_today = False
for t, x1, y1, x2, y2 in nodes:
    y = (int(y1)+int(y2))//2
    if '今天' in t and '条' in t and y < 600:
        seen_today = True
        continue
    if seen_today and len(t) >= 6 and y < 2100:
        print(f'{(int(x1)+int(x2))//2} {y}')
        print(f'TITLE: {t[:30]}', file=__import__('sys').stderr)
        break
" 2>&1)
echo "target: $TARGET"
TAP=$(echo "$TARGET" | head -1)
if [ -n "$TAP" ]; then
  "$ADB" shell "input tap $TAP"
  sleep 3
  shot "05_detail"
  echo "--- detail text ---"
  dump_first
  # 详情页四 Tab：相关/延展/参考 —— 点「延展思路」tab
  dump_to_tmp
  EXT=$(python -c "
import re
with open('$PROJ/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
for m in re.finditer(r'text=\"[^\"]*延展[^\"]*\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
  if [ -n "$EXT" ]; then
    echo "=== 3. 详情页「延展思路」Tab ==="
    "$ADB" shell "input tap $EXT"
    sleep 2
    shot "05b_detail_extend"
    dump_first
  fi
  "$ADB" shell "input keyevent 4"
  sleep 2
else
  echo "WARN: card not found"
fi

echo "=== 4. 搜索屏 ==="
cold_start_home
dump_to_tmp
SEARCH=$(python -c "
import re
with open('$PROJ/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
for m in re.finditer(r'text=\"[^\"]*搜索[^\"]*\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
if [ -z "$SEARCH" ]; then SEARCH="540 310"; echo "(fallback fixed pos)"; fi
echo "search at: $SEARCH"
"$ADB" shell "input tap $SEARCH"
sleep 2
"$ADB" shell "input text 'city'"
sleep 3
shot "09_search"
dump_first
"$ADB" shell "input keyevent 111"
sleep 1
"$ADB" shell "input keyevent 4"
sleep 1

rm -f "$PROJ/ui_dump_tmp.xml"
echo "=== done ==="
