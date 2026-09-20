#!/bin/bash
# 第十四轮验证：记录 Tab / 我的页统计 / 新建灵感集 / 悬空引用空态
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
  "$ADB" shell "am force-stop com.hotfix.avenlo"; sleep 2
  "$ADB" shell "am start -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
  sleep 6
}

wait_device || { echo "FATAL: device not ready"; exit 1; }

echo "=== step1: 装新 APK ==="
"$ADB" install -r "$APK" 2>&1 | tail -1

echo "=== step2: 记录 Tab（S1 时间线） ==="
cold_start_home
"$ADB" shell "input tap 405 2300"
sleep 4
shot "17_records_tab"
dump_first

echo "=== step3: 我的页统计（真实数据） ==="
"$ADB" shell "input tap 945 2300"
sleep 4
shot "18_mine_stats"
dump_first

echo "=== step4: 灵感集 -> 新建对话框 ==="
cold_start_home
"$ADB" shell "input tap 990 310"
sleep 3
# 点右上「+」（页头右侧）
"$ADB" shell "input tap 985 175"
sleep 2
shot "19_create_dialog"
dump_first
# 输入名称（英文，adb 限制）
"$ADB" shell "input text 'DemoTest'"
sleep 1
# 点「创建」按钮（对话框右下）
dump_to_tmp
CREATE=$(python << 'PYEOF'
import re
with open(r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
for m in re.finditer(r'text="创建"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
PYEOF
)
echo "create btn at: [$CREATE]"
if [ -n "$CREATE" ]; then
  "$ADB" shell "input tap $CREATE"
  sleep 3
  shot "20_created"
  echo "--- after create ---"
  dump_first
fi

echo "=== step5: server 侧验证新灵感集 ==="
curl -s --max-time 8 "http://127.0.0.1:8000/collections" | python -c "
import sys, json
data = json.load(sys.stdin)
print('collections total:', len(data))
for c in data:
    print(' ', c['id'], '|', c['name'], '| manual:', c.get('manual'))
"

rm -f "$PROJ/ui_dump_tmp.xml"
echo "=== done ==="
