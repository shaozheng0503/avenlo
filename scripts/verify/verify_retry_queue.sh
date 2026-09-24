#!/usr/bin/env bash
# =============================================================
# 断网补交 E2E（第四十轮 M3-lite 重试队列）
# 验证链路：断网捕捉 → 占位卡滞留「整理中」→ 网络恢复 → 自动补交为真实卡
#
# 前置条件（不满足则跳过，不影响 CI 语义）：
#   1. 模拟器在线（真机不可用——iptables 断网法仅模拟器支持）
#   2. adb root 可用（加/删 iptables 规则需要）
#   3. Mock Server 在本机 8000 端口
#
# 断网方法：iptables REJECT 目标 10.0.2.2:8000（模拟器宿主机回环）
#   ⚠️ 教训（第四十轮实锤）：删规则必须用精确匹配（iptables -D OUTPUT -d ... -j REJECT），
#      按序号删（-D OUTPUT 1）可能删错——链首可能还有系统既有规则。
# =============================================================
set -u
PROJ="$(cd "$(dirname "$0")/../.." && pwd)"
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
[ -x "$ADB" ] || ADB="adb"
SERIAL="${1:-emulator-5554}"
PASS=0; FAIL=0
ok()  { echo "  [PASS] $1"; PASS=$((PASS+1)); }
bad() { echo "  [FAIL] $1"; FAIL=$((FAIL+1)); }

# 沙箱坑位：每个 bash 会话首条 adb 会触发 daemon 重启 → 设备短暂 offline。
# 对策：热身命令丢弃 + sleep 3，后续命令在同一 daemon 生命周期内。
warmup() { "$ADB" devices > /dev/null 2>&1; sleep 3; }

# dump 到 /sdcard 再 cat（/dev/tty 直读偶发空——第三十九轮实锤），按字节数重试
dump_ui() {
  for i in 1 2 3 4; do
    "$ADB" -s "$SERIAL" shell "uiautomator dump /sdcard/ui_retry.xml" > /dev/null 2>&1
    "$ADB" -s "$SERIAL" shell "cat /sdcard/ui_retry.xml" > /tmp/ui_retry.xml 2>/dev/null
    SZ=$(wc -c < /tmp/ui_retry.xml 2>/dev/null || echo 0)
    [ "$SZ" -gt 500 ] && return 0
    sleep 2
  done
  return 1
}

# ---- 前置检查 ----
warmup
if ! "$ADB" -s "$SERIAL" get-state 2>/dev/null | grep -q device; then
  echo "SKIP: 设备 $SERIAL 不在线（断网补交 E2E 仅模拟器可跑）"; exit 0
fi
if ! "$ADB" -s "$SERIAL" shell "id" 2>/dev/null | grep -q "uid=0"; then
  echo "SKIP: adb 非 root（iptables 不可用）。先跑: adb -s $SERIAL root"; exit 0
fi
HEALTH=$(curl -s --max-time 4 "http://127.0.0.1:8000/health" 2>/dev/null)
echo "$HEALTH" | grep -q '"ok":true' || { echo "SKIP: Mock Server 未启动"; exit 0; }

echo "=== 断网补交 E2E（第四十轮重试队列） ==="
BASE_N=$(echo "$HEALTH" | grep -o '"ideas":[0-9]*' | grep -o '[0-9]*$')

# ---- 0. 重置种子态 + 确认无残留 iptables 规则 ----
curl -s -X POST "http://127.0.0.1:8000/admin/reset" > /dev/null
"$ADB" -s "$SERIAL" shell "iptables -D OUTPUT -d 10.0.2.2/32 -p tcp -m tcp --dport 8000 -j REJECT --reject-with icmp-port-unreachable" 2>/dev/null
sleep 1

# ---- 1. 断网（iptables REJECT 8000）----
warmup
"$ADB" -s "$SERIAL" shell "iptables -I OUTPUT 1 -d 10.0.2.2 -p tcp --dport 8000 -j REJECT" > /dev/null 2>&1
sleep 1
echo "  已断网（REJECT 10.0.2.2:8000）"

# ---- 2. 冷启动 → 捕捉屏 → 轻捏开始 → 手动结束 ----
warmup
"$ADB" -s "$SERIAL" shell "am force-stop com.hotfix.avenlo" > /dev/null 2>&1; sleep 1
"$ADB" -s "$SERIAL" shell "am start -f 0x8000 -n com.hotfix.avenlo/com.hotfix.avenlo.app.MainActivity" > /dev/null 2>&1
sleep 10
"$ADB" -s "$SERIAL" shell "input tap 934 1980"   # FAB（1080x2400 AVD 实测）
sleep 4
dump_ui || { bad "进捕捉屏失败"; }
grep -q '轻捏开始' /tmp/ui_retry.xml && ok "进捕捉屏" || bad "捕捉屏「轻捏开始」缺失"

"$ADB" -s "$SERIAL" shell "input tap 540 1345"   # 轻捏开始（bounds 实测中心）
sleep 5                                            # 录 5 秒
"$ADB" -s "$SERIAL" shell "input tap 540 1583"   # 再次轻捏·结束并保存
sleep 12                                           # 撤销窗 3s + 断网提交失败
dump_ui || true
grep -q '出错了\|Failed to connect' /tmp/ui_retry.xml && ok "断网提交失败 → Error 态" || bad "未见 Error 态（网络可能没断）"

# ---- 3. Error 态点「返回」→ 首页应见占位卡「整理中」----
warmup
"$ADB" -s "$SERIAL" shell "input tap 540 1304"   # 返回按钮中心
sleep 5
dump_ui || true
grep -q '整理中' /tmp/ui_retry.xml && ok "占位卡滞留首页（今天组）" || bad "占位卡丢失（重试队列回归！）"
grep -o 'text="今天·[0-9]*条"' /tmp/ui_retry.xml | head -1

# ---- 4. 恢复网络 → 等 QUEUED 轮询自动补交 ----
warmup
"$ADB" -s "$SERIAL" shell "iptables -D OUTPUT -d 10.0.2.2/32 -p tcp -m tcp --dport 8000 -j REJECT --reject-with icmp-port-unreachable" 2>/dev/null
sleep 1
RULES_N=$("$ADB" -s "$SERIAL" shell "iptables -S | grep -c 8000" 2>/dev/null | tr -d '\r')
[ "${RULES_N:-0}" = "0" ] && ok "网络已恢复（规则清零）" || bad "iptables 残留 $RULES_N 条（按精确规则删失败）"

echo "  等自动补交（QUEUED 轮询 2s + server 3s 出卡）..."
sleep 15
dump_ui || true
grep -q '整理中' /tmp/ui_retry.xml && bad "占位卡仍滞留（补交失败）" || ok "占位卡已被服务端卡替换"

# ---- 5. server 侧验证：+1 卡、自动关联 ----
NEW_N=$(curl -s --max-time 4 "http://127.0.0.1:8000/ideas" | python -c "
import sys, json
d = json.load(sys.stdin)
print(1 if len(d) > $BASE_N else 0)")
[ "$NEW_N" = "1" ] && ok "server 收到补交卡（>$BASE_N 条）" || bad "server 未收到补交卡"

REL_N=$(curl -s --max-time 4 "http://127.0.0.1:8000/ideas" | python -c "
import sys, json
d = json.load(sys.stdin)
news = [c for c in d if not (c['id'].startswith('idea_0') or c['id'].startswith('idea_1'))]
print(1 if news and all(len(c.get('related') or []) >= 1 for c in news) else 0)")
[ "$REL_N" = "1" ] && ok "补交卡自动关联 related ≥1" || bad "补交卡 related 为空"

# ---- 清理：恢复种子态 ----
curl -s -X POST "http://127.0.0.1:8000/admin/reset" > /dev/null
echo ""
echo "=== 结果: PASS=$PASS FAIL=$FAIL ==="
[ "$FAIL" -eq 0 ] && echo "✅ 断网补交链路健康" || echo "❌ 存在失败项"
exit $FAIL
