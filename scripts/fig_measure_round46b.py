# -*- coding: utf-8 -*-
"""Round46b: 从设计稿手机切图量化 圆角/边距/底栏/FAB —— 为 Task #24 修复提供精确数值"""
from PIL import Image
import glob, os, json

OUT = {}

def is_bg(p):
    # 设计稿米白背景（含轻微 JPEG 噪声）：亮 + 微暖
    return p[0] >= 242 and p[1] >= 238 and p[2] >= 222

def is_primary(p):
    return abs(p[0]-0xBF) < 22 and abs(p[1]-0x69) < 22 and abs(p[2]-0x40) < 22

for path in sorted(glob.glob('anker-hackathon/figma-export/screens_r46/*.png')):
    name = os.path.basename(path).replace('.png', '')
    im = Image.open(path).convert('RGB')
    w, h = im.size
    px = im.load()

    # ---- 1) 找 FAB（主色团块，位于右下 1/4）----
    fab = None
    for y in range(int(h*0.70), h):
        cnt = sum(1 for x in range(int(w*0.5), w) if is_primary(px[x, y]))
        if cnt > 8:
            if fab is None:
                fab = [y, y, int(w*0.5), 0]
            fab[1] = y
            # 最右主色 x
            for x in range(w-1, int(w*0.5), -1):
                if is_primary(px[x, y]):
                    fab[3] = max(fab[3], x)
                    break
    fab_d = None
    if fab:
        fw = fab[3] - (fab[2] if fab[2] else int(w*0.5))
        # 更准确：量 fab 区域宽高
        xs = []
        for y in range(fab[0], fab[1]+1):
            for x in range(int(w*0.5), w):
                if is_primary(px[x, y]):
                    xs.append(x)
        if xs:
            fab_d = {'y0': fab[0], 'y1': fab[1], 'h_px': fab[1]-fab[0]+1,
                     'x_min': min(xs), 'x_max': max(xs), 'w_px': max(xs)-min(xs)+1,
                     'right_gap_px': w-1-max(xs), 'bottom_gap_px': h-1-fab[1]}

    # ---- 2) 卡片左边距：取中部 30%~60% 高度内的行，量第一段非背景起点 ----
    margins = []
    for y in range(int(h*0.30), int(h*0.60)):
        row = [x for x in range(w) if not is_bg(px[x, y])]
        if row:
            # 只统计内容起始在左侧 0~25% 的行（卡片行），跳过整宽文本噪声
            margins.append(row[0])
    left_margin = None
    if margins:
        margins.sort()
        left_margin = margins[len(margins)//4]  # 下四分位：过滤偶发噪声

    # ---- 3) 卡片圆角：找一条卡片上边缘，沿曲线量内缩 ----
    radius_px = None
    # 在 35%~55% 高度找第一行出现"左侧 margin 起点的连续内容带"（卡片顶边）
    band_top = None
    for y in range(int(h*0.30), int(h*0.60)):
        row = [x for x in range(w) if not is_bg(px[x, y])]
        if row and row[0] <= (left_margin or 8) + 2 and len(row) > w*0.5:
            band_top = y
            break
    if band_top is not None:
        # 从顶边向下走，量每行最左内容 x，收敛到稳定值
        xs = []
        for dy in range(0, 20):
            y = band_top + dy
            if y >= h: break
            row = [x for x in range(w) if not is_bg(px[x, y])]
            if row:
                xs.append(row[0])
            else:
                xs.append(None)
        # 半径 ≈ x 从顶边中心值走到稳定左缘所需的行数
        stable = None
        for x in xs:
            if x is not None and (stable is None or x < stable):
                stable = x
        if stable is not None:
            r = 0
            for x in xs:
                if x is not None and x <= stable + 1:
                    break
                r += 1
            radius_px = r

    # ---- 4) 底部导航带：底部 6% 高度的主色占比与背景色 ----
    nav = {'primary_px': 0, 'bg_px': 0, 'total': 0}
    for y in range(int(h*0.94), h):
        for x in range(w):
            p = px[x, y]
            nav['total'] += 1
            if is_primary(p): nav['primary_px'] += 1
            elif is_bg(p): nav['bg_px'] += 1

    OUT[name] = {'size': [w, h], 'fab': fab_d, 'left_margin_px': left_margin,
                 'card_radius_px': radius_px, 'band_top': band_top, 'nav': nav}

print(json.dumps(OUT, ensure_ascii=False, indent=1))
with open('anker-hackathon/scripts/fig_measure_r46b.json', 'w', encoding='utf-8') as f:
    json.dump(OUT, f, ensure_ascii=False, indent=1)
