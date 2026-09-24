# -*- coding: utf-8 -*-
"""Round46: 模拟器启动诊断 v2 —— 重定向到文件，避免 SIGTERM 丢输出"""
import subprocess

emu = r'C:/Users/huangshaozheng/AppData/Local/Android/Sdk/emulator/emulator.exe'
outp = r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/emu_diag_out.txt'
errp = r'C:/Users/huangshaozheng/WorkBuddy/2026-09-20-14-20-48/anker-hackathon/emu_diag_err.txt'
with open(outp, 'w') as fo, open(errp, 'w') as fe:
    try:
        r = subprocess.run([emu, '-avd', 'avenlo_test', '-no-snapshot-load', '-gpu', 'swiftshader_indirect'],
                           stdout=fo, stderr=fe, timeout=30)
        print('RC', r.returncode)
    except subprocess.TimeoutExpired:
        print('STILL RUNNING after 30s (good)')
import os
print('out size:', os.path.getsize(outp))
print('err size:', os.path.getsize(errp))
print('--- err tail ---')
print(open(errp, 'rb').read().decode('utf-8', 'replace')[-2500:])
