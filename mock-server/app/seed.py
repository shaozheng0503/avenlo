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

# 种子卡互关联（第三十二轮）：评姘认意点开任何卡都应有「相关想法」——
# 语义映射与 _MOCK_RELATED 同口径：咖啡馆→声音/工作；写作→时间记忆；城市系互连；播客→慢生活
RELATED_CAFE = [
    {"id": "idea_12", "title": "咖啡馆与工作", "relation": "similar_theme", "durationMs": 560000, "tag": "创作"},
    {"id": "idea_05", "title": "夜骑的城市观察", "relation": "same_collection", "durationMs": 154000, "tag": "城市"},
]
RELATED_WRITING = [
    {"id": "idea_06", "title": "播客选题：慢生活", "relation": "similar_theme", "durationMs": 208000, "tag": "播客"},
    {"id": "idea_13", "title": "山野徒步", "relation": "same_collection", "tag": "户外"},
]
RELATED_CITY = [
    {"id": "idea_05", "title": "夜骑的城市观察", "relation": "similar_theme", "durationMs": 154000, "tag": "城市"},
    {"id": "idea_07", "title": "菜市场的人类学", "relation": "same_collection", "durationMs": 187000, "tag": "生活"},
]
RELATED_NIGHT_RIDE = [
    {"id": "idea_04", "title": "城市与人", "relation": "similar_theme", "durationMs": 321000, "tag": "旅行"},
]
RELATED_PODCAST = [
    {"id": "idea_01", "title": "关于旅行的灵感", "relation": "similar_theme", "durationMs": 136000, "tag": "旅行"},
    {"id": "idea_07", "title": "菜市场的人类学", "relation": "same_collection", "durationMs": 187000, "tag": "生活"},
]
RELATED_MARKET = [
    {"id": "idea_04", "title": "城市与人", "relation": "similar_theme", "durationMs": 321000, "tag": "旅行"},
]
RELATED_PHOTO = [
    {"id": "idea_11", "title": "清晨的露水", "relation": "similar_theme", "durationMs": 22000, "tag": "自然"},
    {"id": "idea_04", "title": "城市与人", "relation": "same_collection", "durationMs": 321000, "tag": "旅行"},
]
RELATED_DEW = [
    {"id": "idea_08", "title": "一张照片的配色", "relation": "similar_theme", "durationMs": 45000, "tag": "摄影"},
    {"id": "idea_13", "title": "山野徒步", "relation": "same_collection", "tag": "户外"},
]
RELATED_CAFE_WORK = [
    {"id": "idea_03", "title": "生活片段·咖啡馆", "relation": "similar_theme", "durationMs": 96000, "tag": "创作"},
    {"id": "idea_06", "title": "播客选题：慢生活", "relation": "same_collection", "durationMs": 208000, "tag": "播客"},
]
RELATED_HIKING = [
    {"id": "idea_11", "title": "清晨的露水", "relation": "similar_theme", "durationMs": 22000, "tag": "自然"},
    {"id": "idea_01", "title": "关于旅行的灵感", "relation": "same_collection", "durationMs": 136000, "tag": "旅行"},
]

IDEAS = [
    _idea(1, "关于旅行的灵感", "在陌生城市中通过观察、聆听和日常邂逅取得新的视角。", ["旅行", "生活"], 6, 136000, RELATED_TRAVEL),
    _idea(2, "写作素材：时间与记忆", "时间的流逝感与记忆的选择性保存，可以作为叙事的双线结构。", ["灵感", "日记"], 4, 232000, RELATED_WRITING),
    _idea(3, "生活片段·咖啡馆", "咖啡馆里的声音层次：磨豆、低语、杯碟——城市白噪音的样本。", ["创作", "美食"], 2.5, 96000, RELATED_CAFE),
    _idea(4, "城市与人", "城市废墟中的纹理与光影，人与空间的相互塑造。", ["旅行", "摄影"], 50, 321000, RELATED_CITY),
    _idea(5, "夜骑的城市观察", "晚间骑行时城市灯光的节奏变化。", ["城市", "观察"], 74, 154000, RELATED_NIGHT_RIDE),
    _idea(6, "播客选题：慢生活", "与「慢生活」主张者对谈的系列选题。", ["播客", "创作"], 120, 208000, RELATED_PODCAST),
    _idea(7, "菜市场的人类学", "菜市场作为城市公共空间的样本。", ["生活", "观察"], 192, 187000, RELATED_MARKET),
    _idea(8, "一张照片的配色", "废墟场景的低饱和暖色配色方案。", ["摄影", "灵感"], 216, 45000, RELATED_PHOTO),
    # related 引用落卡（idea_01 的 RELATED_TRAVEL 指向这三张）
    _idea(11, "清晨的露水", "晨间露珠的微观摄影与光影观察。", ["自然", "摄影"], 30, 22000, RELATED_DEW),
    _idea(12, "咖啡馆与工作", "咖啡馆作为第三空间的工位选择与观察笔记。", ["创作", "生活"], 44, 560000, RELATED_CAFE_WORK),
    _idea(13, "山野徒步", "城郊徒步路线的体力分配与风景节奏。", ["户外", "旅行"], 60, 315000, RELATED_HIKING),
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
    {"id": "col_01", "name": "旅行灵感", "subtitle": "关于远方、路上与不同的生活方式", "count": 3, "tone": "sage"},
    {"id": "col_02", "name": "项目构思", "subtitle": "创意想法与产品方向", "count": 5, "tone": "peach"},
    {"id": "col_03", "name": "晨间随想", "subtitle": "清晨的灵感与碎片", "count": 3, "tone": "gold"},
    {"id": "col_04", "name": "阅读笔记", "subtitle": "书籍、文章与知识沉淀", "count": 5, "tone": "blue"},
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
