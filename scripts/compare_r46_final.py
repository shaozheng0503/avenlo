# -*- coding: utf-8 -*-
"""Round46 final: 新旧截图像素回归对比（配色校准验证）"""
from PIL import Image
import collections, os

OLD = r'C:\Users\huangshaozheng\WorkBuddy\2026-09-20-14-20-48\anker-hackathon\emulator-screens\round46\S01_home.png'
NEW = r'C:\Users\huangshaozheng\WorkBuddy\2026-09-20-14-20-48\anker-hackathon\emulator-screens\round46_final\F01_home.png'

def fingerprint(path, label):
    im = Image.open(path).convert('RGB')
    px = im.load(); w, h = im.size
    c = collections.Counter()
    for y in range(0, h, 6):
        for x in range(0, w, 6):
            c[px[x, y]] += 1
    print(f'== {label} ({w}x{h}) top10 ==')
    for color, n in c.most_common(10):
        print(f'  {color}  #{color[0]:02X}{color[1]:02X}{color[2]:02X}  x{n}')
    # 主色像素总量（容差 30）
    prim = sum(n for (r, g, b), n in c.items()
               if abs(r-0xBF) < 30 and abs(g-0x69) < 30 and abs(b-0x40) < 30)
    total = sum(c.values())
    print(f'  primary-ish: {prim}/{total} = {100*prim/total:.2f}%')

fingerprint(OLD, 'OLD S01_home (改前)')
print()
fingerprint(NEW, 'NEW F01_home (改后)')
