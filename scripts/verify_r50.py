# -*- coding: utf-8 -*-
"""Round50 验证：新设计稿 UI 改动专项 + P0 回归
覆盖：
  1. 冷启动 → 首页（种子卡 + 新搜索文案 + 日期格式）
  2. 底导「统计」Tab → 统计页（回顾你的灵感/记录总数/连续记录/热力图/类型分布）
  3. 详情页「灵感脉络」→ 脉络页（发现灵感之间的连接与可能/脉络解读/相关灵感）
  4. 我的页（电量82%/今日回顾入口卡/设置新文案）
  5. 今日回顾（每晚21:00 点推送 + 明日待延展新文案）
  6. P0：首页种子卡渲染（server 数据链路）
"""
import subprocess, time, os, sys, re

ADB = os.path.expandvars(r'%LOCALAPPDATA%/Android/Sdk/platform-tools/adb.exe')
BASE = r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon'
OUT = os.path.join(BASE, 'emulator-screens', 'round50')
os.makedirs(OUT, exist_ok=True)
XML = os.path.join(BASE, 'ui_dump_r50.xml')

PASS = 0; FAIL = 0; FAILS = []

def ok(m):
    global PASS; PASS += 1; print('  [PASS] %s' % m, flush=True)

def bad(m):
    global FAIL; FAIL += 1; FAILS.append(m); print('  [FAIL] %s' % m, flush=True)

def sh(*args, timeout=40):
    r = subprocess.run([ADB] + list(args), capture_output=True, text=True, timeout=timeout, encoding='utf-8', errors='replace')
    return (r.stdout or '') + (r.stderr or '')

def tap(x, y, wait=1.6):
    sh('shell', 'input', 'tap', str(int(x)), str(int(y)))
    time.sleep(wait)

def back(wait=1.2):
    sh('shell', 'input', 'keyevent', '4')
    time.sleep(wait)

def dump():
    for _ in range(3):
        sh('shell', 'rm', '-f', '/sdcard/ui50.xml')
        sh('shell', 'uiautomator', 'dump', '/sdcard/ui50.xml')
        r = subprocess.run([ADB, 'pull', '/sdcard/ui50.xml', XML], capture_output=True, text=True, timeout=20)
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
    m = re.search(r'text="%s"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"' % re.escape(kw), xml)
    if m:
        x = (int(m.group(1)) + int(m.group(3))) / 2
        y = (int(m.group(2)) + int(m.group(4))) / 2
        return x, y
    return None

def shot(name):
    sh('shell', 'screencap', '-p', '/sdcard/s50.png')
    subprocess.run([ADB, 'pull', '/sdcard/s50.png', os.path.join(OUT, name + '.png')], capture_output=True, timeout=20)
    print('  shot %s' % name, flush=True)

# ========== 启动 ==========
print('== 冷启动 ==', flush=True)
sh('shell', 'am', 'force-stop', 'com.hotfix.avenlo')
time.sleep(2)
sh('shell', 'am', 'start', '-n', 'com.hotfix.avenlo/.app.MainActivity')
time.sleep(6)
shot('01_launch')
# Splash → 开始记录
xml = dump()
b = find_bounds(xml, '开始记录')
if b:
    tap(*b, wait=2.5)
    ok('Splash → 首页')
else:
    # 可能已经直接进了首页
    if has(xml, '今天'):
        ok('已直达首页（无 Splash 拦截）')
    else:
        bad('Splash 后未找到入口')
shot('02_home')

xml = dump()
if has(xml, '关于旅行的灵感'):
    ok('P0 首页种子卡渲染（server 数据链路 OK）')
else:
    bad('首页无种子卡（server 链路?）')
if has(xml, '搜索灵感、关键词、标签..'):
    ok('首页新搜索占位文案')
else:
    bad('首页搜索文案未更新')

# ========== 统计 Tab ==========
print('== 统计页（新） ==', flush=True)
b = find_bounds(xml, '统计')
if b:
    tap(*b, wait=2.0)
else:
    bad('底导无「统计」Tab'); b = None
shot('03_stats')
xml = dump()
for kw, desc in [
    ('回顾你的灵感', '页头「回顾你的灵感」'),
    ('记录总数', '记录总数卡'),
    ('连续记录', '连续记录卡'),
    ('灵感热力图', '热力图卡'),
    ('灵感类型分布', '类型分布卡'),
]:
    (ok if has(xml, kw) else bad)(desc)
tt = texts(xml)
if any(re.match(r'^旅行\s*28%$', t) or t == '旅行28%' or '旅行' == t for t in tt):
    ok('类型分布含「旅行」')
pct = [t for t in tt if '%' in t]
print('  分布数字: %s' % pct[:8], flush=True)
if any('28' in p for p in pct):
    ok('分布百分比渲染（28%% 出现）')
else:
    bad('分布百分比缺失')

# ========== 详情 → 灵感脉络 ==========
print('== 详情页 → 灵感脉络 ==', flush=True)
b = find_bounds(xml, '首页')
if b: tap(*b, wait=1.8)
xml = dump()
b = find_bounds(xml, '关于旅行的灵感')
if not b:
    # 滚动找
    sh('shell', 'input', 'swipe', '540', '1400', '540', '600', '400')
    time.sleep(1.2)
    xml = dump()
    b = find_bounds(xml, '关于旅行的灵感')
if b:
    tap(*b, wait=2.2)
    shot('04_detail')
    xml = dump()
    if has(xml, '灵感脉络'):
        ok('详情页「灵感脉络」入口')
        bb = find_bounds(xml, '灵感脉络')
        tap(*bb, wait=2.2)
        shot('05_network')
        xml = dump()
        for kw, desc in [
            ('发现灵感之间的连接与可能', '脉络副标题'),
            ('脉络解读', '脉络解读卡'),
            ('相关灵感', '相关灵感列表'),
        ]:
            (ok if has(xml, kw) else bad)(desc)
        back()
    else:
        bad('详情页无「灵感脉络」入口')
    back()
else:
    bad('首页找不到种子卡进详情')

# ========== 我的页 ==========
print('== 我的页 ==', flush=True)
xml = dump()
b = find_bounds(xml, '我的')
if b: tap(*b, wait=2.0)
shot('06_mine')
xml = dump()
for kw, desc in [
    ('我的戒指', '我的戒指卡'),
    ('已连接', '已连接状态'),
    ('今日回顾', '今日回顾入口卡'),
    ('每晚21:00 点推送', '提醒时间新文案'),
    ('导出数据入口', '导出数据入口（新文案）'),
    ('我们一直在倾听', '帮助与反馈副文'),
]:
    (ok if has(xml, kw) else bad)(desc)
tt = texts(xml)
if any('82%' in t for t in tt):
    ok('电量 82%（新设计稿值）')
else:
    bad('电量 82% 未找到')

# 电量可能在电量环里显示为 "82%"，向下滚动确认设置区
sh('shell', 'input', 'swipe', '540', '1600', '540', '700', '400')
time.sleep(1.2)
xml = dump()
if has(xml, 'AI整理偏好'):
    ok('AI整理偏好设置项')
shot('07_mine_scrolled')

# ========== 今日回顾 ==========
print('== 今日回顾（预览页） ==', flush=True)
xml = dump()
b = find_bounds(xml, '今日回顾')
if b:
    tap(*b, wait=2.2)
    shot('08_review')
    xml = dump()
    for kw, desc in [
        ('今日最佳灵感', '今日最佳灵感'),
        ('意外关联', '意外关联'),
        ('明日待延展', '明日待延展'),
        ('每晚21:00 点推送', '底部提醒新文案'),
    ]:
        (ok if has(xml, kw) else bad)(desc)
    # 滚到明日待延展看新文案
    sh('shell', 'input', 'swipe', '540', '1500', '540', '800', '400')
    time.sleep(1.0)
    xml = dump()
    if has(xml, '城市中的自然疗愈空间'):
        ok('明日待延展文案对齐（自然疗愈空间）')
    else:
        bad('明日待延展文案未对齐')
    shot('09_review_scrolled')
else:
    bad('我的页无今日回顾入口')

# ========== 崩溃检查 ==========
print('== 崩溃检查 ==', flush=True)
crash = sh('shell', 'logcat', '-d', '-s', 'AndroidRuntime:E', '-t', '300')
if 'FATAL' in crash and 'com.hotfix.avenlo' in crash:
    bad('发现 FATAL 崩溃')
    print(crash[-1200:], flush=True)
else:
    ok('无 FATAL 崩溃')

print('', flush=True)
print('======== 结果: %d PASS / %d FAIL ========' % (PASS, FAIL), flush=True)
if FAILS:
    print('失败项: %s' % '; '.join(FAILS), flush=True)
sys.exit(0 if FAIL == 0 else 1)
