# -*- coding: utf-8 -*-
"""Round46: 从设计稿样机 PNG 像素级提取每屏视觉 token（唯一视觉基准）。
输出: scripts/fig_extract_round46_design_screens.json
策略:
  1. 三个样机图各自定位手机屏幕区域（找米白色连续区块）
  2. 每屏提取: 背景主色 / 色彩直方图 / 主色(#BF6940系)分布 / 垂直内容带 / 左右边距
"""
import os, json
from PIL import Image
from collections import Counter

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EXP = os.path.join(BASE, 'figma-export')
OUT = os.path.join(BASE, 'scripts', 'fig_extract_round46_design_screens.json')

MOCKUPS = [
    ('三屏样机', 'Figma design - Avenlo 首页与录音三屏样机.png.png'),
    ('双屏样机', 'Figma design - Avenlo 个人与回顾双屏样机.png.png'),
    ('Root', 'Root.png'),
]

def is_bgish(px):
    r, g, b = px
    return r > 220 and g > 215 and b > 195  # 米白系

def find_phone_bands(im):
    """找手机屏幕矩形：米白色区域构成的连通竖带"""
    w, h = im.size
    px = im.load()
    col_content = []
    for x in range(w):
        n = 0
        for y in range(0, h, 3):
            if is_bgish(px[x, y]):
                n += 1
        col_content.append(n)
    thr = max(col_content) * 0.25
    bands = []
    inb = False
    for x, n in enumerate(col_content):
        if n > thr and not inb:
            start = x; inb = True
        elif n <= thr and inb:
            if x - start > 120: bands.append([start, x])
            inb = False
    if inb and w - start > 120: bands.append([start, w])
    # 每带找 y 范围（裁剪到图像内）
    out = []
    for x0, x1 in bands:
        x1 = min(x1, w)
        ys = [y for y in range(h) if any(is_bgish(px[x, y]) for x in range(x0, min(x1, w), 6))]
        if ys:
            out.append((x0, min(ys), x1, max(ys)))  # (left, upper, right, lower)
    return out

def analyze_screen(im, box, name):
    x0, y0, x1, y1 = box
    w = x1 - x0; h = y1 - y0
    region = im.crop(box)
    rp = region.load()
    # 1. 颜色直方图
    c = Counter()
    for y in range(0, h, 2):
        for x in range(0, w, 2):
            c[rp[x, y]] += 1
    total = sum(c.values())
    top = [('#%02X%02X%02X' % col, round(100 * n / total, 2)) for col, n in c.most_common(12)]
    # 2. 主色分布（陶土色系 r>150, g 80~140, b<100）
    primary_rows = []
    for y in range(0, h, 2):
        n = 0
        for x in range(0, w, 2):
            r, g, b = rp[x, y]
            if 140 < r < 220 and 70 < g < 140 and 40 < b < 110:
                n += 1
        if n > 2:
            primary_rows.append((y, n))
    # 3. 垂直内容带（非背景行）
    rows = []
    for y in range(h):
        n = 0
        for x in range(0, w, 4):
            if not is_bgish(rp[x, y]):
                n += 1
        rows.append(n)
    bands_y = []
    inb = False
    for y, n in enumerate(rows):
        if n > w * 0.02 and not inb:
            sy = y; inb = True
        elif n <= w * 0.02 and inb:
            if y - sy > 4: bands_y.append((round(100 * sy / h, 1), round(100 * y / h, 1)))
            inb = False
    if inb: bands_y.append((round(100 * sy / h, 1), 100.0))
    # 4. 左右内容边距（首个/末个非背景列, 全高统计）
    left = None; right = None
    for x in range(w):
        n = sum(1 for y in range(0, h, 4) if not is_bgish(rp[x, y]))
        if n > h * 0.01:
            left = x; break
    for x in range(w - 1, -1, -1):
        n = sum(1 for y in range(0, h, 4) if not is_bgish(rp[x, y]))
        if n > h * 0.01:
            right = x; break
    return {
        'name': name, 'box': box, 'size': [w, h],
        'top_colors': top,
        'primary_rows_sample': primary_rows[:40],
        'content_bands_pct': bands_y[:30],
        'left_margin_px': left, 'right_margin_px': right,
        'margin_pct': [round(100 * left / w, 1), round(100 * (w - right) / w, 1)] if left is not None and right is not None else None,
    }

result = {'screens': []}
for mock_name, fn in MOCKUPS:
    path = os.path.join(EXP, fn)
    im = Image.open(path).convert('RGB')
    bands = find_phone_bands(im)
    print(f'== {mock_name} {im.size} phones={len(bands)} ==')
    for i, box in enumerate(bands):
        info = analyze_screen(im, tuple(box), f'{mock_name}#phone{i+1}')
        result['screens'].append(info)
        print(f"  phone{i+1} box={box} bg={info['top_colors'][0]} margins={info['margin_pct']}")
        print(f"    bands_y={info['content_bands_pct'][:12]}")

with open(OUT, 'w', encoding='utf-8') as f:
    json.dump(result, f, ensure_ascii=False, indent=1)
print('saved ->', OUT)
