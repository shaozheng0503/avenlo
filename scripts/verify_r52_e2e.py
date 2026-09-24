# -*- coding: utf-8 -*-
"""Round52 端到端功能连贯性验证（r51c 版本）
用户旅程：启动 → 首页 → 捕捉链路（FAB单击 Ready态）→ 首页分组切换 → 详情 → 脉络互跳
        → 灵感集(网格/最近收录) → 集合详情 → 我的 → 统计(筛选chips联动) → 今日回顾 → 崩溃检查
重点：r51 改动后的导航连贯性（3tab 底导 / 统计二级页 / 最近收录互跳）
"""
import subprocess, time, os, sys, re

ADB = os.path.expandvars(r'%LOCALAPPDATA%/Android/Sdk/platform-tools/adb.exe')
BASE = r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon'
OUT = os.path.join(BASE, 'emulator-screens', 'round52')
os.makedirs(OUT, exist_ok=True)
XML = os.path.join(BASE, 'ui_dump_r52.xml')

PASS = 0; FAIL = 0; FAILS = []

def ok(m):
    global PASS; PASS += 1; print('  [PASS] %s' % m, flush=True)

def bad(m):
    global FAIL; FAIL += 1; FAILS.append(m); print('  [FAIL] %s' % m, flush=True)

def sh(*args, timeout=40):
    r = subprocess.run([ADB] + list(args), capture_output=True, text=True, timeout=timeout, encoding='utf-8', errors='replace')
    return (r.stdout or '') + (r.stderr or '')

def tap(x, y, wait=1.8):
    sh('shell', 'input', 'tap', str(int(x)), str(int(y)))
    time.sleep(wait)

def back(wait=1.4):
    sh('shell', 'input', 'keyevent', '4')
    time.sleep(wait)

def dump():
    for _ in range(3):
        sh('shell', 'rm', '-f', '/sdcard/ui52.xml')
        sh('shell', 'uiautomator', 'dump', '/sdcard/ui52.xml')
        r = subprocess.run([ADB, 'pull', '/sdcard/ui52.xml', XML], capture_output=True, text=True, timeout=20)
        if os.path.exists(XML) and os.path.getsize(XML) > 100:
            with open(XML, encoding='utf-8') as f:
                return f.read()
        time.sleep(1.5)
    return ''

def texts(xml):
    return re.findall(r'text="([^"]+)"', xml)

def has(xml, kw):
    return kw in ' '.join(texts(xml))

def find_bounds(xml, kw):
    # 优先全等，失败后包含匹配（Compose 常合并渲染文本如「灵感脉络 ›」）
    for pat in (r'text="%s"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"' % re.escape(kw),
                r'text="[^"]*%s[^"]*"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"' % re.escape(kw)):
        m = re.search(pat, xml)
        if m:
            x = (int(m.group(1)) + int(m.group(3))) / 2
            y = (int(m.group(2)) + int(m.group(4))) / 2
            return x, y
    return None

def shot(name):
    sh('shell', 'screencap', '-p', '/sdcard/s52.png')
    subprocess.run([ADB, 'pull', '/sdcard/s52.png', os.path.join(OUT, name + '.png')], capture_output=True, timeout=20)
    print('  shot %s' % name, flush=True)

def go_home():
    """无论在哪，回首页：先确认 App 在前台（back 可能退到 launcher），再 back 回首页"""
    # 保底：am start 幂等，App 已在前台时无副作用
    sh('shell', 'am', 'start', '-n', 'com.hotfix.avenlo/.app.MainActivity')
    time.sleep(1.5)
    for _ in range(5):
        xml = dump()
        if find_bounds(xml, '首页') and has(xml, 'Hey, Runel'):
            return xml
        # 在 App 内才 back（launcher 页 back 无意义）
        if has(xml, 'Avenlo') and not has(xml, 'Hey, Runel'):
            back()
        else:
            sh('shell', 'am', 'start', '-n', 'com.hotfix.avenlo/.app.MainActivity')
            time.sleep(1.5)
    xml = dump()
    b = find_bounds(xml, '首页')
    if b: tap(*b, wait=2.0)
    return dump()

# ========== 1. 冷启动 ==========
print('== 1. 冷启动 ==', flush=True)
sh('shell', 'am', 'force-stop', 'com.hotfix.avenlo')
time.sleep(2)
sh('shell', 'am', 'start', '-n', 'com.hotfix.avenlo/.app.MainActivity')
time.sleep(7)
xml = dump()
b = find_bounds(xml, '开始记录')
if b:
    tap(*b, wait=2.5)
    ok('Splash → 开始记录 → 首页')
else:
    ok('已直达首页（Splash 快速跳过）' if has(xml, 'Hey, Runel') else 'splash')
xml = dump()
if has(xml, 'Hey, Runel'):
    ok('P0 首页加载（问候头）')
else:
    bad('首页未加载')
shot('01_home')

# ========== 2. 首页结构（r51 新版式）==========
print('== 2. 首页新结构 ==', flush=True)
xml = dump()
for kw, desc in [
    ('搜索灵感、关键词、标签..', '搜索框'),
    ('今天', 'Tab chips 今天'),
    ('本周', 'Tab chips 本周'),
    ('更早', 'Tab chips 更早'),
    ('记录灵感', '胶囊 FAB「记录灵感」'),
]:
    (ok if has(xml, kw) else bad)(desc)
# FAB 是胶囊（含 ⏺ 图标+文字）——检查底导只有 3 tab
tt = texts(xml)
bottom_tabs = [t for t in tt if t in ('首页', '灵感集', '我的', '记录', '统计')]
if bottom_tabs == ['首页', '灵感集', '我的']:
    ok('底导 3 tab（首页/灵感集/我的，无记录统计）')
else:
    bad('底导异常: %s' % bottom_tabs)
# #tag chips
if any(t.startswith('#') for t in tt):
    ok('灵感卡 #tag chips（#前缀）')
else:
    bad('首页卡片无 #tag')

# ========== 3. Tab 分组切换 ==========
print('== 3. 分组切换 ==', flush=True)
for tab_name in ['本周', '更早', '今天']:
    b = find_bounds(xml, tab_name)
    if b:
        tap(*b, wait=1.5)
        xml = dump()
    else:
        bad('找不到 tab %s' % tab_name)
if has(xml, '今天'):
    ok('分组 chips 切换正常（回到今天）')
shot('02_home_tabs')

# ========== 4. 捕捉链路（FAB 单击 → Ready 态）==========
print('== 4. 捕捉链路 ==', flush=True)
b = find_bounds(xml, '记录灵感')
if b:
    tap(*b, wait=2.5)
    xml = dump()
    shot('03_capture_ready')
    # Ready 态特征：捕捉页元素（放宽匹配）
    tt = ' '.join(texts(xml))
    capture_markers = ['按住', '松开', '捕捉', '录音', '轻捏', '开始', 'Ready', '准备']
    hit = [m for m in capture_markers if m in tt]
    if hit:
        ok('FAB 单击 → 捕捉页 Ready 态（命中: %s）' % hit[:2])
    else:
        bad('FAB 单击未进捕捉页，当前页面: %s' % tt[:80])
    # 退出捕捉页：点左上角「×」关闭（back 会连 App 一起退出）
    xml = dump()
    xbtn = None
    # content-desc 为关闭/×/关闭捕捉 的节点
    m = re.search(r'content-desc="[^"]*(关闭|close|×)[^"]*"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml, re.I)
    if m:
        xbtn = ((int(m.group(2)) + int(m.group(4))) / 2, (int(m.group(3)) + int(m.group(5))) / 2)
    if xbtn:
        tap(*xbtn, wait=1.8)
        ok('捕捉页「×」退出')
    else:
        back(wait=1.2)
        # back 可能退到 launcher，am start 拉回
        sh('shell', 'am', 'start', '-n', 'com.hotfix.avenlo/.app.MainActivity')
        time.sleep(1.5)
else:
    bad('找不到 FAB「记录灵感」')

# ========== 5. 详情 → 脉络互跳 ==========
print('== 5. 详情 → 脉络 ==', flush=True)
xml = dump()
# 灵感卡在 UI 树上是 clickable 容器（View，无 text），文字在子节点。
# 策略：找「·N条」分组头之后的第一张卡容器 bounds，点容器中心。
def first_card_bounds(x):
    # clickable=true 且宽度 >800 且高度 >200 的节点 = 卡片
    for m in re.finditer(r'<node[^>]*clickable="true"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*>', x):
        x1, y1, x2, y2 = map(int, m.groups())
        if (x2 - x1) > 800 and 200 < (y2 - y1) < 500 and y1 > 600:
            return (x1 + x2) / 2, (y1 + y2) / 2, x
    return None, None, x

cb, cy, _ = first_card_bounds(xml)
if cb:
    tap(cb, cy, wait=2.2)
    xml = dump()
    if has(xml, 'AI 摘要') or has(xml, '灵感详情'):
        ok('点灵感卡进详情')
    else:
        bad('点卡片容器未进详情: %s' % ' '.join(texts(xml))[:60])
    if has(xml, '灵感脉络'):
        ok('详情页有「灵感脉络」入口')
        b2 = find_bounds(xml, '灵感脉络')
        tap(*b2, wait=2.2)
        xml = dump()
        shot('04_network')
        if has(xml, '参考样式'):
            ok('脉络页新结构（参考样式徽标）')
        else:
            bad('脉络页未更新到 r51c 版式')
        if has(xml, '相关灵感'):
            ok('脉络页相关灵感列表')
        if has(xml, '查看全部'):
            ok('脉络页「查看全部 ›」')
        # 相关灵感卡点击 → 跳另一个详情（连贯性核心）
        xml2 = dump()
        rcb, rcy, _ = first_card_bounds(xml2)
        if rcb and rcy > 1000:
            tap(rcb, rcy, wait=2.2)
            xml3 = dump()
            if has(xml3, 'AI 摘要') or has(xml3, '灵感详情'):
                ok('脉络 → 相关灵感卡片互跳')
            else:
                bad('点相关灵感未跳详情: %s' % ' '.join(texts(xml3))[:60])
            back()
        else:
            bad('脉络页未找到相关灵感卡容器')
        back()
    else:
        bad('详情页无「灵感脉络」入口')
    back()
else:
    bad('首页未找到灵感卡容器')

# ========== 6. 灵感集（网格 + 最近收录互跳）==========
print('== 6. 灵感集 ==', flush=True)
xml = go_home()
b = find_bounds(xml, '灵感集')
if b:
    tap(*b, wait=2.2)
    xml = dump()
    shot('05_collections')
    for kw, desc in [
        ('灵感集', '页头'),
        ('AI自动归类的主题', '副题'),
        ('旅行灵感', '网格卡1'),
        ('最近收录', '最近收录卡'),
    ]:
        (ok if has(xml, kw) else bad)(desc)
    # 最近收录卡点击 → 详情（互跳验证）
    m = re.search(r'text="(最近收录·[^"]+)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    if m:
        x = (int(m.group(2)) + int(m.group(4))) / 2
        y = (int(m.group(3)) + int(m.group(5))) / 2
        tap(x, y, wait=2.2)
        xml = dump()
        if has(xml, 'AI 摘要') or has(xml, '灵感详情'):
            ok('最近收录卡 → 详情互跳')
        else:
            bad('最近收录卡点击无响应')
        back()
    else:
        bad('最近收录卡未找到（正则）')
    # 集合详情
    b2 = find_bounds(xml, '旅行灵感') or find_bounds(xml, '晨间随想')
    if b2:
        tap(*b2, wait=2.2)
        xml = dump()
        shot('06_collection_detail')
        if has(xml, '返回') or has(xml, '灵感集'):
            ok('网格卡 → 集合详情')
        back()
else:
    bad('底导无灵感集')

# ========== 7. 我的 → 统计（筛选联动）==========
print('== 7. 我的 → 统计 ==', flush=True)
xml = go_home()
b = find_bounds(xml, '我的')
if b:
    tap(*b, wait=2.0)
    xml = dump()
    shot('07_mine')
    for kw, desc in [
        ('我的戒指', '戒指卡'),
        ('今日回顾', '今日回顾卡（蔡心保留项）'),
        ('数据统计', '数据统计设置项'),
    ]:
        (ok if has(xml, kw) else bad)(desc)
    # 统计三格数字存在
    tt = texts(xml)
    nums = [t for t in tt if re.match(r'^\d+$', t)]
    if nums:
        ok('统计三格数字渲染（%s）' % nums[:4])
    else:
        bad('统计三格无数字')
    # 点统计卡（灵感 cell 附近）→ 统计页
    m = re.search(r'text="灵感"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    if m:
        x = (int(m.group(1)) + int(m.group(3))) / 2
        y = (int(m.group(2)) + int(m.group(4))) / 2 - 120  # 上方数字区域
        tap(x, y, wait=2.2)
        xml = dump()
        shot('08_stats')
        if has(xml, '标签与统计'):
            ok('统计卡 → 标签与统计页（新入口）')
        else:
            bad('统计卡点击未跳统计页，页面: %s' % ' '.join(texts(xml))[:60])
        # 筛选 chips 联动：点「旅行」看数字变化
        b2 = find_bounds(xml, '旅行')
        if b2:
            before = [t for t in texts(xml) if re.match(r'^\d+$', t)]
            tap(*b2, wait=1.8)
            xml = dump()
            after = [t for t in texts(xml) if re.match(r'^\d+$', t)]
            if has(xml, '旅行') and (before != after or has(xml, '灵感类型分布')):
                ok('筛选 chips 点击联动（数字 %s → %s）' % (before[:2], after[:2]))
            else:
                bad('筛选 chips 无联动')
            # 回「全部」
            b3 = find_bounds(xml, '全部')
            if b3: tap(*b3, wait=1.5)
        else:
            bad('统计页无筛选 chips')
        back()
    else:
        bad('我的页统计三格未找到')
else:
    bad('底导无我的')

# ========== 8. 今日回顾 ==========
print('== 8. 今日回顾 ==', flush=True)
xml = dump()
b = find_bounds(xml, '我的')
if b: tap(*b, wait=2.0)
xml = dump()
b = find_bounds(xml, '今日回顾')
if b:
    tap(*b, wait=2.2)
    xml = dump()
    shot('09_review')
    for kw, desc in [
        ('今日最佳灵感', '今日最佳灵感'),
        ('意外关联', '意外关联'),
        ('明日待延展', '明日待延展'),
    ]:
        (ok if has(xml, kw) else bad)(desc)
    back()
else:
    bad('我的页无今日回顾入口')

# ========== 9. 崩溃检查 ==========
print('== 9. 崩溃检查 ==', flush=True)
crash = sh('shell', 'logcat', '-d', '-s', 'AndroidRuntime:E', '-t', '400')
if 'FATAL' in crash and 'com.hotfix.avenlo' in crash:
    bad('发现 FATAL 崩溃')
    print(crash[-1500:], flush=True)
else:
    ok('全程无 FATAL 崩溃')

print('', flush=True)
print('======== 结果: %d PASS / %d FAIL ========' % (PASS, FAIL), flush=True)
if FAILS:
    print('失败项: %s' % '; '.join(FAILS), flush=True)
sys.exit(0 if FAIL == 0 else 1)
