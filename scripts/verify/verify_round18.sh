#!/bin/bash
# 第十八轮验证：搜索屏标签/近期搜索点击 → 真实搜索
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
dump_to_tmp() {
  for i in 1 2 3; do
    "$ADB" shell "rm -f /sdcard/ui.xml; uiautomator dump /sdcard/ui.xml" > /dev/null 2>&1
    "$ADB" pull /sdcard/ui.xml "$PROJ/ui_dump_tmp.xml" > /dev/null 2>&1
    [ -s "$PROJ/ui_dump_tmp.xml" ] && return 0
    sleep 2
  done
  return 1
}
dump_first() { dump_to_tmp; grep -o 'text="[^"]*"' "$PROJ/ui_dump_tmp.xml" | grep -v 'text=""' | head -12; }
find_tap() {
  python << PYEOF
import re
with open(r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
$1
PYEOF
}
shot() { "$ADB" shell "screencap -p /sdcard/s.png"; "$ADB" pull /sdcard/s.png "$OUT/$1.png" > /dev/null 2>&1; echo "saved $1"; }

wait_device || { echo "FATAL: device not ready"; exit 1; }

echo "=== step1: 装新 APK + 冷启动进首页 ==="
"$ADB" install -r "$APK" 2>&1 | tail -1
"$ADB" shell "am force-stop com.hotfix.avenlo"; sleep 2
"$ADB" shell "am start -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
sleep 7

echo "=== step2: 点搜索框进搜索屏 ==="
"$ADB" shell "input tap 135 310"; sleep 3
dump_first

echo "=== step3: 点常用标签「#摄影」→ 应填入 query 并显示结果 ==="
dump_to_tmp
TAG=$(find_tap "
for m in re.finditer(r'text=\"#摄影\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
echo "tag at: $TAG"
if [ -n "$TAG" ]; then
  "$ADB" shell "input tap $TAG"; sleep 2
  dump_first
  if grep -o 'text="[^"]*"' "$PROJ/ui_dump_tmp.xml" | grep -q "找到"; then
    echo ">>> PASS: 标签点击触发搜索，显示结果计数"
  else
    echo ">>> FAIL: 未显示搜索结果"
  fi
  shot "23_search_tag_click"
else
  echo ">>> FAIL: 找不到 #摄影 标签"
fi

echo "=== step4: 冷启动重置 query，点近期搜索第一行 ==="
# daemon 在 Bash 调用间会被沙箱杀掉，这里统一在单脚本内操作
"$ADB" shell "am force-stop com.hotfix.avenlo"; sleep 2
"$ADB" shell "am start -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
sleep 7
"$ADB" shell "input tap 135 310"; sleep 3
dump_to_tmp
HIST=$(find_tap "
for m in re.finditer(r'text=\"东东西设计灵感\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
if [ -n "$HIST" ]; then
  "$ADB" shell "input tap $HIST"; sleep 2
  dump_to_tmp
  grep -o 'text="[^"]*"' "$PROJ/ui_dump_tmp.xml" | grep -v 'text=""' | head -10
  if grep -o 'text="[^"]*"' "$PROJ/ui_dump_tmp.xml" | grep -q "找到"; then
    echo ">>> PASS: 近期搜索点击触发搜索"
  else
    echo ">>> FAIL: 近期搜索未触发搜索"
  fi
  shot "24_search_history_click"
else
  echo ">>> SKIP: 近期搜索第一行不可见"
fi
echo "=== done ==="
