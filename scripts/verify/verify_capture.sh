#!/bin/bash
# 捕捉链路验证：短按 FAB -> 捕捉屏 -> 点「轻捏开始」-> 静默 3s 自动保存 -> 回首页
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"

wait_device() {
  for i in $(seq 1 12); do
    ST=$("$ADB" get-state 2>/dev/null)
    if [ "$ST" = "device" ]; then return 0; fi
    sleep 5
  done
  return 1
}

dump_text() {
  "$ADB" shell "rm -f /sdcard/ui.xml; uiautomator dump /sdcard/ui.xml" > /dev/null 2>&1
  "$ADB" pull /sdcard/ui.xml ./ui_dump.xml > /dev/null 2>&1
  grep -o 'text="[^"]*"' ui_dump.xml | grep -v 'text=""'
}

dump_text_at() {  # 每行前缀时间戳
  dump_text | while read -r line; do echo "$(date +%H:%M:%S) $line"; done
}

echo "=== step0: 等设备 ==="
wait_device || { echo "FATAL: device not ready"; exit 1; }

echo "=== step1: 短按 FAB (934,1980) ==="
"$ADB" shell "input tap 934 1980"
sleep 3
dump_text_at | head -15

echo "=== step2: 找「轻捏开始」按钮并点击 ==="
# 从 dump 里提取按钮坐标
"$ADB" shell "rm -f /sdcard/ui.xml; uiautomator dump /sdcard/ui.xml" > /dev/null 2>&1
"$ADB" pull /sdcard/ui.xml ./ui_dump.xml > /dev/null 2>&1
BTN=$(python -c "
import re
with open('ui_dump.xml', encoding='utf-8') as f:
    xml = f.read()
# 找「轻捏开始」或其他开始按钮
for label in ['轻捏开始', '开始记录', '开始']:
    for m in re.finditer(r'text=\"' + label + r'\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
        x1, y1, x2, y2 = map(int, m.groups())
        print(f'{(x1+x2)//2} {(y1+y2)//2}')
        break
    else:
        continue
    break
")
echo "start button at: $BTN"
if [ -n "$BTN" ]; then
  "$ADB" shell "input tap $BTN"
  echo "tapped start"
else
  echo "WARN: start button not found, dump all text:"
  dump_text | head -20
fi
