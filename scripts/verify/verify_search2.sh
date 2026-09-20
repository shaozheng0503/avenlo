#!/bin/bash
# 搜索正向路径补充验证：用 tag chip 点击填入 query 不可行（chip 无点击填词逻辑），
# 改用筛选 pill「今天」验证 filter 逻辑（应显示 3 条今天的卡）
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
  grep -o 'text="[^"]*"' "$PROJ/ui_dump_tmp.xml" | grep -v 'text=""' | head -12
}

wait_device || { echo "FATAL: device not ready"; exit 1; }

echo "=== 1. 冷启动进首页 ==="
"$ADB" shell "am force-stop com.hotfix.avenlo"; sleep 2
"$ADB" shell "am start -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
sleep 6

echo "=== 2. 点搜索框进搜索屏 ==="
dump_to_tmp
SEARCH=$(python << 'PYEOF'
import re
with open(r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
for m in re.finditer(r'text="[^"]*搜索[^"]*"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
PYEOF
)
"$ADB" shell "input tap $SEARCH"
sleep 2

echo "=== 3. 点筛选 pill「今天」(y~?, 从 dump 找) ==="
dump_to_tmp
TODAY=$(python << 'PYEOF'
import re
with open(r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
for m in re.finditer(r'text="今天"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
PYEOF
)
echo "today pill at: [$TODAY]"
if [ -n "$TODAY" ]; then
  "$ADB" shell "input tap $TODAY"
  sleep 2
  shot "15_search_filter_today"
  echo "--- filtered results（无 query 时 pill 本身不触发搜索结果区，需配合 query） ---"
  dump_first
fi

echo "=== 4. 输入英文占位 + 今天筛选（验证结果数=今天卡数） ==="
dump_to_tmp
SEARCH2=$(python << 'PYEOF'
import re
with open(r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
# TextField 现在是真输入框：找 EditText 类节点
for m in re.finditer(r'class="android.widget.EditText"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
PYEOF
)
echo "edittext at: [$SEARCH2]"
if [ -n "$SEARCH2" ]; then
  "$ADB" shell "input tap $SEARCH2"
  sleep 1
  "$ADB" shell "input text 'cafe'"
  sleep 3
  shot "16_search_cafe_today"
  echo "--- results ---"
  dump_first
fi

rm -f "$PROJ/ui_dump_tmp.xml"
echo "=== done ==="
