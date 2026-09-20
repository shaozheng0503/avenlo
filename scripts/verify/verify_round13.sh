#!/bin/bash
# 第十三轮验证：搜索真实查询 / 回顾接 server / 详情 server 优先 / 灵感集 Tone 修复
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

cold_start_home() {
  "$ADB" shell "am force-stop com.hotfix.avenlo"
  sleep 2
  "$ADB" shell "am start -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
  sleep 6
}

wait_device || { echo "FATAL: device not ready"; exit 1; }

echo "=== step1: 装新 APK ==="
"$ADB" install -r "$APK" 2>&1 | tail -1

echo "=== step2: 冷启动（server 8 张种子 + 日期动态） ==="
cold_start_home
dump_first

echo "=== step3: 搜索真实查询：输入「旅行」 ==="
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
echo "search at: [$SEARCH]"
if [ -n "$SEARCH" ]; then
  "$ADB" shell "input tap $SEARCH"
  sleep 2
  # 中文输入用 adb shell input text 不支持，改用 ASCII 关键词测 server 内容匹配
  "$ADB" shell "input text 'idea'" 2>/dev/null || true
  sleep 3
  shot "11_search_live"
  echo "--- search results ---"
  dump_first
  # 清空重输
  "$ADB" shell "input keyevent 67" ; "$ADB" shell "input keyevent 67" ; "$ADB" shell "input keyevent 67" ; "$ADB" shell "input keyevent 67"
  sleep 1
  "$ADB" shell "input keyevent 111"
  sleep 1
  "$ADB" shell "input keyevent 4"
  sleep 2
fi

echo "=== step4: 统计 Tab（今日回顾接 server） ==="
"$ADB" shell "am start -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
sleep 3
"$ADB" shell "input tap 675 2300"
sleep 4
shot "12_review_server"
dump_first
"$ADB" shell "input tap 135 2300"
sleep 2

echo "=== step5: 灵感集（Tone 枚举 + server 数据） ==="
"$ADB" shell "input tap 990 310"
sleep 4
shot "13_collections_server"
dump_first
"$ADB" shell "input keyevent 4"
sleep 2

echo "=== step6: 详情页 idea_01（server extension 优先） ==="
cold_start_home
dump_to_tmp
TAP=$(python << 'PYEOF'
import re
with open(r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
nodes = re.findall(r'text="([^"]+)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
# 找「关于旅行的灵感」标题（server idea_01）
for t, x1, y1, x2, y2 in nodes:
    if '旅行' in t and len(t) >= 4:
        print(f'{(int(x1)+int(x2))//2} {(int(y1)+int(y2))//2}')
        break
PYEOF
)
echo "idea_01 card at: [$TAP]"
if [ -n "$TAP" ]; then
  "$ADB" shell "input tap $TAP"
  sleep 4
  shot "14_detail_server_ext"
  dump_first
fi

rm -f "$PROJ/ui_dump_tmp.xml"
echo "=== done ==="
