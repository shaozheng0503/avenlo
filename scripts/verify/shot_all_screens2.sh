#!/bin/bash
# 补验证：详情页 + 记录/统计/我的 Tab + 搜索（修正版）
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
  grep -o 'text="[^"]*"' "$PROJ/ui_dump_tmp.xml" | grep -v 'text=""' | head -8
}

ensure_home() {  # 确保 App 在前台且在首页
  "$ADB" shell "am start -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
  sleep 3
}

wait_device || { echo "FATAL: device not ready"; exit 1; }

echo "=== 1. 首页 -> 点具体卡片进详情 ==="
ensure_home
dump_to_tmp
CARD=$(python -c "
import re
with open('$PROJ/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
# 找卡片摘要文本（长文本，属于卡片 body），点它的坐标
for kw in ['口述', '咖啡馆', '磨豆']:
    for m in re.finditer(r'text=\"[^\"]*' + kw + r'[^\"]*\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
        x1, y1, x2, y2 = map(int, m.groups())
        print(f'{(x1+x2)//2} {(y1+y2)//2}')
        break
    else:
        continue
    break
")
echo "card body at: $CARD"
if [ -n "$CARD" ]; then
  "$ADB" shell "input tap $CARD"
  sleep 3
  shot "05_detail"
  dump_first
else
  echo "WARN: card not found"
fi

echo "=== 2. 返回首页 ==="
"$ADB" shell "input keyevent 4"
sleep 2

echo "=== 3. 记录 Tab ==="
ensure_home
"$ADB" shell "input tap 405 2300"
sleep 2
shot "06_records_tab"
dump_first

echo "=== 4. 统计 Tab ==="
ensure_home
"$ADB" shell "input tap 675 2300"
sleep 2
shot "07_stats_tab"
dump_first

echo "=== 5. 我的 Tab ==="
ensure_home
"$ADB" shell "input tap 945 2300"
sleep 2
shot "08_profile_tab"
dump_first

echo "=== 6. 搜索 ==="
ensure_home
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
echo "search at: $SEARCH"
if [ -n "$SEARCH" ]; then
  "$ADB" shell "input tap $SEARCH"
  sleep 2
  "$ADB" shell "input text 'city'"
  sleep 3
  shot "09_search"
  dump_first
  "$ADB" shell "input keyevent 111"  # ESC 关键盘
  sleep 1
fi

rm -f "$PROJ/ui_dump_tmp.xml"
echo "=== done ==="
