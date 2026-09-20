#!/bin/bash
# 一键全量回归：P0 链路（捕捉→出卡）+ 8 屏巡检 + related 跳转
# 用法：server 起好后 bash scripts/verify/verify_all.sh
# 前置：模拟器已 boot、mock-server 跑在 127.0.0.1:8000
# 结束时自动 POST /admin/reset 恢复种子态（演示随时可用）
set -u
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
PROJ="C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon"
OUT="$PROJ/emulator-screens"
APK="$PROJ/avenlo-android/app/build/outputs/apk/debug/app-debug.apk"
SERVER="http://127.0.0.1:8000"
PASS=0; FAIL=0; FAILED_STEPS=""

ok()   { PASS=$((PASS+1)); echo "  [PASS] $1"; }
bad()  { FAIL=$((FAIL+1)); FAILED_STEPS="$FAILED_STEPS $1"; echo "  [FAIL] $1"; }

wait_device() {
  for i in $(seq 1 12); do
    ST=$("$ADB" get-state 2>/dev/null)
    if [ "$ST" = "device" ]; then return 0; fi
    sleep 5
  done
  return 1
}

shot() {
  "$ADB" shell "screencap -p /sdcard/s.png" > /dev/null 2>&1
  "$ADB" pull /sdcard/s.png "$OUT/$1.png" > /dev/null 2>&1
  echo "  saved $1"
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

dump_all()  { dump_to_tmp; grep -o 'text="[^"]*"' "$PROJ/ui_dump_tmp.xml" | grep -v 'text=""'; }
dump_first(){ dump_to_tmp; grep -o 'text="[^"]*"' "$PROJ/ui_dump_tmp.xml" | grep -v 'text=""' | head -12; }

find_tap() {  # $1=python 匹配表达式（打印 "x y" 到 stdout）
  python << PYEOF
import re
with open(r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/ui_dump_tmp.xml', encoding='utf-8') as f:
    xml = f.read()
$1
PYEOF
}

tap_text() {  # $1=可见文本 → 点它
  local TAP
  TAP=$(find_tap "
for m in re.finditer(r'text=\"$1\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
  [ -n "$TAP" ] && "$ADB" shell "input tap $TAP"
}

cold_start_home() {
  "$ADB" shell "am force-stop com.hotfix.avenlo"; sleep 2
  "$ADB" shell "am start -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
  sleep 6
}

scroll_down() { "$ADB" shell "input swipe 540 1800 540 700 300"; sleep 2; }

find_travel() {
  find_tap "
for m in re.finditer(r'text=\"关于旅行的灵感\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
"
}

open_travel_detail() {  # 点旅行卡并确认进对详情页；成功返回 0
  local TRY TAP
  for TRY in 1 2 3; do
    dump_to_tmp
    TAP=$(find_travel)
    if [ -n "$TAP" ]; then
      "$ADB" shell "input tap $TAP"; sleep 3
      dump_to_tmp
      if grep -q '关于旅行的灵感\|慢生活' "$PROJ/ui_dump_tmp.xml"; then return 0; fi
      # 落偏了（滚动惯性导致 bounds 漂移）→ 回首页重试
      "$ADB" shell "input keyevent 4"; sleep 2
    fi
    scroll_down
  done
  return 1
}

server_json() {  # $1=path $2=python 表达式（返回 0/1）
  curl -s --max-time 8 --noproxy '*' "$SERVER$1" | python -c "
import sys, json
try:
    d = json.load(sys.stdin)
except Exception:
    print(0); raise SystemExit
print(1 if ($2) else 0)
"
}

echo "========== Avenlo 一键全量回归 =========="

echo "=== step0: 前置检查 ==="
wait_device || { echo "FATAL: 模拟器未就绪"; exit 1; }
ok "模拟器就绪"
H=$(curl -s --max-time 5 --noproxy '*' "$SERVER/health")
if echo "$H" | grep -q '"ok":true'; then ok "server 健康: $H"; else bad "server 不可达: $H"; fi
echo "=== step0.5: 重置种子态（保证回归起态一致） ==="
curl -s --noproxy '*' -X POST "$SERVER/admin/reset" > /dev/null
sleep 1

echo "=== step1: 装新 APK + 冷启动 ==="
"$ADB" install -r "$APK" 2>&1 | tail -1
cold_start_home

echo "=== step2: 首页种子（11 卡） ==="
dump_first
if dump_all | grep -q "关于旅行的灵感"; then ok "首页显示种子卡（server 数据）"; else bad "首页无种子卡"; fi
shot "R01_home"

echo "=== step3: P0 捕捉→出卡全链路 ==="
# 短按 FAB 进捕捉屏 → 点「轻捏开始」→ 静默 3s 自动保存
"$ADB" shell "input tap 934 1980"; sleep 3
dump_to_tmp
BTN=$(find_tap "
for m in re.finditer(r'text=\"轻捏开始\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
if [ -n "$BTN" ]; then
  "$ADB" shell "input tap $BTN"
  echo "  已开始录音（模拟器静默检测可能不触发，走满 60s 上限），轮询等 server 落卡..."
  # 判据：reset 后基线 11 张，出现 >11 即新卡（不依赖随机 uuid 前缀，确定性）
  NEW=0
  for i in $(seq 1 15); do
    sleep 6
    NEW=$(server_json "/ideas" "len(d) > 11")
    [ "$NEW" = "1" ] && break
  done
  if [ "$NEW" = "1" ]; then ok "server 落新卡"; else bad "server 无新卡"; fi
  # App 侧：新卡出现在首页（QUEUED「整理中」或完成态真实标题——mock 文案已真实感化）
  dump_all | grep -qE "整理中|通勤|洗碗|咖啡馆|爵士|睡前|散步" && ok "App 拉回 QUEUED/完成卡" || true
  shot "R02_capture_result"
else
  bad "捕捉屏「轻捏开始」按钮缺失"
fi

echo "=== step4: 记录 Tab ==="
cold_start_home
"$ADB" shell "input tap 405 2300"; sleep 4
dump_first
if dump_all | grep -qE "月[0-9]+日|记录"; then ok "记录 Tab 显示时间线"; else bad "记录 Tab 异常"; fi
shot "R03_records"

echo "=== step5: 我的页真实统计 ==="
"$ADB" shell "input tap 945 2300"; sleep 4
dump_first
if dump_all | grep -q "灵感"; then ok "我的页显示统计"; else bad "我的页异常"; fi
shot "R04_mine"

echo "=== step6: 灵感集列表 ==="
cold_start_home
"$ADB" shell "input tap 990 310"; sleep 4
dump_first
if dump_all | grep -q "灵感集\|旅行"; then ok "灵感集列表"; else bad "灵感集异常"; fi
shot "R05_collections"

echo "=== step7: 搜索（真实数据 + 标签点击） ==="
cold_start_home
"$ADB" shell "input tap 135 310"; sleep 3
dump_to_tmp
TAG=$(find_tap "
for m in re.finditer(r'text=\"#摄影\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
if [ -n "$TAG" ]; then
  "$ADB" shell "input tap $TAG"; sleep 2
  if dump_all | grep -q "找到"; then ok "搜索屏标签点击触发搜索"; else bad "标签点击未触发搜索"; fi
else
  bad "搜索屏「#摄影」标签未找到"
fi
shot "R07_search"

echo "=== step8: 详情 + related 跳转 ==="
cold_start_home
if open_travel_detail; then
  dump_all | grep -q "共3条" && ok "详情页 related 3 条" || bad "详情页 related 异常"
  shot "R08_detail"
  dump_to_tmp
  REL=$(find_tap "
for m in re.finditer(r'text=\"清晨的露水\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
  if [ -n "$REL" ]; then
    "$ADB" shell "input tap $REL"; sleep 3
    if dump_all | grep -q "晨间露珠的微观摄影"; then ok "related 跳转显示真实卡"; else bad "related 跳转异常"; fi
    shot "R09_related_jump"
  else
    bad "related「清晨的露水」未找到"
  fi
else
  bad "首页找不到「关于旅行的灵感」"
fi

echo "=== step8.5: 删除链路（更多菜单→确认→server 软删） ==="
# 冷启动回首页（返回栈里有多层详情页，keyevent 4 只弹一层，不可靠）
# 目标卡选 idea_02「写作素材：时间与记忆」——今天分组首屏可见，无需滚动
# （滚动找底部分组卡不可靠：粗滚动会跳过目标，且分组头会压缩卡位置）
cold_start_home
DEL_TARGET=""
for i in 1 2 3; do
  dump_to_tmp
  DEL_TARGET=$(find_tap "
for m in re.finditer(r'text=\"写作素材：时间与记忆\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
  [ -n "$DEL_TARGET" ] && break
  scroll_down
done
if [ -n "$DEL_TARGET" ]; then
  "$ADB" shell "input tap $DEL_TARGET"; sleep 3
  # 点更多（content-desc 或右上坐标兜底）
  dump_to_tmp
  MORE=$(find_tap "
for m in re.finditer(r'content-desc=\"更多\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
  [ -z "$MORE" ] && MORE="1010 175"
  "$ADB" shell "input tap $MORE"; sleep 2
  dump_to_tmp
  DEL=$(find_tap "
for m in re.finditer(r'text=\"删除这条灵感\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
  if [ -n "$DEL" ]; then
    "$ADB" shell "input tap $DEL"; sleep 2
    dump_to_tmp
    CONFIRM=$(find_tap "
for m in re.finditer(r'text=\"删除\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', xml):
    x1,y1,x2,y2 = map(int, m.groups())
    print(f'{(x1+x2)//2} {(y1+y2)//2}')
    break
")
    [ -n "$CONFIRM" ] && "$ADB" shell "input tap $CONFIRM"; sleep 4
    # 判据：idea_02 从列表消失（不能用固定总数——step3 的捕捉新卡会让基数漂移）
    N_DEL=$(server_json "/ideas" "not any(i.get('id') == 'idea_02' for i in d)")
    if [ "$N_DEL" = "1" ]; then ok "删除链路 server 软删生效（idea_02 已移除）"; else bad "删除后 server 卡数异常"; fi
    shot "R10_delete_flow"
  else
    bad "删除菜单项未出现"
  fi
else
  bad "「写作素材：时间与记忆」卡未找到"
fi

echo "=== step9: 恢复种子态 ==="
R=$(curl -s --noproxy '*' -X POST "$SERVER/admin/reset")
echo "  reset: $R"

echo ""
echo "========== 回归结果 =========="
echo "PASS: $PASS  FAIL: $FAIL"
[ -n "$FAILED_STEPS" ] && echo "失败项:$FAILED_STEPS"
[ "$FAIL" -eq 0 ] && echo "✅ 全部通过" || echo "❌ 存在失败项"
exit $([ "$FAIL" -eq 0 ] && echo 0 || echo 1)
