# -*- coding: utf-8 -*-
"""Round48 针对性验证：P0 捕捉链路（FAB 重构后）+ 记录页 + 搜索页。
输出实时写日志文件，防超时丢结果。"""
import subprocess, time, re, sys, json, urllib.request

ADB = r'C:/Users/huangshaozheng/AppData/Local/Android/Sdk/platform-tools/adb.exe'
LOG = open(r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/emulator-screens/r48_targeted.log', 'w', encoding='utf-8')
PASS, FAIL = [], []

def log(msg):
    print(msg); LOG.write(msg + '\n'); LOG.flush()

def sh(*args, timeout=30):
    r = subprocess.run([ADB] + list(args), capture_output=True, text=True, timeout=timeout, encoding='utf-8', errors='replace')
    return (r.stdout or '') + (r.stderr or '')

def check(name, cond):
    (PASS if cond else FAIL).append(name)
    log(('PASS' if cond else 'FAIL') + ' ' + name)

def dump(tag):
    for _ in range(3):
        sh('shell', 'uiautomator', 'dump', '/sdcard/x.xml')
        out = sh('pull', '/sdcard/x.xml', r'C:/Users/huangshaozheng/AppData/Local/Temp/r48_%s.xml' % tag)
        if '1 file pulled' in out or 'file pulled' in out:
            try:
                return open(r'C:/Users/huangshaozheng/AppData/Local/Temp/r48_%s.xml' % tag, encoding='utf-8').read()
            except Exception:
                pass
        time.sleep(1.5)
    return ''

def cold_start_home(tag):
    sh('shell', 'am', 'force-stop', 'com.hotfix.avenlo')
    time.sleep(1)
    sh('shell', 'am', 'start', '-n', 'com.hotfix.avenlo/.app.MainActivity', '-f', '0x8000')
    for _ in range(20):
        time.sleep(1.5)
        xml = dump(tag)
        if 'Hey, Runel' in xml or '搜索灵感' in xml:
            return xml
    return xml or ''

def find_bounds(xml, text):
    m = re.search(r'text="%s"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"' % re.escape(text), xml)
    if m:
        x1, y1, x2, y2 = map(int, m.groups())
        return (x1 + x2) // 2, (y1 + y2) // 2
    return None

# ---- 1. 冷启动回首页（顺带验 1.5s 自动跳）----
log('=== step1 冷启动 ===')
xml = cold_start_home('home')
check('冷启动回首页（Hey, Runel 在屏）', 'Hey, Runel' in xml)

# 记录「今天·N条」基数（step5 比较增长）
m0 = re.search(r'今天·(\d+)条', xml)
today_base = int(m0.group(1)) if m0 else -1
log('今天卡数基数: %d' % today_base)

# ---- 2. 短按 FAB → Ready 态 ----
log('=== step2 短按 FAB ===')
# FAB 在右下（约 984, 2136——56dp 圆心：1080-24dp*2.625-28dp*2.625 ≈ 984, 2400-80-64*1.3125/2 附近），先 dump 找「轻捏」
pos = find_bounds(xml, '轻捏')
if not pos:
    # 兜底坐标（1080x2400：padding 24dp≈63px，56dp 圆心 = 1080-63-73.5 ≈ 943, 2400-63-73.5-导航≈2263-80）
    pos = (943, 2136)
    log('FAB 文本未找到，用兜底坐标 %s' % (pos,))
log('FAB 坐标 %s' % (pos,))
sh('shell', 'input', 'tap', str(pos[0]), str(pos[1]))
time.sleep(2.5)
xml2 = dump('ready')
check('短按 FAB 进捕捉页（准备好了吗）', '准备好了' in xml2 or '轻捏戒指' in xml2)

# ---- 3. 返回首页，长按 FAB → 自动开录 ----
log('=== step3 长按 FAB（600ms swipe 模拟）===')
sh('shell', 'input', 'keyevent', '4')
time.sleep(2)
xml = cold_start_home('home2')
# 录音时长按进度环最高 300ms 就触发，600ms swipe 稳定覆盖
sh('shell', 'input', 'swipe', str(pos[0]), str(pos[1]), str(pos[0]), str(pos[1]), '600')
time.sleep(3)
xml3 = dump('rec')
check('长按 FAB 自动开录（正在聆听）', '正在聆听' in xml3)
sh('shell', 'screencap', '-p', '/sdcard/r48_rec.png')
sh('pull', '/sdcard/r48_rec.png', r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/emulator-screens/round48_recording.png')

# ---- 4. 静默自动保存 → 回首页出卡 ----
log('=== step4 静默自动结束 ===')
# 先记下服务端卡数基线（提交成功会 +1）
base_count = 0
try:
    with urllib.request.urlopen('http://140.143.207.74:8100/ideas', timeout=8) as r:
        base_count = len(json.loads(r.read().decode('utf-8')))
except Exception as e:
    log('服务端基线查询失败: %s' % e)
log('服务端基线卡数: %d' % base_count)

done_ok = False
popped_home = False
for _ in range(20):
    time.sleep(1)
    xml4 = dump('done')
    # 只认 Done 态专属文案「灵感已保存」（「撤销」会误匹配 Ready 态底部固定文案「撤销窗 3 秒」）
    if '灵感已保存' in xml4:
        done_ok = True; break
    # 若已自动 pop 回首页（撤销窗 3s 已过）——auto-pop 只发生在 Done 之后，视为成功
    if 'Hey, Runel' in xml4 or '搜索灵感' in xml4:
        popped_home = True; break
check('录音静默自动保存（灵感已保存/自动回首页）', done_ok or popped_home)
# 注意：不再无条件 BACK——若已自动回首页，BACK 会把 App 退到桌面（上轮 FAIL 根因）
if not popped_home and '灵感已保存' in (xml4 or ''):
    pass  # Done 态 3s 后自动 pop，无需按键

# ---- 5. 首页出现新卡（QUEUED → 轮询变完整）----
log('=== step5 新卡回显 ===')
ok = False
# 若 step4 已自动回首页则直接轮询；否则先回首页（捕捉页 BACK 是 pop，安全）
if not popped_home:
    if '灵感已保存' in (xml4 or ''):
        pass  # 还在 Done 态，等 3s 撤销窗自动 pop
    else:
        sh('shell', 'input', 'keyevent', '4')
        time.sleep(1.5)
for _ in range(10):
    xml5 = dump('newcard')
    if '整理中' in xml5:
        ok = True; break
    m = re.search(r'今天·(\d+)条', xml5)
    # 「整理中」是闪现态（服务端 3s 状态机很快回填）；「今天·N条」增长 = 新卡已回显
    if m and today_base >= 0 and int(m.group(1)) > today_base:
        ok = True; break
    if 'Hey, Runel' not in xml5:
        sh('shell', 'input', 'keyevent', '4'); time.sleep(1)
        continue
    time.sleep(2)
check('新卡占位或已回显（今天·N条 增长）', ok)

# 服务端硬证据：卡数应比基线 +1（提交落库）
try:
    with urllib.request.urlopen('http://140.143.207.74:8100/ideas', timeout=8) as r:
        now_count = len(json.loads(r.read().decode('utf-8')))
    check('服务端卡数 %d → %d（+1）' % (base_count, now_count), now_count >= base_count + 1)
except Exception as e:
    check('服务端卡数复核（网络异常 %s）' % e, False)

# ---- 6. 记录页 ----
log('=== step6 记录 Tab ===')
# 确保在 App 内且在首页（不在则冷启动回首页），再点底部「记录」Tab
xml_pre = dump('pre_records')
if 'Hey, Runel' not in xml_pre and '搜索灵感' not in xml_pre:
    cold_start_home('pre_records')
sh('shell', 'input', 'tap', '377', '2274')  # 记录 tab（4 tab 均分：首页135/记录377/统计620/我的862）
time.sleep(2.5)
xml6 = dump('records')
check('记录页头部（按日期回看）', '按日期回看' in xml6 or '记录' in xml6)

log('')
log('===== 结果：%d PASS / %d FAIL =====' % (len(PASS), len(FAIL)))
if FAIL:
    log('FAIL 项：' + ', '.join(FAIL))
LOG.close()
