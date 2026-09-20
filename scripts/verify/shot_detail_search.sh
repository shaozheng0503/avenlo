#!/bin/bash
# 补验证：详情页 + 搜索屏（用 tag chip 与 resource-id 定位）
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

ensure_home() {
  "$ADB" shell "am start -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
  sleep 3
}

wait_device || { echo "FATAL: device not ready"; exit 1; }

echo "=== 1. 详情页：点「生活」tag chip（第一张卡的标签） ==="
ensure_home
dump_to_tmp
# 打印完整 dump 里所有带 bounds 的 text 节点（前 25 个），供诊断
echo "--- all text nodes ---"
grep -o 'text="[^"]*"[^>]*bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' "$PROJ/ui_dump_tmp.xml" 2>/dev/null | head -3
python -c "
import re
with open('$PROJ/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
nodes = re.findall(r'text=\"([^\"]+)\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml)
for t, x1, y1, x2, y2 in nodes[:25]:
    print(f'{t[:28]:30s} [{x1},{y1}][{x2},{y2}]')
"

echo "=== 2. 点第一张卡的标题（今天分组下的卡片标题） ==="
# 卡片标题在「今天」分组头之后，选 y 坐标 > 分组头且 < 底栏的第一张
TARGET=$(python -c "
import re
with open('$PROJ/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
nodes = re.findall(r'text=\"([^\"]+)\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml)
# 找到「今天」分组头之后第一个非 chip 文本节点（卡片标题）
seen_today = False
for t, x1, y1, x2, y2 in nodes:
    if '今天' in t and '条' in t:
        seen_today = True
        continue
    if seen_today and len(t) >= 6 and (y1+y2)//2 < 2100:
        print(f'{(int(x1)+int(x2))//2} {(int(y1)+int(y2))//2}')
        print(f'# target: {t[:30]}', file=__import__('sys').stderr)
        break
" 2>&1)
echo "target: $TARGET"
if [ -n "$TARGET" ]; then
  TAP=$(echo "$TARGET" | head -1)
  "$ADB" shell "input tap $TAP"
  sleep 3
  shot "05_detail"
  echo "--- detail screen text ---"
  dump_first
  "$ADB" shell "input keyevent 4"
  sleep 2
else
  echo "WARN: card title not found"
fi

echo "=== 3. 搜索屏：点搜索框（首页顶部胶囊） ==="
ensure_home
dump_to_tmp
# 搜索框是 TextField with placeholder，试 resource-id 或 hint 属性
python -c "
import re
with open('$PROJ/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
# 找包含搜索的任何属性节点
for m in re.finditer(r'<node[^>]*(?:text|content-desc)=\"[^\"]*搜索[^\"]*\"[^>]*>', xml):
    tag = m.group(0)
    bm = re.search(r'bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', tag)
    if bm:
        x1, y1, x2, y2 = map(int, bm.groups())
        print(f'search_node: [{x1},{y1}][{x2},{y2}] center=({(x1+x2)//2},{(y1+y2)//2})')
        break
"
SEARCH=$(python -c "
import re
with open('$PROJ/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
for m in re.finditer(r'<node[^>]*text=\"[^\"]*搜索[^\"]*\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
if [ -z "$SEARCH" ]; then
  # 搜索框可能无 text（placeholder 在 EditText 里），用固定坐标：顶部胶囊区域
  SEARCH="540 310"
  echo "fallback to fixed pos: $SEARCH"
fi
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
