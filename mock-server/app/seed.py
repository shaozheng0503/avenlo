"""预灌种子数据 —— 文案来自 .fig 源文件提取（方案 1.4.2），演示数据 ≠ 造假"""
import time
from datetime import date as _date

NOW = int(time.time() * 1000)

WEEKDAY_CN = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"]
TODAY_CN = f"{_date.today().year}年{_date.today().month}月{_date.today().day}日·{WEEKDAY_CN[_date.today().weekday()]}"


def _idea(i, title, summary, tags, hours_ago, duration_ms, related=None, status="ok"):
    return {
        "id": f"idea_{i:02d}",
        "title": title,
        "summary": summary,
        "transcript": summary,
        "tags": tags,
        "status": status,
        "related": related or [],
        "extension": {},
        "collectionId": None,
        "audioUrl": None,
        "createdAt": NOW - int(hours_ago * 3600 * 1000),
        "durationMs": duration_ms,
    }


RELATED_TRAVEL = [
    {"id": "idea_11", "title": "清晨的露水", "relation": "similar_theme", "durationMs": 22000, "tag": "自然"},
    {"id": "idea_12", "title": "咖啡馆与工作", "relation": "same_collection", "durationMs": 560000, "tag": "创作"},
    {"id": "idea_13", "title": "山野徒步", "relation": "similar_theme", "tag": "户外"},
]

IDEAS = [
    _idea(1, "关于旅行的灵感", "在陌生城市中通过观察、聆听和日常邂逅取得新的视角。", ["旅行", "生活"], 6, 136000, RELATED_TRAVEL),
    _idea(2, "写作素材：时间与记忆", "时间的流逝感与记忆的选择性保存，可以作为叙事的双线结构。", ["灵感", "日记"], 4, 232000),
    _idea(3, "生活片段·咖啡馆", "咖啡馆里的声音层次：磨豆、低语、杯碟——城市白噪音的样本。", ["创作", "美食"], 2.5, 96000),
    _idea(4, "城市与人", "城市废墟中的纹理与光影，人与空间的相互塑造。", ["旅行", "摄影"], 50, 321000),
    _idea(5, "夜骑的城市观察", "晚间骑行时城市灯光的节奏变化。", ["城市", "观察"], 74, 154000),
    _idea(6, "播客选题：慢生活", "与「慢生活」主张者对谈的系列选题。", ["播客", "创作"], 120, 208000),
    _idea(7, "菜市场的人类学", "菜市场作为城市公共空间的样本。", ["生活", "观察"], 192, 187000),
    _idea(8, "一张照片的配色", "废墟场景的低饱和暖色配色方案。", ["摄影", "灵感"], 216, 45000),
]

# 详情页（idea_01）完整延展 —— .fig 实读文案
IDEAS[0]["summary"] = (
    "这条灵感表达了「旅行中的慢生活」的体验。强调在陌生城市中通过观察、聆听和日常邂逅取得新的视角，"
    "它可以延展到产品设计、生活方式内容与品牌故事的方向。"
)
IDEAS[0]["extension"] = {
    "perspectives": [
        {"title": "旅行 × 当地文化体验", "desc": "与当地人交流并参与文化活动，打破游客视角的旅行方式"},
        {"title": "旅行与内容创作结合", "desc": "将旅行中的体验转化为图文、视频、播客等内容的可能"},
        {"title": "轻量化旅行用品设计", "desc": "面向「轻旅行」人群，设计更轻便、更灵活的旅行用品"},
    ],
    "references": [
        {"title": "The Way We Travel", "url": "https://example.com/way-we-travel"},
        {"title": "慢城市主义：以人的尺度重建街道", "url": "https://example.com/slow-urbanism"},
        {"title": "设计中的观察法", "url": "https://example.com/observation-in-design"},
    ],
    "directions": [
        {"title": "城市观察笔记系列内容", "desc": "把「观察—记录—延展」做成栏目"},
        {"title": "旅行主题灵感集运营", "desc": "沉淀为可分享的灵感集合"},
    ],
}
IDEAS[0]["collectionId"] = "col_01"

COLLECTIONS = [
    {"id": "col_01", "name": "旅行灵感", "subtitle": "关于远方、路上与不同的生活方式", "count": 12, "tone": "sage"},
    {"id": "col_02", "name": "项目构思", "subtitle": "创意想法与产品方向", "count": 5, "tone": "peach"},
    {"id": "col_03", "name": "晨间随想", "subtitle": "清晨的灵感与碎片", "count": 15, "tone": "gold"},
    {"id": "col_04", "name": "阅读笔记", "subtitle": "书籍、文章与知识沉淀", "count": 7, "tone": "blue"},
]

DAILY_REVIEW = {
    "date": TODAY_CN,
    "bestQuote": "真正的放松不是躺平，而是把注意力收回来，转回当下的感受。",
    "bestTags": ["生活", "心情", "旅行"],
    "serendipityDesc": "两个想法在时间与空间上形成了有趣的关联",
    "pairLeft": {"title": "酒店空间", "subtitle": "度假 × 设计语言", "tag": "研究笔记"},
    "pairRight": {"title": "个人成长", "subtitle": "独处时光 × 自我关照", "tag": "思考片段"},
    "directions": [
        {"title": "城市中的自愈疗愈空间", "desc": "在高密度城市里创造自然疗愈的角落"},
        {"title": "旅行与工作方式的融合", "desc": "是否可以设计一种新的远程工作场景？"},
    ],
}
