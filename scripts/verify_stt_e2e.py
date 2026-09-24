# -*- coding: utf-8 -*-
"""端侧 STT 端到端验证：长按 FAB → 录音（宿主播放中文 wav）→ Done 态本地转写草稿。"""
import subprocess, time, re, os

ADB = r'C:/Users/huangshaozheng/AppData/Local/Android/Sdk/platform-tools/adb.exe'
TEMP = os.environ['TEMP']
WAV = r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/stt-models/zipformer-zh-en-int8/test_wavs/0.wav'

def sh(*args, timeout=30):
    r = subprocess.run([ADB] + list(args), capture_output=True, text=True, timeout=timeout, encoding='utf-8', errors='replace')
    return (r.stdout or '') + (r.stderr or '')

def dump(tag):
    for _ in range(3):
        sh('shell', 'uiautomator', 'dump', '/sdcard/x.xml')
        out = sh('pull', '/sdcard/x.xml', os.path.join(TEMP, f'stt_{tag}.xml'))
        if 'file pulled' in out:
            try:
                return open(os.path.join(TEMP, f'stt_{tag}.xml'), encoding='utf-8').read()
            except Exception:
                pass
        time.sleep(1.5)
    return ''

# 1. 回首页
sh('shell', 'am', 'force-stop', 'com.hotfix.avenlo')
time.sleep(1)
sh('shell', 'am', 'start', '-n', 'com.hotfix.avenlo/.app.MainActivity', '-f', '0x8000')
for _ in range(15):
    time.sleep(1.5)
    xml = dump('home')
    if 'Hey, Runel' in xml:
        break
print('home ok:', 'Hey, Runel' in xml)

# FAB 坐标
m = re.search(r'text="轻捏"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
if m:
    x = (int(m.group(1)) + int(m.group(3))) // 2
    y = (int(m.group(2)) + int(m.group(4))) // 2
else:
    x, y = 894, 1931
print('FAB:', x, y)

# 2. 推 wav 到模拟器并准备播放（录的同时播，模拟器 mic 走宿主混音可能录不到，
#    但 MediaRecorder 也会录到 emulator 虚拟麦克风底噪——先验证链路不 crash）
sh('push', WAV, '/sdcard/stt_test.wav')

# 3. 长按 FAB（时间驱动手势，600ms swipe）
sh('shell', 'input', 'swipe', str(x), str(y), str(x), str(y), '600')
time.sleep(2)
xml2 = dump('rec')
print('recording:', '正在聆听' in xml2)

# 4. 播放 wav（让麦克风通道有内容；模拟器 -audio 默认主机混音，MediaRecorder 可能录到）
t_play = subprocess.Popen([ADB, 'shell', 'am', 'start', '-a', 'android.intent.action.VIEW',
                           '-d', 'file:///sdcard/stt_test.wav', '-t', 'audio/wav'])
# 等静默自动保存（无声音则 3s 静默自动结束 + 撤销窗 3s）
draft_found = False
for _ in range(20):
    time.sleep(1.5)
    x3 = dump('done')
    if '灵感已保存' in x3 or '本地转写草稿' in x3:
        draft_found = '本地转写草稿' in x3
        break
    if 'Hey, Runel' in x3:
        break
print('done state:', '灵感已保存' in x3)
print('draft shown:', draft_found)
# 草稿文本内容
if draft_found:
    texts = re.findall(r'text="([^"]{0,120})"', x3)
    for t in texts:
        if '草稿' in t or ('昨天' in t) or ('MONDAY' in t.upper()):
            print('  draft text:', t[:100])
            break

# 5. crash 检查
crash = sh('logcat', '-d', '-b', 'crash', timeout=20)
print('crash:', 'FATAL' in crash)
if 'FATAL' in crash:
    print(crash[-1500:])

sh('shell', 'screencap', '-p', '/sdcard/stt_done.png')
sh('pull', '/sdcard/stt_done.png', r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/emulator-screens/round48_stt_done.png')
print('DONE')
