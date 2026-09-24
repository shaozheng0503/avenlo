# -*- coding: utf-8 -*-
"""Round46: 解析 aiforce 导出 fig 的节点名视图树（Type_x_y_N 带坐标）。
输出按 PNG 样机分组的节点流 + 具名色 + 字体字号上下文。
"""
import json, os, re

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
d = json.load(open(os.path.join(BASE, 'figma-export', 'strings.json'), encoding='utf-8'))
items = [s for s in d if isinstance(s, str)]

# 1) 全量节点名 Type_x_y_N
node_re = re.compile(r'^(ImageView|TextView|Button|ViewGroup|Background|EditText|CheckBox|RecyclerView|LinearLayout|FrameLayout|RelativeLayout|Switch|ProgressBar|ImageView_TextView)_(\d+)_(\d+)_(\d+)$')
nodes = []
for i, s in enumerate(items):
    m = node_re.match(s)
    if m:
        nodes.append((i, m.group(1), int(m.group(2)), int(m.group(3)), int(m.group(4)), s))
print('total nodes:', len(nodes))

# 2) 找文本节点附近的中文文案（节点名后紧跟的字符串很可能是其文本）
# 建立序号 -> 文本 的映射：TextView_x_y_N 后 1~3 个字符串中找中文
node_by_idx = {i: (t, x, y, n) for i, t, x, y, n, _ in nodes}
text_map = {}   # node idx -> text
for i, t, x, y, n, name in nodes:
    if t in ('TextView', 'Button', 'EditText'):
        # 在后续 1..5 个字符串里找第一个非节点名的中英文本
        for j in range(i + 1, min(i + 6, len(items))):
            s2 = items[j]
            if node_re.match(s2):
                break
            if s2 in ('Light', 'Medium', 'Regular', 'Bold', 'SemiBold') or s2.startswith('bg_'):
                continue
            if len(s2) >= 1 and not re.fullmatch(r'[A-Za-z0-9 _/.\-]+', s2) or re.match(r'^[A-Za-z]', s2) and len(s2) <= 30:
                text_map[i] = s2
                break

# 3) 分组输出：按样机 PNG 名分隔。PNG 名出现在 items 中（'Figma design - Avenlo ' + '机.png' 拆开了）
# 直接用坐标聚类：x<300 第一台, 300-600 第二台... 用 y 排序输出
nodes_sorted = sorted(nodes, key=lambda v: (v[2], v[3]))  # 按 x,y
# 聚类 x
xs = sorted(set(v[2] for v in nodes))
print('x range:', xs[0], '-', xs[-1])

# 4) 输出 TextView/Button 的 (x, y, text) 列表（还原每屏文字布局）
out_lines = []
for i, t, x, y, n, name in sorted(nodes, key=lambda v: (v[3], v[2])):
    txt = text_map.get(i, '')
    if t in ('TextView', 'Button', 'EditText') and txt:
        out_lines.append(f'{x:>5},{y:>5}  {t:<9} {txt[:44]}')
print('text nodes with content:', len(out_lines))
with open(os.path.join(BASE, 'scripts', 'fig_nodes_r46.txt'), 'w', encoding='utf-8') as f:
    f.write('\n'.join(out_lines))
print('saved -> scripts/fig_nodes_r46.txt')

# 5) 具名色与 hex 对照（Figma 常用色名 → 需从二进制颜色值确认，先列出名字）
named = ['Dune','Fuscous Gray','Napa','Brown Rust','Di Serria','Laurel','Kimberly','Dawn Pink','Japonica','Blue Ribbon','Mine Shaft']
for nm in named:
    idxs = [i for i, s in enumerate(items) if s == nm]
    print(nm, 'count:', len(idxs), 'first:', idxs[:3])
