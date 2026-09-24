# -*- coding: utf-8 -*-
"""Round46: 切出设计稿每屏并导出高清放大图，供逐屏比对。"""
import os, json
from PIL import Image

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EXP = os.path.join(BASE, 'figma-export')
OUTDIR = os.path.join(EXP, 'screens_r46')
os.makedirs(OUTDIR, exist_ok=True)

data = json.load(open(os.path.join(BASE, 'scripts', 'fig_extract_round46_design_screens.json'), encoding='utf-8'))

MOCK_FILES = {
    '三屏样机': 'Figma design - Avenlo 首页与录音三屏样机.png.png',
    '双屏样机': 'Figma design - Avenlo 个人与回顾双屏样机.png.png',
    'Root': 'Root.png',
}
# Root.png: phone1=首页 phone2=捕捉/录音 phone3=灵感集（按内容带推断，屏幕名待内容确认）
NAME_MAP = {
    ('Root', 0): 'root_phone1', ('Root', 1): 'root_phone2', ('Root', 2): 'root_phone3',
    ('三屏样机', 0): 'mock3_phone1', ('三屏样机', 1): 'mock3_phone2',
    ('双屏样机', 0): 'mock2_phone1', ('双屏样机', 1): 'mock2_phone2',
    ('双屏样机', 2): 'mock2_phone3', ('双屏样机', 3): 'mock2_phone4',
}

for s in data['screens']:
    mock, idx = s['name'].split('#phone')
    idx = int(idx) - 1
    key = (mock, idx)
    if key not in NAME_MAP:
        continue
    im = Image.open(os.path.join(EXP, MOCK_FILES[mock])).convert('RGB')
    crop = im.crop(tuple(s['box']))
    # 放大到宽 720 便于查看
    w, h = crop.size
    scale = 720 / w
    big = crop.resize((720, int(h * scale)), Image.LANCZOS)
    out = os.path.join(OUTDIR, f"{NAME_MAP[key]}.png")
    big.save(out)
    print(NAME_MAP[key], crop.size, '->', big.size)
print('done ->', OUTDIR)
