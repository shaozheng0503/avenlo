#!/usr/bin/env bash
# =============================================================
# 断网补交·跨进程持久化 E2E（第四十五轮 PendingStore）
# 验证链路：断网捕捉 → 杀进程（队列落盘）→ 冷启动重启 →
#           占位卡恢复「整理中」→ 网络恢复 → 跨进程自动补交为真实卡
#
# 与 verify_retry_queue.sh（第四十轮，同进程内存队列）的区别：
#   本脚本在第 3 步多一次 am force-stop + 冷启动——进程死后队列不丢。
#
# 前置条件（不满足则跳过）：模拟器在线 + adb root + Mock Server 8000
# 断网方法：iptables REJECT 10.0.2.2:8000（删规则必须精确匹配——第四十轮实锤）
# =============================================================
set -u
PROJ="$(cd "$(dirname "$0")/../.." && pwd)"
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
[ -x "$ADB" ] || ADB="adb"
SERIAL="${1:-emulator-5554}"
PASS=0; FAIL=0
ok()  { echo "  [PASS] $1"; PASS=$((PASS+1)); }
bad() { echo "  [FAIL] $1"; FAIL=$((FAIL+1)); }

warmup() { "$ADB" devices > /dev/null 2>&1; sleep 3; }

dump_ui() {
  for i in 1 2 3 4; do
    "$ADB" -s "$SERIAL" shell "uiautomator dump /sdcard/ui_persist.xml" > /dev/null 2>&1
    "$ADB" -s "$SERIAL" shell "cat /sdcard/ui_persist.xml" > /tmp/ui_persist.xml 2>/dev/null
    SZ=$(wc -c < /tmp/ui_persist.xml 2>/dev/null || echo 0)
    [ "$SZ" -gt 500 ] && return 0
    sleep 2
  done
  return 1
}

wait_home() {
  for i in 1 2 3 4 5 6; do
    sleep 5
    dump_ui || true
    grep -q '搜索灵感、关键词、标签\|Hey, Runel' /tmp/ui_persist.xml 2>/dev/null && return 0
  done
  return 1
}

# ---- 前置检查 ----
warmup
if ! "$ADB" -s "$SERIAL" get-state 2>/dev/null | grep -q device; then
  echo "SKIP: 设备 $SERIAL 不在线"; exit 0
fi
if ! "$ADB" -s "$SERIAL" shell "id" 2>/dev/null | grep -q "uid=0"; then
  echo "SKIP: adb 非 root。先跑: adb -s $SERIAL root"; exit 0
fi
HEALTH=$(curl -s --max-time 4 "http://127.0.0.1:8000/health" 2>/dev/null)
echo "$HEALTH" | grep -q '"ok":true' || { echo "SKIP: Mock Server 未启动"; exit 0; }

echo "=== 跨进程持久化 E2E（第四十五轮 PendingStore） ==="
BASE_N=$(echo "$HEALTH" | grep -o '"ideas":[0-9]*' | grep -o '[0-9]*$')

# ---- 0. 重置 + 确保无残留规则 ----
curl -s -X POST "http://127.0.0.1:8000/admin/reset" > /dev/null
"$ADB" -s "$SERIAL" shell "iptables -D OUTPUT -d 10.0.2.2/32 -p tcp -m tcp --dport 8000 -j REJECT --reject-with icmp-port-unreachable" 2>/dev/null
sleep 1

# ---- 1. 断网 ----
warmup
"$ADB" -s "$SERIAL" shell "iptables -I OUTPUT 1 -d 10.0.2.2 -p tcp --dport 8000 -j REJECT" > /dev/null 2>&1
sleep 1
echo "  已断网（REJECT 10.0.2.2:8000）"

# ---- 2. 冷启动 → 捕捉 → 录 5s → 手动结束 → Error 态 ----
warmup
"$ADB" -s "$SERIAL" shell "am force-stop com.hotfix.avenlo" > /dev/null 2>&1; sleep 1
"$ADB" -s "$SERIAL" shell "am start -f 0x8000 -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
wait_home || { echo "FATAL: 冷启动失败"; exit 1; }
"$ADB" -s "$SERIAL" shell "input tap 934 1980"   # FAB
sleep 4
"$ADB" -s "$SERIAL" shell "input tap 540 1345"   # 轻捏开始
sleep 5
"$ADB" -s "$SERIAL" shell "input tap 540 1583"   # 结束并保存
sleep 12                                          # 撤销窗 + 断网提交失败
dump_ui || true
grep -q '出错了\|Failed to connect' /tmp/ui_persist.xml && ok "断网提交失败 → Error 态" || bad "未见 Error 态（网络可能没断）"

# 点返回回首页（占位卡已在内存 + 已落盘）
"$ADB" -s "$SERIAL" shell "input tap 540 1304"
sleep 5
dump_ui || true
grep -q '整理中' /tmp/ui_persist.xml && ok "占位卡在屏（落盘前内存态）" || bad "占位卡丢失"

# ---- 3. 杀进程（核心差异步骤：第四十轮内存队列在此会丢）----
warmup
"$ADB" -s "$SERIAL" shell "am force-stop com.hotfix.avenlo" > /dev/null 2>&1
sleep 2
# 队列文件应已在 files/avenlo/pending_queue.json
QUEUE_FILE=$("$ADB" -s "$SERIAL" shell "run-as com.hotfix.avenlo ls files/avenlo/pending_queue.json 2>/dev/null || su 0 ls /data/data/com.hotfix.avenlo/files/avenlo/pending_queue.json" 2>/dev/null | tr -d '\r')
[ -n "$QUEUE_FILE" ] && ok "队列文件已落盘（pending_queue.json）" || bad "队列文件不存在（持久化失败）"
echo "  进程已杀，队列文件: $QUEUE_FILE"

# ---- 4. 冷启动重启 → 占位卡应恢复 ----
warmup
"$ADB" -s "$SERIAL" shell "am start -f 0x8000 -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
wait_home || { bad "重启后冷启动失败"; }
dump_ui || true
grep -q '整理中' /tmp/ui_persist.xml && ok "杀进程重启后占位卡恢复（跨进程持久化生效）" || bad "重启后占位卡丢失（第四十轮边界未根治）"

# ---- 5. 恢复网络 → 等跨进程自动补交 ----
warmup
"$ADB" -s "$SERIAL" shell "iptables -D OUTPUT -d 10.0.2.2/32 -p tcp -m tcp --dport 8000 -j REJECT --reject-with icmp-port-unreachable" 2>/dev/null
sleep 1
RULES_N=$("$ADB" -s "$SERIAL" shell "iptables -S | grep -c 8000" 2>/dev/null | tr -d '\r')
[ "${RULES_N:-0}" = "0" ] && ok "网络已恢复（规则清零）" || bad "iptables 残留 $RULES_N 条"

echo "  等自动补交（QUEUED 轮询 2s + server 3s 出卡）..."
sleep 15
dump_ui || true
grep -q '整理中' /tmp/ui_persist.xml && bad "占位卡仍滞留（跨进程补交失败）" || ok "占位卡已被服务端卡替换（跨进程补交）"

# ---- 6. server 侧验证 ----
NEW_N=$(curl -s --max-time 4 "http://127.0.0.1:8000/ideas" | python -c "
import sys, json
d = json.load(sys.stdin)
print(1 if len(d) > $BASE_N else 0)")
[ "$NEW_N" = "1" ] && ok "server 收到跨进程补交卡（>$BASE_N 条）" || bad "server 未收到补交卡"

# 队列文件应清空（补交完成出队 + persist）
QUEUE_AFTER=$("$ADB" -s "$SERIAL" shell "run-as com.hotfix.avenlo cat files/avenlo/pending_queue.json 2>/dev/null || su 0 cat /data/data/com.hotfix.avenlo/files/avenlo/pending_queue.json" 2>/dev/null | tr -d '\r')
echo "$QUEUE_AFTER" | grep -q '"localId"' && bad "队列文件未清空" || ok "队列文件已清空（补交出队落盘）"

# ---- 清理 ----
curl -s -X POST "http://127.0.0.1:8000/admin/reset" > /dev/null
echo ""
echo "=== 结果: PASS=$PASS FAIL=$FAIL ==="
[ "$FAIL" -eq 0 ] && echo "✅ 跨进程持久化补交链路健康" || echo "❌ 存在失败项"
exit $FAIL
