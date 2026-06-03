"""
从免费图源下载商品图片到 data/images/
策略: Unsplash → Picsum (fallback) → PIL生成占位图 (兜底)
"""

import json
import os
import sys
import time
import io
import hashlib
from pathlib import Path

import requests
from PIL import Image, ImageDraw, ImageFont, ImageColor

# ── 配置 ──────────────────────────────────────────────────
PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent.parent
DATA_FILE = Path(__file__).resolve().parent.parent / "data" / "qdrant" / "products_expanded_100.json"
IMAGES_DIR = PROJECT_ROOT / "data" / "images"

# 品类 → 英文搜索关键词映射
CATEGORY_KEYWORDS = {
    "T恤": "t-shirt clothing fashion",
    "手机": "smartphone mobile phone technology",
    "耳机": "headphones earphones audio",
    "笔记本电脑": "laptop computer notebook",
    "显示器": "monitor display screen",
    "平板电脑": "tablet ipad device",
    "运动鞋": "sneakers running shoes sports",
    "羽绒服": "down jacket winter coat",
    "双肩包": "backpack bag travel",
    "面膜": "face mask skincare beauty",
    "口红": "lipstick makeup cosmetic",
    "眼霜": "eye cream skincare beauty",
    "防晒": "sunscreen skincare protection",
    "香水": "perfume fragrance beauty",
    "洗面奶": "facial cleanser skincare",
    "粉底液": "foundation makeup cosmetic",
    "精华液": "serum essence skincare",
    "面霜": "face cream moisturizer",
    "零食": "snacks food chips",
    "坚果": "nuts food snack",
    "饮料": "beverage drink",
    "瑜伽": "yoga fitness mat sports",
    "跑步机": "treadmill fitness equipment",
    "洗发水": "shampoo hair care",
    "凉鞋": "sandals shoes summer",
    "短靴": "boots shoes ankle",
    "连衣裙": "dress women clothing",
    "手表": "watch wrist accessory",
    "手链": "bracelet jewelry accessory",
}

USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# ── 图片下载策略 ──────────────────────────────────────────

def search_unsplash(query, width=800, height=800):
    """Unsplash Source API (非官方免费接口，无key)"""
    url = f"https://source.unsplash.com/{width}x{height}/?{query}"
    try:
        resp = requests.get(url, headers={"User-Agent": USER_AGENT}, timeout=15, allow_redirects=True)
        if resp.status_code == 200 and len(resp.content) > 1000:
            return resp.content
    except Exception:
        pass
    return None


def search_picsum(product_id, width=800, height=800):
    """Picsum Photos (免费无key，基于seed的一致性图片)"""
    seed = abs(int(hashlib.md5(product_id.encode()).hexdigest(), 16)) % 1000
    url = f"https://picsum.photos/seed/{seed}/{width}/{height}"
    try:
        resp = requests.get(url, headers={"User-Agent": USER_AGENT}, timeout=15, allow_redirects=True)
        if resp.status_code == 200 and len(resp.content) > 1000:
            return resp.content
    except Exception:
        pass
    return None


def search_pexels(query, api_key=None):
    """Pexels API (需要key，免费注册)"""
    if not api_key:
        return None
    url = f"https://api.pexels.com/v1/search?query={query}&per_page=1&size=medium"
    try:
        resp = requests.get(url, headers={"Authorization": api_key}, timeout=10)
        if resp.status_code == 200:
            data = resp.json()
            photos = data.get("photos", [])
            if photos:
                img_url = photos[0]["src"]["medium"]
                img_resp = requests.get(img_url, timeout=15)
                if img_resp.status_code == 200:
                    return img_resp.content
    except Exception:
        pass
    return None


# ── 占位图生成 (最终兜底) ─────────────────────────────────

# 品类 → 主题色
CATEGORY_COLORS = {
    "T恤": "#4A90D9", "手机": "#2C3E50", "耳机": "#E67E22", "笔记本电脑": "#3498DB",
    "显示器": "#1ABC9C", "平板电脑": "#9B59B6", "运动鞋": "#E74C3C", "羽绒服": "#2980B9",
    "双肩包": "#27AE60", "面膜": "#F39C12", "口红": "#E91E63", "眼霜": "#8BC34A",
    "防晒": "#FF9800", "香水": "#9C27B0", "洗面奶": "#00BCD4", "粉底液": "#FF6F00",
    "精华液": "#4CAF50", "面霜": "#03A9F4", "零食": "#FF5722", "坚果": "#795548",
    "饮料": "#2196F3", "瑜伽": "#7CB342", "跑步机": "#607D8B", "洗发水": "#5C6BC0",
    "凉鞋": "#8D6E63", "短靴": "#6D4C41", "连衣裙": "#EC407A", "手表": "#546E7A",
    "手链": "#FFB300",
}


def generate_placeholder(title, category, product_id, size=(800, 800)):
    """使用PIL生成干净的占位商品图"""
    bg_color = CATEGORY_COLORS.get(category, "#888888")
    img = Image.new("RGB", size, bg_color)

    draw = ImageDraw.Draw(img)

    # 尝试加载中文字体
    font_large = None
    font_small = None
    font_paths = [
        "C:/Windows/Fonts/msyh.ttc",
        "C:/Windows/Fonts/simhei.ttf",
        "C:/Windows/Fonts/simsun.ttc",
        "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf",
    ]
    for fp in font_paths:
        if os.path.exists(fp):
            try:
                font_large = ImageFont.truetype(fp, 48)
                font_small = ImageFont.truetype(fp, 28)
                break
            except Exception:
                continue

    if font_large is None:
        font_large = ImageFont.load_default()
        font_small = ImageFont.load_default()

    # 绘制品类图标背景圆
    cx, cy = size[0] // 2, size[1] // 2 - 60
    r = 120
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill="#FFFFFF33", outline="#FFFFFF66", width=3)

    # 品类文字
    cat_bbox = draw.textbbox((0, 0), category[:4], font=font_large)
    cat_w = cat_bbox[2] - cat_bbox[0]
    draw.text((cx - cat_w // 2, cy - 30), category[:4], fill="#FFFFFFCC", font=font_large)

    # 产品ID
    id_text = product_id[:20]
    id_bbox = draw.textbbox((0, 0), id_text, font=font_small)
    id_w = id_bbox[2] - id_bbox[0]
    draw.text((cx - id_w // 2, cy + 100), id_text, fill="#FFFFFF88", font=font_small)

    # 标题（截短）
    title_short = title[:12] + ("..." if len(title) > 12 else "")
    t_bbox = draw.textbbox((0, 0), title_short, font=font_small)
    t_w = t_bbox[2] - t_bbox[0]
    draw.text((cx - t_w // 2, cy + 150), title_short, fill="#FFFFFFAA", font=font_small)

    # 底部装饰条
    bar_h = 6
    draw.rectangle([0, size[1] - bar_h, size[0], size[1]], fill="#FFFFFF44")

    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=85, optimize=True)
    return buf.getvalue()


# ── 主流程 ──────────────────────────────────────────────

def main():
    print("=== 商品图片下载器 ===")
    print(f"数据文件: {DATA_FILE}")
    print(f"图片目录: {IMAGES_DIR}")

    with open(DATA_FILE, "r", encoding="utf-8") as f:
        products = json.load(f)

    # 仅处理需要本地图片的产品 (image_url 以 /images/ 开头)
    local_products = [p for p in products if p.get("image_url", "").startswith("/images/")]
    print(f"\n总产品: {len(products)}, 需要下载图片: {len(local_products)}")

    # 创建目录结构
    categories = set()
    for p in local_products:
        parts = p["image_url"].split("/")
        cat_dir = parts[2]
        categories.add(cat_dir)

    for cat in categories:
        (IMAGES_DIR / cat).mkdir(parents=True, exist_ok=True)
    print(f"品类目录: {len(categories)}")

    # 获取搜索关键词
    def get_query(product):
        cat = product.get("category", "")
        title = product.get("title", "")
        # 品类关键词 + 从标题提取前几个有意义的词
        base = CATEGORY_KEYWORDS.get(cat, cat)
        return f"{base}"

    # 下载
    success_count = 0
    unsplash_count = 0
    picsum_count = 0
    placeholder_count = 0
    fail_count = 0

    for i, p in enumerate(local_products):
        parts = p["image_url"].split("/")
        cat_dir = parts[2]
        filename = parts[3]
        dest_path = IMAGES_DIR / cat_dir / filename
        product_id = p.get("product_id", f"unknown_{i}")
        category = p.get("category", "")
        title = p.get("title", "")

        # 跳过已存在的
        if dest_path.exists():
            success_count += 1
            continue

        query = get_query(p)
        print(f"[{i+1}/{len(local_products)}] {product_id} | {category} | {title[:30]}...", end=" ")

        img_data = None

        # 策略1: Unsplash Source
        img_data = search_unsplash(query)
        if img_data:
            unsplash_count += 1
            print("-> Unsplash")

        # 策略2: Picsum
        if not img_data:
            img_data = search_picsum(product_id)
            if img_data:
                picsum_count += 1
                print("-> Picsum")

        # 策略3: PIL 生成占位图
        if not img_data:
            img_data = generate_placeholder(title, category, product_id)
            placeholder_count += 1
            print("-> Placeholder")

        # 写入
        try:
            dest_path.write_bytes(img_data)
            success_count += 1
        except Exception as e:
            print(f"  FAIL: {e}")
            fail_count += 1
            continue

        # 限速
        time.sleep(0.3)

    print(f"\n=== 完成 ===")
    print(f"成功: {success_count}")
    print(f"  Unsplash: {unsplash_count}")
    print(f"  Picsum: {picsum_count}")
    print(f"  Placeholder: {placeholder_count}")
    print(f"失败: {fail_count}")

    # 验证
    total_files = sum(1 for _ in IMAGES_DIR.rglob("*.jpg"))
    total_size = sum(f.stat().st_size for f in IMAGES_DIR.rglob("*.jpg"))
    print(f"图片总数: {total_files}, 总大小: {total_size / 1024 / 1024:.1f} MB")
    print(f"静态目录: {IMAGES_DIR} → exists={IMAGES_DIR.exists()}")


if __name__ == "__main__":
    main()
