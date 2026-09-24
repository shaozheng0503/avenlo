# -*- coding: utf-8 -*-
"""Round46: 模拟器常驻启动 + 自检 boot。脚本自身不退出（保持父进程活），boot 完成后打印并轮询守护。
用法: python emu_daemon_r46.py   (配合 Bash run_in_background)"""
import subprocess, time, os, sys

ADB = r'C:/Users/huangshaozheng/AppData/Local/Android/Sdk/platform-tools/adb.exe'
emu = r'C:/Users/huangshaozheng/AppData/Local/Android/Sdk/emulator/emulator.exe'

def log(msg):
    print(msg, flush=True)

# 1) 若已有进程则跳过启动
r = subprocess.run(['tasklist'], capture_output=True)
out = r.stdout.decode('gbk', 'replace')
if 'qemu-system-x86_64' in out:
    log('emulator already running')
else:
    log('launching emulator...')
    subprocess.Popen([emu, '-avd', 'avenlo_test', '-no-snapshot-load', '-gpu', 'swiftshader_indirect'],
                     creationflags=0x00000008 | 0x00000200)

# 2) 轮询 boot
booted = False
for i in range(100):
    time.sleep(5)
    try:
        v = subprocess.run([ADB, 'shell', 'getprop', 'sys.boot_completed'],
                           capture_output=True, text=True, timeout=15).stdout.strip()
    except Exception:
        v = ''
    if v == '1':
        log(f'BOOTED at ~{(i+1)*5}s')
        booted = True
        break
    if i % 12 == 0:
        r = subprocess.run(['tasklist'], capture_output=True)
        o = r.stdout.decode('gbk', 'replace')
        alive = 'qemu-system-x86_64' in o
        log(f'{(i+1)*5}s qemu_alive={alive}')
        if not alive:
            log('QEMU DIED - abort')
            break

if booted:
    log('waiting extra 15s for window manager...')
    time.sleep(15)
    r = subprocess.run([ADB, 'shell', 'getprop', 'init.svc.bootanim'], capture_output=True, text=True)
    log('bootanim: ' + r.stdout.strip())
    log('DAEMON READY - keeping process alive')
    # 守护循环：每 60s 检查 adb 连接，保持进程存活
    while True:
        time.sleep(60)
        try:
            subprocess.run([ADB, 'shell', 'true'], capture_output=True, timeout=15)
        except Exception:
            log('adb check failed, still alive loop')
else:
    log('DAEMON EXIT - boot failed')
