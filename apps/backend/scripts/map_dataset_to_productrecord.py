"""
P0 — 数据源对接 + 映射脚本
将 ecommerce_agent_dataset (100条) 映射为 ProductRecord 格式

用法: python map_dataset_to_productrecord.py
输出: products_expanded_100.json + products_expanded_100.jsonl
      images/ 目录下按品类存放商品图片
"""

import json
import os
import re
import shutil
import sys
from pathlib import Path

# 修复 Windows GBK 终端 Unicode 输出
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

# ── 配置 ──────────────────────────────────────────────────
DATASET_DIR = os.path.expanduser(
    "~/Downloads/ecommerce_agent_dataset_供参考 (1)/ecommerce_agent_dataset"
)
PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent.parent
IMAGES_OUT = PROJECT_ROOT / "data" / "images"
PRODUCTS_JSON = Path(__file__).resolve().parent.parent / "data" / "qdrant" / "products_expanded_100.json"
PRODUCTS_JSONL = Path(__file__).resolve().parent.parent / "data" / "qdrant" / "products_expanded_100.jsonl"

# ── 子类 → 标准品类映射 (对齐 DATA-CONTRACT.md §6) ────────
SUBCATEGORY_MAP = {
    # ── 美妆护肤 → 标准品类 ──
    "精华": "精华液", "眼霜": "精华液", "化妆水": "精华液",
    "面霜": "面霜", "面膜": "面霜", "蜜粉": "面霜", "粉底液": "面霜",
    "防晒": "防晒",
    "口红": "口红", "唇釉": "口红", "眉笔": "口红",
    "洗发水": "洗发水", "洁面": "洗发水", "卸妆": "洗发水",
    # ── 数码电子 → 标准品类 ──
    "智能手机": "手机",
    "笔记本电脑": "电脑", "平板电脑": "电脑",
    "真无线耳机": "耳机", "头戴式降噪耳机": "耳机", "运动蓝牙耳机": "耳机",
    "智能手表": "手表", "智能手环": "手表",
    "机械键盘": "键盘",
    # ── 服饰运动 → 标准品类 ──
    "短袖T恤": "T恤", "长袖T恤": "T恤", "速干T恤": "T恤",
    "长袖衬衫": "衬衫",
    "跑步鞋": "跑鞋", "缓震跑鞋": "跑鞋",
    "篮球鞋": "运动鞋", "徒步鞋": "运动鞋", "运动跑鞋": "运动鞋",
    "运动长裤": "裤装", "运动短裤": "裤装", "压缩裤": "裤装",
    "卫衣": "外套", "连帽卫衣": "外套", "羊绒围巾": "外套",
    "防晒衣": "运动服",
    "瑜伽裤": "瑜伽",
    "冲锋裤": "户外", "冲锋衣": "户外", "户外裤": "户外",
    "背包": "双肩包",
    "帽子": "户外",
    # ── 食品饮料 → 标准品类 ──
    "咖啡": "咖啡",
    "坚果": "坚果", "坚果/零食": "坚果",
    "肉干": "肉干",
    "方便食品": "零食", "巧克力": "零食",
    "碳酸饮料": "饮料", "功能饮料": "饮料", "茶饮": "饮料",
    "牛奶": "饮料", "酸奶": "饮料", "茶叶": "饮料",
    "调味品": "零食",
}


def list_dataset_files():
    """遍历数据集目录，返回 [(json_path, image_path), ...]"""
    pairs = []
    for cat_dir in sorted(Path(DATASET_DIR).iterdir()):
        if not cat_dir.is_dir():
            continue
        data_dir = cat_dir / "data"
        images_dir = cat_dir / "images"
        if not data_dir.exists():
            continue
        for json_file in sorted(data_dir.glob("*.json")):
            stem = json_file.stem  # e.g., p_beauty_001
            img_file = images_dir / f"{stem}_live.jpg"
            pairs.append((json_file, img_file if img_file.exists() else None))
    return pairs


def extract_highlights(marketing_desc, brand, sub_category, attributes):
    """从营销描述提取 <=5 个卖点标签"""
    highlights = []
    desc = marketing_desc

    # 1. 提取数值化规格 (续航/容量/防水等)，格式化为简短标签
    spec_patterns = [
        (r"(\d+)\s*小时.*?[续电航]", lambda m: f"{m.group(1)}小时续航"),
        (r"(\d+)\s*ml", lambda m: f"{m.group(1)}ml"),
        (r"IP\d+[XW]\d*", lambda m: m.group(0)),
        (r"(\d+)\s*颗装", lambda m: f"{m.group(1)}颗装"),
        (r"(\d+)\s*GB", lambda m: f"{m.group(1)}GB"),
        (r"(\d+)\s*TB", lambda m: f"{m.group(1)}TB"),
        (r"(\d+)\s*克|(\d+)\s*g\b", lambda m: f"{m.group(1) or m.group(2)}g"),
    ]
    for pattern, formatter in spec_patterns:
        m = re.search(pattern, desc)
        if m:
            label = formatter(m)
            if label not in highlights:
                highlights.append(label)
        if len(highlights) >= 3:
            break

    # 2. 提取技术/成分关键词
    tech_keywords = [
        ("A19 Pro", "A19 Pro芯片"), ("A19 ", "A19芯片"),
        ("AIRism", "AIRism凉感"), ("二裂酵母", "二裂酵母修护"),
        ("透明质酸", "透明质酸保湿"), ("冻干", "冻干锁鲜技术"),
        ("ANC", "ANC主动降噪"), ("降噪", "主动降噪"),
        ("LDAC", "LDAC高清"), ("空间音频", "空间音频"),
        ("氮化镓", "氮化镓快充"), ("GaN", "氮化镓快充"),
        ("超即溶", "3秒超即溶"), ("抗初老", "抗初老"),
        ("淡纹", "淡纹紧致"), ("保湿", "深层保湿"),
        ("速干", "吸湿速干"), ("防晒", "高倍防晒"),
        ("SPF", "高倍防晒"), ("轻薄", "轻薄便携"),
        ("透气", "透气舒适"), ("缓震", "缓震回弹"),
        ("纯棉", "纯棉面料"), ("天然", "天然成分"),
        ("低糖", "低糖健康"), ("无添加", "无添加"),
        ("益生菌", "活性益生菌"), ("有机", "有机认证"),
        ("Nulu", "Nulu裸感面料"), ("裸感", "裸感面料"),
        ("高腰", "高腰收腹"), ("收腹", "高腰收腹"),
        ("超长续航", "超长续航"), ("快充", "快充技术"),
        ("高刷", "高刷屏幕"), ("全面屏", "全面屏"),
        ("4K", "4K画质"), ("HDR", "HDR显示"),
        ("耐磨", "耐磨耐用"),
        ("防滑", "防滑设计"), ("护眼", "护眼屏"),
        ("美式", "美式风味"), ("炭烧", "炭烧风味"),
        ("拿铁", "拿铁口感"), ("便携", "便携设计"),
        ("高蛋白", "高蛋白"), ("低脂", "低脂健康"),
        ("无糖", "无糖配方"), ("酵素", "酵素发酵"),
        ("氨基酸", "氨基酸"), ("胶原蛋白", "胶原蛋白"),
        ("敏感肌", "敏感肌适用"), ("温和", "温和配方"),
        ("持妆", "持妆持久"), ("哑光", "哑光雾面"),
        ("锁水", "锁水保湿"), ("修护", "修护肌肤"),
        ("抗老", "抗老紧致"), ("提亮", "提亮肤色"),
        ("控油", "控油"), ("舒缓", "舒缓修护"),
        ("隔离", "隔离防护"), ("定妆", "定妆持久"),
        ("遮瑕", "遮瑕"), ("防水", "防水防汗"),
    ]
    # 只在包含 A19 Pro 时才添加, 避免重复添加 A19
    added_a19pro = False
    for kw, label in tech_keywords:
        if kw in desc and label not in highlights:
            if label == "A19 Pro芯片":
                added_a19pro = True
                highlights.append(label)
            elif label == "A19芯片" and added_a19pro:
                continue  # 已经添加了更精确的 A19 Pro
            else:
                highlights.append(label)
        if len(highlights) >= 5:
            break

    # 3. 如果还不够，从第一句提取核心描述
    # 过滤掉明显不是卖点的短语
    _bad_phrases = [
        "不用", "不会", "没有", "不是", "只是", "可惜", "后悔", "浪费",
        "不适合", "太贵", "不划算", "看起来", "摸上去", "感觉", "觉得",
        "包装设计", "宣传", "品控", "能不能", "可以",
    ]
    if len(highlights) < 3:
        first_sent = desc.split("。")[0]
        if brand in first_sent:
            first_sent = first_sent.split(brand, 1)[-1]
        chunks = re.split(r"[，,、]", first_sent)
        for chunk in chunks:
            clean = re.sub(r"[是为即该这款可已，。、；：！？\s]", "", chunk.strip())
            if len(clean) < 4 or len(clean) > 14:
                continue
            if clean in highlights:
                continue
            if any(bad in clean for bad in _bad_phrases):
                continue
            highlights.append(clean)
            if len(highlights) >= 5:
                break

    return highlights[:5]


def _clean_highlight(text):
    """清洗提取的卖点文本"""
    text = re.sub(r"[是为即该这款可已，。、；：！？\s]", "", text)
    text = text.strip()
    if len(text) > 16:
        text = text[:16]
    return text


def extract_scenarios(marketing_desc, category):
    """从营销描述推断使用场景（≤5个）"""
    scenario_keywords = {
        "通勤": ["通勤", "上班", "办公", "出差", "商务"],
        "运动": ["运动", "跑步", "健身", "训练", "户外", "马拉松"],
        "日常": ["日常", "休闲", "居家", "周末"],
        "旅行": ["旅行", "旅游", "出行", "露营"],
        "学生": ["学生", "上学", "学习", "考研", "宿舍"],
        "音乐": ["音乐", "音质", "降噪", "听歌", "HIFI"],
        "拍摄": ["拍照", "视频", "Vlog", "摄影", "录制"],
        "护肤": ["护肤", "夜间", "抗初老", "保湿", "修护"],
        "聚会": ["聚会", "约会", "派对", "逛街"],
        "驾驶": ["开车", "车载", "驾车"],
    }
    scenarios = []
    for label, keywords in scenario_keywords.items():
        for kw in keywords:
            if kw in marketing_desc:
                scenarios.append(label)
                break
    if not scenarios:
        scenarios.append("日常")
    return scenarios[:5]


def compute_rating(reviews):
    """从用户评价计算平均评分和评价数"""
    if not reviews:
        return 3.0, 0
    ratings = [r["rating"] for r in reviews]
    avg = round(sum(ratings) / len(ratings), 1)
    return avg, len(ratings)


def merge_sku_attributes(skus):
    """合并所有 SKU 的属性为统一的 attributes dict（去重）"""
    attrs = {}
    for sku in skus:
        for key, val in sku.get("properties", {}).items():
            if key not in attrs:
                attrs[key] = val
            else:
                existing_vals = set(attrs[key].split("/"))
                existing_vals.add(val)
                # 如果能合并为范围就保持简洁，否则列出唯一值
                unique = sorted(existing_vals)
                if len(unique) <= 3:
                    attrs[key] = "/".join(unique)
                else:
                    attrs[key] = f"{unique[0]}~{unique[-1]}"
    return attrs


def map_one(json_path, image_path):
    """将单条 dataset JSON 映射为 ProductRecord"""
    with open(json_path, "r", encoding="utf-8") as f:
        src = json.load(f)

    knowledge = src.get("rag_knowledge", {})
    reviews = knowledge.get("user_reviews", [])
    rating, rating_count = compute_rating(reviews)

    sub_cat = src.get("sub_category", "")
    category = SUBCATEGORY_MAP.get(sub_cat, sub_cat)

    attributes = merge_sku_attributes(src.get("skus", []))

    marketing = knowledge.get("marketing_description", "")
    highlights = extract_highlights(marketing, src.get("brand", ""), sub_cat, attributes)
    scenarios = extract_scenarios(marketing, category)

    # 图片路径
    image_url = None
    image_urls = []
    if image_path and image_path.exists():
        cat_name = category
        dest_dir = IMAGES_OUT / cat_name
        dest_dir.mkdir(parents=True, exist_ok=True)
        dest_file = dest_dir / image_path.name
        shutil.copy2(image_path, dest_file)
        # 使用相对路径作为 image_url（后续可替换为实际 URL）
        rel_path = f"/images/{cat_name}/{image_path.name}"
        image_url = rel_path
        image_urls = [rel_path]

    # 拼接评价内容用于 embedding 富化
    review_texts = " ".join(
        f"用户评价{r['rating']}星: {r['content']}" for r in reviews[:5]
    )

    return {
        "product_id": src["product_id"],
        "title": src["title"],
        "brand": src.get("brand", ""),
        "category": category,
        "price": float(src.get("base_price", 0)),
        "rating": rating,
        "rating_count": rating_count,
        "highlights": highlights,
        "attributes": attributes,
        "scenarios": scenarios,
        "image_url": image_url,
        "image_urls": image_urls,
        "description": marketing,
        "review_summary": review_texts,
        "source": "",
    }


def main():
    pairs = list_dataset_files()
    print(f"找到 {len(pairs)} 个数据文件")

    # 验证：先处理 2-3 条样本
    sample_indices = [0, 25, 50, 75]
    print("\n=== 样本验证 (前3条) ===")
    for idx in sample_indices[:3]:
        if idx < len(pairs):
            json_path, img_path = pairs[idx]
            rec = map_one(json_path, img_path)
            print(f"\n--- {rec['product_id']} ---")
            print(f"  title: {rec['title'][:50]}...")
            print(f"  category: {rec['category']}")
            print(f"  price: {rec['price']}")
            print(f"  rating: {rec['rating']} ({rec['rating_count']}条评价)")
            print(f"  highlights: {rec['highlights']}")
            print(f"  attributes: {rec['attributes']}")
            print(f"  scenarios: {rec['scenarios']}")
            print(f"  image_url: {rec['image_url']}")

    # 全量处理
    print(f"\n=== 全量映射 {len(pairs)} 条 ===")
    products = []
    for json_path, img_path in pairs:
        rec = map_one(json_path, img_path)
        products.append(rec)

    # 去重检查 vs 现有 seed_products.json
    seed_path = Path(__file__).resolve().parent.parent / "data" / "qdrant" / "seed_products.json"
    existing_ids = set()
    if seed_path.exists():
        with open(seed_path, "r", encoding="utf-8") as f:
            existing = json.load(f)
        existing_ids = {p["product_id"] for p in existing}
        overlap = {p["product_id"] for p in products} & existing_ids
        if overlap:
            print(f"⚠️ 发现 {len(overlap)} 条与现有数据 product_id 重叠: {overlap}")
        else:
            print(f"✅ 无 product_id 重叠 (现有 {len(existing_ids)} 条, 新增 {len(products)} 条)")

    # 输出 JSON
    PRODUCTS_JSON.parent.mkdir(parents=True, exist_ok=True)
    with open(PRODUCTS_JSON, "w", encoding="utf-8") as f:
        json.dump(products, f, ensure_ascii=False, indent=2)
    print(f"✅ JSON 输出: {PRODUCTS_JSON} ({len(products)} 条)")

    # 输出 JSONL (每行一条)
    with open(PRODUCTS_JSONL, "w", encoding="utf-8") as f:
        for p in products:
            f.write(json.dumps(p, ensure_ascii=False) + "\n")
    print(f"✅ JSONL 输出: {PRODUCTS_JSONL} ({len(products)} 条)")

    # 品类分布统计
    cat_counts = {}
    for p in products:
        cat = p["category"]
        cat_counts[cat] = cat_counts.get(cat, 0) + 1
    print(f"\n品类分布: {len(cat_counts)} 个品类")
    for cat, cnt in sorted(cat_counts.items(), key=lambda x: -x[1]):
        print(f"  {cat}: {cnt}")

    # 图片统计
    with_img = sum(1 for p in products if p["image_url"])
    print(f"\n图片: {with_img}/{len(products)} 件商品已复制图片到 {IMAGES_OUT}")

    print("\n✅ P0 完成！下一步: python ingest_to_qdrant.py --input products_expanded_100.jsonl")


if __name__ == "__main__":
    main()
