#!/bin/bash
# 完整链路验证：装新 APK -> 启动 -> 冷启动 refresh 验证 -> 捕捉 -> server 新卡 -> 轮询拉回
set -u
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
PROJ="C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon"
APK="$PROJ/avenlo-android/app/build/outputs/apk/debug/app-debug.apk"

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
  "$ADB" pull /sdcard/ui.xml "$PROJ/ui_dump.xml" > /dev/null 2>&1
  grep -o 'text="[^"]*"' "$PROJ/ui_dump.xml" | grep -v 'text=""'
}

get_btn_center() {  # $1=label
  python -c "
import re
with open('$PROJ/ui_dump.xml', encoding='utf-8') as f:
    xml = f.read()
for m in re.finditer(r'text=\"$1\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
"
}

echo "=== step0: 等设备 ==="
wait_device || { echo "FATAL: device not ready"; exit 1; }

echo "=== step1: 安装新 APK ==="
"$ADB" install -r "$APK" 2>&1 | tail -1

echo "=== step2: 启动 App ==="
"$ADB" shell "am start -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity"
sleep 6

echo "=== step3: 验证首页（server 种子） ==="
dump_text | head -12

echo "=== step4: 短按 FAB 进捕捉屏 ==="
"$ADB" shell "input tap 934 1980"
sleep 3
dump_text | head -6

echo "=== step5: 点「轻捏开始」 ==="
BTN=$(get_btn_center "轻捏开始")
echo "btn: $BTN"
if [ -z "$BTN" ]; then echo "FATAL: start button missing"; dump_text | head -20; exit 1; fi
"$ADB" shell "input tap $BTN"

echo "=== step6: 等录音 + 静默 3s 自动保存 + server 处理 ==="
sleep 15

echo "=== step7: server 侧验证新卡 ==="
curl -s --max-time 8 "http://127.0.0.1:8000/ideas" | python -c "
import sys, json
data = json.load(sys.stdin)
ideas = data if isinstance(data, list) else data.get('ideas', [])
print('total:', len(ideas))
for i in ideas:
    if not str(i.get('id','')).startswith('idea_'):
        print('NEW:', i.get('id'), '|', i.get('title'), '|', i.get('status'))
"

echo "=== step8: App 侧验证轮询拉回 ==="
sleep 5
dump_text | head -16
