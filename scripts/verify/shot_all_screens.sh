#!/bin/bash
# 遍历 8 屏截图验证：首页 -> 详情 -> 灵感集 -> 搜索 -> 回顾 -> 我的（捕捉屏已验证过）
# 依赖：模拟器在跑、App 已装（默认版 APK 10.0.2.2）
set -u
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
PROJ="C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon"
OUT="$PROJ/emulator-screens"
mkdir -p "$OUT"

wait_device() {
  for i in $(seq 1 12); do
    ST=$("$ADB" get-state 2>/dev/null)
    if [ "$ST" = "device" ]; then return 0; fi
    sleep 5
  done
  return 1
}

shot() {  # $1=文件名
  "$ADB" shell "screencap -p /sdcard/s.png"
  "$ADB" pull /sdcard/s.png "$OUT/$1.png" > /dev/null 2>&1
  echo "saved $1"
}

dump_first() {  # 打印前几条 text 便于核对
  "$ADB" shell "rm -f /sdcard/ui.xml; uiautomator dump /sdcard/ui.xml" > /dev/null 2>&1
  "$ADB" pull /sdcard/ui.xml "$PROJ/ui_dump_tmp.xml" > /dev/null 2>&1
  grep -o 'text="[^"]*"' "$PROJ/ui_dump_tmp.xml" | grep -v 'text=""' | head -8
}

wait_device || { echo "FATAL: device not ready"; exit 1; }

echo "=== 1. 启动 App 进首页 ==="
"$ADB" shell "am start -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity"
sleep 6
shot "04_home_full"

echo "=== 2. 点第一张卡进详情 ==="
# 第一张卡 bounds 从 dump 找（咖啡馆那张）
"$ADB" shell "rm -f /sdcard/ui.xml; uiautomator dump /sdcard/ui.xml" > /dev/null 2>&1
"$ADB" pull /sdcard/ui.xml "$PROJ/ui_dump_tmp.xml" > /dev/null 2>&1
CARD=$(python -c "
import re
with open('$PROJ/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
for m in re.finditer(r'text=\"[^\"]{4,}\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1, y1, x2, y2 = map(int, m.groups())
    # 只点列表区域内的卡标题（避开顶栏和底栏）
    if 400 < (y1+y2)//2 < 2100:
        print(f'{(x1+x2)//2} {(y1+y2)//2}')
        break
")
echo "card at: $CARD"
if [ -n "$CARD" ]; then
  "$ADB" shell "input tap $CARD"
  sleep 3
  shot "05_detail"
  dump_first
  # 返回首页
  "$ADB" shell "input keyevent 4"
  sleep 2
fi

echo "=== 3. 底部导航：记录 Tab ==="
# 底栏 4 Tab：首页/记录/统计/我的 —— 屏宽 1080，4 等分
"$ADB" shell "input tap 405 2300"
sleep 2
shot "06_records_tab"
dump_first

echo "=== 4. 底部导航：统计 Tab ==="
"$ADB" shell "input tap 675 2300"
sleep 2
shot "07_stats_tab"
dump_first

echo "=== 5. 底部导航：我的 Tab ==="
"$ADB" shell "input tap 945 2300"
sleep 2
shot "08_profile_tab"
dump_first

echo "=== 6. 回首页 -> 搜索 ==="
"$ADB" shell "input tap 135 2300"
sleep 2
# 点搜索框（顶部）
"$ADB" shell "rm -f /sdcard/ui.xml; uiautomator dump /sdcard/ui.xml" > /dev/null 2>&1
"$ADB" pull /sdcard/ui.xml "$PROJ/ui_dump_tmp.xml" > /dev/null 2>&1
SEARCH=$(python -c "
import re
with open('$PROJ/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
for m in re.finditer(r'text=\"搜索灵感、关键词、标签\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
echo "search at: $SEARCH"
if [ -n "$SEARCH" ]; then
  "$ADB" shell "input tap $SEARCH"
  sleep 2
  # 输入搜索词
  "$ADB" shell "input text 'city'"
  sleep 2
  shot "09_search"
  dump_first
  "$ADB" shell "input keyevent 4"
  sleep 1
  "$ADB" shell "input keyevent 4"
  sleep 1
fi

echo "=== 7. 灵感集入口（首页搜索框右侧图标） ==="
"$ADB" shell "am start -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity"
sleep 4
"$ADB" shell "rm -f /sdcard/ui.xml; uiautomator dump /sdcard/ui.xml" > /dev/null 2>&1
"$ADB" pull /sdcard/ui.xml "$PROJ/ui_dump_tmp.xml" > /dev/null 2>&1
# 找灵感集 icon（content-desc 或用固定位置：搜索框右侧）
"$ADB" shell "input tap 990 310"
sleep 2
shot "10_collections"
dump_first

rm -f "$PROJ/ui_dump_tmp.xml"
echo "=== done ==="
